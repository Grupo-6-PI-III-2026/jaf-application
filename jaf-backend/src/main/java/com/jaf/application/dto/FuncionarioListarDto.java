package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

public class FuncionarioListarDto {
    @Schema(description = "ID do funcionário", example = "1")
    private Long id;

    @Schema(description = "Nome do funcionário", example = "Guilherme")
    private String nome;

    @Schema(description = "E-mail do funcionário", example = "guilherme@gmail.com")
    private String email;

    @Schema(description = "Cargo do funcionário", example = "Administrador")
    private Cargo cargo;

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
}
