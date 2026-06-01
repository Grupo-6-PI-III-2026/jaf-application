package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.TipoFuncionario;
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
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String senha;

    @NotNull(message = "Cargo é obrigatório")
    private Cargo cargo;

    private String documento; // Opcional para internos

    private TipoFuncionario tipoFuncionario; // Opcional, default será INTERNO

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

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public TipoFuncionario getTipoFuncionario() {
        return tipoFuncionario;
    }

    public void setTipoFuncionario(TipoFuncionario tipoFuncionario) {
        this.tipoFuncionario = tipoFuncionario;
    }
}
