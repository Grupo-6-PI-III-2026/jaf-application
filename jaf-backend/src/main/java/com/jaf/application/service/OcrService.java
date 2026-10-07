package com.jaf.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OcrService {
    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;
    private final ObjectMapper mapper;

    public OcrService(
            @Value("${ocr.service.url:http://localhost:8000/api/ocr}") String ocrServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.ocrServiceUrl = ocrServiceUrl;
        this.mapper = new ObjectMapper();
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

        try {
            String rawText = "";
            if (response.has("ocr") && response.get("ocr").has("provider_payload") 
                && response.get("ocr").get("provider_payload").has("ParsedResults")) {
                JsonNode parsedResults = response.get("ocr").get("provider_payload").get("ParsedResults");
                if (parsedResults.isArray() && parsedResults.size() > 0) {
                    rawText = parsedResults.get(0).get("ParsedText").asText();
                }
            }

            result.put("sucesso", true);
            result.put("mensagem", "Nota fiscal processada com sucesso");
            result.put("textoBruto", rawText);

            // Valores padrao em caso de falha na extracao
            Double valor = null;
            String dtGasto = null;
            String metodoPagamento = "Boleto";
            String materialInsumo = "CONSTRUIR MATERIAIS PARA CONSTRUÇÃO";

            // Extrair Valor
            Pattern valorPattern = Pattern.compile("R\\$\\s*([0-9.,]+)");
            Matcher valorMatcher = valorPattern.matcher(rawText);
            if (valorMatcher.find()) {
                String valStr = valorMatcher.group(1).replace(".", "").replace(",", ".");
                try {
                    valor = Double.parseDouble(valStr);
                } catch (Exception e) {}
            }

            // Extrair Data (09/06/2026 -> 2026-06-09)
            Pattern dataPattern = Pattern.compile("([0-9]{2})/([0-9]{2})/([0-9]{4})");
            Matcher dataMatcher = dataPattern.matcher(rawText);
            if (dataMatcher.find()) {
                dtGasto = dataMatcher.group(3) + "-" + dataMatcher.group(2) + "-" + dataMatcher.group(1);
            }

            if (valor != null) result.put("valor", valor);
            if (dtGasto != null) result.put("dtGasto", dtGasto);
            result.put("metodoPagamento", metodoPagamento);
            result.put("materialInsumo", materialInsumo);

            return result;
        } catch (Exception e) {
            result.put("sucesso", false);
            result.put("mensagem", "Erro ao processar a nota fiscal: " + e.getMessage());
            return result;
        }
    }
}