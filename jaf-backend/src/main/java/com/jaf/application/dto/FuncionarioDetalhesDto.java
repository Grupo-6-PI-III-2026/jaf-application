package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FuncionarioDetalhesDto implements UserDetails {
    private final String nome;
    private final String email;
    private final String senha;
    private final Cargo cargo;

    public FuncionarioDetalhesDto(Funcionario funcionario) {
        this.nome = funcionario.getNome();
        this.email = funcionario.getEmail();
        this.senha = funcionario.getSenha();
        this.cargo = funcionario.getCargoGlobal() != null ? funcionario.getCargoGlobal() : Cargo.OPERADOR_LANCAMENTO;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public Cargo getCargo() {
        return cargo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + cargo.name()));
        cargo.getPermissoes().forEach(permissao ->
                authorities.add(new SimpleGrantedAuthority(permissao.name())));
        return authorities;
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
