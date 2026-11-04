package com.jaf.application.repository;

import com.jaf.application.model.Presenca;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {
    List<Presenca> findByObraId(Long obraId);
    List<Presenca> findByFuncionarioId(Long funcionarioId);
    boolean existsByFuncionarioId(Long funcionarioId);
    Optional<Presenca> findByFuncionarioIdAndObraIdAndData(Long funcionarioId, Long obraId, LocalDate data);
    @EntityGraph(attributePaths = {"funcionario"})
    List<Presenca> findByObraIdAndData(Long obraId, LocalDate data);
    @EntityGraph(attributePaths = {"funcionario"})
    List<Presenca> findByObraIdAndDataBetween(Long obraId, LocalDate from, LocalDate to);
    boolean existsByFuncionarioIdAndObraIdAndData(Long funcionarioId, Long obraId, LocalDate data);
}
