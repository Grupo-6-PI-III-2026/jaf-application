package com.jaf.application.service;

import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Relatorio;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.RelatorioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    @Mock
    private RelatorioRepository relatorioRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @InjectMocks
    private RelatorioService relatorioService;

    private Relatorio relatorio;
    private RelatorioDto relatorioDto;
    private Funcionario funcionario;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");

        relatorio = new Relatorio();
        relatorio.setId(1L);
        relatorio.setTitulo("Relatório Mensal");
        relatorio.setDtEmissao(LocalDate.now());
        relatorio.setFuncionarioResponsavel(funcionario);

        relatorioDto = new RelatorioDto();
        relatorioDto.setTitulo("Relatório Mensal");
        relatorioDto.setDtEmissao(LocalDate.now());
        relatorioDto.setFuncionarioResponsavelId(1L);
    }

    @Test
    void testCriarRelatorioComSucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(relatorioRepository.save(any(Relatorio.class))).thenReturn(relatorio);

        Relatorio resultado = relatorioService.criar(relatorioDto);

        assertNotNull(resultado);
        assertEquals("Relatório Mensal", resultado.getTitulo());
        verify(relatorioRepository).save(any(Relatorio.class));
    }

    @Test
    void testCriarRelatorioFuncionarioNaoExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> relatorioService.criar(relatorioDto));
        verify(relatorioRepository, never()).save(any(Relatorio.class));
    }

    @Test
    void testListarRelatorios() {
        when(relatorioRepository.findAll()).thenReturn(List.of(relatorio));

        List<Relatorio> resultado = relatorioService.listar();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(relatorioRepository).findAll();
    }

    @Test
    void testBuscarPorIdComSucesso() {
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));

        Relatorio resultado = relatorioService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("Relatório Mensal", resultado.getTitulo());
    }

    @Test
    void testBuscarPorIdNaoExistente() {
        when(relatorioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> relatorioService.buscarPorId(99L));
    }

    @Test
    void testAtualizarRelatorioComSucesso() {
        when(relatorioRepository.findById(1L)).thenReturn(Optional.of(relatorio));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(relatorioRepository.save(any(Relatorio.class))).thenReturn(relatorio);

        Relatorio resultado = relatorioService.atualizar(1L, relatorioDto);

        assertNotNull(resultado);
        verify(relatorioRepository).save(any(Relatorio.class));
    }

    @Test
    void testDeletarRelatorio() {
        when(relatorioRepository.existsById(1L)).thenReturn(true);

        relatorioService.deletar(1L);

        verify(relatorioRepository).deleteById(1L);
    }

    @Test
    void testDeletarRelatorioNaoExistente() {
        when(relatorioRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> relatorioService.deletar(99L));
        verify(relatorioRepository, never()).deleteById(anyLong());
    }
}
