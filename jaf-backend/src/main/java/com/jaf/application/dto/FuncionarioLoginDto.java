package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public class FuncionarioLoginDto {

    @NotBlank(message = "E-mail não pode estar vazio!")
    @Schema(description = "E-mail do funcionário", example = "admin@gmail.com")
    private String email;

    @NotBlank(message = "Senha não pode estar vazia!")
    @Schema(description = "Senha do funcionário", example = "Admin@123")
    private String senha;

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

}
