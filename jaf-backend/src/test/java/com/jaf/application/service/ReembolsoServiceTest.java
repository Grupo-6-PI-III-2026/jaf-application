package com.jaf.application.service;

import com.jaf.application.dto.PagamentoReembolsoRequest;
import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReembolsoServiceTest {
    @Mock GastoRepository gastos;
    @Mock PagamentoReembolsoRepository pagamentos;
    @Mock AcessoObraService acesso;
    @InjectMocks ReembolsoService service;
    Gasto gasto;

    @BeforeEach void preparar() {
        Obra obra = new Obra(); obra.setId(1L);
        Funcionario pessoa = new Funcionario(); pessoa.setId(2L); pessoa.setNome("Pessoa");
        gasto = new Gasto(); gasto.setId(3L); gasto.setObra(obra); gasto.setFuncionario(pessoa);
        gasto.setValor(new BigDecimal("100.00")); gasto.setMetodoPagamento("REEMBOLSO");
        gasto.setDtGasto(LocalDate.now().minusDays(5));
    }

    private PagamentoReembolso parcela(String valor) {
        var p = new PagamentoReembolso(); p.setId(5L); p.setGasto(gasto);
        p.setValor(new BigDecimal(valor)); p.setDataPagamento(LocalDate.now()); return p;
    }

    @Test void registraParcelaEDevolveHistoricoParcial() {
        when(gastos.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        when(pagamentos.totalPago(3L)).thenReturn(BigDecimal.ZERO);
        when(pagamentos.findByGastoIdOrderByDataPagamentoAscIdAsc(3L)).thenReturn(List.of(parcela("30.00")));
        var resposta = service.registrar(3L, new PagamentoReembolsoRequest(new BigDecimal("30.00"), LocalDate.now(), "Primeira parcela"));
        assertEquals(StatusReembolso.PARCIAL, resposta.status());
        assertEquals(new BigDecimal("70.00"), resposta.saldoPendente());
        assertEquals(1, resposta.pagamentos().size());
        assertFalse(gasto.getReembolsoConcluido());
        verify(gastos).buscarParaAtualizar(3L);
        verify(pagamentos).saveAndFlush(any());
    }

    @Test void quitaSaldoSemDuplicarParcelaAnterior() {
        when(gastos.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        when(pagamentos.totalPago(3L)).thenReturn(new BigDecimal("30.00"));
        when(pagamentos.findByGastoIdOrderByDataPagamentoAscIdAsc(3L)).thenReturn(List.of(parcela("30.00"), parcela("70.00")));
        var resposta = service.registrar(3L, new PagamentoReembolsoRequest(new BigDecimal("70.00"), LocalDate.now(), null));
        assertEquals(StatusReembolso.PAGO, resposta.status());
        assertEquals(new BigDecimal("100.00"), resposta.totalPago());
        assertEquals(0, resposta.saldoPendente().signum());
        assertTrue(gasto.getReembolsoConcluido());
    }

    @Test void rejeitaSobrepagamento() {
        when(gastos.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        when(pagamentos.totalPago(3L)).thenReturn(new BigDecimal("90.00"));
        assertThrows(BadRequest.class, () -> service.registrar(3L, new PagamentoReembolsoRequest(new BigDecimal("10.01"), LocalDate.now(), null)));
        verify(pagamentos, never()).saveAndFlush(any());
    }

    @Test void rejeitaValoresInvalidosEDatasInvalidas() {
        when(gastos.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        for (String valor : List.of("0", "-1", "0.001", "100000000000000000"))
            assertThrows(BadRequest.class, () -> service.registrar(3L, new PagamentoReembolsoRequest(new BigDecimal(valor), LocalDate.now(), null)));
        for (LocalDate data : List.of(LocalDate.now().plusDays(1), gasto.getDtGasto().minusDays(1)))
            assertThrows(BadRequest.class, () -> service.registrar(3L, new PagamentoReembolsoRequest(BigDecimal.ONE, data, null)));
        verify(pagamentos, never()).saveAndFlush(any());
    }

    @Test void rejeitaRegistroSemCargoOuEscopo() {
        when(gastos.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        doThrow(new Forbidden("Sem acesso")).when(acesso).exigirRegistroReembolso(1L);
        assertThrows(Forbidden.class, () -> service.registrar(3L, new PagamentoReembolsoRequest(BigDecimal.ONE, LocalDate.now(), null)));
        verifyNoInteractions(pagamentos);
    }

    @Test void rejeitaGastoQueNaoEReembolso() {
        gasto.setMetodoPagamento("PIX");
        when(gastos.findById(3L)).thenReturn(Optional.of(gasto));
        assertThrows(BadRequest.class, () -> service.buscar(3L));
    }

    @Test void resumoAgrupaPessoaComTotaisReais() {
        var total = mock(PagamentoReembolsoRepository.TotalPorGasto.class);
        when(total.getGastoId()).thenReturn(3L); when(total.getTotalPago()).thenReturn(new BigDecimal("40.00"));
        when(gastos.findByObraIdAndMetodoPagamento(1L, "REEMBOLSO")).thenReturn(List.of(gasto));
        when(pagamentos.totaisPorGasto(List.of(3L))).thenReturn(List.of(total));
        var resposta = service.resumo(1L).getFirst();
        assertEquals(2L, resposta.funcionarioId());
        assertEquals(new BigDecimal("40.00"), resposta.totalPago());
        assertEquals(new BigDecimal("60.00"), resposta.saldoPendente());
    }
}