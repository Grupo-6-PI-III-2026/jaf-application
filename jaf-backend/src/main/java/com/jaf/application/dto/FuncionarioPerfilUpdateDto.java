package com.jaf.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FuncionarioPerfilUpdateDto {
    @NotBlank(message = "Nome nao pode ser vazio!")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres!")
    private String nome;

    @NotBlank(message = "E-mail nao deve ser vazio!")
    @Email(message = "E-mail invalido!")
    private String email;

    @Size(max = 512, message = "Foto URL deve ter no maximo 512 caracteres")
    private String fotoUrl;

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
}
