package com.jaf.application.dto;

import jakarta.validation.constraints.*;

public class FuncionarioDto {
    @NotBlank(message = "Nome não pode ser vazio!")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres!")
    private String nome;

    @NotBlank(message = "E-mail não deve ser vazio!")
    @Email(message = "E-mail inválido!")
    private String email;

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{6,}$",
            message = "Senha deve ter pelo menos 1 letra maiúscula, 1 minúscula e 1 número"
    )
    @NotBlank(message = "Senha não deve ser vazia!")
    @Size(min = 6, message = "Senha")
    private String senha;

    @NotBlank(message = "Cargo é obrigatório!")
    @Size(max = 50, message = "Cargo deve ter no máximo 50 caracteres")
    private String cargo;

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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }
}
