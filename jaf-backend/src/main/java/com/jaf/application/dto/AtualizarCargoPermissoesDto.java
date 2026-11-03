package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import java.util.List;
import jakarta.validation.constraints.Size;

public class AtualizarCargoPermissoesDto {
    private Cargo cargo;
    private List<String> permissoes;
    public Cargo getCargo() { return cargo; }
    public void setCargo(Cargo cargo) { this.cargo = cargo; }

    public List<String> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(List<String> permissoes) {
        this.permissoes = permissoes;
    }
}