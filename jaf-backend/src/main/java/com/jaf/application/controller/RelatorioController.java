package com.jaf.application.controller;

import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.dto.RelatorioResponse;
import com.jaf.application.model.Relatorio;
import com.jaf.application.service.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/relatorios")
@SecurityRequirement(name = "Bearer")
@Transactional
public class RelatorioController {
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GERAR_RELATORIO')")
    public ResponseEntity<RelatorioResponse> criar(@Valid @RequestBody RelatorioDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RelatorioResponse.de(relatorioService.criar(dto)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_RELATORIO')")
    public ResponseEntity<List<RelatorioResponse>> listar() {
        return ResponseEntity.ok(relatorioService.listar().stream().map(RelatorioResponse::de).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_RELATORIO')")
    public ResponseEntity<RelatorioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(RelatorioResponse.de(relatorioService.buscarPorId(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RelatorioResponse> atualizar(@PathVariable Long id, @Valid @RequestBody RelatorioDto dto) {
        return ResponseEntity.ok(RelatorioResponse.de(relatorioService.atualizar(id, dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        relatorioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
