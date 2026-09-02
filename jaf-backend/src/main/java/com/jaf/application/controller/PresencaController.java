package com.jaf.application.controller;

import com.jaf.application.dto.PresencaDto;
import com.jaf.application.dto.PresencaListarDto;
import com.jaf.application.dto.PresencaResponseDto;
import com.jaf.application.service.PresencaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/presencas")
@SecurityRequirement(name = "Bearer")
public class PresencaController {
    private final PresencaService presencaService;

    public PresencaController(PresencaService presencaService) {
        this.presencaService = presencaService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REGISTRAR_PRESENCA')")
    public ResponseEntity<PresencaResponseDto> criar(@Valid @RequestBody PresencaDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(presencaService.criar(dto));
    }

    @GetMapping("/obra/{obraId}/data/{data}")
    @PreAuthorize("hasAuthority('VISUALIZAR_PRESENCAS')")
    public ResponseEntity<List<PresencaListarDto>> listarPorObraEData(
            @PathVariable Long obraId,
            @PathVariable String data) {
        LocalDate dataLocalDate = LocalDate.parse(data);
        return ResponseEntity.ok(presencaService.listarPorObraEData(obraId, dataLocalDate));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_PRESENCAS')")
    public ResponseEntity<PresencaResponseDto> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(presencaService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_PRESENCA')")
    public ResponseEntity<PresencaResponseDto> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody PresencaDto dto) {
        return ResponseEntity.ok(presencaService.atualizar(id, dto));
    }

    @PatchMapping("/{id}/alternar")
    @PreAuthorize("hasAuthority('EDITAR_PRESENCA')")
    public ResponseEntity<Void> alternarPresenca(@PathVariable Long id) {
        presencaService.alternarPresenca(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_PRESENCA')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        presencaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
