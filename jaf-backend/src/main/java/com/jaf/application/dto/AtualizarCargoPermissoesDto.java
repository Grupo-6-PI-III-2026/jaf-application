package com.jaf.application.dto;

import java.util.List;

public class AtualizarCargoPermissoesDto {
    private List<String> permissoes;

    public List<String> getPermissoes() {
        return permissoes;
    }

    public void setPermissoes(List<String> permissoes) {
        this.permissoes = permissoes;
    }
}