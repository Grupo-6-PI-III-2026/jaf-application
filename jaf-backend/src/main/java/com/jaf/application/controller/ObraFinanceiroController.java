package com.jaf.application.controller;

import com.jaf.application.dto.ObraFinanceiroDto;
import com.jaf.application.dto.ObraFinanceiroRequest;
import com.jaf.application.service.ObraFinanceiroService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/obras/{id}/financeiro")
public class ObraFinanceiroController {
    private final ObraFinanceiroService service;
    public ObraFinanceiroController(ObraFinanceiroService service) { this.service = service; }
    @GetMapping
    public ObraFinanceiroDto buscar(@PathVariable Long id) { return service.buscar(id); }
    @PutMapping
    public ObraFinanceiroDto atualizar(@PathVariable Long id, @Valid @RequestBody ObraFinanceiroRequest request) {
        return service.atualizar(id, request);
    }
}