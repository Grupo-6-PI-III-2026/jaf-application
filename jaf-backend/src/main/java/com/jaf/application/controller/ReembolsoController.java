package com.jaf.application.controller;

import com.jaf.application.dto.*;
import com.jaf.application.service.ReembolsoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/gastos")
public class ReembolsoController {
    private final ReembolsoService service;
    public ReembolsoController(ReembolsoService service) { this.service = service; }
    @GetMapping("/{id}/reembolsos")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS') and @acessoObraService.podeVerObra(@reembolsoService.obraId(#id))")
    public ReembolsoResponse buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping("/{id}/reembolsos")
    @PreAuthorize("@acessoObraService.podeRegistrarReembolso(@reembolsoService.obraId(#id))")
    @ResponseStatus(HttpStatus.CREATED)
    public ReembolsoResponse registrar(@PathVariable Long id, @Valid @RequestBody PagamentoReembolsoRequest request) {
        return service.registrar(id, request);
    }
    @PostMapping("/{id}/reembolsos/{pagamentoId}/estorno")
    @PreAuthorize("hasRole('ADMIN')")
    public ReembolsoResponse estornar(@PathVariable Long id, @PathVariable Long pagamentoId,
                                      @Valid @RequestBody EstornoPagamentoRequest request) {
        return service.estornar(id, pagamentoId, request.motivo());
    }
    @GetMapping("/reembolsos/resumo")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS') and @acessoObraService.podeVerObra(#obraId)")
    public List<ReembolsoResumo> resumo(@RequestParam Long obraId) { return service.resumo(obraId); }
}