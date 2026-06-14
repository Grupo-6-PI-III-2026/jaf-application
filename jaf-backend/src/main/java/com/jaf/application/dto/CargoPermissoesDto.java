package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;

import java.util.List;

public class CargoPermissoesDto {
    private Cargo cargo;
    private List<String> permissoes;

    public CargoPermissoesDto() {
    }

    public CargoPermissoesDto(Cargo cargo, List<String> permissoes) {
        this.cargo = cargo;
        this.permissoes = permissoes;
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