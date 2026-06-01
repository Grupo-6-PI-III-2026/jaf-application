package com.jaf.application.security;

import com.jaf.application.config.GerenciadorTokenJwt;
import com.jaf.application.config.SecurityConfiguracao;
import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.TipoFuncionario;
import com.jaf.application.model.Funcionario;
import com.jaf.application.service.AutenticacaoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private SecurityConfiguracao securityConfiguracao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AutenticacaoService autenticacaoService;

    @MockBean
    private GerenciadorTokenJwt gerenciadorTokenJwt;

    @Test
    void testPasswordEncoderBeanExists() {
        assertNotNull(passwordEncoder);
        assertTrue(passwordEncoder.matches("senha123", passwordEncoder.encode("senha123")));
    }

    @Test
    void testPasswordEncoder_NaoPlaintext() {
        String senhaOriginal = "senha123";
        String senhaCodificada = passwordEncoder.encode(senhaOriginal);

        assertNotEquals(senhaOriginal, senhaCodificada);
        assertTrue(senhaCodificada.startsWith("$2a$"));
    }

    @Test
    void testCargo_PermissoesAdministrador() {
        Cargo admin = Cargo.ADMIN;
        assertFalse(admin.getPermissoes().isEmpty());
        assertEquals(24, admin.getPermissoes().size()); // Todas as permissões
    }

    @Test
    void testCargo_PermissoesGestorObra() {
        Cargo gestorObra = Cargo.GESTOR_OBRA;
        assertFalse(gestorObra.getPermissoes().isEmpty());
        assertTrue(gestorObra.getPermissoes().size() < 24);
        assertFalse(gestorObra.getPermissoes().contains(com.jaf.application.enums.Permissao.DELETAR_FUNCIONARIO));
    }

    @Test
    void testCargo_PermissoesOperadorLancamento() {
        Cargo operador = Cargo.OPERADOR_LANCAMENTO;
        assertFalse(operador.getPermissoes().isEmpty());
        assertTrue(operador.getPermissoes().size() < gestorObraPermissaoCount());
        assertFalse(operador.getPermissoes().contains(com.jaf.application.enums.Permissao.CRIAR_OBRA));
    }

    private int gestorObraPermissaoCount() {
        return Cargo.GESTOR_OBRA.getPermissoes().size();
    }

    @Test
    void testTipoFuncionario_ValoresValidos() {
        assertEquals(2, TipoFuncionario.values().length);
        assertNotNull(TipoFuncionario.INTERNO);
        assertNotNull(TipoFuncionario.EXTERNO);
    }

    @Test
    void testFuncionarioExterno_SemSenha() {
        Funcionario externo = new Funcionario();
        externo.setTipoFuncionario(TipoFuncionario.EXTERNO);
        externo.setSenha(null);

        assertEquals(TipoFuncionario.EXTERNO, externo.getTipoFuncionario());
        assertNull(externo.getSenha());
    }

    @Test
    void testFuncionarioInterno_ComSenha() {
        Funcionario interno = new Funcionario();
        interno.setTipoFuncionario(TipoFuncionario.INTERNO);
        interno.setSenha("senha123");

        assertEquals(TipoFuncionario.INTERNO, interno.getTipoFuncionario());
        assertNotNull(interno.getSenha());
    }

    @Test
    void testSecurityConfiguracao_BeanExists() {
        assertNotNull(securityConfiguracao);
    }
}
