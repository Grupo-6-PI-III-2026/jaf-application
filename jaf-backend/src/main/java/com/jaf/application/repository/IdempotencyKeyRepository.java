package com.jaf.application.repository;

import com.jaf.application.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    Optional<IdempotencyKey> findByChaveAndUsuarioId(UUID chave, Long usuarioId);

    @Modifying
    @Query("delete from IdempotencyKey k where k.criadoEm < :limite")
    int apagarAnterioresA(LocalDateTime limite);
}
