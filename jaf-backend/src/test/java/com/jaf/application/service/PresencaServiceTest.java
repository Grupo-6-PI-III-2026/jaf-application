package com.jaf.application.service;

import com.jaf.application.dto.PresencaDto;
import com.jaf.application.dto.PresencaResponseDto;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.model.Presenca;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.ObraRepository;
import com.jaf.application.repository.PresencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresencaServiceTest {

    @Mock
    private PresencaRepository presencaRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private AlocacaoObraRepository alocacaoObraRepository;

    @InjectMocks
    private PresencaService presencaService;

    private Presenca presenca;
    private PresencaDto presencaDto;
    private Funcionario funcionario;
    private Obra obra;
    private AlocacaoObra alocacao;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");

        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Teste");

        alocacao = new AlocacaoObra();
        alocacao.setId(1L);
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);

        presenca = new Presenca();
        presenca.setId(1L);
        presenca.setFuncionario(funcionario);
        presenca.setObra(obra);
        presenca.setData(LocalDate.now());
        presenca.setPresente(true);
        presenca.setHorarioEntrada(LocalTime.of(8, 0));
        presenca.setHorarioSaida(LocalTime.of(17, 0));

        presencaDto = new PresencaDto();
        presencaDto.setFuncionarioId(1L);
        presencaDto.setObraId(1L);
        presencaDto.setData(LocalDate.now());
        presencaDto.setPresente(true);
        presencaDto.setHorarioEntrada(LocalTime.of(8, 0));
        presencaDto.setHorarioSaida(LocalTime.of(17, 0));
    }

    @Test
    void testCriarPresencaComSucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.findByFuncionarioIdAndObraId(1L, 1L)).thenReturn(List.of(alocacao));
        when(presencaRepository.existsByFuncionarioIdAndObraIdAndData(1L, 1L, LocalDate.now())).thenReturn(false);
        when(presencaRepository.save(any(Presenca.class))).thenReturn(presenca);

        PresencaResponseDto resultado = presencaService.criar(presencaDto);

        assertNotNull(resultado);
        assertTrue(resultado.getPresente());
        verify(presencaRepository).save(any(Presenca.class));
    }

    @Test
    void testCriarPresencaFuncionarioNaoAlocado() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.findByFuncionarioIdAndObraId(1L, 1L)).thenReturn(List.of());

        assertThrows(Conflict.class, () -> presencaService.criar(presencaDto));
        verify(presencaRepository, never()).save(any(Presenca.class));
    }

    @Test
    void testCriarPresencaComRegistroExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.findByFuncionarioIdAndObraId(1L, 1L)).thenReturn(List.of(alocacao));
        when(presencaRepository.existsByFuncionarioIdAndObraIdAndData(1L, 1L, LocalDate.now())).thenReturn(true);

        assertThrows(Conflict.class, () -> presencaService.criar(presencaDto));
        verify(presencaRepository, never()).save(any(Presenca.class));
    }

    @Test
    void testBuscarPorIdComSucesso() {
        when(presencaRepository.findById(1L)).thenReturn(Optional.of(presenca));

        PresencaResponseDto resultado = presencaService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
    }

    @Test
    void testBuscarPorIdNaoExistente() {
        when(presencaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> presencaService.buscarPorId(99L));
    }

    @Test
    void testAlternarPresenca() {
        when(presencaRepository.findById(1L)).thenReturn(Optional.of(presenca));
        when(presencaRepository.save(any(Presenca.class))).thenReturn(presenca);

        presencaService.alternarPresenca(1L);

        verify(presencaRepository).save(any(Presenca.class));
    }

    @Test
    void testAlternarPresencaNaoExistente() {
        when(presencaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> presencaService.alternarPresenca(99L));
        verify(presencaRepository, never()).save(any(Presenca.class));
    }

    @Test
    void testDeletarPresenca() {
        when(presencaRepository.existsById(1L)).thenReturn(true);

        presencaService.deletar(1L);

        verify(presencaRepository).deleteById(1L);
    }

    @Test
    void testDeletarPresencaNaoExistente() {
        when(presencaRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> presencaService.deletar(99L));
        verify(presencaRepository, never()).deleteById(anyLong());
    }
}
