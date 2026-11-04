package com.jaf.application.repository;

import com.jaf.application.model.Relatorio;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelatorioRepository extends JpaRepository<Relatorio, Long> {
    boolean existsByFuncionarioResponsavelId(Long funcionarioId);

    @Override
    @EntityGraph(attributePaths = {"funcionarioResponsavel"})
    List<Relatorio> findAll();

    @EntityGraph(attributePaths = {"funcionarioResponsavel"})
    List<Relatorio> findByFuncionarioResponsavelId(Long funcionarioId);
}
