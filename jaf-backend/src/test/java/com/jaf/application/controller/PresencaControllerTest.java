package com.jaf.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaf.application.dto.PresencaDto;
import com.jaf.application.dto.PresencaListarDto;
import com.jaf.application.dto.PresencaResponseDto;
import com.jaf.application.exceptions.Conflict;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.Funcionario;
import com.jaf.application.model.Obra;
import com.jaf.application.model.Presenca;
import com.jaf.application.service.PresencaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PresencaController.class)
class PresencaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PresencaService presencaService;

    private Presenca presenca;
    private PresencaDto presencaDto;
    private PresencaResponseDto presencaResponseDto;
    private Funcionario funcionario;
    private Obra obra;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");

        obra = new Obra();
        obra.setId(1L);
        obra.setTitulo("Obra Centro");

        presenca = new Presenca();
        presenca.setId(1L);
        presenca.setFuncionario(funcionario);
        presenca.setObra(obra);
        presenca.setData(LocalDate.now());
        presenca.setPresente(true);
        presenca.setHorarioEntrada(LocalTime.of(8, 0));
        presenca.setHorarioSaida(LocalTime.of(17, 0));

        presencaDto = new PresencaDto();
        presencaDto.setFuncionarioId(1L);
        presencaDto.setObraId(1L);
        presencaDto.setData(LocalDate.now());
        presencaDto.setPresente(true);
        presencaDto.setHorarioEntrada(LocalTime.of(8, 0));
        presencaDto.setHorarioSaida(LocalTime.of(17, 0));

        presencaResponseDto = new PresencaResponseDto();
        presencaResponseDto.setId(1L);
        presencaResponseDto.setFuncionarioId(1L);
        presencaResponseDto.setFuncionarioNome("João Silva");
        presencaResponseDto.setObraId(1L);
        presencaResponseDto.setObraTitulo("Obra Centro");
        presencaResponseDto.setData(LocalDate.now());
        presencaResponseDto.setPresente(true);
        presencaResponseDto.setHorarioEntrada(LocalTime.of(8, 0));
        presencaResponseDto.setHorarioSaida(LocalTime.of(17, 0));
    }

    @Test
    @WithMockUser(authorities = "REGISTRAR_PRESENCA")
    void testCriarPresenca_Sucesso() throws Exception {
        when(presencaService.criar(any(PresencaDto.class))).thenReturn(presencaResponseDto);

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.funcionarioNome").value("João Silva"))
                .andExpect(jsonPath("$.presente").value(true));

        verify(presencaService).criar(any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "REGISTRAR_PRESENCA")
    void testCriarPresenca_SemAlocacao() throws Exception {
        when(presencaService.criar(any(PresencaDto.class)))
                .thenThrow(new Conflict("Funcionario não está alocado nesta obra."));

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isConflict());

        verify(presencaService).criar(any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "REGISTRAR_PRESENCA")
    void testCriarPresenca_Duplicata() throws Exception {
        when(presencaService.criar(any(PresencaDto.class)))
                .thenThrow(new Conflict("Já existe registro de presença para este funcionário nesta data."));

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isConflict());

        verify(presencaService).criar(any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testListarPresencasPorObraEData_Sucesso() throws Exception {
        List<PresencaListarDto> presencas = Arrays.asList(
                new PresencaListarDto(1L, 1L, "João Silva", "PEDREIRO", LocalDate.now(), true, false)
        );
        when(presencaService.listarPorObraEData(eq(1L), any(LocalDate.class))).thenReturn(presencas);

        mockMvc.perform(get("/presencas/obra/1/data/2024-01-01")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].funcionarioNome").value("João Silva"));

        verify(presencaService).listarPorObraEData(eq(1L), any(LocalDate.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testListarPresencasPorObraEData_Vazio() throws Exception {
        when(presencaService.listarPorObraEData(eq(1L), any(LocalDate.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/presencas/obra/1/data/2024-01-01")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());

        verify(presencaService).listarPorObraEData(eq(1L), any(LocalDate.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testListarPresencasPorObraEData_DataInvalida() throws Exception {
        mockMvc.perform(get("/presencas/obra/1/data/data-invalida")
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(presencaService, never()).listarPorObraEData(anyLong(), any(LocalDate.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testBuscarPresencaPorId_Sucesso() throws Exception {
        when(presencaService.buscarPorId(1L)).thenReturn(presencaResponseDto);

        mockMvc.perform(get("/presencas/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioNome").value("João Silva"));

        verify(presencaService).buscarPorId(1L);
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testBuscarPresencaPorId_NaoEncontrado() throws Exception {
        when(presencaService.buscarPorId(99L))
                .thenThrow(new NotFoundException("Presença não encontrada."));

        mockMvc.perform(get("/presencas/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(presencaService).buscarPorId(99L);
    }

    @Test
    @WithMockUser(authorities = "EDITAR_PRESENCA")
    void testAtualizarPresenca_Sucesso() throws Exception {
        when(presencaService.atualizar(eq(1L), any(PresencaDto.class))).thenReturn(presencaResponseDto);

        mockMvc.perform(put("/presencas/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.funcionarioNome").value("João Silva"));

        verify(presencaService).atualizar(eq(1L), any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "EDITAR_PRESENCA")
    void testAtualizarPresenca_Duplicata() throws Exception {
        when(presencaService.atualizar(eq(1L), any(PresencaDto.class)))
                .thenThrow(new Conflict("Já existe registro de presença para este funcionário nesta data."));

        mockMvc.perform(put("/presencas/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isConflict());

        verify(presencaService).atualizar(eq(1L), any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "EDITAR_PRESENCA")
    void testAlternarPresenca_Sucesso() throws Exception {
        doNothing().when(presencaService).alternarPresenca(1L);

        mockMvc.perform(patch("/presencas/1/alternar")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(presencaService).alternarPresenca(1L);
    }

    @Test
    @WithMockUser(authorities = "EDITAR_PRESENCA")
    void testAlternarPresenca_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Presença não encontrada.")).when(presencaService).alternarPresenca(99L);

        mockMvc.perform(patch("/presencas/99/alternar")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(presencaService).alternarPresenca(99L);
    }

    @Test
    @WithMockUser(authorities = "DELETAR_PRESENCA")
    void testDeletarPresenca_Sucesso() throws Exception {
        doNothing().when(presencaService).deletar(1L);

        mockMvc.perform(delete("/presencas/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(presencaService).deletar(1L);
    }

    @Test
    @WithMockUser(authorities = "DELETAR_PRESENCA")
    void testDeletarPresenca_NaoEncontrado() throws Exception {
        doThrow(new NotFoundException("Presença não encontrada.")).when(presencaService).deletar(99L);

        mockMvc.perform(delete("/presencas/99")
                        .with(csrf()))
                .andExpect(status().isNotFound());

        verify(presencaService).deletar(99L);
    }

    @Test
    void testCriarPresenca_SemPermissao() throws Exception {
        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isForbidden());

        verify(presencaService, never()).criar(any(PresencaDto.class));
    }

    @Test
    void testListarPresencas_SemAutenticacao() throws Exception {
        mockMvc.perform(get("/presencas/obra/1/data/2024-01-01")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(presencaService, never()).listarPorObraEData(anyLong(), any(LocalDate.class));
    }

    @Test
    @WithMockUser(authorities = "VISUALIZAR_PRESENCAS")
    void testCriarPresenca_PayloadInvalido() throws Exception {
        String payloadInvalido = "{\"funcionarioId\":null}";

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadInvalido))
                .andExpect(status().isBadRequest());

        verify(presencaService, never()).criar(any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "REGISTRAR_PRESENCA")
    void testCriarPresenca_HorarioEntradaDepoisSaida() throws Exception {
        presencaDto.setHorarioEntrada(LocalTime.of(17, 0));
        presencaDto.setHorarioSaida(LocalTime.of(8, 0));

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isBadRequest());

        verify(presencaService, never()).criar(any(PresencaDto.class));
    }

    @Test
    @WithMockUser(authorities = "REGISTRAR_PRESENCA")
    void testCriarPresenca_DataFutura() throws Exception {
        presencaDto.setData(LocalDate.now().plusDays(30));

        mockMvc.perform(post("/presencas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(presencaDto)))
                .andExpect(status().isBadRequest());

        verify(presencaService, never()).criar(any(PresencaDto.class));
    }
}
