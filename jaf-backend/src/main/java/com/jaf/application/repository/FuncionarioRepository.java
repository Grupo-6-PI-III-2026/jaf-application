package com.jaf.application.repository;

import com.jaf.application.model.Funcionario;
import com.jaf.application.enums.Cargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    @Query("select f from Funcionario f where lower(f.email) = lower(:email)")
    Optional<Funcionario> findByEmailIgnoreCase(@Param("email") String email);
    long countByCargoGlobalAndAtivo(Cargo cargoGlobal, boolean ativo);
}
