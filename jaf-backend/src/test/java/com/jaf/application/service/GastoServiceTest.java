package com.jaf.application.service;

import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GastoServiceTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private AlocacaoObraRepository alocacaoObraRepository;

    @InjectMocks
    private GastoService gastoService;

    private Gasto gasto;
    private GastoDto gastoDto;
    private Funcionario funcionario;
    private Obra obra;
    private AlocacaoObra alocacao;

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

        gasto = new Gasto();
        gasto.setId(1L);
        gasto.setDescricao("Material de construção");
        gasto.setCategoria("Material");
        gasto.setMetodoPagamento("Cartão");
        gasto.setEtapa("Fundação");
        gasto.setValor(new BigDecimal("1000.00"));
        gasto.setDtGasto(LocalDate.now());
        gasto.setFuncionario(funcionario);
        gasto.setObra(obra);

        gastoDto = new GastoDto();
        gastoDto.setDescricao("Material de construção");
        gastoDto.setCategoria("Material");
        gastoDto.setMetodoPagamento("Cartão");
        gastoDto.setEtapa("Fundação");
        gastoDto.setValor(new BigDecimal("1000.00"));
        gastoDto.setDtGasto(LocalDate.now());
        gastoDto.setFuncionarioId(1L);
        gastoDto.setObraId(1L);
    }

    @Test
    void testCriarGastoComSucesso() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(true);
        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        Gasto resultado = gastoService.criar(gastoDto);

        assertNotNull(resultado);
        assertEquals("Material de construção", resultado.getDescricao());
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    void testCriarGastoFuncionarioNaoAlocado() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(false);

        assertThrows(Forbidden.class, () -> gastoService.criar(gastoDto));
        verify(gastoRepository, never()).save(any(Gasto.class));
    }

    @Test
    void testCriarGastoFuncionarioNaoExistente() {
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> gastoService.criar(gastoDto));
        verify(gastoRepository, never()).save(any(Gasto.class));
    }

    @Test
    void testListarGastosComoAdmin() {
        funcionario.setCargoGlobal(Cargo.ADMIN);
        when(funcionarioRepository.findByEmailIgnoreCase("admin@email.com")).thenReturn(Optional.of(funcionario));
        when(gastoRepository.findAll()).thenReturn(List.of(gasto));

        List<Gasto> resultado = gastoService.listarPorUsuario("admin@email.com", null);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        verify(gastoRepository).findAll();
    }

    @Test
    void testBuscarPorIdComEscopoSucesso() {
        funcionario.setCargoGlobal(Cargo.ADMIN);
        when(funcionarioRepository.findByEmailIgnoreCase("admin@email.com")).thenReturn(Optional.of(funcionario));
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));

        Gasto resultado = gastoService.buscarPorIdComEscopo(1L, "admin@email.com");

        assertNotNull(resultado);
        assertEquals("Material de construção", resultado.getDescricao());
    }

    @Test
    void testAtualizarGastoComSucesso() {
        when(gastoRepository.findById(1L)).thenReturn(Optional.of(gasto));
        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionario));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(true);
        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        Gasto resultado = gastoService.atualizar(1L, gastoDto);

        assertNotNull(resultado);
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    void testDeletarGasto() {
        when(gastoRepository.existsById(1L)).thenReturn(true);

        gastoService.deletar(1L);

        verify(gastoRepository).deleteById(1L);
    }

    @Test
    void testDeletarGastoNaoExistente() {
        when(gastoRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> gastoService.deletar(99L));
        verify(gastoRepository, never()).deleteById(anyLong());
    }
}
