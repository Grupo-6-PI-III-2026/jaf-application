package com.jaf.application.controller;

import com.jaf.application.dto.FuncionarioDetalhesDto;
import com.jaf.application.dto.OcrResponseDto;
import com.jaf.application.enums.StatusOcr;
import com.jaf.application.service.OcrService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ocr")
@SecurityRequirement(name = "Bearer")
public class OcrController {
    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    @PostMapping(value = "/nota-fiscal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<OcrResponseDto> processarNota(@RequestParam("arquivo") MultipartFile arquivo,
                                                        Authentication authentication) {
        OcrResponseDto resposta = ocrService.processarNota(arquivo, idDe(authentication), authentication.getName());
        HttpStatus status = resposta.getStatus() == StatusOcr.PROCESSANDO
                ? HttpStatus.ACCEPTED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(resposta);
    }

    @GetMapping("/nota-fiscal/{jobId}")
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<OcrResponseDto> consultarProcessamento(@PathVariable String jobId,
                                                                  Authentication authentication) {
        return ResponseEntity.ok(ocrService.consultar(jobId, idDe(authentication), authentication.getName()));
    }

    private Long idDe(Authentication authentication) {
        return authentication.getPrincipal() instanceof FuncionarioDetalhesDto detalhes ? detalhes.getId() : null;
    }
}
