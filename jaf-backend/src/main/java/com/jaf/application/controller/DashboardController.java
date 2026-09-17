package com.jaf.application.controller;

import com.jaf.application.dto.DashboardResponseDto;
import com.jaf.application.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@SecurityRequirement(name = "Bearer")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VISUALIZAR_GASTOS')")
    public ResponseEntity<DashboardResponseDto> buscarStats(
            @RequestParam Long obraId,
            @RequestParam(defaultValue = "ETAPA 1") String etapa) {
        return ResponseEntity.ok(dashboardService.buscarStats(obraId, etapa));
    }
}
