package com.jaf.application.enums;

import java.util.Arrays;
import java.util.List;

public enum Cargo {
    ADMIN(Arrays.asList(Permissao.values())),

    GESTOR_OBRA(Arrays.asList(
            Permissao.CRIAR_OBRA,
            Permissao.EDITAR_OBRA,
            Permissao.VISUALIZAR_OBRA,
            Permissao.VISUALIZAR_FUNCIONARIOS,
            Permissao.CRIAR_GASTO,
            Permissao.EDITAR_GASTO,
            Permissao.VISUALIZAR_GASTOS,
            Permissao.CRIAR_ALOCACAO,
            Permissao.EDITAR_ALOCACAO,
            Permissao.VISUALIZAR_ALOCACOES,
            Permissao.GERAR_RELATORIO,
            Permissao.VISUALIZAR_RELATORIO,
            Permissao.REGISTRAR_PRESENCA,
            Permissao.EDITAR_PRESENCA,
            Permissao.DELETAR_PRESENCA,
            Permissao.VISUALIZAR_PRESENCAS
    )),

    OPERADOR_LANCAMENTO(Arrays.asList(
            Permissao.VISUALIZAR_OBRA,
            Permissao.CRIAR_GASTO,
            Permissao.EDITAR_GASTO,
            Permissao.VISUALIZAR_GASTOS,
            Permissao.VISUALIZAR_ALOCACOES,
            Permissao.VISUALIZAR_RELATORIO,
            Permissao.VISUALIZAR_PRESENCAS
        )),

        MESTRE_DE_OBRAS(Arrays.asList(
            Permissao.VISUALIZAR_OBRA,
            Permissao.CRIAR_GASTO,
            Permissao.EDITAR_GASTO,
            Permissao.VISUALIZAR_GASTOS,
            Permissao.VISUALIZAR_ALOCACOES,
            Permissao.VISUALIZAR_PRESENCAS,
            Permissao.REGISTRAR_PRESENCA,
            Permissao.EDITAR_PRESENCA
        )),

        ENGENHEIRO(Arrays.asList(
            Permissao.VISUALIZAR_OBRA,
            Permissao.CRIAR_OBRA,
            Permissao.EDITAR_OBRA,
            Permissao.VISUALIZAR_FUNCIONARIOS,
            Permissao.CRIAR_GASTO,
            Permissao.EDITAR_GASTO,
            Permissao.VISUALIZAR_GASTOS,
            Permissao.CRIAR_ALOCACAO,
            Permissao.EDITAR_ALOCACAO,
            Permissao.VISUALIZAR_ALOCACOES,
            Permissao.VISUALIZAR_RELATORIO,
            Permissao.GERAR_RELATORIO,
            Permissao.VISUALIZAR_PRESENCAS,
            Permissao.REGISTRAR_PRESENCA,
            Permissao.EDITAR_PRESENCA
        )),

            ARQUITETO(Arrays.asList(
                Permissao.VISUALIZAR_OBRA,
                Permissao.CRIAR_OBRA,
                Permissao.EDITAR_OBRA,
                Permissao.VISUALIZAR_FUNCIONARIOS,
                Permissao.CRIAR_GASTO,
                Permissao.EDITAR_GASTO,
                Permissao.VISUALIZAR_GASTOS,
                Permissao.CRIAR_ALOCACAO,
                Permissao.EDITAR_ALOCACAO,
                Permissao.VISUALIZAR_ALOCACOES,
                Permissao.VISUALIZAR_RELATORIO,
                Permissao.GERAR_RELATORIO,
                Permissao.VISUALIZAR_PRESENCAS,
                Permissao.REGISTRAR_PRESENCA,
                Permissao.EDITAR_PRESENCA
            )),

        PEDREIRO(Arrays.asList(
            Permissao.VISUALIZAR_OBRA,
            Permissao.VISUALIZAR_ALOCACOES,
            Permissao.VISUALIZAR_PRESENCAS,
            Permissao.REGISTRAR_PRESENCA
        ));

    private final List<Permissao> permissoes;

    Cargo(List<Permissao> permissoes) {
        this.permissoes = permissoes;
    }

    public List<Permissao> getPermissoes() {
        return permissoes;
    }
}
