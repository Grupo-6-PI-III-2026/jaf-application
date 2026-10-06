package com.jaf.application.controller;

import com.jaf.application.dto.OcrResponseDto;
import com.jaf.application.service.OcrService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr")
public class OcrController {
    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping(value = "/nota-fiscal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<OcrResponseDto> processarNota(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(ocrService.processarNota(arquivo));
    }
}