package com.jaf.application.repository;

import com.jaf.application.model.Presenca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {
    List<Presenca> findByObraId(Long obraId);
    List<Presenca> findByFuncionarioId(Long funcionarioId);
    Optional<Presenca> findByFuncionarioIdAndObraIdAndData(Long funcionarioId, Long obraId, LocalDate data);
    List<Presenca> findByObraIdAndData(Long obraId, LocalDate data);
    boolean existsByFuncionarioIdAndObraIdAndData(Long funcionarioId, Long obraId, LocalDate data);
}
