package com.jaf.application.service;

import com.jaf.application.dto.ObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObraServiceTest {

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private AlocacaoObraRepository alocacaoObraRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @InjectMocks
    private ObraService obraService;

    private Obra obra;
    private ObraDto obraDto;
    private Funcionario funcionarioAdmin;
    private Funcionario funcionarioGestor;

    @BeforeEach
    void setUp() {
        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Teste");
        obra.setOrcamento("100000");
        obra.setStatus("EM_ANDAMENTO");
        obra.setDtInicio(LocalDate.of(2024, 1, 1));
        obra.setDtTerminoPrevisto(LocalDate.of(2024, 12, 31));

        obraDto = new ObraDto();
        obraDto.setTitulo("Obra Teste");
        obraDto.setOrcamento("100000");
        obraDto.setStatus("EM_ANDAMENTO");
        obraDto.setDtInicio(LocalDate.of(2024, 1, 1));
        obraDto.setDtTerminoPrevisto(LocalDate.of(2024, 12, 31));

        funcionarioAdmin = new Funcionario();
        funcionarioAdmin.setId(1L);
        funcionarioAdmin.setEmail("admin@email.com");
        funcionarioAdmin.setCargoGlobal(Cargo.ADMIN);

        funcionarioGestor = new Funcionario();
        funcionarioGestor.setId(2L);
        funcionarioGestor.setEmail("gestor@email.com");
        funcionarioGestor.setCargoGlobal(Cargo.GESTOR_OBRA);
    }

    @Test
    void testCriarObraComSucesso() {
        when(obraRepository.existsByTitulo(anyString())).thenReturn(false);
        when(obraRepository.save(any(Obra.class))).thenReturn(obra);

        Obra resultado = obraService.criar(obraDto);

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
        verify(obraRepository).save(any(Obra.class));
    }

    @Test
    void testCriarObraComTituloExistente() {
        when(obraRepository.existsByTitulo(anyString())).thenReturn(true);

        assertThrows(Conflict.class, () -> obraService.criar(obraDto));
        verify(obraRepository, never()).save(any(Obra.class));
    }

    @Test
    void testListarObrasComoAdmin() {
        when(funcionarioRepository.findByEmailIgnoreCase("admin@email.com")).thenReturn(Optional.of(funcionarioAdmin));
        when(obraRepository.findAll()).thenReturn(List.of(obra));

        List<Obra> resultado = obraService.listarPorUsuario("admin@email.com");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(obraRepository).findAll();
    }

    @Test
    void testListarObrasComoGestor() {
        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setObra(obra);
        alocacao.setFuncionario(funcionarioGestor);

        when(funcionarioRepository.findByEmailIgnoreCase("gestor@email.com")).thenReturn(Optional.of(funcionarioGestor));
        when(alocacaoObraRepository.findByFuncionarioId(2L)).thenReturn(List.of(alocacao));
        when(obraRepository.findByIdIn(any(Set.class))).thenReturn(List.of(obra));

        List<Obra> resultado = obraService.listarPorUsuario("gestor@email.com");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(obraRepository).findByIdIn(any(Set.class));
    }

    @Test
    void testBuscarPorIdComEscopoComoAdmin() {
        when(funcionarioRepository.findByEmailIgnoreCase("admin@email.com")).thenReturn(Optional.of(funcionarioAdmin));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));

        Obra resultado = obraService.buscarPorIdComEscopo(1L, "admin@email.com");

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
    }

    @Test
    void testBuscarPorIdComEscopoComoGestorNaoAlocado() {
        when(funcionarioRepository.findByEmailIgnoreCase("gestor@email.com")).thenReturn(Optional.of(funcionarioGestor));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(false);

        assertThrows(Forbidden.class, () -> obraService.buscarPorIdComEscopo(1L, "gestor@email.com"));
    }
}
