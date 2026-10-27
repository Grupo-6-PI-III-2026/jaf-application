package com.jaf.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.http.ResponseEntity;
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
            "image/jpeg", "image/png", "image/gif", "image/bmp",
            "image/webp", "image/tiff", "application/pdf");

    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;
    private final ObjectMapper mapper;
    private final RabbitTemplate rabbitTemplate;
    private final OcrProcessamentoRepository processamentoRepository;

    public OcrService(
            @Value("${ocr.service.url:http://localhost:8000/api/ocr}") String ocrServiceUrl,
            RabbitTemplate rabbitTemplate,
            OcrProcessamentoRepository processamentoRepository) {
        this.restTemplate = new RestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
        this.mapper = new ObjectMapper();
        this.rabbitTemplate = rabbitTemplate;
        this.processamentoRepository = processamentoRepository;
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
        fileHeaders.setContentType(MediaType.parseMediaType(
                file.getContentType() != null
                        ? file.getContentType()
                        : MediaType.APPLICATION_OCTET_STREAM_VALUE));

        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(resource, fileHeaders);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", filePart);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        try {
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    ocrServiceUrl,
                    new HttpEntity<>(body, requestHeaders),
                    JsonNode.class);
            return response.getBody();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Falha ao comunicar com o servico de OCR.", exception);
        }
    }

    public JsonNode processarNotaFiscal(MultipartFile file) throws IOException {
        JsonNode response = process(file);
        ObjectNode result = mapper.createObjectNode();

        String rawText = "";
        JsonNode parsedResults = response == null
                ? null
                : response.path("ocr").path("provider_payload").path("ParsedResults");
        if (parsedResults.isArray() && !parsedResults.isEmpty()) {
            rawText = parsedResults.get(0).path("ParsedText").asText("");
        }

        result.put("sucesso", true);
        result.put("mensagem", "Nota fiscal processada com sucesso");
        result.put("textoBruto", rawText);

        Matcher valorMatcher = Pattern.compile("R\\$\\s*([0-9.,]+)").matcher(rawText);
        if (valorMatcher.find()) {
            result.put("valor", converterValor(valorMatcher.group(1)).doubleValue());
        }

        Matcher dataMatcher = Pattern.compile("([0-9]{2})/([0-9]{2})/([0-9]{4})").matcher(rawText);
        if (dataMatcher.find()) {
            result.put("dtGasto", dataMatcher.group(3) + "-" + dataMatcher.group(2) + "-" + dataMatcher.group(1));
        }
        result.put("metodoPagamento", "Boleto");
        result.put("materialInsumo", "CONSTRUIR MATERIAIS PARA CONSTRUÇÃO");
        return result;
    }

    public OcrResponseDto processarNota(MultipartFile arquivo, String solicitante) {
        String erroValidacao = validarArquivo(arquivo);
        if (erroValidacao != null) {
            return respostaErro(erroValidacao);
        }

        OcrProcessamento processamento = processamentoRepository.save(
                new OcrProcessamento(UUID.randomUUID().toString(), solicitante));
        try {
            OcrSolicitacaoMensagem mensagem = new OcrSolicitacaoMensagem(
                    processamento.getId(),
                    arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "nota-fiscal.jpg",
                    Base64.getEncoder().encodeToString(arquivo.getBytes()));
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

    private OcrResponseDto respostaErro(String mensagem) {
        OcrResponseDto resposta = new OcrResponseDto();
        resposta.setStatus(StatusOcr.ERRO);
        resposta.setSucesso(false);
        resposta.setMensagem(mensagem);
        return resposta;
    }

    private String validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            return "Selecione uma imagem ou PDF da nota fiscal.";
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            return "A nota fiscal deve ter no máximo 10 MB.";
        }
        if (arquivo.getContentType() == null || !TIPOS_ACEITOS.contains(arquivo.getContentType())) {
            return "Formato inválido. Envie JPG, PNG, WEBP, TIFF ou PDF.";
        }
        return null;
    }

    private void extrairCampos(String texto, OcrResponseDto resposta) {
        resposta.setValor(extrairValor(texto));
        resposta.setDtGasto(extrairData(texto));
        resposta.setMetodoPagamento(extrairMetodoPagamento(texto));
        resposta.setMaterialInsumo("CONSTRUIR MATERIAIS PARA CONSTRUÇÃO");
    }

    private BigDecimal extrairValor(String texto) {
        Matcher matcher = Pattern.compile("R\\$\\s*([0-9.,]+)").matcher(texto);
        return matcher.find() ? converterValor(matcher.group(1)) : null;
    }

    private BigDecimal converterValor(String valor) {
        return new BigDecimal(valor.replace(".", "").replace(",", "."));
    }

    private LocalDate extrairData(String texto) {
        Matcher matcher = Pattern.compile("(\\d{2}/\\d{2}/\\d{4})").matcher(texto);
        if (!matcher.find()) {
            return null;
        }
        try {
            return LocalDate.parse(matcher.group(1), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String extrairMetodoPagamento(String texto) {
        Matcher matcher = Pattern.compile("(?i)(PIX|CART[AÃ]O|BOLETO|DINHEIRO)").matcher(texto);
        return matcher.find() ? matcher.group(1).toUpperCase() : null;
    }
}
