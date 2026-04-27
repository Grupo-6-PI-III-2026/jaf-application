package com.jaf.application.dto;

import com.jaf.application.model.Funcionario;
import com.jaf.application.enums.Cargo;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class FuncionarioDetalhesDto implements UserDetails {
    private String nome;
    private String email;
    private String senha;
    private Cargo cargo;

    public FuncionarioDetalhesDto(Funcionario funcionario) {
        this.nome = funcionario.getNome();
        this.email = funcionario.getEmail();
        this.senha = funcionario.getSenha();
        this.cargo = funcionario.getCargoGlobal();
    }

    public FuncionarioDetalhesDto(String nome, String email, String senha, Cargo cargo) {
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cargo = cargo;
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
        return Collections.emptyList(); // Sem níveis de autorização por enquanto
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
