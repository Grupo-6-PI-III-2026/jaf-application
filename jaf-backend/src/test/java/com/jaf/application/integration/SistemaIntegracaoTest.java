package com.jaf.application.integration;

import com.jaf.application.dto.*;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.CargoNaObra;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.*;
import com.jaf.application.repository.AlocacaoObraRepository;
import com.jaf.application.repository.FuncionarioRepository;
import com.jaf.application.repository.GastoRepository;
import com.jaf.application.repository.ObraRepository;
import com.jaf.application.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste de integração do sistema como um todo
 * Testa o fluxo completo de criação de funcionários, alocação e gastos
 * com foco em funcionários externos e controle de acesso
 */
@ExtendWith(MockitoExtension.class)
class SistemaIntegracaoTest {

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

    @InjectMocks
    private AlocacaoObraService alocacaoObraService;

    @InjectMocks
    private GastoService gastoService;

    // Funcionários para teste
    private Funcionario admin;
    private Funcionario gestorObra;
    private Funcionario funcionarioInterno;
    private Funcionario funcionarioExterno;

    // Obras para teste
    private Obra obra1;
    private Obra obra2;

    @BeforeEach
    void setUp() {
        // Setup Admin
        admin = new Funcionario();
        admin.setId(1L);
        admin.setNome("Admin");
        admin.setEmail("admin@jaf.com");
        admin.setCargoGlobal(Cargo.ADMIN);
        admin.setTipoFuncionario(TipoFuncionario.INTERNO);

        // Setup Gestor de Obra
        gestorObra = new Funcionario();
        gestorObra.setId(2L);
        gestorObra.setNome("Gestor Obra");
        gestorObra.setEmail("gestor@jaf.com");
        gestorObra.setCargoGlobal(Cargo.GESTOR_OBRA);
        gestorObra.setTipoFuncionario(TipoFuncionario.INTERNO);

        // Setup Funcionário Interno
        funcionarioInterno = new Funcionario();
        funcionarioInterno.setId(3L);
        funcionarioInterno.setNome("João Silva");
        funcionarioInterno.setEmail("joao@jaf.com");
        funcionarioInterno.setCargoGlobal(Cargo.OPERADOR_LANCAMENTO);
        funcionarioInterno.setTipoFuncionario(TipoFuncionario.INTERNO);

        // Setup Funcionário Externo
        funcionarioExterno = new Funcionario();
        funcionarioExterno.setId(4L);
        funcionarioExterno.setNome("Pedro Costa");
        funcionarioExterno.setDocumento("12345678901");
        funcionarioExterno.setTipoFuncionario(TipoFuncionario.EXTERNO);
        funcionarioExterno.setSenha(null);

        // Setup Obras
        obra1 = new Obra();
        obra1.setId(1L);
        obra1.setTitulo("Obra Centro");
        obra1.setStatus("EM_ANDAMENTO");

        obra2 = new Obra();
        obra2.setId(2L);
        obra2.setTitulo("Obra Norte");
        obra2.setStatus("EM_ANDAMENTO");
    }

    @Test
    void testFluxoCompleto_AdminCriaFuncionarioExterno_Aloca_RegistraGastos() {
        // 1. Admin cria funcionário externo
        FuncionarioExternoDto externoDto = new FuncionarioExternoDto();
        externoDto.setNome("Pedro Costa");
        externoDto.setDocumento("12345678901");
        externoDto.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Pedro Costa")).thenReturn(false);
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioExterno);

        FuncionarioResponseDto resultadoExterno = funcionarioService.criarExterno(externoDto);

        assertNotNull(resultadoExterno);
        assertEquals("Pedro Costa", resultadoExterno.getNome());
        assertEquals(TipoFuncionario.EXTERNO, resultadoExterno.getTipoFuncionario());
        assertNull(resultadoExterno.getCargo()); // Externos não têm cargo global
        verify(funcionarioRepository).save(any(Funcionario.class));

        // 2. Admin aloca funcionário externo em obra
        AlocacaoObraDto alocacaoDto = new AlocacaoObraDto();
        alocacaoDto.setFuncionarioId(4L);
        alocacaoDto.setObraId(1L);
        alocacaoDto.setCargoNaObra(CargoNaObra.AJUDANTE);

        when(funcionarioRepository.findById(4L)).thenReturn(Optional.of(funcionarioExterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra1));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(4L, 1L)).thenReturn(false);

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setId(1L);
        alocacao.setFuncionario(funcionarioExterno);
        alocacao.setObra(obra1);
        alocacao.setCargo(CargoNaObra.AJUDANTE);

        when(alocacaoObraRepository.save(any(AlocacaoObra.class))).thenReturn(alocacao);

        AlocacaoObra resultadoAlocacao = alocacaoObraService.criar(alocacaoDto);

        assertNotNull(resultadoAlocacao);
        assertEquals(CargoNaObra.AJUDANTE, resultadoAlocacao.getCargo());
        verify(alocacaoObraRepository).save(any(AlocacaoObra.class));

        // 3. Admin registra gastos para funcionário externo (sem necessidade de alocação)
        GastoDto gastoDto = new GastoDto();
        gastoDto.setDescricao("Diária externo");
        gastoDto.setCategoria("Mão de obra");
        gastoDto.setMetodoPagamento("Dinheiro");
        gastoDto.setEtapa("Construção");
        gastoDto.setValor(new BigDecimal("150.00"));
        gastoDto.setDtGasto(LocalDate.now());
        gastoDto.setFuncionarioId(4L);
        gastoDto.setObraId(1L);

        when(funcionarioRepository.findById(4L)).thenReturn(Optional.of(funcionarioExterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra1));

        Gasto gasto = new Gasto();
        gasto.setId(1L);
        gasto.setDescricao("Diária externo");
        gasto.setValor(new BigDecimal("150.00"));
        gasto.setFuncionario(funcionarioExterno);
        gasto.setObra(obra1);

        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        Gasto resultadoGasto = gastoService.criar(gastoDto);

        assertNotNull(resultadoGasto);
        assertEquals("Diária externo", resultadoGasto.getDescricao());
        assertEquals(new BigDecimal("150.00"), resultadoGasto.getValor());

        // Verifica que NÃO verificou alocação (por ser funcionário externo)
        verify(alocacaoObraRepository, never()).existsByFuncionarioIdAndObraId(4L, 1L);
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    void testFluxoFuncionarioInterno_RequerAlocacaoParaGastos() {
        // 1. Cria funcionário interno
        FuncionarioDto internoDto = new FuncionarioDto();
        internoDto.setNome("João Silva");
        internoDto.setEmail("joao@jaf.com");
        internoDto.setSenha("Senha123");
        internoDto.setCargo(Cargo.OPERADOR_LANCAMENTO);

        when(funcionarioRepository.existsByNome("João Silva")).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase("joao@jaf.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Senha123")).thenReturn("encoded_password");
        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(funcionarioInterno);

        FuncionarioResponseDto resultadoInterno = funcionarioService.criar(internoDto);

        assertNotNull(resultadoInterno);
        assertEquals("João Silva", resultadoInterno.getNome());
        assertEquals(Cargo.OPERADOR_LANCAMENTO, resultadoInterno.getCargo());
        assertEquals(TipoFuncionario.INTERNO, resultadoInterno.getTipoFuncionario());

        // 2. Tenta registrar gasto SEM alocação (deve falhar)
        GastoDto gastoDto = new GastoDto();
        gastoDto.setDescricao("Material");
        gastoDto.setCategoria("Material");
        gastoDto.setMetodoPagamento("Dinheiro");
        gastoDto.setEtapa("Fundação");
        gastoDto.setValor(new BigDecimal("500.00"));
        gastoDto.setDtGasto(LocalDate.now());
        gastoDto.setFuncionarioId(3L); // Funcionário interno
        gastoDto.setObraId(1L);

        when(funcionarioRepository.findById(3L)).thenReturn(Optional.of(funcionarioInterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra1));
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(3L, 1L)).thenReturn(false);

        // Deve lançar Forbidden porque interno não está alocado
        assertThrows(Forbidden.class, () -> gastoService.criar(gastoDto));

        // Verifica que verificou alocação (por ser funcionário interno)
        verify(alocacaoObraRepository).existsByFuncionarioIdAndObraId(3L, 1L);
        verify(gastoRepository, never()).save(any(Gasto.class));

        // 3. Aloca funcionário interno
        AlocacaoObraDto alocacaoDto = new AlocacaoObraDto();
        alocacaoDto.setFuncionarioId(3L);
        alocacaoDto.setObraId(1L);
        alocacaoDto.setCargoNaObra(CargoNaObra.PEDREIRO);

        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(3L, 1L)).thenReturn(false);

        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setId(2L);
        alocacao.setFuncionario(funcionarioInterno);
        alocacao.setObra(obra1);
        alocacao.setCargo(CargoNaObra.PEDREIRO);

        when(alocacaoObraRepository.save(any(AlocacaoObra.class))).thenReturn(alocacao);

        AlocacaoObra resultadoAlocacao = alocacaoObraService.criar(alocacaoDto);

        assertNotNull(resultadoAlocacao);

        // 4. Agora tenta registrar gasto COM alocação (deve funcionar)
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(3L, 1L)).thenReturn(true);

        Gasto gasto = new Gasto();
        gasto.setId(2L);
        gasto.setDescricao("Material");
        gasto.setValor(new BigDecimal("500.00"));
        gasto.setFuncionario(funcionarioInterno);
        gasto.setObra(obra1);

        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto);

        Gasto resultadoGasto = gastoService.criar(gastoDto);

        assertNotNull(resultadoGasto);
        assertEquals("Material", resultadoGasto.getDescricao());
        verify(gastoRepository).save(any(Gasto.class));
    }

    @Test
    void testControleAcesso_AdminVsGestorVsExterno() {
        // 1. Admin pode ver tudo
        when(funcionarioRepository.findByEmailIgnoreCase("admin@jaf.com")).thenReturn(Optional.of(admin));
        when(alocacaoObraRepository.findAll()).thenReturn(Arrays.asList(new AlocacaoObra()));

        List<AlocacaoObra> alocacoesAdmin = alocacaoObraService.listarPorUsuario("admin@jaf.com");

        assertNotNull(alocacoesAdmin);
        verify(alocacaoObraRepository).findAll();

        // 2. Gestor de obra pode ver suas alocações
        AlocacaoObra alocacaoGestor = new AlocacaoObra();
        alocacaoGestor.setFuncionario(gestorObra);
        alocacaoGestor.setObra(obra1);

        when(funcionarioRepository.findByEmailIgnoreCase("gestor@jaf.com")).thenReturn(Optional.of(gestorObra));
        when(alocacaoObraRepository.findByFuncionarioId(2L)).thenReturn(Arrays.asList(alocacaoGestor));

        List<AlocacaoObra> alocacoesGestor = alocacaoObraService.listarPorUsuario("gestor@jaf.com");

        assertNotNull(alocacoesGestor);
        verify(alocacaoObraRepository).findByFuncionarioId(2L);

        // 3. Funcionário externo NÃO pode ver alocações
        when(funcionarioRepository.findByEmailIgnoreCase("pedro@externo.com")).thenReturn(Optional.of(funcionarioExterno));

        assertThrows(Forbidden.class, () -> alocacaoObraService.listarPorUsuario("pedro@externo.com"));

        // 4. Funcionário externo NÃO pode ver gastos
        assertThrows(Forbidden.class, () -> gastoService.listarPorUsuario("pedro@externo.com", null));
    }

    @Test
    void testListagemFuncionarios_ComQuantidadeAlocacoes() {
        // Setup múltiplas alocações
        AlocacaoObra alocacao1 = new AlocacaoObra();
        alocacao1.setFuncionario(funcionarioInterno);
        alocacao1.setObra(obra1);

        AlocacaoObra alocacao2 = new AlocacaoObra();
        alocacao2.setFuncionario(funcionarioInterno);
        alocacao2.setObra(obra2);

        when(funcionarioRepository.findAll()).thenReturn(Arrays.asList(admin, gestorObra, funcionarioInterno, funcionarioExterno));
        when(alocacaoObraRepository.findByFuncionarioId(1L)).thenReturn(Collections.emptyList()); // Admin sem alocações
        when(alocacaoObraRepository.findByFuncionarioId(2L)).thenReturn(Collections.singletonList(alocacao1)); // Gestor com 1 alocação
        when(alocacaoObraRepository.findByFuncionarioId(3L)).thenReturn(Arrays.asList(alocacao1, alocacao2)); // Interno com 2 alocações
        when(alocacaoObraRepository.findByFuncionarioId(4L)).thenReturn(Collections.emptyList()); // Externo sem alocações

        List<FuncionarioListarDto> funcionarios = funcionarioService.listarTodos();

        assertNotNull(funcionarios);
        assertEquals(4, funcionarios.size());

        // Verifica quantidades de alocações
        assertEquals(0, funcionarios.get(0).getQuantidadeAlocacoes()); // Admin
        assertEquals(1, funcionarios.get(1).getQuantidadeAlocacoes()); // Gestor
        assertEquals(2, funcionarios.get(2).getQuantidadeAlocacoes()); // Interno
        assertEquals(0, funcionarios.get(3).getQuantidadeAlocacoes()); // Externo

        // Verifica tipos
        assertEquals(TipoFuncionario.INTERNO, funcionarios.get(0).getTipoFuncionario());
        assertEquals(TipoFuncionario.INTERNO, funcionarios.get(1).getTipoFuncionario());
        assertEquals(TipoFuncionario.INTERNO, funcionarios.get(2).getTipoFuncionario());
        assertEquals(TipoFuncionario.EXTERNO, funcionarios.get(3).getTipoFuncionario());
    }

    @Test
    void testEscopoPorObra_FuncionarioSoPodeVerPropriaObra() {
        // Setup alocações
        AlocacaoObra alocacaoObra1 = new AlocacaoObra();
        alocacaoObra1.setId(1L);
        alocacaoObra1.setFuncionario(gestorObra);
        alocacaoObra1.setObra(obra1);

        AlocacaoObra alocacaoObra2 = new AlocacaoObra();
        alocacaoObra2.setId(2L);
        alocacaoObra2.setFuncionario(gestorObra);
        alocacaoObra2.setObra(obra2);

        when(funcionarioRepository.findByEmailIgnoreCase("gestor@jaf.com")).thenReturn(Optional.of(gestorObra));

        // 1. Gestor está alocado apenas na obra1
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 1L)).thenReturn(true);
        when(alocacaoObraRepository.existsByFuncionarioIdAndObraId(2L, 2L)).thenReturn(false);
        when(alocacaoObraRepository.findByObraId(1L)).thenReturn(Arrays.asList(alocacaoObra1));

        // Pode ver obra onde está alocado
        List<AlocacaoObra> alocacoesObra1 = alocacaoObraService.listarPorObraComEscopo(1L, "gestor@jaf.com");
        assertNotNull(alocacoesObra1);

        // 2. Não pode ver obra onde NÃO está alocado
        assertThrows(Forbidden.class, () -> alocacaoObraService.listarPorObraComEscopo(2L, "gestor@jaf.com"));

        // 3. Admin pode ver qualquer obra
        when(funcionarioRepository.findByEmailIgnoreCase("admin@jaf.com")).thenReturn(Optional.of(admin));
        when(alocacaoObraRepository.findByObraId(2L)).thenReturn(Arrays.asList(alocacaoObra2));

        List<AlocacaoObra> alocacoesObra2Admin = alocacaoObraService.listarPorObraComEscopo(2L, "admin@jaf.com");
        assertNotNull(alocacoesObra2Admin);
    }

    @Test
    void testValidacaoCriacaoFuncionarioExterno() {
        // 1. Não pode criar com nome duplicado
        FuncionarioExternoDto dtoDuplicado = new FuncionarioExternoDto();
        dtoDuplicado.setNome("Pedro Costa");
        dtoDuplicado.setDocumento("12345678901");
        dtoDuplicado.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Pedro Costa")).thenReturn(true);

        assertThrows(Conflict.class, () -> funcionarioService.criarExterno(dtoDuplicado));

        // 2. Não pode criar com email duplicado (se fornecido)
        FuncionarioExternoDto dtoEmailDuplicado = new FuncionarioExternoDto();
        dtoEmailDuplicado.setNome("Novo Externo");
        dtoEmailDuplicado.setEmail("joao@jaf.com");
        dtoEmailDuplicado.setDocumento("98765432100");
        dtoEmailDuplicado.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Novo Externo")).thenReturn(false);
        when(funcionarioRepository.findByEmailIgnoreCase("joao@jaf.com")).thenReturn(Optional.of(funcionarioInterno));

        assertThrows(Conflict.class, () -> funcionarioService.criarExterno(dtoEmailDuplicado));

        // 3. Criação com sucesso (sem email)
        FuncionarioExternoDto dtoSucesso = new FuncionarioExternoDto();
        dtoSucesso.setNome("Maria Externa");
        dtoSucesso.setDocumento("11122233344");
        dtoSucesso.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.existsByNome("Maria Externa")).thenReturn(false);

        Funcionario novaExterna = new Funcionario();
        novaExterna.setId(5L);
        novaExterna.setNome("Maria Externa");
        novaExterna.setDocumento("11122233344");
        novaExterna.setTipoFuncionario(TipoFuncionario.EXTERNO);

        when(funcionarioRepository.save(any(Funcionario.class))).thenReturn(novaExterna);

        FuncionarioResponseDto resultado = funcionarioService.criarExterno(dtoSucesso);

        assertNotNull(resultado);
        assertEquals("Maria Externa", resultado.getNome());
        verify(funcionarioRepository).save(any(Funcionario.class));
    }

    @Test
    void testGastoFuncionarioExterno_MultiplasObrasSemAlocacao() {
        // Funcionário externo pode ter gastos em múltiplas obras sem estar alocado
        GastoDto gastoObra1 = new GastoDto();
        gastoObra1.setDescricao("Diária obra 1");
        gastoObra1.setCategoria("Mão de obra");
        gastoObra1.setMetodoPagamento("Dinheiro");
        gastoObra1.setValor(new BigDecimal("150.00"));
        gastoObra1.setDtGasto(LocalDate.now());
        gastoObra1.setFuncionarioId(4L);
        gastoObra1.setObraId(1L);

        GastoDto gastoObra2 = new GastoDto();
        gastoObra2.setDescricao("Diária obra 2");
        gastoObra2.setCategoria("Mão de obra");
        gastoObra2.setMetodoPagamento("Dinheiro");
        gastoObra2.setValor(new BigDecimal("180.00"));
        gastoObra2.setDtGasto(LocalDate.now());
        gastoObra2.setFuncionarioId(4L);
        gastoObra2.setObraId(2L);

        when(funcionarioRepository.findById(4L)).thenReturn(Optional.of(funcionarioExterno));
        when(obraRepository.findById(1L)).thenReturn(Optional.of(obra1));
        when(obraRepository.findById(2L)).thenReturn(Optional.of(obra2));

        Gasto gasto1 = new Gasto();
        gasto1.setId(1L);
        gasto1.setValor(new BigDecimal("150.00"));
        gasto1.setFuncionario(funcionarioExterno);
        gasto1.setObra(obra1);

        Gasto gasto2 = new Gasto();
        gasto2.setId(2L);
        gasto2.setValor(new BigDecimal("180.00"));
        gasto2.setFuncionario(funcionarioExterno);
        gasto2.setObra(obra2);

        when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto1).thenReturn(gasto2);

        // Cria gasto na obra 1 (sem alocação)
        Gasto resultado1 = gastoService.criar(gastoObra1);
        assertNotNull(resultado1);

        // Cria gasto na obra 2 (sem alocação)
        Gasto resultado2 = gastoService.criar(gastoObra2);
        assertNotNull(resultado2);

        // Verifica que nunca verificou alocação
        verify(alocacaoObraRepository, never()).existsByFuncionarioIdAndObraId(anyLong(), anyLong());
        verify(gastoRepository, times(2)).save(any(Gasto.class));
    }
}
