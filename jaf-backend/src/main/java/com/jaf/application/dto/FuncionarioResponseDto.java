package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.model.Funcionario;

public class FuncionarioResponseDto {
    private Long id;
    private String nome;
    private String email;
    private Cargo cargo;

    public FuncionarioResponseDto(Funcionario funcionario) {
        this.id = funcionario.getId();
        this.nome = funcionario.getNome();
        this.email = funcionario.getEmail();
        this.cargo = funcionario.getCargoGlobal();
    }

    public Long getId() {
        return id;
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

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }
}
