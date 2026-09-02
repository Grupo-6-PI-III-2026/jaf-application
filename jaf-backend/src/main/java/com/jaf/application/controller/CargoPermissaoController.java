package com.jaf.application.controller;

import com.jaf.application.dto.AtualizarCargoPermissoesDto;
import com.jaf.application.dto.CargoPermissoesDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.service.CargoPermissaoService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/permissoes/cargos")
@SecurityRequirement(name = "Bearer")
public class CargoPermissaoController {
    private final CargoPermissaoService service;

    public CargoPermissaoController(CargoPermissaoService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<List<CargoPermissoesDto>> listar() {
        return ResponseEntity.ok(service.listarCargosSistema());
    }

    @PutMapping("/{cargo}")
    @PreAuthorize("hasAuthority('EDITAR_FUNCIONARIO')")
    public ResponseEntity<CargoPermissoesDto> atualizar(
            @PathVariable Cargo cargo,
            @RequestBody AtualizarCargoPermissoesDto dto) {
        return ResponseEntity.ok(service.atualizar(cargo, dto.getPermissoes()));
    }
}