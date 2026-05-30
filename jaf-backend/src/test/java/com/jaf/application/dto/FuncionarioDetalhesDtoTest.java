package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class FuncionarioDetalhesDtoTest {

    private Funcionario funcionario;
    private FuncionarioDetalhesDto funcionarioDetalhesDto;

    @BeforeEach
    void setUp() {
        funcionario = new Funcionario();
        funcionario.setId(1L);
        funcionario.setNome("João Silva");
        funcionario.setEmail("joao@email.com");
        funcionario.setSenha("hashedPassword");
        funcionario.setCargoGlobal(Cargo.ADMIN);

        funcionarioDetalhesDto = new FuncionarioDetalhesDto(funcionario);
    }

    @Test
    void testGetAuthoritiesReturnsRoleAndPermissions() {
        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = funcionarioDetalhesDto.getAuthorities();

        assertNotNull(authorities);
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("CRIAR_OBRA")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("VISUALIZAR_FUNCIONARIOS")));
    }

    @Test
    void testGetAuthoritiesForGestorObra() {
        funcionario.setCargoGlobal(Cargo.GESTOR_OBRA);
        funcionarioDetalhesDto = new FuncionarioDetalhesDto(funcionario);

        Collection<? extends org.springframework.security.core.GrantedAuthority> authorities = funcionarioDetalhesDto.getAuthorities();

        assertNotNull(authorities);
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_GESTOR_OBRA")));
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("CRIAR_OBRA")));
        assertFalse(authorities.stream().anyMatch(a -> a.getAuthority().equals("CRIAR_FUNCIONARIO")));
    }

    @Test
    void testGetUsernameReturnsEmail() {
        assertEquals("joao@email.com", funcionarioDetalhesDto.getUsername());
    }

    @Test
    void testGetPasswordReturnsSenha() {
        assertEquals("hashedPassword", funcionarioDetalhesDto.getPassword());
    }

    @Test
    void testIsAccountNonExpiredReturnsTrue() {
        assertTrue(funcionarioDetalhesDto.isAccountNonExpired());
    }

    @Test
    void testIsAccountNonLockedReturnsTrue() {
        assertTrue(funcionarioDetalhesDto.isAccountNonLocked());
    }

    @Test
    void testIsCredentialsNonExpiredReturnsTrue() {
        assertTrue(funcionarioDetalhesDto.isCredentialsNonExpired());
    }

    @Test
    void testIsEnabledReturnsTrue() {
        assertTrue(funcionarioDetalhesDto.isEnabled());
    }

    @Test
    void testDefaultCargoWhenNull() {
        funcionario.setCargoGlobal(null);
        funcionarioDetalhesDto = new FuncionarioDetalhesDto(funcionario);

        assertEquals(Cargo.OPERADOR_LANCAMENTO, funcionarioDetalhesDto.getCargo());
    }
}
