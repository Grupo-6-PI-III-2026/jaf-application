package com.jaf.application.controller;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.dto.GastoResponse;
import com.jaf.application.dto.PageResponse;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/gastos")
@SecurityRequirement(name = "Bearer")
@Transactional
public class GastoController {
    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    public ResponseEntity<GastoResponse> criar(@Valid @RequestBody GastoDto dto, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(gastoService.criar(dto), authentication));
    }
 
    @GetMapping
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<List<GastoResponse>> listar(Authentication authentication,
                                              @RequestParam(required = false) Long obraId,
                                              @RequestParam(required = false) Long funcionarioId,
                                              @RequestParam(required = false) String categoria,
                                              @RequestParam(required = false) String metodoPagamento,
                                              @RequestParam(required = false) com.jaf.application.enums.StatusReembolso statusReembolso,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ListaLimitada.responder(respostas(gastoService.listarFiltrado(obraId, funcionarioId, categoria, metodoPagamento, statusReembolso, from, to), authentication), "GET /gastos");
    }

    @GetMapping("/paginado")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<PageResponse<GastoResponse>> listarPaginado(
            Authentication authentication,
            @RequestParam(required = false) Long obraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "dtGasto") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String metodoPagamento,
            @RequestParam(required = false) Long funcionarioId,
            @RequestParam(required = false) com.jaf.application.enums.StatusReembolso statusReembolso,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String busca) {
        Page<Gasto> pagina = gastoService.listarPaginadoFiltrado(
                obraId, funcionarioId, categoria, metodoPagamento, statusReembolso, from, to, busca, page, size, sortBy, sortDir);
        var totais = gastoService.totaisPagos(pagina.getContent());
        Page<GastoResponse> resposta = pagina.map(gasto -> GastoResponse.de(gasto, isAdmin(authentication), totais));
        return ResponseEntity.ok(PageResponse.de(resposta));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<GastoResponse> buscarPorId(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(resposta(gastoService.buscarPorIdComEscopo(id, authentication.getName()), authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EDITAR_GASTO')")
    public ResponseEntity<GastoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody GastoDto dto, Authentication authentication) {
        return ResponseEntity.ok(resposta(gastoService.atualizar(id, dto), authentication));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETAR_GASTO')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        gastoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private GastoResponse resposta(Gasto gasto, Authentication authentication) {
        return GastoResponse.de(gasto, isAdmin(authentication), gastoService.totaisPagos(List.of(gasto)));
    }

    private List<GastoResponse> respostas(List<Gasto> gastos, Authentication authentication) {
        var totais = gastoService.totaisPagos(gastos);
        boolean admin = isAdmin(authentication);
        return gastos.stream().map(gasto -> GastoResponse.de(gasto, admin, totais)).toList();
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()) || "ADMIN".equals(authority.getAuthority()));
    }
}
