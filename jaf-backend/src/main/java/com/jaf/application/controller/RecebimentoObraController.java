package com.jaf.application.controller;

import com.jaf.application.dto.RecebimentoObraRequest;
import com.jaf.application.dto.RecebimentoObraResponse;
import com.jaf.application.service.RecebimentoObraService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/obras/{obraId}/recebimentos")
@Transactional
public class RecebimentoObraController {
    private final RecebimentoObraService service;
    public RecebimentoObraController(RecebimentoObraService service) { this.service = service; }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public List<RecebimentoObraResponse> listar(@PathVariable Long obraId) {
        return service.listar(obraId).stream().map(RecebimentoObraResponse::de).toList();
    }
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecebimentoObraResponse> criar(@PathVariable Long obraId, @Valid @RequestBody RecebimentoObraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RecebimentoObraResponse.de(service.criar(obraId, request)));
    }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> estornar(@PathVariable Long obraId, @PathVariable Long id) {
        service.estornar(obraId, id); return ResponseEntity.noContent().build();
    }
}
