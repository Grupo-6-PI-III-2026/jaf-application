package com.jaf.application.repository;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.model.PagamentoReembolso;
import com.jaf.application.service.AcessoObraService;
import com.jaf.application.service.DashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Confere, com o banco, que as consultas agregadas devolvem os mesmos números do cálculo em memória. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"}, showSql = false)
@Import({DashboardService.class, AcessoObraService.class})
class DashboardAgregacaoTest {
    @Autowired GastoRepository gastos;
    @Autowired ObraRepository obras;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired PagamentoReembolsoRepository pagamentos;
    @Autowired DashboardService service;

    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    private Gasto gasto(Obra obra, Funcionario pessoa, String valor, LocalDate data, String etapa, String categoria, boolean reembolso) {
        var gasto = new Gasto();
        gasto.setObra(obra); gasto.setFuncionario(pessoa);
        gasto.setValor(new BigDecimal(valor)); gasto.setDtGasto(data); gasto.setEtapa(etapa);
        gasto.setCategoria(categoria);
        gasto.setMetodoPagamento(reembolso ? "REEMBOLSO" : "PIX");
        gasto.setReembolsavel(reembolso);
        return gastos.saveAndFlush(gasto);
    }

    private void pagar(Gasto gasto, String valor, boolean cancelado) {
        var pagamento = new PagamentoReembolso();
        pagamento.setGasto(gasto); pagamento.setValor(new BigDecimal(valor)); pagamento.setDataPagamento(LocalDate.of(2026, 3, 20));
        if (cancelado) pagamento.setCanceladoEm(java.time.LocalDateTime.now());
        pagamentos.saveAndFlush(pagamento);
    }

    @Test void numerosIguaisAoCalculoEmMemoria() {
        var admin = new Funcionario(); admin.setNome("Admin"); admin.setEmail(UUID.randomUUID() + "@test.invalid"); admin.setCargoGlobal(Cargo.ADMIN);
        admin = funcionarios.saveAndFlush(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getEmail(), "", List.of()));
        var obra = new Obra(); obra.setOrcamento(new BigDecimal("1000.00")); obra.setTotalRecebido(new BigDecimal("200.00"));
        obra = obras.saveAndFlush(obra);
        var outraObra = obras.saveAndFlush(new Obra());

        var inicio = LocalDate.of(2026, 3, 1);
        var fim = LocalDate.of(2026, 3, 31);
        gasto(obra, admin, "50.00", inicio.minusDays(1), "ETAPA 1", "Material", false);
        var selecionado = gasto(obra, admin, "100.00", inicio, "ETAPA 1", "Custos extras", true);
        gasto(obra, admin, "20.00", fim, "ETAPA 2", "Material", false);
        gasto(obra, admin, "40.00", fim.plusDays(1), "ETAPA 1", null, false);
        gasto(outraObra, admin, "999.00", inicio, "ETAPA 1", "Material", false);
        pagar(selecionado, "30.00", false);
        pagar(selecionado, "25.00", true);

        var filtrado = service.buscarStats(obra.getId(), "ETAPA 1", inicio, fim);
        var geral = service.buscarStats(obra.getId(), "TODAS");

        // Saldo acumulado ignora período e etapa: recebido 200 - (50 + 100 + 20 + 40).
        assertEquals(new BigDecimal("-10.00"), filtrado.getSaldoRestante());
        assertEquals(filtrado.getSaldoRestante(), geral.getSaldoRestante());
        assertEquals(105, filtrado.getProgressoSaldo());
        assertEquals(0, new BigDecimal("100.00").compareTo(filtrado.getGastosEtapa()));
        assertEquals(10, filtrado.getProgressoEtapa());
        // Pagamento estornado não conta.
        assertEquals(0, new BigDecimal("70.00").compareTo(filtrado.getReembolsosPendentes()));
        assertEquals(30.0, filtrado.getReembolsosPizza().getFirst().value());
        assertEquals(1, filtrado.getGastosImprevistos().size());
        assertEquals(100.0, filtrado.getGastosImprevistos().getFirst().valor());
        assertEquals(1, filtrado.getGastosPorCategoria().size());
        assertEquals("Custos extras", filtrado.getGastosPorCategoria().getFirst().categoria());

        // Sem filtros, categoria nula aparece como "Sem categoria".
        assertEquals(3, geral.getGastosPorCategoria().size());
        assertEquals(0, new BigDecimal("210.00").compareTo(geral.getGastosEtapa()));
    }
}
