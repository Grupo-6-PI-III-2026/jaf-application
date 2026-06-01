package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.dto.AlocacaoObraDto;
import com.jaf.application.enums.CargoNaObra;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.service.AlocacaoObraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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

@WebMvcTest(AlocacaoObraController.class)
class AlocacaoObraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
        funcionario.setTipoFuncionario(TipoFuncionario.INTERNO);

        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Centro");

        alocacao = new AlocacaoObra();
        alocacao.setId(1L);
        alocacao.setFuncionario(funcionario);
        alocacao.setObra(obra);
        alocacao.setCargo(CargoNaObra.PEDREIRO);

        alocacaoDto = new AlocacaoObraDto();
        alocacaoDto.setFuncionarioId(1L);
        alocacaoDto.setObraId(1L);
        alocacaoDto.setCargoNaObra(CargoNaObra.PEDREIRO);
    }

    @Test
    @WithMockUser(authorities = "CRIAR_ALOCACAO")
    void testCriarAlocacao_Sucesso() throws Exception {
        when(alocacaoObraService.criar(any(AlocacaoObraDto.class))).thenReturn(alocacao);

        mockMvc.perform(post("/alocacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alocacaoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cargo").value("PEDREIRO"));

        verify(alocacaoObraService).criar(any(AlocacaoObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_ALOCACAO")
    void testCriarAlocacao_AlocacaoDuplicada() throws Exception {
        when(alocacaoObraService.criar(any(AlocacaoObraDto.class)))
                .thenThrow(new Conflict("Funcionario ja esta alocado nesta obra."));

        mockMvc.perform(post("/alocacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alocacaoDto)))
                .andExpect(status().isConflict());

        verify(alocacaoObraService).criar(any(AlocacaoObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoes_Sucesso() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorUsuario(anyString())).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].cargo").value("PEDREIRO"));

        verify(alocacaoObraService).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoes_FuncionarioExterno() throws Exception {
        when(alocacaoObraService.listarPorUsuario(anyString()))
                .thenThrow(new Forbidden("Funcionarios externos nao podem visualizar alocacoes."));

        mockMvc.perform(get("/alocacoes")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(alocacaoObraService).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesComFiltro_PorObra() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorObra(1L)).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes/filtro")
                        .with(csrf())
                        .param("obraId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorObra(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesComFiltro_PorFuncionario() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorFuncionario(1L)).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes/filtro")
                        .with(csrf())
                        .param("funcionarioId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorFuncionario(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesComFiltro_SemFiltro() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorUsuario(anyString())).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes/filtro")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesPorObra_Sucesso() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorObraComEscopo(eq(1L), anyString())).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes/obra/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorObraComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesPorObra_SemEscopo() throws Exception {
        when(alocacaoObraService.listarPorObraComEscopo(eq(1L), anyString()))
                .thenThrow(new Forbidden("Funcionario nao esta alocado nesta obra."));

        mockMvc.perform(get("/alocacoes/obra/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(alocacaoObraService).listarPorObraComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesPorFuncionario_Sucesso() throws Exception {
        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorFuncionarioComEscopo(eq(1L), anyString())).thenReturn(alocacoes);

        mockMvc.perform(get("/alocacoes/funcionario/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorFuncionarioComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesPorFuncionario_SemPermissao() throws Exception {
        when(alocacaoObraService.listarPorFuncionarioComEscopo(eq(1L), anyString()))
                .thenThrow(new Forbidden("Funcionario so pode ver suas proprias alocacoes."));

        mockMvc.perform(get("/alocacoes/funcionario/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(alocacaoObraService).listarPorFuncionarioComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testBuscarAlocacaoPorId_Sucesso() throws Exception {
        when(alocacaoObraService.buscarPorId(1L)).thenReturn(alocacao);

        mockMvc.perform(get("/alocacoes/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("PEDREIRO"));

        verify(alocacaoObraService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testBuscarAlocacaoPorId_NaoEncontrado() throws Exception {
        when(alocacaoObraService.buscarPorId(99L))
                .thenThrow(new NotFoundException("Alocacao nao encontrada."));

        mockMvc.perform(get("/alocacoes/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(alocacaoObraService).buscarPorId(99L);
    }

    @Test
    @WithMockUser(authorities = "EDITAR_ALOCACAO")
    void testAtualizarAlocacao_Sucesso() throws Exception {
        when(alocacaoObraService.atualizar(eq(1L), any(AlocacaoObraDto.class))).thenReturn(alocacao);

        mockMvc.perform(put("/alocacoes/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alocacaoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cargo").value("PEDREIRO"));

        verify(alocacaoObraService).atualizar(eq(1L), any(AlocacaoObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "DELETAR_ALOCACAO")
    void testDeletarAlocacao_Sucesso() throws Exception {
        doNothing().when(alocacaoObraService).deletar(1L);

        mockMvc.perform(delete("/alocacoes/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(alocacaoObraService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "DELETAR_ALOCACAO")
    void testDeletarAlocacao_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Alocacao nao encontrada.")).when(alocacaoObraService).deletar(99L);

        mockMvc.perform(delete("/alocacoes/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(alocacaoObraService).deletar(99L);
    }

    @Test
    void testCriarAlocacao_SemPermissao() throws Exception {
        mockMvc.perform(post("/alocacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alocacaoDto)))
                .andExpect(status().isForbidden());

        verify(alocacaoObraService, never()).criar(any(AlocacaoObraDto.class));
    }

    @Test
    void testListarAlocacoes_SemAutenticacao() throws Exception {
        mockMvc.perform(get("/alocacoes")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(alocacaoObraService, never()).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testCriarAlocacao_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"funcionarioId\":null}";

        mockMvc.perform(post("/alocacoes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(alocacaoObraService, never()).criar(any(AlocacaoObraDto.class));
    }
}
