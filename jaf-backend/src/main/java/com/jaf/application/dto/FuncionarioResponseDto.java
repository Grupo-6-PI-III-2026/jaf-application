package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;

import java.time.LocalDate;

public class FuncionarioResponseDto {
    private Long id;
    private String nome;
    private String email;
    private String fotoUrl;
    private Cargo cargo;
    private LocalDate createdAt;
    private boolean ativo;

    public FuncionarioResponseDto(Funcionario funcionario) {
        this.ativo = funcionario.isAtivo();
        this.id = funcionario.getId();
        this.nome = funcionario.getNome();
        this.email = funcionario.getEmail();
        this.fotoUrl = funcionario.getFotoUrl();
        this.cargo = funcionario.getCargoGlobal();
        this.createdAt = funcionario.getCreatedAt();
    }

    public static FuncionarioResponseDto from(Funcionario funcionario) {
        return new FuncionarioResponseDto(funcionario);
    }

    public Long getId() {
        return id;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }
}
