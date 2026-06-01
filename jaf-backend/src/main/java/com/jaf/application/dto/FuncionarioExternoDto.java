package com.jaf.application.dto;

import com.jaf.application.enums.TipoFuncionario;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FuncionarioExternoDto {
    @NotBlank(message = "Nome não pode ser vazio!")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres!")
    private String nome;

    private String email; // Opcional para externos

    @NotBlank(message = "Documento é obrigatório para controle de presença!")
    @Size(min = 11, max = 20, message = "Documento deve ter entre 11 e 20 caracteres!")
    private String documento;

    @NotNull(message = "Tipo de funcionário é obrigatório")
    private TipoFuncionario tipoFuncionario;

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
