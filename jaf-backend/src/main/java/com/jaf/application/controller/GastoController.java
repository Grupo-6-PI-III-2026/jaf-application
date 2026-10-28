package com.jaf.application.controller;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.model.Gasto;
import com.jaf.application.service.GastoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<Gasto> criar(@Valid @RequestBody GastoDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gastoService.criar(dto));
    }
 
    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<List<Gasto>> listar(Authentication authentication,
                                              @RequestParam(required = false) Long obraId,
                                              @RequestParam(required = false) Long funcionarioId,
                                              @RequestParam(required = false) String categoria,
                                              @RequestParam(required = false) String metodoPagamento,
                                              @RequestParam(required = false) com.jaf.application.enums.StatusReembolso statusReembolso) {
        return ResponseEntity.ok(gastoService.listarFiltrado(obraId, funcionarioId, categoria, metodoPagamento, statusReembolso));
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<Page<Gasto>> listarPaginado(
            Authentication authentication,
            @RequestParam(required = false) Long obraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dtGasto") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String metodoPagamento,
            @RequestParam(required = false) Long funcionarioId,
            @RequestParam(required = false) com.jaf.application.enums.StatusReembolso statusReembolso) {
        return ResponseEntity.ok(gastoService.listarPaginadoFiltrado(
                obraId, funcionarioId, categoria, metodoPagamento, statusReembolso, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<Gasto> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(gastoService.buscarPorIdComEscopo(id, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_GASTO')")
    public ResponseEntity<Gasto> atualizar(@PathVariable Long id, @Valid @RequestBody GastoDto dto) {
        return ResponseEntity.ok(gastoService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_GASTO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        gastoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
