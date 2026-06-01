package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.dto.FuncionarioDto;
import com.jaf.application.dto.FuncionarioExternoDto;
import com.jaf.application.dto.FuncionarioListarDto;
import com.jaf.application.dto.FuncionarioResponseDto;
import com.jaf.application.dto.FuncionarioTokenDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.service.FuncionarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FuncionarioController.class)
class FuncionarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FuncionarioService funcionarioService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    private Funcionario funcionario;
    private FuncionarioDto funcionarioDto;
    private FuncionarioExternoDto funcionarioExternoDto;
    private FuncionarioResponseDto funcionarioResponseDto;
    private FuncionarioTokenDto funcionarioTokenDto;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");
        funcionario.setEmail("joao@test.com");
        funcionario.setCargoGlobal(Cargo.ADMIN);
        funcionario.setTipoFuncionario(TipoFuncionario.INTERNO);

        funcionarioDto = new FuncionarioDto();
        funcionarioDto.setNome("João Silva");
        funcionarioDto.setEmail("joao@test.com");
        funcionarioDto.setSenha("senha123");
        funcionarioDto.setCargo(Cargo.ADMIN);
        funcionarioDto.setTipoFuncionario(TipoFuncionario.INTERNO);

        funcionarioExternoDto = new FuncionarioExternoDto();
        funcionarioExternoDto.setNome("Pedro Externo");
        funcionarioExternoDto.setDocumento("12345678901");
        funcionarioExternoDto.setTipoFuncionario(TipoFuncionario.EXTERNO);

        funcionarioResponseDto = new FuncionarioResponseDto(funcionario);

        funcionarioTokenDto = new FuncionarioTokenDto();
        funcionarioTokenDto.setId(1L);
        funcionarioTokenDto.setEmail("joao@test.com");
        funcionarioTokenDto.setNome("João Silva");
        funcionarioTokenDto.setCargo(Cargo.ADMIN);
        funcionarioTokenDto.setToken("jwt-token");
    }

    @Test
    void testLogin_Sucesso() throws Exception {
        when(funcionarioService.buscarPorEmail("joao@test.com")).thenReturn(funcionario);
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(gerenciadorTokenJwt.generateToken(any())).thenReturn("jwt-token");

        mockMvc.perform(post("/funcionarios/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("joao@test.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.cargo").value("ADMIN"));

        verify(funcionarioService).buscarPorEmail("joao@test.com");
        verify(authenticationManager).authenticate(any());
        verify(gerenciadorTokenJwt).generateToken(any());
    }

    @Test
    void testLogin_CredenciaisInvalidas() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas"));

        mockMvc.perform(post("/funcionarios/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isUnauthorized());

        verify(authenticationManager).authenticate(any());
        verify(funcionarioService, never()).buscarPorEmail(anyString());
    }

    @Test
    @WithMockUser(authorities = "CRIAR_FUNCIONARIO")
    void testCriarFuncionario_Sucesso() throws Exception {
        when(funcionarioService.criar(any(FuncionarioDto.class))).thenReturn(funcionarioResponseDto);

        mockMvc.perform(post("/funcionarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@test.com"));

        verify(funcionarioService).criar(any(FuncionarioDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_FUNCIONARIO")
    void testCriarFuncionario_NomeDuplicado() throws Exception {
        when(funcionarioService.criar(any(FuncionarioDto.class)))
                .thenThrow(new Conflict("Usuário já existe."));

        mockMvc.perform(post("/funcionarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isConflict());

        verify(funcionarioService).criar(any(FuncionarioDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testListarFuncionarios_Sucesso() throws Exception {
        List<FuncionarioListarDto> funcionarios = Arrays.asList(
                new FuncionarioListarDto(1L, "João Silva", "joao@test.com", Cargo.ADMIN, TipoFuncionario.INTERNO, 0),
                new FuncionarioListarDto(2L, "Maria Santos", "maria@test.com", Cargo.GESTOR_OBRA, TipoFuncionario.INTERNO, 1)
        );
        when(funcionarioService.listarTodos()).thenReturn(funcionarios);

        mockMvc.perform(get("/funcionarios")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].nome").value("João Silva"))
                .andExpect(jsonPath("$[1].nome").value("Maria Santos"));

        verify(funcionarioService).listarTodos();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testListarFuncionarios_Vazia() throws Exception {
        when(funcionarioService.listarTodos()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/funcionarios")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(funcionarioService).listarTodos();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testBuscarFuncionarioPorId_Sucesso() throws Exception {
        when(funcionarioService.buscarPorId(1L)).thenReturn(funcionarioResponseDto);

        mockMvc.perform(get("/funcionarios/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.email").value("joao@test.com"));

        verify(funcionarioService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testBuscarFuncionarioPorId_NaoEncontrado() throws Exception {
        when(funcionarioService.buscarPorId(99L))
                .thenThrow(new NotFoundException("Usuário não encontrado."));

        mockMvc.perform(get("/funcionarios/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(funcionarioService).buscarPorId(99L);
    }

    @Test
    @WithMockUser(authorities = "EDITAR_FUNCIONARIO")
    void testAtualizarFuncionario_Sucesso() throws Exception {
        when(funcionarioService.atualizar(eq(1L), any(FuncionarioDto.class)))
                .thenReturn(funcionarioResponseDto);

        mockMvc.perform(put("/funcionarios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("João Silva"));

        verify(funcionarioService).atualizar(eq(1L), any(FuncionarioDto.class));
    }

    @Test
    @WithMockUser(authorities = "DELETAR_FUNCIONARIO")
    void testDeletarFuncionario_Sucesso() throws Exception {
        doNothing().when(funcionarioService).deletar(1L);

        mockMvc.perform(delete("/funcionarios/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(funcionarioService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "CRIAR_FUNCIONARIO")
    void testCriarFuncionarioExterno_Sucesso() throws Exception {
        Funcionario funcionarioExterno = new Funcionario();
        funcionarioExterno.setId(2L);
        funcionarioExterno.setNome("Pedro Externo");
        funcionarioExterno.setDocumento("12345678901");
        funcionarioExterno.setTipoFuncionario(TipoFuncionario.EXTERNO);

        FuncionarioResponseDto response = new FuncionarioResponseDto(funcionarioExterno);

        when(funcionarioService.criarExterno(any(FuncionarioExternoDto.class))).thenReturn(response);

        mockMvc.perform(post("/funcionarios/externo")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioExternoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Pedro Externo"))
                .andExpect(jsonPath("$.tipoFuncionario").value("EXTERNO"));

        verify(funcionarioService).criarExterno(any(FuncionarioExternoDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testListarExternos_Sucesso() throws Exception {
        List<FuncionarioListarDto> externos = Arrays.asList(
                new FuncionarioListarDto(2L, "Pedro Externo", null, null, TipoFuncionario.EXTERNO, 0)
        );
        when(funcionarioService.listarExternos()).thenReturn(externos);

        mockMvc.perform(get("/funcionarios/externo")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Pedro Externo"))
                .andExpect(jsonPath("$[0].tipoFuncionario").value("EXTERNO"));

        verify(funcionarioService).listarExternos();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testListarInternos_Sucesso() throws Exception {
        List<FuncionarioListarDto> internos = Arrays.asList(
                new FuncionarioListarDto(1L, "João Silva", "joao@test.com", Cargo.ADMIN, TipoFuncionario.INTERNO, 0)
        );
        when(funcionarioService.listarInternos()).thenReturn(internos);

        mockMvc.perform(get("/funcionarios/interno")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("João Silva"))
                .andExpect(jsonPath("$[0].tipoFuncionario").value("INTERNO"));

        verify(funcionarioService).listarInternos();
    }

    @Test
    void testCriarFuncionario_SemPermissao() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).criar(any(FuncionarioDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_FUNCIONARIOS")
    void testCriarFuncionario_ComPermissaoIncorreta() throws Exception {
        mockMvc.perform(post("/funcionarios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(funcionarioDto)))
                .andExpect(status().isForbidden());

        verify(funcionarioService, never()).criar(any(FuncionarioDto.class));
    }

    @Test
    void testLogin_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"email\":\"\", \"senha\":\"\"}";

        mockMvc.perform(post("/funcionarios/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(funcionarioService, never()).buscarPorEmail(anyString());
    }
}
