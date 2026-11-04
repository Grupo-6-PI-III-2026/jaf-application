package com.jaf.application.service;

import com.jaf.application.model.IdempotencyKey;
import com.jaf.application.repository.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {
    @Mock IdempotencyKeyRepository repository;
    final Clock relogio = Clock.system(ZoneId.of("America/Sao_Paulo"));
    IdempotencyService service;
    final UUID chave = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        service = new IdempotencyService(repository, relogio);
    }

    private IdempotencyKey registro(IdempotencyKey.Estado estado, String fingerprint, LocalDateTime criadoEm) {
        var k = new IdempotencyKey();
        k.setChave(chave);
        k.setUsuarioId(1L);
        k.setFingerprint(fingerprint);
        k.setEstado(estado);
        k.setCriadoEm(criadoEm);
        k.setStatus(201);
        k.setCorpo("{\"id\":1}");
        return k;
    }

    @Test
    void chaveNovaEhReservada() {
        when(repository.findByChaveAndUsuarioId(chave, 1L)).thenReturn(Optional.empty());
        assertEquals(IdempotencyService.Tipo.NOVA, service.reservar(chave, 1L, "POST", "/gastos", "abc").tipo());
        verify(repository).saveAndFlush(any());
    }

    @Test
    void corridaNaReservaViraEmAndamento() {
        when(repository.findByChaveAndUsuarioId(chave, 1L)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicada"));
        assertEquals(IdempotencyService.Tipo.EM_ANDAMENTO, service.reservar(chave, 1L, "POST", "/gastos", "abc").tipo());
    }

    @Test
    void mesmaChaveConcluidaDevolveMesmaResposta() {
        when(repository.findByChaveAndUsuarioId(chave, 1L))
                .thenReturn(Optional.of(registro(IdempotencyKey.Estado.CONCLUIDA, "abc", LocalDateTime.now(relogio))));
        var resultado = service.reservar(chave, 1L, "POST", "/gastos", "abc");
        assertEquals(IdempotencyService.Tipo.REPETIDA, resultado.tipo());
        assertEquals(201, resultado.status());
        assertEquals("{\"id\":1}", resultado.corpo());
    }

    @Test
    void mesmaChavePendenteRetornaEmAndamento() {
        when(repository.findByChaveAndUsuarioId(chave, 1L))
                .thenReturn(Optional.of(registro(IdempotencyKey.Estado.PENDENTE, "abc", LocalDateTime.now(relogio))));
        assertEquals(IdempotencyService.Tipo.EM_ANDAMENTO, service.reservar(chave, 1L, "POST", "/gastos", "abc").tipo());
    }

    @Test
    void conteudoDiferenteRejeitaReuso() {
        when(repository.findByChaveAndUsuarioId(chave, 1L))
                .thenReturn(Optional.of(registro(IdempotencyKey.Estado.CONCLUIDA, "abc", LocalDateTime.now(relogio))));
        assertEquals(IdempotencyService.Tipo.CHAVE_REUTILIZADA, service.reservar(chave, 1L, "POST", "/gastos", "outro").tipo());
    }

    @Test
    void pendenciaAbandonadaPodeSerReaproveitada() {
        when(repository.findByChaveAndUsuarioId(chave, 1L))
                .thenReturn(Optional.of(registro(IdempotencyKey.Estado.PENDENTE, "abc", LocalDateTime.now(relogio).minusMinutes(10))));
        assertEquals(IdempotencyService.Tipo.NOVA, service.reservar(chave, 1L, "POST", "/gastos", "abc").tipo());
        verify(repository).delete(any());
    }
}
