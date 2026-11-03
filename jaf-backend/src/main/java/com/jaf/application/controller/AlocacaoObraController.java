package com.jaf.application.controller;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.dto.AlocacaoResponse;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.service.AlocacaoObraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/alocacoes")
@SecurityRequirement(name = "Bearer")
@Transactional
public class AlocacaoObraController {
    private final AlocacaoObraService alocacaoObraService;

    public AlocacaoObraController(AlocacaoObraService alocacaoObraService) {
        this.alocacaoObraService = alocacaoObraService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRIAR_ALOCACAO')")
    public ResponseEntity<AlocacaoResponse> criar(@Valid @RequestBody AlocacaoObraDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AlocacaoResponse.de(alocacaoObraService.criar(dto), isAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoResponse>> listar(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(alocacaoObraService.listarPorUsuario(authentication.getName()).stream().map(a -> AlocacaoResponse.de(a, admin)).toList());
    }

    @GetMapping("/filtro")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoResponse>> listarComFiltro(@RequestParam(required = false) Long obraId,
                                                               @RequestParam(required = false) Long funcionarioId,
                                                               Authentication authentication) {
        if (obraId != null) {
            return ResponseEntity.ok(alocacaoObraService.listarPorObra(obraId).stream().map(a -> AlocacaoResponse.de(a, isAdmin(authentication))).toList());
        }
        if (funcionarioId != null) {
            return ResponseEntity.ok(alocacaoObraService.listarPorFuncionario(funcionarioId).stream().map(a -> AlocacaoResponse.de(a, isAdmin(authentication))).toList());
        }
        return ResponseEntity.ok(alocacaoObraService.listarPorUsuario(authentication.getName()).stream().map(a -> AlocacaoResponse.de(a, isAdmin(authentication))).toList());
    }

    @GetMapping("/obra/{obraId}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoResponse>> listarPorObra(@PathVariable Long obraId, Authentication authentication) {
        return ResponseEntity.ok(alocacaoObraService.listarPorObra(obraId).stream().map(a -> AlocacaoResponse.de(a, isAdmin(authentication))).toList());
    }

    @GetMapping("/funcionario/{funcionarioId}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoResponse>> listarPorFuncionario(@PathVariable Long funcionarioId, Authentication authentication) {
        return ResponseEntity.ok(alocacaoObraService.listarPorFuncionario(funcionarioId).stream().map(a -> AlocacaoResponse.de(a, isAdmin(authentication))).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<AlocacaoResponse> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(AlocacaoResponse.de(alocacaoObraService.buscarPorId(id), isAdmin(authentication)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_ALOCACAO')")
    public ResponseEntity<AlocacaoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody AlocacaoObraDto dto, Authentication authentication) {
        return ResponseEntity.ok(AlocacaoResponse.de(alocacaoObraService.atualizar(id, dto), isAdmin(authentication)));
    }

    @PatchMapping("/{id}/encerrar")
    @PreAuthorize("hasAuthority('EDITAR_ALOCACAO')")
    public ResponseEntity<AlocacaoResponse> encerrar(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(AlocacaoResponse.de(alocacaoObraService.encerrar(id), isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_ALOCACAO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        alocacaoObraService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()) || "ADMIN".equals(authority.getAuthority()));
    }
}
