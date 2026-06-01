package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.dto.RelatorioDto;
import com.jaf.application.exceptions.NoContent;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Relatorio;
import com.jaf.application.service.RelatorioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RelatorioController.class)
class RelatorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
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
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testCriarRelatorio_Sucesso() throws Exception {
        when(relatorioService.criar(any(RelatorioDto.class))).thenReturn(relatorio);

        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Relatório Mensal"));

        verify(relatorioService).criar(any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testCriarRelatorio_FuncionarioNaoEncontrado() throws Exception {
        when(relatorioService.criar(any(RelatorioDto.class)))
                .thenThrow(new NotFoundException("Funcionario nao encontrado"));

        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isNotFound());

        verify(relatorioService).criar(any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testListarRelatorios_Sucesso() throws Exception {
        List<Relatorio> relatorios = Arrays.asList(relatorio);
        when(relatorioService.listar()).thenReturn(relatorios);

        mockMvc.perform(get("/relatorios")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].titulo").value("Relatório Mensal"));

        verify(relatorioService).listar();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testListarRelatorios_Vazio() throws Exception {
        when(relatorioService.listar()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/relatorios")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(relatorioService).listar();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testListarRelatorios_SemConteudo() throws Exception {
        when(relatorioService.listar())
                .thenThrow(new NoContent("Lista de Relatórios vazia."));

        mockMvc.perform(get("/relatorios")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(relatorioService).listar();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testBuscarRelatorioPorId_Sucesso() throws Exception {
        when(relatorioService.buscarPorId(1L)).thenReturn(relatorio);

        mockMvc.perform(get("/relatorios/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Relatório Mensal"));

        verify(relatorioService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testBuscarRelatorioPorId_NaoEncontrado() throws Exception {
        when(relatorioService.buscarPorId(99L))
                .thenThrow(new NotFoundException("Relatorio nao encontrado"));

        mockMvc.perform(get("/relatorios/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(relatorioService).buscarPorId(99L);
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testAtualizarRelatorio_Sucesso() throws Exception {
        when(relatorioService.atualizar(eq(1L), any(RelatorioDto.class))).thenReturn(relatorio);

        mockMvc.perform(put("/relatorios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Relatório Mensal"));

        verify(relatorioService).atualizar(eq(1L), any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testAtualizarRelatorio_NaoEncontrado() throws Exception {
        when(relatorioService.atualizar(eq(99L), any(RelatorioDto.class)))
                .thenThrow(new NotFoundException("Relatorio nao encontrado"));

        mockMvc.perform(put("/relatorios/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isNotFound());

        verify(relatorioService).atualizar(eq(99L), any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testDeletarRelatorio_Sucesso() throws Exception {
        doNothing().when(relatorioService).deletar(1L);

        mockMvc.perform(delete("/relatorios/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(relatorioService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testDeletarRelatorio_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Relatorio nao encontrado")).when(relatorioService).deletar(99L);

        mockMvc.perform(delete("/relatorios/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(relatorioService).deletar(99L);
    }

    @Test
    void testCriarRelatorio_SemPermissao() throws Exception {
        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isForbidden());

        verify(relatorioService, never()).criar(any(RelatorioDto.class));
    }

    @Test
    void testListarRelatorios_SemAutenticacao() throws Exception {
        mockMvc.perform(get("/relatorios")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(relatorioService, never()).listar();
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testCriarRelatorio_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"titulo\":\"\", \"funcionarioResponsavelId\":null}";

        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(relatorioService, never()).criar(any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_RELATORIO")
    void testAtualizarRelatorio_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"titulo\":\"\"}";

        mockMvc.perform(put("/relatorios/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(relatorioService, never()).atualizar(anyLong(), any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testCriarRelatorio_DataFutura() throws Exception {
        relatorioDto.setDtEmissao(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isBadRequest());

        verify(relatorioService, never()).criar(any(RelatorioDto.class));
    }

    @Test
    @WithMockUser(authorities = "GERAR_RELATORIO")
    void testCriarRelatorio_TituloVazio() throws Exception {
        relatorioDto.setTitulo("");

        mockMvc.perform(post("/relatorios")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(relatorioDto)))
                .andExpect(status().isBadRequest());

        verify(relatorioService, never()).criar(any(RelatorioDto.class));
    }
}
