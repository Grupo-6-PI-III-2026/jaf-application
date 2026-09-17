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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private Funcionario funcionarioEngenheiro;

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
        funcionarioAdmin.setNome("Admin");
        funcionarioAdmin.setEmail("admin@teste.com");
        funcionarioAdmin.setCargoGlobal(Cargo.ADMIN);

        funcionarioEngenheiro = new Funcionario();
        funcionarioEngenheiro.setId(2L);
        funcionarioEngenheiro.setNome("Engenheiro");
        funcionarioEngenheiro.setEmail("engenheiro@teste.com");
        funcionarioEngenheiro.setCargoGlobal(Cargo.ENGENHEIRO);
    }

    @Test
    void criarObra_Sucesso() {
        when(obraRepository.existsByTitulo(anyString())).thenReturn(false);
        when(obraRepository.save(any(Obra.class))).thenReturn(obra);

        var resultado = obraService.criar(obraDto);

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
        verify(obraRepository, times(1)).save(any(Obra.class));
    }

    @Test
    void criarObra_TituloDuplicado_Conflict() {
        when(obraRepository.existsByTitulo(anyString())).thenReturn(true);

        assertThrows(Conflict.class, () -> obraService.criar(obraDto));
        verify(obraRepository, never()).save(any(Obra.class));
    }

    @Test
    void listarPorUsuario_Admin_RetornaTodasObras() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionarioAdmin));
        when(obraRepository.findAll()).thenReturn(List.of(obra));

        var resultado = obraService.listarPorUsuario("admin@teste.com");

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(obraRepository, times(1)).findAll();
    }

    @Test
    void listarPorUsuario_UsuarioNaoEncontrado_NotFoundException() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> obraService.listarPorUsuario("inexistente@teste.com"));
    }

    @Test
    void listarPorUsuario_EngenheiroSemAlocacoes_RetornaVazio() {
        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setObra(obra);
        alocacao.setFuncionario(funcionarioEngenheiro);

        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionarioEngenheiro));
        when(alocacaoObraRepository.findByFuncionarioId(anyLong())).thenReturn(Collections.emptyList());

        var resultado = obraService.listarPorUsuario("engenheiro@teste.com");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    void buscarPorIdComEscopo_Admin_AcessoPermitido() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionarioAdmin));
        when(obraRepository.findById(anyLong())).thenReturn(Optional.of(obra));

        var resultado = obraService.buscarPorIdComEscopo(1L, "admin@teste.com");

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
        verify(alocacaoObraRepository, never()).existsByFuncionarioIdAndObraId(anyLong(), anyLong());
    }

    @Test
    void buscarPorIdComEscopo_EngenheiroAlocado_AcessoPermitido() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionarioEngenheiro));
        when(obraRepository.findById(anyLong())).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(anyLong(), anyLong())).thenReturn(true);

        var resultado = obraService.buscarPorIdComEscopo(1L, "engenheiro@teste.com");

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
    }

    @Test
    void buscarPorIdComEscopo_EngenheiroNaoAlocado_Forbidden() {
        when(funcionarioRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.of(funcionarioEngenheiro));
        when(obraRepository.findById(anyLong())).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(anyLong(), anyLong())).thenReturn(false);

        assertThrows(Forbidden.class, () -> obraService.buscarPorIdComEscopo(1L, "engenheiro@teste.com"));
    }

    @Test
    void buscarPorId_Existente_Sucesso() {
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));

        var resultado = obraService.buscarPorId(1L);

        assertNotNull(resultado);
        assertEquals("Obra Teste", resultado.getTitulo());
    }

    @Test
    void buscarPorId_Inexistente_NotFoundException() {
        when(obraRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> obraService.buscarPorId(999L));
    }

    @Test
    void atualizarObra_Sucesso() {
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(obraRepository.save(any(Obra.class))).thenReturn(obra);

        var resultado = obraService.atualizar(1L, obraDto);

        assertNotNull(resultado);
        verify(obraRepository, times(1)).save(any(Obra.class));
    }

    @Test
    void atualizarObra_Inexistente_NotFoundException() {
        when(obraRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> obraService.atualizar(999L, obraDto));
        verify(obraRepository, never()).save(any(Obra.class));
    }

    @Test
    void deletarObra_Sucesso() {
        when(obraRepository.existsById(1L)).thenReturn(true);
        doNothing().when(obraRepository).deleteById(anyLong());

        obraService.deletar(1L);

        verify(obraRepository, times(1)).deleteById(1L);
    }

    @Test
    void deletarObra_Inexistente_NotFoundException() {
        when(obraRepository.existsById(999L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> obraService.deletar(999L));
        verify(obraRepository, never()).deleteById(anyLong());
    }
}