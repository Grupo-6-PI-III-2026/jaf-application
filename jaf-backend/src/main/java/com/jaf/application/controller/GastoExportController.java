package com.jaf.application.controller;

import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.service.ExportacaoGastosService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/gastos")
@SecurityRequirement(name = "Bearer")
public class GastoExportController {
    private final ExportacaoGastosService service;
    public GastoExportController(ExportacaoGastosService service) { this.service = service; }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('GERAR_RELATORIO')")
    public ResponseEntity<byte[]> exportar(@RequestParam Long obraId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String etapa,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String metodoPagamento,
            @RequestParam(required = false) Long funcionarioId,
            @RequestParam(required = false) StatusReembolso statusReembolso,
            @RequestParam(defaultValue = "csv") String formato) {
        var exportacao = service.exportar(obraId, from, to, etapa, categoria, metodoPagamento,
                funcionarioId, statusReembolso, formato);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportacao.contentType()))
                .contentLength(exportacao.conteudo().length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(exportacao.nomeArquivo()).build().toString())
                .body(exportacao.conteudo());
    }
}
