package com.jaf.application.controller;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.service.AlocacaoObraService;
import com.jaf.application.service.GastoService;
import com.jaf.application.service.ObraService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/obras")
@SecurityRequirement(name = "Bearer")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Obra> criar(@Valid @RequestBody ObraDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(obraService.criar(dto));
    }

    @GetMapping
    public ResponseEntity<List<Obra>> listar(Authentication authentication) {
        return ResponseEntity.ok(obraService.listarPorUsuario(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Obra> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(obraService.buscarPorIdComEscopo(id, authentication.getName()));
    }

    @GetMapping("/{id}/gastos")
    public ResponseEntity<List<Gasto>> listarGastosDaObra(@PathVariable Long id, Authentication authentication) {
        obraService.buscarPorIdComEscopo(id, authentication.getName());
        return ResponseEntity.ok(gastoService.listarPorUsuario(authentication.getName(), id));
    }

    @GetMapping("/{id}/alocacoes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AlocacaoObra>> listarAlocacoesDaObra(@PathVariable Long id) {
        return ResponseEntity.ok(alocacaoObraService.listarPorObra(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Obra> atualizar(@PathVariable Long id, @Valid @RequestBody ObraDto dto) {
        return ResponseEntity.ok(obraService.atualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        obraService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
