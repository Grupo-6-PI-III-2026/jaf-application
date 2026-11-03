package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class FuncionarioUpdateRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    @Pattern(regexp = "^[\\p{L}][\\p{L}\\s'.\\-]+$")
    private String nome;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @NotNull
    private Cargo cargo;

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
