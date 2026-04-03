package com.jaf.application.enums;

import java.util.Arrays;
import java.util.List;

public enum Cargo {
    ADMIN(Arrays.asList(Permissao.values())),

    GESTOR_OBRA(Arrays.asList(
            Permissao.GERENCIAR_OBRA,
            Permissao.REGISTRAR_GASTO,
            Permissao.GERAR_RELATORIO
    )),

    OPERADOR_LANCAMENTO(Arrays.asList(
            Permissao.REGISTRAR_GASTO
    ));

    private final List<Permissao> permissoes;

    Cargo(List<Permissao> permissoes) {
        this.permissoes = permissoes;
    }

    public List<Permissao> getPermissoes() {
        return permissoes;
    }
}
