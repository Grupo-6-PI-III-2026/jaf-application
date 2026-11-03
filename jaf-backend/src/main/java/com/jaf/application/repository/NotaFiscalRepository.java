package com.jaf.application.repository;

import com.jaf.application.model.NotaFiscal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotaFiscalRepository extends JpaRepository<NotaFiscal, Long> {
    List<NotaFiscal> findByGastoIdOrderByIdAsc(Long gastoId);
    boolean existsByGastoIdAndSha256(Long gastoId, String sha256);
}
