package com.jaf.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaf.application.dto.ObraFinanceiroRequest;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.*;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.http.converter.json.SpringHandlerInstantiator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcessoFinanceiroTest {
    @Mock FuncionarioRepository funcionarios;
    @Mock AlocacaoObraRepository alocacoes;
    @Mock ObraRepository obras;
    @Mock GastoRepository gastos;
    AcessoObraService acesso;
    Funcionario usuario;

    @BeforeEach void preparar() {
        acesso = new AcessoObraService(funcionarios, alocacoes);
        usuario = new Funcionario(); usuario.setId(7L); usuario.setCargoGlobal(Cargo.ENGENHEIRO);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("teste", "", List.of()));
        lenient().when(funcionarios.findByEmailIgnoreCase("teste")).thenReturn(Optional.of(usuario));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    @Test void financeiroSomenteAdminMesmoComPermissoesDelegadas() {
        var service = new ObraFinanceiroService(obras, gastos, acesso);
        assertThrows(Forbidden.class, () -> service.buscar(1L));
        usuario.setCargoGlobal(Cargo.RESPONSAVEL_ADMINISTRATIVO);
        assertThrows(Forbidden.class, () -> service.atualizar(1L, new ObraFinanceiroRequest(BigDecimal.ONE)));
        verifyNoInteractions(obras, gastos);
    }
    @Test void reembolsoExigeCargoEAlocacaoParaResponsavel() {
        assertThrows(Forbidden.class, () -> acesso.exigirRegistroReembolso(1L));
        usuario.setCargoGlobal(Cargo.RESPONSAVEL_ADMINISTRATIVO);
        assertThrows(Forbidden.class, () -> acesso.exigirRegistroReembolso(1L));
        when(alocacoes.existsByFuncionarioIdAndObraId(7L, 1L)).thenReturn(true);
        assertDoesNotThrow(() -> acesso.exigirRegistroReembolso(1L));
        usuario.setCargoGlobal(Cargo.ADMIN);
        assertDoesNotThrow(() -> acesso.exigirRegistroReembolso(99L));
    }
    @Test void semAutenticacaoNaoExibeFinanceiro() {
        SecurityContextHolder.clearContext();
        assertFalse(acesso.isAdmin());
        assertThrows(Forbidden.class, () -> acesso.exigirObra(1L));
    }
    @Test void serializacaoProtegeObrasDiretasEAninhadas() throws Exception {
        var factory = new DefaultListableBeanFactory(); factory.registerSingleton("acesso", acesso);
        var mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        mapper.setHandlerInstantiator(new SpringHandlerInstantiator(factory));
        var obra = new Obra(); obra.setId(1L); obra.setTitulo("Obra"); obra.setOrcamento("99999"); obra.setTotalRecebido(new BigDecimal("500"));
        var gasto = new Gasto(); gasto.setObra(obra);
        for (Cargo cargo : List.of(Cargo.ENGENHEIRO, Cargo.RESPONSAVEL_ADMINISTRATIVO)) {
            usuario.setCargoGlobal(cargo);
            var direto = mapper.readTree(mapper.writeValueAsString(obra));
            var aninhado = mapper.readTree(mapper.writeValueAsString(gasto)).get("obra");
            assertFalse(direto.has("orcamento")); assertFalse(direto.has("totalRecebido"));
            assertFalse(aninhado.has("orcamento")); assertFalse(aninhado.has("totalRecebido"));
            assertEquals("Obra", direto.get("titulo").asText());
        }
        usuario.setCargoGlobal(Cargo.ADMIN);
        assertTrue(mapper.readTree(mapper.writeValueAsString(obra)).has("orcamento"));
        assertTrue(mapper.readTree(mapper.writeValueAsString(gasto)).get("obra").has("totalRecebido"));
    }
    @Test void saldoUsaRecebidoNaoOrcamentoNemParcelasEmDuplicidade() {
        usuario.setCargoGlobal(Cargo.ADMIN);
        var obra = new Obra(); obra.setId(1L); obra.setOrcamento("9999");
        var gasto = new Gasto(); gasto.setValor(new BigDecimal("70.00"));
        when(obras.findById(1L)).thenReturn(Optional.of(obra));
        when(gastos.findByObraId(1L)).thenReturn(List.of(gasto));
        var service = new ObraFinanceiroService(obras, gastos, acesso);
        var resultado = service.atualizar(1L, new ObraFinanceiroRequest(new BigDecimal("100.00")));
        assertEquals(new BigDecimal("30.00"), resultado.saldo());
        assertEquals(new BigDecimal("70.00"), resultado.totalGasto());
        assertEquals("9999", obra.getOrcamento());
        assertThrows(BadRequest.class, () -> service.atualizar(1L, new ObraFinanceiroRequest(new BigDecimal("-1"))));
    }
}