package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.dto.ObraDto;
import com.jaf.application.enums.Cargo;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.Forbidden;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.AlocacaoObra;
import com.jaf.application.model.Gasto;
import com.jaf.application.model.Obra;
import com.jaf.application.service.ObraService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
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

@WebMvcTest(ObraController.class)
class ObraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ObraService obraService;

    @MockBean
    private com.jaf.application.service.GastoService gastoService;

    @MockBean
    private com.jaf.application.service.AlocacaoObraService alocacaoObraService;

    private Obra obra;
    private ObraDto obraDto;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Centro");
        obra.setOrcamento("1000000.00");
        obra.setStatus("EM_ANDAMENTO");
        obra.setDtInicio(LocalDate.of(2024, 1, 1));
        obra.setDtTerminoPrevisto(LocalDate.of(2024, 12, 31));
        obra.setResponsavel("João Silva");
        obra.setEndereco("Rua Centro, 123");
        obra.setCidade("São Paulo");
        obra.setEstado("SP");

        obraDto = new ObraDto();
        obraDto.setTitulo("Obra Centro");
        obraDto.setOrcamento("1000000.00");
        obraDto.setStatus("EM_ANDAMENTO");
        obraDto.setDtInicio(LocalDate.of(2024, 1, 1));
        obraDto.setDtTerminoPrevisto(LocalDate.of(2024, 12, 31));
        obraDto.setResponsavel("João Silva");
        obraDto.setEndereco("Rua Centro, 123");
        obraDto.setCidade("São Paulo");
        obraDto.setEstado("SP");

        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@test.com");
    }

    @Test
    @WithMockUser(authorities = "CRIAR_OBRA")
    void testCriarObra_Sucesso() throws Exception {
        when(obraService.criar(any(ObraDto.class))).thenReturn(obra);

        mockMvc.perform(post("/obras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obraDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Obra Centro"))
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));

        verify(obraService).criar(any(ObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "CRIAR_OBRA")
    void testCriarObra_TituloDuplicado() throws Exception {
        when(obraService.criar(any(ObraDto.class)))
                .thenThrow(new Conflict("Obra ja existente."));

        mockMvc.perform(post("/obras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obraDto)))
                .andExpect(status().isConflict());

        verify(obraService).criar(any(ObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testListarObras_Admin_Sucesso() throws Exception {
        List<Obra> obras = Arrays.asList(obra);
        when(obraService.listarPorUsuario(anyString())).thenReturn(obras);

        mockMvc.perform(get("/obras")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].titulo").value("Obra Centro"));

        verify(obraService).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testListarObras_Vazia() throws Exception {
        when(obraService.listarPorUsuario(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/obras")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(obraService).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testBuscarObraPorId_Sucesso() throws Exception {
        when(obraService.buscarPorIdComEscopo(eq(1L), anyString())).thenReturn(obra);

        mockMvc.perform(get("/obras/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Obra Centro"));

        verify(obraService).buscarPorIdComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testBuscarObraPorId_SemPermissaoEscopo() throws Exception {
        when(obraService.buscarPorIdComEscopo(eq(1L), anyString()))
                .thenThrow(new Forbidden("Funcionario nao esta alocado nesta obra."));

        mockMvc.perform(get("/obras/1")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(obraService).buscarPorIdComEscopo(eq(1L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testBuscarObraPorId_NaoEncontrado() throws Exception {
        when(obraService.buscarPorIdComEscopo(eq(99L), anyString()))
                .thenThrow(new NotFoundException("Obra nao encontrada."));

        mockMvc.perform(get("/obras/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(obraService).buscarPorIdComEscopo(eq(99L), anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_GASTOS")
    void testListarGastosDaObra_Sucesso() throws Exception {
        Gasto gasto = new Gasto();
        gasto.setId(1L);
        gasto.setDescricao("Material");
        gasto.setValor(new BigDecimal("500.00"));

        List<Gasto> gastos = Arrays.asList(gasto);
        when(gastoService.listarPorUsuario(anyString(), eq(1L))).thenReturn(gastos);

        mockMvc.perform(get("/obras/1/gastos")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].descricao").value("Material"));

        verify(obraService).buscarPorIdComEscopo(eq(1L), anyString());
        verify(gastoService).listarPorUsuario(anyString(), eq(1L));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_ALOCACOES")
    void testListarAlocacoesDaObra_Sucesso() throws Exception {
        AlocacaoObra alocacao = new AlocacaoObra();
        alocacao.setId(1L);

        List<AlocacaoObra> alocacoes = Arrays.asList(alocacao);
        when(alocacaoObraService.listarPorObra(1L)).thenReturn(alocacoes);

        mockMvc.perform(get("/obras/1/alocacoes")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        verify(alocacaoObraService).listarPorObra(1L);
    }

    @Test
    @WithMockUser(authorities = "EDITAR_OBRA")
    void testAtualizarObra_Sucesso() throws Exception {
        when(obraService.atualizar(eq(1L), any(ObraDto.class))).thenReturn(obra);

        mockMvc.perform(put("/obras/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obraDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Obra Centro"));

        verify(obraService).atualizar(eq(1L), any(ObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "DELETAR_OBRA")
    void testDeletarObra_Sucesso() throws Exception {
        doNothing().when(obraService).deletar(1L);

        mockMvc.perform(delete("/obras/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(obraService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "DELETAR_OBRA")
    void testDeletarObra_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Obra nao encontrada")).when(obraService).deletar(99L);

        mockMvc.perform(delete("/obras/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(obraService).deletar(99L);
    }

    @Test
    void testCriarObra_SemPermissao() throws Exception {
        mockMvc.perform(post("/obras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obraDto)))
                .andExpect(status().isForbidden());

        verify(obraService, never()).criar(any(ObraDto.class));
    }

    @Test
    void testListarObras_SemAutenticacao() throws Exception {
        mockMvc.perform(get("/obras")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(obraService, never()).listarPorUsuario(anyString());
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testCriarObra_ComPermissaoIncorreta() throws Exception {
        mockMvc.perform(post("/obras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(obraDto)))
                .andExpect(status().isForbidden());

        verify(obraService, never()).criar(any(ObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testCriarObra_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"titulo\":\"\"}";

        mockMvc.perform(post("/obras")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(obraService, never()).criar(any(ObraDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_OBRA")
    void testAtualizarObra_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"titulo\":\"\"}";

        mockMvc.perform(put("/obras/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(obraService, never()).atualizar(anyLong(), any(ObraDto.class));
    }
}
