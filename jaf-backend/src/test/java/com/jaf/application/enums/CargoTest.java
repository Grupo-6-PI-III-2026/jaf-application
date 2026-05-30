package com.jaf.application.enums;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CargoTest {

    @Test
    void testAdminTemTodasPermissoes() {
        Cargo admin = Cargo.ADMIN;
        List<Permissao> permissoes = admin.getPermissoes();

        assertNotNull(permissoes);
        assertEquals(Permissao.values().length, permissoes.size());
        assertTrue(permissoes.contains(Permissao.CRIAR_OBRA));
        assertTrue(permissoes.contains(Permissao.DELETAR_FUNCIONARIO));
    }

    @Test
    void testGestorObraTemPermissoesCorretas() {
        Cargo gestor = Cargo.GESTOR_OBRA;
        List<Permissao> permissoes = gestor.getPermissoes();

        assertNotNull(permissoes);
        assertTrue(permissoes.contains(Permissao.CRIAR_OBRA));
        assertTrue(permissoes.contains(Permissao.EDITAR_OBRA));
        assertTrue(permissoes.contains(Permissao.VISUALIZAR_OBRA));
        assertFalse(permissoes.contains(Permissao.CRIAR_FUNCIONARIO));
        assertFalse(permissoes.contains(Permissao.DELETAR_FUNCIONARIO));
    }

    @Test
    void testOperadorLancamentoTemPermissoesCorretas() {
        Cargo operador = Cargo.OPERADOR_LANCAMENTO;
        List<Permissao> permissoes = operador.getPermissoes();

        assertNotNull(permissoes);
        assertTrue(permissoes.contains(Permissao.VISUALIZAR_OBRA));
        assertTrue(permissoes.contains(Permissao.CRIAR_GASTO));
        assertTrue(permissoes.contains(Permissao.EDITAR_GASTO));
        assertFalse(permissoes.contains(Permissao.CRIAR_OBRA));
        assertFalse(permissoes.contains(Permissao.DELETAR_OBRA));
    }

    @Test
    void testGestorObraPodeGerarRelatorio() {
        Cargo gestor = Cargo.GESTOR_OBRA;
        List<Permissao> permissoes = gestor.getPermissoes();

        assertTrue(permissoes.contains(Permissao.GERAR_RELATORIO));
        assertTrue(permissoes.contains(Permissao.VISUALIZAR_RELATORIO));
    }

    @Test
    void testOperadorLancamentoNaoPodeGerarRelatorio() {
        Cargo operador = Cargo.OPERADOR_LANCAMENTO;
        List<Permissao> permissoes = operador.getPermissoes();

        assertFalse(permissoes.contains(Permissao.GERAR_RELATORIO));
        assertTrue(permissoes.contains(Permissao.VISUALIZAR_RELATORIO));
    }
}
