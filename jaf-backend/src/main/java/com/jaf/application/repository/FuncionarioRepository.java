package com.jaf.application.repository;

import com.jaf.application.model.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
	Optional<Funcionario> findByEmailIgnoreCase(String email);
}
