package com.jaf.application.controller;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.dto.ObraResponse;
import com.jaf.application.dto.GastoResponse;
import com.jaf.application.dto.AlocacaoResponse;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.service.AlocacaoObraService;
import com.jaf.application.service.GastoService;
import com.jaf.application.service.ObraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
@RequestMapping("/obras")
@SecurityRequirement(name = "Bearer")
@Transactional
public class ObraController {
    private final ObraService obraService;
    private final GastoService gastoService;
    private final AlocacaoObraService alocacaoObraService;

    public ObraController(ObraService obraService,
                          GastoService gastoService,
                          AlocacaoObraService alocacaoObraService) {
        this.obraService = obraService;
        this.gastoService = gastoService;
        this.alocacaoObraService = alocacaoObraService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRIAR_OBRA')")
    public ResponseEntity<ObraResponse> criar(@Valid @RequestBody ObraDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ObraResponse.de(obraService.criar(dto), isAdmin(authentication)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_OBRA')")
    public ResponseEntity<List<ObraResponse>> listar(Authentication authentication) {
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(obraService.listarPorUsuario(authentication.getName()).stream().map(obra -> ObraResponse.de(obra, admin)).toList());
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('VISUALIZAR_OBRA')")
    public ResponseEntity<com.jaf.application.dto.PageResponse<ObraResponse>> listarPaginado(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titulo") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String status) {
        boolean admin = isAdmin(authentication);
        Page<ObraResponse> resposta = obraService.listarPaginadoPorUsuario(
                authentication.getName(), page, size, sortBy, sortDir, status).map(obra -> ObraResponse.de(obra, admin));
        return ResponseEntity.ok(com.jaf.application.dto.PageResponse.de(resposta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_OBRA')")
    public ResponseEntity<ObraResponse> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(ObraResponse.de(obraService.buscarPorIdComEscopo(id, authentication.getName()), isAdmin(authentication)));
    }

    @GetMapping("/{id}/gastos")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<List<GastoResponse>> listarGastosDaObra(@PathVariable Long id, Authentication authentication) {
        obraService.buscarPorIdComEscopo(id, authentication.getName());
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(gastoService.listarPorUsuario(authentication.getName(), id).stream().map(gasto -> GastoResponse.de(gasto, admin)).toList());
    }

    @GetMapping("/{id}/alocacoes")
    @PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES')")
    public ResponseEntity<List<AlocacaoResponse>> listarAlocacoesDaObra(@PathVariable Long id, Authentication authentication) {
        obraService.buscarPorIdComEscopo(id, authentication.getName());
        boolean admin = isAdmin(authentication);
        return ResponseEntity.ok(alocacaoObraService.listarPorObra(id).stream().map(alocacao -> AlocacaoResponse.de(alocacao, admin)).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_OBRA')")
    public ResponseEntity<ObraResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ObraDto dto, Authentication authentication) {
        obraService.buscarPorIdComEscopo(id, authentication.getName());
        return ResponseEntity.ok(ObraResponse.de(obraService.atualizar(id, dto), isAdmin(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_OBRA')")
    public ResponseEntity<Void> deletar(@PathVariable Long id, Authentication authentication) {
        obraService.buscarPorIdComEscopo(id, authentication.getName());
        obraService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()) || "ADMIN".equals(authority.getAuthority()));
    }
}
