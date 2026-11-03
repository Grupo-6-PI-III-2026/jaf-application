package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;

import java.util.List;

public class FuncionarioPermissoesAcessoDto {
    private Long funcionarioId;
    private Cargo cargo;
    private List<String> permissoes;
    private boolean personalizada;

    public FuncionarioPermissoesAcessoDto() {
    }

    public FuncionarioPermissoesAcessoDto(Long funcionarioId, Cargo cargo, List<String> permissoes) {
        this.funcionarioId = funcionarioId;
        this.cargo = cargo;
        this.permissoes = permissoes;
    }
    public FuncionarioPermissoesAcessoDto(Long funcionarioId, Cargo cargo, List<String> permissoes, boolean personalizada) {
        this(funcionarioId, cargo, permissoes);
        this.personalizada = personalizada;
    }
    public boolean isPersonalizada() { return personalizada; }
    public void setPersonalizada(boolean personalizada) { this.personalizada = personalizada; }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public List<String> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(List<String> permissoes) {
        this.permissoes = permissoes;
    }
}