package com.jaf.application.service;

import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardServiceTest {
    @Test void parcelasAfetamPendenciaEPizzaNaoTotalDoGasto() {
        var gastos = mock(GastoRepository.class); var obras = mock(ObraRepository.class);
        var acesso = mock(AcessoObraService.class); var reembolsos = mock(ReembolsoService.class);
        var service = new DashboardService(gastos, obras, acesso, reembolsos);
        var obra = new Obra(); obra.setOrcamento("1000"); obra.setTotalRecebido(new BigDecimal("200.00"));
        var gasto = new Gasto(); gasto.setId(2L); gasto.setMetodoPagamento("REEMBOLSO");
        gasto.setValor(new BigDecimal("100.00")); gasto.setDtGasto(LocalDate.now());
        when(obras.findById(1L)).thenReturn(Optional.of(obra)); when(gastos.findByObraId(1L)).thenReturn(List.of(gasto));
        when(reembolsos.totais(List.of(gasto))).thenReturn(Map.of(2L, new BigDecimal("30.00")));
        var resultado = service.buscarStats(1L, "TODAS");
        assertEquals(new BigDecimal("70.00"), resultado.getReembolsosPendentes());
        assertEquals(new BigDecimal("100.00"), resultado.getGastosEtapa());
        assertEquals(new BigDecimal("100.00"), resultado.getSaldoRestante());
        assertEquals(30.0, resultado.getReembolsosPizza().getFirst().value());
    }
    @Test void saldoAcumuladoIgnoraPeriodoEEtapaMasGraficosMantemFiltros() {
        var gastos = mock(GastoRepository.class);
        var obras = mock(ObraRepository.class);
        var acesso = mock(AcessoObraService.class);
        var reembolsos = mock(ReembolsoService.class);
        var service = new DashboardService(gastos, obras, acesso, reembolsos);
        var obra = new Obra();
        obra.setOrcamento("1000");
        obra.setTotalRecebido(new BigDecimal("200.00"));
        LocalDate inicio = LocalDate.of(2026, 3, 1);
        LocalDate fim = LocalDate.of(2026, 3, 31);
        var anterior = gasto(1L, "50.00", inicio.minusDays(1), "ETAPA 1");
        var selecionado = gasto(2L, "100.00", inicio, "ETAPA 1");
        var outraEtapa = gasto(3L, "20.00", fim, "ETAPA 2");
        var posterior = gasto(4L, "40.00", fim.plusDays(1), "ETAPA 1");
        when(obras.findById(1L)).thenReturn(Optional.of(obra));
        when(gastos.findByObraId(1L)).thenReturn(List.of(anterior, selecionado, outraEtapa, posterior));
        when(reembolsos.totais(List.of(selecionado))).thenReturn(Map.of(2L, new BigDecimal("30.00")));

        var resultado = service.buscarStats(1L, "ETAPA 1", inicio, fim);
        var semFiltros = service.buscarStats(1L, "TODAS");

        assertEquals(new BigDecimal("-10.00"), resultado.getSaldoRestante());
        assertEquals(105, resultado.getProgressoSaldo());
        assertEquals(semFiltros.getSaldoRestante(), resultado.getSaldoRestante());
        assertEquals(semFiltros.getProgressoSaldo(), resultado.getProgressoSaldo());
        assertEquals(new BigDecimal("100.00"), resultado.getGastosEtapa());
        assertEquals(10, resultado.getProgressoEtapa());
        assertEquals(new BigDecimal("70.00"), resultado.getReembolsosPendentes());
        assertEquals(30.0, resultado.getReembolsosPizza().getFirst().value());
        assertEquals(1, resultado.getGastosImprevistos().size());
        assertEquals(100.0, resultado.getGastosImprevistos().getFirst().valor());
        assertEquals(1, resultado.getGastosPorCategoria().size());
        assertEquals(100.0, resultado.getGastosPorCategoria().getFirst().valor());
    }

    private Gasto gasto(Long id, String valor, LocalDate data, String etapa) {
        var gasto = new Gasto();
        gasto.setId(id);
        gasto.setValor(new BigDecimal(valor));
        gasto.setDtGasto(data);
        gasto.setEtapa(etapa);
        gasto.setCategoria("Custos extras");
        gasto.setMetodoPagamento("REEMBOLSO");
        return gasto;
    }

    @Test void naoAdminNaoConsultaDados() {
        var gastos = mock(GastoRepository.class); var obras = mock(ObraRepository.class);
        var acesso = mock(AcessoObraService.class); var reembolsos = mock(ReembolsoService.class);
        doThrow(new Forbidden("Admin apenas")).when(acesso).exigirAdmin();
        var service = new DashboardService(gastos, obras, acesso, reembolsos);
        assertThrows(Forbidden.class, () -> service.buscarStats(1L, "TODAS"));
        verifyNoInteractions(gastos, obras, reembolsos);
    }
}