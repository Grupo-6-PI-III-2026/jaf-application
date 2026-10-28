package com.jaf.application.service;

import com.jaf.application.dto.PresencaDto;
import com.jaf.application.exceptions.*;
import com.jaf.application.model.*;
import com.jaf.application.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresencaServiceTest {
    @Mock PresencaRepository presencaRepository;
    @Mock FuncionarioRepository funcionarioRepository;
    @Mock ObraRepository obraRepository;
    @Mock AlocacaoObraRepository alocacaoObraRepository;
    @Mock AcessoObraService acesso;
    @InjectMocks PresencaService service;

    private PresencaDto dto() {
        var dto = new PresencaDto(); dto.setObraId(1L); dto.setFuncionarioId(2L);
        dto.setData(LocalDate.now()); dto.setPresente(true); return dto;
    }
    @Test void leituraEMutacaoExigemEscopo() {
        doThrow(new Forbidden("Sem escopo")).when(acesso).exigirObra(1L);
        assertThrows(Forbidden.class, () -> service.listarPorObraEData(1L, LocalDate.now()));
        assertThrows(Forbidden.class, () -> service.criar(dto()));
        verifyNoInteractions(presencaRepository);
    }
    @Test void rejeitaFuturoEPeriodoInvertido() {
        var dto = dto(); dto.setData(LocalDate.now().plusDays(1));
        assertThrows(BadRequest.class, () -> service.criar(dto));
        when(obraRepository.existsById(1L)).thenReturn(true);
        assertThrows(BadRequest.class, () -> service.listarPorObraEPeriodo(1L, LocalDate.now(), LocalDate.now().minusDays(1)));
    }
    @Test void edicaoExigeAlocacaoNaData() {
        var registro = new Presenca(); var obra = new Obra(); obra.setId(1L); registro.setObra(obra);
        when(presencaRepository.findById(3L)).thenReturn(Optional.of(registro));
        when(alocacaoObraRepository.findByFuncionarioIdAndObraId(2L, 1L)).thenReturn(List.of());
        assertThrows(Conflict.class, () -> service.atualizar(3L, dto()));
    }
    @Test void rejeitaHorariosIncoerentes() {
        var alocacao = new AlocacaoObra();
        when(alocacaoObraRepository.findByFuncionarioIdAndObraId(2L, 1L)).thenReturn(List.of(alocacao));
        var dto = dto(); dto.setHorarioEntrada(LocalTime.of(10, 0)); dto.setHorarioSaida(LocalTime.of(9, 0));
        assertThrows(BadRequest.class, () -> service.criar(dto));
        dto.setPresente(false); dto.setHorarioSaida(null);
        assertThrows(BadRequest.class, () -> service.criar(dto));
    }
    @Test void alternarParaAusenteLimpaHorarios() {
        var registro = new Presenca(); var obra = new Obra(); obra.setId(1L); registro.setObra(obra);
        registro.setData(LocalDate.now()); registro.setPresente(true); registro.setHorarioEntrada(LocalTime.of(8, 0));
        when(presencaRepository.findById(3L)).thenReturn(Optional.of(registro));
        service.alternarPresenca(3L);
        assertFalse(registro.getPresente()); assertNull(registro.getHorarioEntrada());
        verify(presencaRepository).save(registro);
    }
}