package com.sptech.school.jaf.controller;

import com.sptech.school.jaf.dto.OcrResultDTO;
import com.sptech.school.jaf.service.OcrService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/jaf/ocr")
@CrossOrigin(origins = "*") // necessário para a página HTML acessar o backend local
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping("/processar")
public ResponseEntity<?> processarNotaFiscal(
        @RequestParam("arquivo") MultipartFile arquivo) {

    try {

        OcrResultDTO resultado =
                ocrService.processarNotaFiscal(arquivo);

        if (resultado == null) {

            return ResponseEntity.ok(
                    "OCR não encontrou dados na imagem"
            );

        }

        return ResponseEntity.ok(resultado);

    } catch (Exception e) {

        return ResponseEntity
                .status(500)
                .body(
                    "Erro ao processar OCR: "
                    + e.getMessage()
                );
    }
}
}
