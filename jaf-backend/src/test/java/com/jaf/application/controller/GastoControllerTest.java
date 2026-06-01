package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.dto.GastoDto;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.service.GastoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(GastoController.class)
class GastoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GastoService gastoService;

    private Gasto gasto;
    private GastoDto gastoDto;
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

        gasto = new Gasto();
        gasto.setId(1L);
        gasto.setDescricao("Material de construção");
        gasto.setCategoria("Material");
        gasto.setMetodoPagamento("Dinheiro");
        gasto.setEtapa("Fundação");
        gasto.setValor(new BigDecimal("500.00"));
        gasto.setDtGasto(LocalDate.now());
        gasto.setFuncionario(funcionario);
        gasto.setObra(obra);

        gastoDto = new GastoDto();
        gastoDto.setDescricao("Material de construção");
        gastoDto.setCategoria("Material");
        gastoDto.setMetodoPagamento("Dinheiro");
        gastoDto.setEtapa("Fundação");
        gastoDto.setValor(new BigDecimal("500.00"));
        gastoDto.setDtGasto(LocalDate.now());
        gastoDto.setFuncionarioId(1L);
        gastoDto.setObraId(1L);
    }

    @Test
    @WithMockUser(authorities = "CRIAR_GASTO")
    void testCriarGasto_Sucesso() throws Exception {
        when(gastoService.criar(any(GastoDto.class))).thenReturn(gasto);

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Material de construção"))
                .andExpect(jsonPath("$.valor").value(500.00));

        verify(gastoService).criar(any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_GASTO")
    void testCriarGasto_SemAlocacao() throws Exception {
        when(gastoService.criar(any(GastoDto.class)))
                .thenThrow(new Forbidden("Funcionario nao esta alocado nesta obra e nao pode registrar gastos."));

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isForbidden());

        verify(gastoService).criar(any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_GASTO")
    void testCriarGasto_FuncionarioNaoEncontrado() throws Exception {
        when(gastoService.criar(any(GastoDto.class)))
                .thenThrow(new NotFoundException("Usuario nao encontrado."));

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isNotFound());

        verify(gastoService).criar(any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testListarGastos_Sucesso() throws Exception {
        List<Gasto> gastos = Arrays.asList(gasto);
        when(gastoService.listarPorUsuario(anyString(), isNull())).thenReturn(gastos);

        mockMvc.perform(get("/gastos")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].descricao").value("Material de construção"));

        verify(gastoService).listarPorUsuario(anyString(), isNull());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testListarGastos_PorObra() throws Exception {
        List<Gasto> gastos = Arrays.asList(gasto);
        when(gastoService.listarPorUsuario(anyString(), eq(1L))).thenReturn(gastos);

        mockMvc.perform(get("/gastos")
                        .with(csrf())
                        .param("obraId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(gastoService).listarPorUsuario(anyString(), eq(1L));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testListarGastos_FuncionarioExterno() throws Exception {
        when(gastoService.listarPorUsuario(anyString(), isNull()))
                .thenThrow(new Forbidden("Funcionarios externos nao podem visualizar gastos."));

        mockMvc.perform(get("/gastos")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(gastoService).listarPorUsuario(anyString(), isNull());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testListarGastos_Vazio() throws Exception {
        when(gastoService.listarPorUsuario(anyString(), isNull())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/gastos")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(gastoService).listarPorUsuario(anyString(), isNull());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testBuscarGastoPorId_Sucesso() throws Exception {
        when(gastoService.buscarPorIdComEscopo(eq(1L), anyString())).thenReturn(gasto);

        mockMvc.perform(get("/gastos/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Material de construção"));

        verify(gastoService).buscarPorIdComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testBuscarGastoPorId_SemEscopo() throws Exception {
        when(gastoService.buscarPorIdComEscopo(eq(1L), anyString()))
                .thenThrow(new Forbidden("Funcionario nao possui acesso a este gasto."));

        mockMvc.perform(get("/gastos/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(gastoService).buscarPorIdComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testBuscarGastoPorId_NaoEncontrado() throws Exception {
        when(gastoService.buscarPorIdComEscopo(eq(99L), anyString()))
                .thenThrow(new NotFoundException("Gasto nao encontrado."));

        mockMvc.perform(get("/gastos/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(gastoService).buscarPorIdComEscopo(eq(99L), anyString());
    }

    @Test
    @WithMockUser(authorities = "EDITAR_GASTO")
    void testAtualizarGasto_Sucesso() throws Exception {
        when(gastoService.atualizar(eq(1L), any(GastoDto.class))).thenReturn(gasto);

        mockMvc.perform(put("/gastos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Material de construção"));

        verify(gastoService).atualizar(eq(1L), any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "EDITAR_GASTO")
    void testAtualizarGasto_SemAlocacao() throws Exception {
        when(gastoService.atualizar(eq(1L), any(GastoDto.class)))
                .thenThrow(new Forbidden("Funcionario nao esta alocado nesta obra."));

        mockMvc.perform(put("/gastos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isForbidden());

        verify(gastoService).atualizar(eq(1L), any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "DELETAR_GASTO")
    void testDeletarGasto_Sucesso() throws Exception {
        doNothing().when(gastoService).deletar(1L);

        mockMvc.perform(delete("/gastos/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(gastoService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "DELETAR_GASTO")
    void testDeletarGasto_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Gasto nao encontrado")).when(gastoService).deletar(99L);

        mockMvc.perform(delete("/gastos/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(gastoService).deletar(99L);
    }

    @Test
    void testCriarGasto_SemPermissao() throws Exception {
        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isForbidden());

        verify(gastoService, never()).criar(any(GastoDto.class));
    }

    @Test
    void testListarGastos_SemAutenticacao() throws Exception {
        mockMvc.perform(get("/gastos")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(gastoService, never()).listarPorUsuario(anyString(), any());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testCriarGasto_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"descricao\":\"\", \"valor\":null}";

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(gastoService, never()).criar(any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testAtualizarGasto_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"descricao\":\"\"}";

        mockMvc.perform(put("/gastos/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(gastoService, never()).atualizar(anyLong(), any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_GASTO")
    void testCriarGasto_ValorNegativo() throws Exception {
        gastoDto.setValor(new BigDecimal("-100.00"));

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isBadRequest());

        verify(gastoService, never()).criar(any(GastoDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_GASTO")
    void testCriarGasto_DataFutura() throws Exception {
        gastoDto.setDtGasto(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/gastos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gastoDto)))
                .andExpect(status().isBadRequest());

        verify(gastoService, never()).criar(any(GastoDto.class));
    }
}
