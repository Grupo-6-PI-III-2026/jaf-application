package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import io.swagger.v3.oas.annotations.media.Schema;

public class FuncionarioTokenDto {
    private Long id;
    private String nome;
    private String email;
    private Cargo cargo;
    private String token;

    public FuncionarioTokenDto() {
    }

    public FuncionarioTokenDto(String email, String token) {
        this.email = email;
        this.token = token;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public String getToken() { return token; }

    public void setToken(String token) { this.token = token; }
}
