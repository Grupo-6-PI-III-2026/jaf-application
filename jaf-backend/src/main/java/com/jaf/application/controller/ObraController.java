
package com.jaf.application.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.model.Obra;
import com.jaf.application.service.ObraService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/obras")
@SecurityRequirement(name = "Bearer")
public class ObraController {
    private final ObraService obraService;

    public ObraController(ObraService obraService) {
        this.obraService = obraService;
    }

    @PostMapping
    public ResponseEntity<Obra> criar(@Valid @RequestBody ObraDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(obraService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<Obra>> listar() {
        return ResponseEntity.ok(obraService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Obra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(obraService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Obra> atualizar(@PathVariable Long id, @Valid @RequestBody ObraDto dto) {
        return ResponseEntity.ok(obraService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        obraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
