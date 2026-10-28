package com.jaf.application.repository;

import com.jaf.application.dto.PagamentoReembolsoRequest;
import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.*;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.model.*;
import com.jaf.application.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"}, showSql = false)
@Import({ReembolsoService.class, AcessoObraService.class, GastoService.class})
class ReembolsoPersistenciaTest {
    @Autowired GastoRepository gastos;
    @Autowired ObraRepository obras;
    @Autowired FuncionarioRepository funcionarios;
    @Autowired AlocacaoObraRepository alocacoes;
    @Autowired PagamentoReembolsoRepository pagamentos;
    @Autowired ReembolsoService reembolsos;
    @Autowired GastoService gastoService;
    @Autowired PlatformTransactionManager transactionManager;

    private void autenticar(String email) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email, "", List.of()));
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }

    private Funcionario pessoa(Cargo cargo) {
        var pessoa = new Funcionario(); pessoa.setNome("Pessoa"); pessoa.setEmail(UUID.randomUUID() + "@test.invalid");
        pessoa.setCargoGlobal(cargo); return funcionarios.saveAndFlush(pessoa);
    }
    private Gasto gasto(Obra obra, Funcionario pessoa, String metodo) {
        var gasto = new Gasto(); gasto.setObra(obra); gasto.setFuncionario(pessoa); gasto.setMetodoPagamento(metodo);
        gasto.setCategoria("Material"); gasto.setValor(new BigDecimal("100.00")); gasto.setDtGasto(LocalDate.now().minusDays(1));
        return gastos.saveAndFlush(gasto);
    }
    private void pagar(Gasto gasto, String valor) {
        reembolsos.registrar(gasto.getId(), new PagamentoReembolsoRequest(new BigDecimal(valor), LocalDate.now(), null));
    }

    @Test void filtrosCombinadosUsamSomaRealEPreservamEscopoNasDuasListagens() {
        var admin = pessoa(Cargo.ADMIN); autenticar(admin.getEmail());
        var pessoa = pessoa(Cargo.RESPONSAVEL_ADMINISTRATIVO);
        var outra = pessoa(Cargo.ENGENHEIRO);
        var obra = obras.saveAndFlush(new Obra()); var fora = obras.saveAndFlush(new Obra());
        var pendente = gasto(obra, pessoa, "REEMBOLSO");
        var parcial = gasto(obra, pessoa, "REEMBOLSO"); pagar(parcial, "30.00");
        var pago = gasto(obra, pessoa, "REEMBOLSO"); pagar(pago, "100.00");
        var semEscopo = gasto(fora, pessoa, "REEMBOLSO"); pagar(semEscopo, "30.00");
        gasto(obra, outra, "REEMBOLSO"); gasto(obra, pessoa, "PIX");
        var alocacao = new AlocacaoObra(); alocacao.setObra(obra); alocacao.setFuncionario(pessoa);
        alocacao.setCargo(Cargo.RESPONSAVEL_ADMINISTRATIVO); alocacoes.saveAndFlush(alocacao);
        autenticar(pessoa.getEmail());
        assertEquals(List.of(parcial.getId()), gastoService.listarFiltrado(null, pessoa.getId(), "Material", "REEMBOLSO", StatusReembolso.PARCIAL).stream().map(Gasto::getId).toList());
        var pagina = gastoService.listarPaginadoFiltrado(null, pessoa.getId(), "Material", "REEMBOLSO", StatusReembolso.PARCIAL, 0, 20, "id", "asc");
        assertEquals(1, pagina.getTotalElements()); assertEquals(parcial.getId(), pagina.getContent().getFirst().getId());
        assertEquals(pendente.getId(), gastoService.listarFiltrado(obra.getId(), pessoa.getId(), null, null, StatusReembolso.PENDENTE).getFirst().getId());
        assertEquals(pago.getId(), gastoService.listarFiltrado(obra.getId(), pessoa.getId(), null, null, StatusReembolso.PAGO).getFirst().getId());
        assertTrue(gastoService.listarFiltrado(fora.getId(), null, null, null, null).isEmpty());
        assertTrue(gastoService.listarFiltrado(null, pessoa.getId(), "Outra categoria", "REEMBOLSO", null).isEmpty());
        var resumo = reembolsos.resumo(obra.getId());
        var linha = resumo.stream().filter(r -> r.funcionarioId().equals(pessoa.getId())).findFirst().orElseThrow();
        assertEquals(new BigDecimal("300.00"), linha.valorTotal());
        assertEquals(new BigDecimal("130.00"), linha.totalPago());
        assertEquals(new BigDecimal("170.00"), linha.saldoPendente());
    }

    @Test void edicaoRespeitaMenorDataEntreVariasParcelas() {
        var admin = pessoa(Cargo.ADMIN);
        autenticar(admin.getEmail());
        var obra = obras.saveAndFlush(new Obra());
        var gasto = gasto(obra, admin, "REEMBOLSO");
        LocalDate dataOriginal = LocalDate.now().minusDays(5);
        LocalDate primeiroPagamento = LocalDate.now().minusDays(2);
        gasto.setDtGasto(dataOriginal);
        gastos.saveAndFlush(gasto);
        // Insere fora de ordem: a validação não pode depender do ID da parcela.
        pagar(gasto, "20.00");
        reembolsos.registrar(gasto.getId(), new PagamentoReembolsoRequest(
                new BigDecimal("30.00"), primeiroPagamento, null));

        assertFalse(pagamentos.existsByGastoIdAndDataPagamentoBefore(gasto.getId(), primeiroPagamento));
        assertFalse(pagamentos.existsByGastoIdAndDataPagamentoBefore(gasto.getId(), primeiroPagamento.minusDays(1)));
        assertTrue(pagamentos.existsByGastoIdAndDataPagamentoBefore(gasto.getId(), primeiroPagamento.plusDays(1)));

        var dto = new GastoDto();
        dto.setObraId(obra.getId());
        dto.setFuncionarioId(admin.getId());
        dto.setMetodoPagamento("REEMBOLSO");
        dto.setValor(gasto.getValor());
        dto.setDtGasto(primeiroPagamento.plusDays(1));
        assertThrows(Conflict.class, () -> gastoService.atualizar(gasto.getId(), dto));
        assertEquals(dataOriginal, gasto.getDtGasto());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void lockImpedeSobrepagamentoConcorrente() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        record Dados(Long gastoId, String email) {}
        Dados dados = tx.execute(status -> {
            var admin = pessoa(Cargo.ADMIN); var obra = obras.saveAndFlush(new Obra());
            return new Dados(gasto(obra, admin, "REEMBOLSO").getId(), admin.getEmail());
        });
        var inicio = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> tarefa = () -> {
                autenticar(dados.email());
                try {
                    inicio.await(10, TimeUnit.SECONDS);
                    reembolsos.registrar(dados.gastoId(), new PagamentoReembolsoRequest(new BigDecimal("70.00"), LocalDate.now(), null));
                    return true;
                } catch (BadRequest ex) { return false; }
                finally { SecurityContextHolder.clearContext(); }
            };
            var primeira = pool.submit(tarefa); var segunda = pool.submit(tarefa); inicio.countDown();
            assertNotEquals(primeira.get(20, TimeUnit.SECONDS), segunda.get(20, TimeUnit.SECONDS));
        }
        assertEquals(new BigDecimal("70.00"), pagamentos.totalPago(dados.gastoId()));
        assertEquals(1, pagamentos.findByGastoIdOrderByDataPagamentoAscIdAsc(dados.gastoId()).size());
    }
}