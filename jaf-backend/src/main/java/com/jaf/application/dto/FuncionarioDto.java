package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import jakarta.validation.constraints.*;

public class FuncionarioDto {
    @NotBlank(message = "Nome não pode ser vazio!")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres!")
    @Pattern(regexp = "^[a-zA-ZÀ-ÿ\\s]+$", message = "Nome deve conter apenas letras e espaços")
    private String nome;

    @NotBlank(message = "E-mail não deve ser vazio!")
    @Email(message = "E-mail inválido!")
    @Size(max = 100, message = "E-mail deve ter no máximo 100 caracteres")
    private String email;

    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Senha deve ter no mínimo 8 caracteres, incluindo 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial"
    )
    @NotBlank(message = "Senha não deve ser vazia!")
    private String senha;

    @NotNull(message = "Cargo é obrigatório")
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

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public void setCargo(Cargo cargo) {
        this.cargo = cargo;
    }
}
