package com.jaf.application.repository;

import com.jaf.application.model.FuncionarioPermissao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuncionarioPermissaoRepository extends JpaRepository<FuncionarioPermissao, Long> {
    List<FuncionarioPermissao> findByFuncionarioId(Long funcionarioId);

    void deleteByFuncionarioId(Long funcionarioId);
}