
package com.jaf.application.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.model.Relatorio;
import com.jaf.application.service.RelatorioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/relatorios")
@SecurityRequirement(name = "Bearer")
public class RelatorioController {
    private final RelatorioService relatorioService;

    public RelatorioController(RelatorioService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @PostMapping
    public ResponseEntity<Relatorio> criar(@Valid @RequestBody RelatorioDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(relatorioService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<Relatorio>> listar() {
        return ResponseEntity.ok(relatorioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Relatorio> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(relatorioService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Relatorio> atualizar(@PathVariable Long id, @Valid @RequestBody RelatorioDto dto) {
        return ResponseEntity.ok(relatorioService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        relatorioService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
