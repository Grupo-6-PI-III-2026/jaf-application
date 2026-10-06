package com.jaf.application.service;

import com.jaf.application.dto.OcrResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {
    private static final String OCR_SPACE_URL = "https://api.ocr.space/parse/image";
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

    @Value("${ocr.space.api-key:}")
    private String ocrApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public OcrResponseDto processarNota(MultipartFile arquivo) {
        OcrResponseDto resposta = new OcrResponseDto();

        String erroValidacao = validarArquivo(arquivo);
        if (erroValidacao != null) {
            resposta.setSucesso(false);
            resposta.setMensagem(erroValidacao);
            return resposta;
        }

        try {
            String textoBruto = chamarOcrSpace(arquivo);
            if (textoBruto == null || textoBruto.isBlank()) {
                resposta.setSucesso(false);
                resposta.setMensagem("Não foi possível extrair texto da nota fiscal.");
                return resposta;
            }

            resposta.setTextoBruto(textoBruto);
            extrairCampos(textoBruto, resposta);
            resposta.setSucesso(true);
            resposta.setMensagem("Nota fiscal processada. Revise os campos antes de salvar.");
            return resposta;
        } catch (Exception exception) {
            resposta.setSucesso(false);
            resposta.setMensagem("Erro ao processar a nota fiscal: " + exception.getMessage());
            return resposta;
        }
    }

    private String validarArquivo(MultipartFile arquivo) {
        if (ocrApiKey == null || ocrApiKey.isBlank()) {
            return "OCR_SPACE_API_KEY não configurada no backend.";
        }
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

    @SuppressWarnings("unchecked")
    private String chamarOcrSpace(MultipartFile arquivo) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("apikey", ocrApiKey.trim());

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(arquivo.getBytes()) {
            @Override
            public String getFilename() {
                return arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "nota-fiscal.jpg";
            }
        });
        body.add("language", "por");
        body.add("isOverlayRequired", "false");
        body.add("OCREngine", "2");
        body.add("isTable", "true");
        body.add("scale", "true");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(OCR_SPACE_URL, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("OCR.space retornou status " + response.getStatusCode());
        }

        Map<String, Object> payload = response.getBody();
        if (Boolean.TRUE.equals(payload.get("IsErroredOnProcessing"))) {
            throw new RuntimeException(String.valueOf(payload.getOrDefault("ErrorMessage", "erro desconhecido")));
        }

        Object parsedResultsObject = payload.get("ParsedResults");
        if (!(parsedResultsObject instanceof List<?> parsedResults) || parsedResults.isEmpty()) {
            return "";
        }

        StringBuilder textoTotal = new StringBuilder();
        for (Object item : parsedResults) {
            if (item instanceof Map<?, ?> resultado) {
                Object parsedText = resultado.get("ParsedText");
                if (parsedText instanceof String texto && !texto.isBlank()) {
                    textoTotal.append(texto.trim()).append("\n");
                }
            }
        }
        return textoTotal.toString().trim();
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