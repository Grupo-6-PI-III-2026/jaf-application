package com.jaf.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaf.application.config.RabbitMQConfig;
import com.jaf.application.dto.OcrResponseDto;
import com.jaf.application.dto.OcrResultadoMensagem;
import com.jaf.application.dto.OcrSolicitacaoMensagem;
import com.jaf.application.enums.StatusOcr;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.OcrProcessamento;
import com.jaf.application.repository.OcrProcessamentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);
    private static final long TAMANHO_MAXIMO = 10L * 1024L * 1024L;
    private static final List<String> TIPOS_ACEITOS = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/bmp",
            "image/webp",
            "image/tiff",
            "application/pdf"
    );

    private final RabbitTemplate rabbitTemplate;
    private final OcrProcessamentoRepository processamentoRepository;
    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;

    public OcrService(RabbitTemplate rabbitTemplate,
                      OcrProcessamentoRepository processamentoRepository,
                      @Value("${ocr.service.url:http://localhost:8000/api/ocr}") String ocrServiceUrl) {
        this.rabbitTemplate = rabbitTemplate;
        this.processamentoRepository = processamentoRepository;
        this.restTemplate = new RestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
    }

    public JsonNode process(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo invalido.");
        }

        ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(
                MediaType.parseMediaType(file.getContentType() != null
                        ? file.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new HttpEntity<>(resource, fileHeaders));

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            return restTemplate.postForEntity(ocrServiceUrl, new HttpEntity<>(body, requestHeaders), JsonNode.class)
                    .getBody();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Falha ao comunicar com o servico de OCR.", exception);
        }
    }

    public OcrResponseDto processarNota(MultipartFile arquivo, String solicitante) {
        String erroValidacao = validarArquivo(arquivo);
        if (erroValidacao != null) {
            OcrResponseDto resposta = new OcrResponseDto();
            resposta.setStatus(StatusOcr.ERRO);
            resposta.setSucesso(false);
            resposta.setMensagem(erroValidacao);
            return resposta;
        }

        // Salvo antes de publicar: o resultado pode voltar antes deste método terminar.
        OcrProcessamento processamento = processamentoRepository.save(
                new OcrProcessamento(UUID.randomUUID().toString(), solicitante));

        try {
            OcrSolicitacaoMensagem mensagem = new OcrSolicitacaoMensagem(
                    processamento.getId(),
                    arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "nota-fiscal.jpg",
                    Base64.getEncoder().encodeToString(arquivo.getBytes())
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.RK_SOLICITAR, mensagem);
        } catch (IOException | AmqpException exception) {
            LOGGER.error("Falha ao enviar nota fiscal {} para o OCR", processamento.getId(), exception);
            processamento.marcarErro("Serviço de leitura de nota fiscal indisponível. Tente novamente em instantes.");
            processamentoRepository.save(processamento);
        }

        return paraResposta(processamento);
    }

    public OcrResponseDto consultar(String jobId, String solicitante) {
        return processamentoRepository.findById(jobId)
                .filter(processamento -> processamento.getSolicitante().equals(solicitante))
                .map(this::paraResposta)
                .orElseThrow(() -> new NotFoundException("Processamento de nota fiscal não encontrado."));
    }

    @RabbitListener(queues = RabbitMQConfig.FILA_RESULTADOS)
    public void receberResultado(OcrResultadoMensagem resultado) {
        if (resultado.jobId() == null) {
            LOGGER.warn("Resultado de OCR descartado: mensagem sem jobId");
            return;
        }

        OcrProcessamento processamento = processamentoRepository.findById(resultado.jobId()).orElse(null);
        if (processamento == null) {
            LOGGER.warn("Resultado de OCR descartado: job {} não encontrado", resultado.jobId());
            return;
        }

        if (resultado.status() != StatusOcr.CONCLUIDO) {
            processamento.marcarErro("Erro ao processar a nota fiscal: " + resultado.erro());
        } else if (resultado.textoBruto() == null || resultado.textoBruto().isBlank()) {
            processamento.marcarErro("Não foi possível extrair texto da nota fiscal.");
        } else {
            processamento.marcarConcluido(resultado.textoBruto());
        }
        processamentoRepository.save(processamento);
    }

    private OcrResponseDto paraResposta(OcrProcessamento processamento) {
        OcrResponseDto resposta = new OcrResponseDto();
        resposta.setJobId(processamento.getId());
        resposta.setStatus(processamento.getStatus());

        switch (processamento.getStatus()) {
            case PROCESSANDO -> {
                resposta.setSucesso(true);
                resposta.setMensagem("Nota fiscal recebida. Processando leitura...");
            }
            case CONCLUIDO -> {
                resposta.setSucesso(true);
                resposta.setTextoBruto(processamento.getTextoBruto());
                extrairCampos(processamento.getTextoBruto(), resposta);
                resposta.setMensagem("Nota fiscal processada. Revise os campos antes de salvar.");
            }
            case ERRO -> {
                resposta.setSucesso(false);
                resposta.setMensagem(processamento.getMensagemErro());
            }
        }
        return resposta;
    }

    private String validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return "Selecione uma imagem ou PDF da nota fiscal.";
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            return "A nota fiscal deve ter no máximo 10 MB.";
        }
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_ACEITOS.contains(contentType)) {
            return "Formato inválido. Envie JPG, PNG, WEBP, TIFF ou PDF.";
        }
        return null;
    }

    private void extrairCampos(String texto, OcrResponseDto resposta) {
        resposta.setValor(extrairValor(texto));
        resposta.setDtGasto(extrairData(texto));
        resposta.setMetodoPagamento(extrairMetodoPagamento(texto));
        resposta.setMaterialInsumo(extrairMaterialInsumo(texto));
        resposta.setDescricaoAdicional(extrairDescricaoAdicional(texto));
    }

    private BigDecimal extrairValor(String texto) {
        List<Pattern> padroes = List.of(
                Pattern.compile("(?i)(?:total\\s*a\\s*pagar|total\\s*geral)[\\s:R$]*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})"),
                Pattern.compile("(?i)(?:vl\\.?\\s*total|valor\\s*total|total)[\\s:R$]*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})"),
                Pattern.compile("R\\$\\s*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})")
        );

        for (Pattern padrao : padroes) {
            Matcher matcher = padrao.matcher(texto);
            if (matcher.find()) {
                String valor = matcher.group(1).replace(".", "").replace(",", ".");
                try {
                    return new BigDecimal(valor);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private LocalDate extrairData(String texto) {
        List<String[]> tentativas = List.of(
                new String[]{"(?i)(?:data|emissao|emissão|dt\\.?\\s*emis)[\\s:./]*([\\d]{2}/[\\d]{2}/[\\d]{4})", "dd/MM/yyyy"},
                new String[]{"([\\d]{2}/[\\d]{2}/[\\d]{4})", "dd/MM/yyyy"},
                new String[]{"([\\d]{2}-[\\d]{2}-[\\d]{4})", "dd-MM-yyyy"},
                new String[]{"([\\d]{4}-[\\d]{2}-[\\d]{2})", "yyyy-MM-dd"}
        );

        for (String[] tentativa : tentativas) {
            Matcher matcher = Pattern.compile(tentativa[0]).matcher(texto);
            if (matcher.find()) {
                try {
                    return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern(tentativa[1]));
                } catch (DateTimeParseException ignored) {
                }
            }
        }
        return null;
    }

    private String extrairMetodoPagamento(String texto) {
        String normalizado = normalizar(texto);
        if (normalizado.contains("pix")) return "PIX";
        if (normalizado.contains("debito")) return "CARTAO_DEBITO";
        if (normalizado.contains("cartao") || normalizado.contains("credito") || normalizado.contains("visa")
                || normalizado.contains("mastercard") || normalizado.contains("elo") || normalizado.contains("hipercard")) {
            return "CARTAO_CREDITO";
        }
        if (normalizado.contains("dinheiro") || normalizado.contains("especie")) return "DINHEIRO";
        return null;
    }

    private String extrairMaterialInsumo(String texto) {
        List<Pattern> padroes = List.of(
                Pattern.compile("(?i)(?:item|produto|descricao|descrição|material|insumo)[:\\s]+(.{5,80})"),
                Pattern.compile("(?i)(?:servico|serviço)[:\\s]+(.{5,80})")
        );

        for (Pattern padrao : padroes) {
            Matcher matcher = padrao.matcher(texto);
            if (matcher.find()) {
                String candidato = matcher.group(1).trim().split("\\r?\\n")[0].trim();
                if (candidato.length() >= 5) {
                    return limitar(candidato, 255);
                }
            }
        }

        Matcher matcherCupom = Pattern.compile("(?m)^([A-ZÀ-Ú][A-ZÀ-Ú0-9 ]{4,50})\\s+\\d+\\s+(?:UN|KG|MT|PC|CX|SC|LT|GL)")
                .matcher(texto.toUpperCase());
        if (matcherCupom.find()) {
            return limitar(matcherCupom.group(1).trim(), 255);
        }

        return null;
    }

    private String extrairDescricaoAdicional(String texto) {
        StringBuilder info = new StringBuilder();
        for (String linha : texto.split("\\r?\\n")) {
            String limpa = linha.trim();
            if (limpa.length() > 5 && !limpa.matches("[\\d\\s.,/*:R$()-]+")
                    && !limpa.toUpperCase().matches(".*(CNPJ|CPF|IE|CEP|TEL|FAX|HTTP|WWW).*")) {
                info.append("Estabelecimento: ").append(limpa).append("\n");
                break;
            }
        }

        Matcher cnpj = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}").matcher(texto);
        if (cnpj.find()) {
            info.append("CNPJ: ").append(cnpj.group()).append("\n");
        }

        Matcher nf = Pattern.compile("(?i)(?:nf-?e?|nota\\s*fiscal)[\\s:nº#]*([\\d]{1,9})").matcher(texto);
        if (nf.find()) {
            info.append("NF: ").append(nf.group(1)).append("\n");
        }

        String resultado = info.toString().trim();
        return resultado.isEmpty() ? null : limitar(resultado, 500);
    }

    private String normalizar(String texto) {
        return texto.toLowerCase()
                .replace("ã", "a")
                .replace("á", "a")
                .replace("â", "a")
                .replace("é", "e")
                .replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ô", "o")
                .replace("ú", "u")
                .replace("ç", "c");
    }

    private String limitar(String valor, int maximo) {
        return valor.length() > maximo ? valor.substring(0, maximo - 3) + "..." : valor;
    }
}