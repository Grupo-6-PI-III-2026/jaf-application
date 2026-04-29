package com.jaf.application.controller;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.service.AlocacaoObraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/alocacoes")
@SecurityRequirement(name = "Bearer")
public class AlocacaoObraController {
    private final AlocacaoObraService alocacaoObraService;

    public AlocacaoObraController(AlocacaoObraService alocacaoObraService) {
        this.alocacaoObraService = alocacaoObraService;
    }

    @PostMapping
    public ResponseEntity<AlocacaoObra> criar(@Valid @RequestBody AlocacaoObraDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alocacaoObraService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<AlocacaoObra>> listar() {
        return ResponseEntity.ok(alocacaoObraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlocacaoObra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alocacaoObraService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlocacaoObra> atualizar(@PathVariable Long id, @Valid @RequestBody AlocacaoObraDto dto) {
        return ResponseEntity.ok(alocacaoObraService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        alocacaoObraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
