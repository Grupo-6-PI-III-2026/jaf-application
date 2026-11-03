package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;

public record FuncionarioResumo(Long id, String nome, String email, Cargo cargoGlobal, String fotoUrl) {
    public static FuncionarioResumo de(Funcionario funcionario) {
        return new FuncionarioResumo(funcionario.getId(), funcionario.getNome(), funcionario.getEmail(),
                funcionario.getCargoGlobal(), funcionario.getFotoUrl());
    }

    public FuncionarioResumo semFoto() {
        return new FuncionarioResumo(id, nome, email, cargoGlobal, null);
    }
}
