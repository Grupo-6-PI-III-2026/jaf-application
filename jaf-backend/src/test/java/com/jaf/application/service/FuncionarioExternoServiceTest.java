package com.jaf.application.service;

import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.dto.FuncionarioExternoDto;
import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.CargoNaObra;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Conflict;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FuncionarioExternoServiceTest {

    @Mock
    private FuncionarioRepository funcionarioRepository;

    @Mock
    private ObraRepository obraRepository;

    @Mock
    private AlocacaoObraRepository alocacaoObraRepository;

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private FuncionarioService funcionarioService;

    private AlocacaoObraService alocacaoObraService;
    private GastoService gastoService;

    private Funcionario funcionarioInterno;
    private Funcionario funcionarioExterno;
    private Obra obra;
    private AlocacaoObraDto alocacaoDto;
    private GastoDto gastoDto;

    @BeforeEach
    void setUp() {
        // Inicializa serviços manualmente
        alocacaoObraService = new AlocacaoObraService(alocacaoObraRepository, funcionarioRepository, obraRepository);
        gastoService = new GastoService(gastoRepository, funcionarioRepository, obraRepository, alocacaoObraRepository);

        // Funcionário Interno
        funcionarioInterno = new Funcionario();
        funcionarioInterno.setId(1L);
        funcionarioInterno.setNome("João Silva");
        funcionarioInterno.setEmail("joao@email.com");
        funcionarioInterno.setCargoGlobal(Cargo.GESTOR_OBRA);
        funcionarioInterno.setTipoFuncionario(TipoFuncionario.INTERNO);

        // Funcionário Externo
        funcionarioExterno = new Funcionario();
        funcionarioExterno.setId(2L);
        funcionarioExterno.setNome("Pedro Costa");
        funcionarioExterno.setDocumento("12345678901");
        funcionarioExterno.setTipoFuncionario(TipoFuncionario.EXTERNO);
        funcionarioExterno.setSenha(null);

        // Obra
        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Teste");

        // Alocacao DTO
        alocacaoDto = new AlocacaoObraDto();
        alocacaoDto.setFuncionarioId(2L); // Funcionário externo
        alocacaoDto.setObraId(1L);
        alocacaoDto.setCargoNaObra(CargoNaObra.AJUDANTE);

        // Gasto DTO
        gastoDto = new GastoDto();
        gastoDto.setDescricao("Material de construção");
        gastoDto.setCategoria("Material");
        gastoDto.setMetodoPagamento("Dinheiro");
        gastoDto.setEtapa("Fundação");
        gastoDto.setValor(new BigDecimal("500.00"));
        gastoDto.setDtGasto(LocalDate.now());
        gastoDto.setFuncionarioId(2L); // Funcionário externo
        gastoDto.setObraId(1L);
    }

    @Test
    void testCriarFuncionarioExternoComSucesso() {
        FuncionarioExternoDto dto = new FuncionarioExternoDto();
        dto.setNome("Pedro Costa");
        dto.setDocumento("12345678901");
        dto.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Pedro Costa")).thenReturn(false);
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioExterno);

        var resultado = funcionarioService.criarExterno(dto);

        assertNotNull(resultado);
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void testCriarFuncionarioExternoComNomeDuplicado() {
        FuncionarioExternoDto dto = new FuncionarioExternoDto();
        dto.setNome("Pedro Costa");
        dto.setDocumento("12345678901");
        dto.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Pedro Costa")).thenReturn(true);

        assertThrows(Conflict.class, () -> funcionarioService.criarExterno(dto));
        verify(funcionarioRepository, never()).save(any(Funcionario.class));
    }

    @Test
    void testAlocarFuncionarioExternoComSucesso() {
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(funcionarioExterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(false);

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setId(1L);
        alocacao.setFuncionario(funcionarioExterno);
        alocacao.setObra(obra);
        alocacao.setCargo(CargoNaObra.AJUDANTE);

        when(alocacaoObraRepository.save(any(AlocacaoObra.class))).thenReturn(alocacao);

        AlocacaoObra resultado = alocacaoObraService.criar(alocacaoDto);

        assertNotNull(resultado);
        assertEquals(CargoNaObra.AJUDANTE, resultado.getCargo());
        verify(alocacaoObraRepository).save(any(AlocacaoObra.class));
    }

    @Test
    void testCriarGastoParaFuncionarioExternoSemAlocacao() {
        // Funcionários externos não precisam estar alocados para ter gastos
        when(funcionarioRepository.findById(2L)).thenReturn(Optional.of(funcionarioExterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));

        Gasto gasto = new Gasto();
        gasto.setId(1L);
        gasto.setDescricao("Material de construção");
        gasto.setValor(new BigDecimal("500.00"));
        gasto.setFuncionario(funcionarioExterno);
        gasto.setObra(obra);

        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        Gasto resultado = gastoService.criar(gastoDto);

        assertNotNull(resultado);
        assertEquals("Material de construção", resultado.getDescricao());
        // Não verifica existsByFuncionarioIdAndObraId para funcionários externos
        verify(alocacaoObraRepository, never()).existsByFuncionarioIdAndObraId(anyLong(), anyLong());
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    void testCriarGastoParaFuncionarioInternoSemAlocacao() {
        // Funcionários internos PRECISAM estar alocados
        GastoDto gastoDtoInterno = new GastoDto();
        gastoDtoInterno.setDescricao("Material de construção");
        gastoDtoInterno.setCategoria("Material");
        gastoDtoInterno.setMetodoPagamento("Dinheiro");
        gastoDtoInterno.setEtapa("Fundação");
        gastoDtoInterno.setValor(new BigDecimal("500.00"));
        gastoDtoInterno.setDtGasto(LocalDate.now());
        gastoDtoInterno.setFuncionarioId(1L); // Funcionário interno
        gastoDtoInterno.setObraId(1L);

        when(funcionarioRepository.findById(1L)).thenReturn(Optional.of(funcionarioInterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(1L, 1L)).thenReturn(false);

        assertThrows(com.jaf.application.exceptions.Forbidden.class,
            () -> gastoService.criar(gastoDtoInterno));
        verify(alocacaoObraRepository).existsByFuncionarioIdAndObraId(1L, 1L);
        verify(gastoRepository, never()).save(any(Gasto.class));
    }

    @Test
    void testListarFuncionariosExternos() {
        when(funcionarioRepository.findByTipoFuncionario(TipoFuncionario.EXTERNO))
            .thenReturn(List.of(funcionarioExterno));
        when(alocacaoObraRepository.findByFuncionarioId(2L))
            .thenReturn(Collections.emptyList());

        var resultado = funcionarioService.listarExternos();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Pedro Costa", resultado.get(0).getNome());
        assertEquals(TipoFuncionario.EXTERNO, resultado.get(0).getTipoFuncionario());
    }

    @Test
    void testListarFuncionariosComQuantidadeAlocacoes() {
        when(funcionarioRepository.findAll()).thenReturn(List.of(funcionarioInterno, funcionarioExterno));
        when(alocacaoObraRepository.findByFuncionarioId(1L)).thenReturn(List.of(new AlocacaoObra()));
        when(alocacaoObraRepository.findByFuncionarioId(2L)).thenReturn(Collections.emptyList());

        var resultado = funcionarioService.listarTodos();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals(1, resultado.get(0).getQuantidadeAlocacoes()); // Interno com 1 alocação
        assertEquals(0, resultado.get(1).getQuantidadeAlocacoes()); // Externo sem alocação
    }

    @Test
    void testFuncionarioExternoNaoPodeVisualizarAlocacoes() {
        when(funcionarioRepository.findByEmailIgnoreCase("pedro@email.com"))
            .thenReturn(Optional.of(funcionarioExterno));

        assertThrows(com.jaf.application.exceptions.Forbidden.class,
            () -> alocacaoObraService.listarPorUsuario("pedro@email.com"));
    }

    @Test
    void testFuncionarioExternoNaoPodeVisualizarGastos() {
        when(funcionarioRepository.findByEmailIgnoreCase("pedro@email.com"))
            .thenReturn(Optional.of(funcionarioExterno));

        assertThrows(com.jaf.application.exceptions.Forbidden.class,
            () -> gastoService.listarPorUsuario("pedro@email.com", null));
    }
}
