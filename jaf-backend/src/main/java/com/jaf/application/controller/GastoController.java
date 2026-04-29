package com.jaf.application.controller;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.model.Gasto;
import com.jaf.application.service.GastoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.List;

@RestController
@RequestMapping("/gastos")
@SecurityRequirement(name = "Bearer")
public class GastoController {
    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    public ResponseEntity<Gasto> criar(@Valid @RequestBody GastoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gastoService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<Gasto>> listar() {
        return ResponseEntity.ok(gastoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Gasto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Gasto> atualizar(@PathVariable Long id, @Valid @RequestBody GastoDto dto) {
        return ResponseEntity.ok(gastoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        gastoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
