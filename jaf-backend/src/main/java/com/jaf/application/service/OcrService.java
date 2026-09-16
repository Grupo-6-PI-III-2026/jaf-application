package com.jaf.application.service;

import com.fasterxml.jackson.databind.JsonNode;
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

@Service
public class OcrService {
    private final RestTemplate restTemplate;
    private final String ocrServiceUrl;

    public OcrService(
            @Value("${ocr.service.url:http://localhost:8000/api/ocr}") String ocrServiceUrl) {
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
}