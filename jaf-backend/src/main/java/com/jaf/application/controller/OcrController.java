package com.jaf.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.jaf.application.service.OcrService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/ocr")
@SecurityRequirement(name = "Bearer")
public class OcrController {
    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRIAR_OBRA')")
    public ResponseEntity<JsonNode> processar(@RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(ocrService.process(file));
    }

    @PostMapping(value = "/nota-fiscal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<JsonNode> processarNota(@RequestParam("arquivo") MultipartFile arquivo) 
            throws IOException {
        return ResponseEntity.ok(ocrService.process(arquivo));
    }
}