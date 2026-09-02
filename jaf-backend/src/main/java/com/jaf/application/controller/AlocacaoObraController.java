package com.jaf.application.controller;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.service.AlocacaoObraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority('CRIAR_ALOCACAO')")
    public ResponseEntity<AlocacaoObra> criar(@Valid @RequestBody AlocacaoObraDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(alocacaoObraService.criar(dto));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoObra>> listar(Authentication authentication) {
        return ResponseEntity.ok(alocacaoObraService.listarPorUsuario(authentication.getName()));
    }

    @GetMapping("/filtro")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoObra>> listarComFiltro(@RequestParam(required = false) Long obraId,
                                                               @RequestParam(required = false) Long funcionarioId,
                                                               Authentication authentication) {
        if (obraId != null) {
            return ResponseEntity.ok(alocacaoObraService.listarPorObra(obraId));
        }
        if (funcionarioId != null) {
            return ResponseEntity.ok(alocacaoObraService.listarPorFuncionario(funcionarioId));
        }
        return ResponseEntity.ok(alocacaoObraService.listarPorUsuario(authentication.getName()));
    }

    @GetMapping("/obra/{obraId}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoObra>> listarPorObra(@PathVariable Long obraId) {
        return ResponseEntity.ok(alocacaoObraService.listarPorObra(obraId));
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoObra>> listarPorFuncionario(@PathVariable Long funcionarioId) {
        return ResponseEntity.ok(alocacaoObraService.listarPorFuncionario(funcionarioId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<AlocacaoObra> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(alocacaoObraService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ALOCACAO')")
    public ResponseEntity<AlocacaoObra> atualizar(@PathVariable Long id, @Valid @RequestBody AlocacaoObraDto dto) {
        return ResponseEntity.ok(alocacaoObraService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_ALOCACAO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        alocacaoObraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
