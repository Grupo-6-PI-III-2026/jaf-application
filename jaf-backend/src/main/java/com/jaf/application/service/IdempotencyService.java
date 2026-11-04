package com.jaf.application.service;

import com.jaf.application.model.IdempotencyKey;
import com.jaf.application.repository.IdempotencyKeyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class IdempotencyService {
    public enum Tipo { NOVA, REPETIDA, EM_ANDAMENTO, CHAVE_REUTILIZADA }

    public record Resultado(Tipo tipo, Integer status, String corpo) {
        static Resultado de(Tipo tipo) { return new Resultado(tipo, null, null); }
    }

    private final IdempotencyKeyRepository repository;
    private final Clock clock;

    public IdempotencyService(IdempotencyKeyRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Resultado reservar(UUID chave, Long usuarioId, String metodo, String caminho, String fingerprint) {
        var existente = repository.findByChaveAndUsuarioId(chave, usuarioId);
        if (existente.isPresent() && abandonada(existente.get())) {
            repository.delete(existente.get());
            repository.flush();
            existente = java.util.Optional.empty();
        }
        if (existente.isPresent()) return avaliar(existente.get(), fingerprint);

        IdempotencyKey nova = new IdempotencyKey();
        nova.setChave(chave);
        nova.setUsuarioId(usuarioId);
        nova.setMetodo(metodo);
        nova.setCaminho(caminho);
        nova.setFingerprint(fingerprint);
        nova.setEstado(IdempotencyKey.Estado.PENDENTE);
        nova.setCriadoEm(LocalDateTime.now(clock));
        try {
            repository.saveAndFlush(nova);
            return Resultado.de(Tipo.NOVA);
        } catch (DataIntegrityViolationException corrida) {
            return Resultado.de(Tipo.EM_ANDAMENTO);
        }
    }

    private boolean abandonada(IdempotencyKey registro) {
        return registro.getEstado() == IdempotencyKey.Estado.PENDENTE
                && registro.getCriadoEm().isBefore(LocalDateTime.now(clock).minusMinutes(2));
    }

    private Resultado avaliar(IdempotencyKey existente, String fingerprint) {
        if (!existente.getFingerprint().equals(fingerprint)) return Resultado.de(Tipo.CHAVE_REUTILIZADA);
        if (existente.getEstado() == IdempotencyKey.Estado.CONCLUIDA) {
            return new Resultado(Tipo.REPETIDA, existente.getStatus(), existente.getCorpo());
        }
        return Resultado.de(Tipo.EM_ANDAMENTO);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void concluir(UUID chave, Long usuarioId, int status, String corpo) {
        repository.findByChaveAndUsuarioId(chave, usuarioId).ifPresent(registro -> {
            registro.setEstado(IdempotencyKey.Estado.CONCLUIDA);
            registro.setStatus(status);
            registro.setCorpo(corpo);
            repository.save(registro);
        });
    }

    // Só respostas de sucesso ficam registradas; falhas liberam a chave para nova tentativa.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void liberar(UUID chave, Long usuarioId) {
        repository.findByChaveAndUsuarioId(chave, usuarioId).ifPresent(repository::delete);
    }

    @Scheduled(cron = "0 15 3 * * *")
    @Transactional
    public void limparExpiradas() {
        repository.apagarAnterioresA(LocalDateTime.now(clock).minusHours(24));
    }
}
