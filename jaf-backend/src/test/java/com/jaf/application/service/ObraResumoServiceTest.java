package com.jaf.application.service;

import com.jaf.application.enums.FuncaoObra;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.GastoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class ObraResumoServiceTest {
    private final ObraService obraService = mock(ObraService.class);
    private final GastoRepository gastos = mock(GastoRepository.class);
    private final AlocacaoObraRepository alocacoes = mock(AlocacaoObraRepository.class);
    private final ObraResumoService service = new ObraResumoService(obraService, gastos, alocacoes);

    private Obra obra(long id) {
        var obra = new Obra();
        obra.setId(id);
        obra.setTitulo("Obra " + id);
        obra.setStatus("EM_ANDAMENTO");
        obra.setDtInicio(LocalDate.of(2026, 1, 1));
        obra.setDtTerminoPrevisto(LocalDate.of(2026, 12, 31));
        return obra;
    }

    private Funcionario pessoa(long id, String nome) {
        var pessoa = new Funcionario();
        pessoa.setId(id);
        pessoa.setNome(nome);
        return pessoa;
    }

    private AlocacaoObra alocacao(Obra obra, Funcionario pessoa, FuncaoObra funcao) {
        var alocacao = new AlocacaoObra();
        alocacao.setObra(obra);
        alocacao.setFuncionario(pessoa);
        alocacao.setCargo(funcao);
        return alocacao;
    }

    private GastoRepository.TotalPorObra total(long obraId, String valor) {
        var total = mock(GastoRepository.TotalPorObra.class);
        when(total.getObraId()).thenReturn(obraId);
        when(total.getTotal()).thenReturn(new BigDecimal(valor));
        return total;
    }

    private void paginaDeObras(Obra... obras) {
        when(obraService.listarPaginadoPorUsuario(any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(obras), PageRequest.of(0, 50), obras.length));
    }

    @Test void adminRecebeTotalAgregadoEResponsavelPreferindoOGestor() {
        var a = obra(1); var b = obra(2);
        paginaDeObras(a, b);
        var totalDaObra1 = total(1, "150.50");
        when(gastos.totaisPorObra(anyCollection())).thenReturn(List.of(totalDaObra1));
        when(alocacoes.findComFuncionarioByObraIds(anyCollection())).thenReturn(List.of(
                alocacao(a, pessoa(7, "Eng"), FuncaoObra.ENGENHEIRO),
                alocacao(a, pessoa(8, "Gestor"), FuncaoObra.GESTOR_OBRA)));

        var pagina = service.resumo("admin@x", 0, 50, "titulo", "asc", null, true, true);

        assertEquals(new BigDecimal("150.50"), pagina.getContent().get(0).totalGasto());
        assertEquals("Gestor", pagina.getContent().get(0).responsavel().nome());
        // Obra sem gastos aparece com total zero (não "indisponível") e sem responsável.
        assertEquals(BigDecimal.ZERO, pagina.getContent().get(1).totalGasto());
        assertNull(pagina.getContent().get(1).responsavel());
        verify(gastos, times(1)).totaisPorObra(anyCollection());
    }

    @Test void naoAdminNaoRecebeNemCalculaTotal() {
        paginaDeObras(obra(1));
        when(alocacoes.findComFuncionarioByObraIds(anyCollection())).thenReturn(List.of());

        var pagina = service.resumo("eng@x", 0, 50, "titulo", "asc", null, false, true);

        assertNull(pagina.getContent().get(0).totalGasto());
        verifyNoInteractions(gastos);
    }

    @Test void semPermissaoDeAlocacoesNaoConsultaResponsavel() {
        paginaDeObras(obra(1));

        var pagina = service.resumo("x@x", 0, 50, "titulo", "asc", null, false, false);

        assertNull(pagina.getContent().get(0).responsavel());
        verifyNoInteractions(alocacoes);
    }

    @Test void pessoasJuntamAlocadosEAutoresQueSairamDaObra() {
        var obra = obra(1);
        when(alocacoes.findByObraId(1L)).thenReturn(List.of(alocacao(obra, pessoa(2, "Zeca"), FuncaoObra.PEDREIRO)));
        var antigo = mock(GastoRepository.PessoaDoGasto.class);
        when(antigo.getId()).thenReturn(5L);
        when(antigo.getNome()).thenReturn("Ana");
        var repetido = mock(GastoRepository.PessoaDoGasto.class);
        when(repetido.getId()).thenReturn(2L);
        when(repetido.getNome()).thenReturn("Zeca");
        when(gastos.pessoasComGastoNaObra(1L)).thenReturn(List.of(antigo, repetido));

        var pessoas = service.pessoas(1L);

        assertEquals(List.of("Ana", "Zeca"), pessoas.stream().map(p -> p.nome()).toList());
        assertEquals(FuncaoObra.PEDREIRO, pessoas.get(1).funcaoNaObra());
        assertNull(pessoas.get(0).funcaoNaObra());
    }
}
