package com.jaf.application.service;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.exceptions.*;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {
    @Mock GastoRepository gastoRepository;
    @Mock FuncionarioRepository funcionarioRepository;
    @Mock ObraRepository obraRepository;
    @Mock AlocacaoObraRepository alocacaoObraRepository;
    @Mock PagamentoReembolsoRepository pagamentos;
    @Mock AcessoObraService acesso;
    @Mock OcrProcessamentoRepository processamentos;
    @Mock com.jaf.application.armazenamento.ArquivoService arquivos;
    @InjectMocks GastoService service;
    Gasto gasto; GastoDto dto;

    @BeforeEach void preparar() {
        var obra = new Obra(); obra.setId(1L);
        var pessoa = new Funcionario(); pessoa.setId(2L);
        gasto = new Gasto(); gasto.setId(3L); gasto.setObra(obra); gasto.setFuncionario(pessoa);
        gasto.setValor(new BigDecimal("100.00")); gasto.setMetodoPagamento("REEMBOLSO");
        dto = new GastoDto(); dto.setObraId(1L); dto.setFuncionarioId(2L); dto.setMetodoPagamento("REEMBOLSO");
        dto.setValor(new BigDecimal("100.00")); dto.setDtGasto(LocalDate.now());
    }

    private void comPagamento() {
        when(gastoRepository.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        when(pagamentos.totalPago(3L)).thenReturn(new BigDecimal("40.00"));
        when(pagamentos.existsByGastoId(3L)).thenReturn(true);
    }

    @Test void naoPermiteReduzirValorAbaixoDoPago() {
        comPagamento(); dto.setValor(new BigDecimal("39.99"));
        assertThrows(Conflict.class, () -> service.atualizar(3L, dto));
        verify(gastoRepository, never()).save(any());
    }
    @Test void naoPermiteMudarDonoMetodoOuObraComHistorico() {
        comPagamento(); dto.setFuncionarioId(9L);
        assertThrows(Conflict.class, () -> service.atualizar(3L, dto));
        dto.setFuncionarioId(2L); dto.setMetodoPagamento("PIX");
        assertThrows(Conflict.class, () -> service.atualizar(3L, dto));
        dto.setMetodoPagamento("REEMBOLSO"); dto.setObraId(9L);
        assertThrows(Conflict.class, () -> service.atualizar(3L, dto));
    }
    @Test void preservaHistoricoAoEditarDescricaoEValor() {
        comPagamento(); dto.setDescricao("Nota corrigida"); dto.setValor(new BigDecimal("120.00"));
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(gasto.getFuncionario()));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(gasto.getObra()));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(true);
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("Nota corrigida", service.atualizar(3L, dto).getDescricao());
        verify(pagamentos, never()).deleteAll();
        assertFalse(gasto.getReembolsoConcluido());
    }
    @Test void naoPermiteDataDoGastoPosteriorAoPrimeiroPagamento() {
        comPagamento();
        LocalDate dataOriginal = LocalDate.of(2026, 1, 1);
        gasto.setDtGasto(dataOriginal);
        dto.setDtGasto(LocalDate.of(2026, 1, 11));
        when(pagamentos.existsByGastoIdAndDataPagamentoBefore(3L, dto.getDtGasto())).thenReturn(true);

        assertThrows(Conflict.class, () -> service.atualizar(3L, dto));

        assertEquals(dataOriginal, gasto.getDtGasto());
        verify(gastoRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void permiteDataDoGastoAnteriorOuIgualAoPrimeiroPagamento(int dias) {
        comPagamento();
        dto.setDtGasto(LocalDate.of(2026, 1, 10).plusDays(dias));
        when(pagamentos.existsByGastoIdAndDataPagamentoBefore(3L, dto.getDtGasto())).thenReturn(false);
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(gasto.getFuncionario()));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(gasto.getObra()));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(true);
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertEquals(dto.getDtGasto(), service.atualizar(3L, dto).getDtGasto());
        verify(pagamentos).existsByGastoIdAndDataPagamentoBefore(3L, dto.getDtGasto());
        verify(pagamentos, never()).deleteAll();
    }

    @Test void naoDeletaGastoComPagamentos() {
        when(gastoRepository.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        when(pagamentos.existsByGastoId(3L)).thenReturn(true);
        assertThrows(Conflict.class, () -> service.deletar(3L));
        verify(gastoRepository, never()).deleteById(anyLong());
    }
    @Test void naoPermiteQuitarPorFlagLegada() {
        dto.setReembolsoConcluido(true);
        assertThrows(BadRequest.class, () -> service.criar(dto));
        verify(gastoRepository, never()).save(any());
    }
    @Test void exigeEscopoNaOrigemENoDestino() {
        when(gastoRepository.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));
        doThrow(new Forbidden("Sem acesso")).when(acesso).exigirObra(1L);
        assertThrows(Forbidden.class, () -> service.atualizar(3L, dto));
        verifyNoInteractions(pagamentos);
    }
    @Test void paginacaoInvalidaRetornaErroDeCliente() {
        assertThrows(BadRequest.class, () -> service.listarPaginadoFiltrado(null, null, null, null, null, -1, 20, "id", "asc"));
        assertThrows(BadRequest.class, () -> service.listarPaginadoFiltrado(null, null, null, null, null, 0, 201, "id", "asc"));
    }

    private void podeCriar() {
        dto.setMetodoPagamento("PIX");
        var autor = new Funcionario(); autor.setEmail("autor@jaf.com");
        when(acesso.atual()).thenReturn(autor);
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(gasto.getFuncionario()));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(gasto.getObra()));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(true);
    }

    @Test void criarComOcrTransfereANotaDoProcessamento() {
        podeCriar(); dto.setOcrJobId("job-1");
        var processamento = new OcrProcessamento("job-1", "autor@jaf.com", "notas/nf.pdf");
        when(processamentos.findById("job-1")).thenReturn(Optional.of(processamento));
        when(gastoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Gasto criado = service.criar(dto);

        assertEquals("notas/nf.pdf", criado.getNotaFiscalChave());
        assertTrue(criado.possuiNotaFiscal());
        assertNull(processamento.getArquivoChave());
        verify(processamentos).save(processamento);
    }

    @Test void naoAceitaNotaDoOcrDeOutroUsuario() {
        podeCriar(); dto.setOcrJobId("job-2");
        when(processamentos.findById("job-2"))
                .thenReturn(Optional.of(new OcrProcessamento("job-2", "outro@jaf.com", "notas/alheia.pdf")));

        assertThrows(NotFoundException.class, () -> service.criar(dto));
        verify(gastoRepository, never()).save(any());
    }

    @Test void naoReaproveitaNotaJaAnexada() {
        podeCriar(); dto.setOcrJobId("job-3");
        when(processamentos.findById("job-3"))
                .thenReturn(Optional.of(new OcrProcessamento("job-3", "autor@jaf.com", null)));

        assertThrows(Conflict.class, () -> service.criar(dto));
        verify(gastoRepository, never()).save(any());
    }

    @Test void urlDaNotaExigeEscopoDoGasto() {
        var admin = new Funcionario(); admin.setId(1L); admin.setCargoGlobal(com.jaf.application.enums.Cargo.ENGENHEIRO);
        when(funcionarioRepository.findByEmailIgnoreCase("eng@jaf.com")).thenReturn(Optional.of(admin));
        gasto.setNotaFiscalChave("notas/nf.pdf");
        when(gastoRepository.findById(3L)).thenReturn(Optional.of(gasto));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(false);

        assertThrows(Forbidden.class, () -> service.urlNotaFiscal(3L, "eng@jaf.com"));
        verifyNoInteractions(arquivos);
    }

    @Test void deletarRemoveANotaAnexada() {
        gasto.setNotaFiscalChave("notas/nf.pdf");
        when(gastoRepository.buscarParaAtualizar(3L)).thenReturn(Optional.of(gasto));

        service.deletar(3L);

        verify(gastoRepository).deleteById(3L);
        verify(arquivos).removerAposCommit("notas/nf.pdf");
    }
}