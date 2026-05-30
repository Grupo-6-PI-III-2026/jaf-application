package com.jaf.application.service;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlocacaoObraServiceTest {

    @Mock
    private AlocacaoObraRepository alocacaoObraRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ObraRepository obraRepository;

    @InjectMocks
    private AlocacaoObraService alocacaoObraService;

    private AlocacaoObra alocacao;
    private AlocacaoObraDto alocacaoDto;
    private Funcionario funcionario;
    private Obra obra;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");
        funcionario.setEmail("joao@email.com");

        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Teste");

        alocacao = new AlocacaoObra();
        alocacao.setId(1L);
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(Cargo.GESTOR_OBRA);

        alocacaoDto = new AlocacaoObraDto();
        alocacaoDto.setFuncionarioId(1L);
        alocacaoDto.setObraId(1L);
        alocacaoDto.setCargoNaObra(Cargo.GESTOR_OBRA);
    }

    @Test
    void testCriarAlocacaoComSucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(false);
        when(alocacaoObraRepository.save(any(AlocacaoObra.class))).thenReturn(alocacao);

        AlocacaoObra resultado = alocacaoObraService.criar(alocacaoDto);

        assertNotNull(resultado);
        assertEquals(Cargo.GESTOR_OBRA, resultado.getCargo());
        verify(alocacaoObraRepository).save(any(AlocacaoObra.class));
    }

    @Test
    void testCriarAlocacaoFuncionarioNaoExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> alocacaoObraService.criar(alocacaoDto));
        verify(alocacaoObraRepository, never()).save(any(AlocacaoObra.class));
    }

    @Test
    void testCriarAlocacaoObraNaoExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> alocacaoObraService.criar(alocacaoDto));
        verify(alocacaoObraRepository, never()).save(any(AlocacaoObra.class));
    }

    @Test
    void testCriarAlocacaoJaExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(true);

        assertThrows(Conflict.class, () -> alocacaoObraService.criar(alocacaoDto));
        verify(alocacaoObraRepository, never()).save(any(AlocacaoObra.class));
    }

    @Test
    void testListarPorObra() {
        when(alocacaoObraRepository.findByObraId(1L)).thenReturn(List.of(alocacao));

        List<AlocacaoObra> resultado = alocacaoObraService.listarPorObra(1L);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(alocacaoObraRepository).findByObraId(1L);
    }

    @Test
    void testDeletarAlocacao() {
        when(alocacaoObraRepository.existsById(1L)).thenReturn(true);

        alocacaoObraService.deletar(1L);

        verify(alocacaoObraRepository).deleteById(1L);
    }

    @Test
    void testDeletarAlocacaoNaoExistente() {
        when(alocacaoObraRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> alocacaoObraService.deletar(99L));
        verify(alocacaoObraRepository, never()).deleteById(anyLong());
    }
}
