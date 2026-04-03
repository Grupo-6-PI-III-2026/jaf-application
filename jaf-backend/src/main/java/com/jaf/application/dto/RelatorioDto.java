package com.jaf.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;

public class RelatorioDto {

    @NotBlank(message = "O título do relatório é obrigatório")
    @Size(min = 3, max = 100, message = "O título deve ter entre 3 e 100 caracteres")
    private String titulo;

    @NotNull(message = "A data de emissão é obrigatória")
    private Date dtEmissao;

    @NotNull(message = "O ID do funcionário responsável é obrigatório")
    private Long funcionarioResponsavelId;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Date getDtEmissao() {
        return dtEmissao;
    }

    public void setDtEmissao(Date dtEmissao) {
        this.dtEmissao = dtEmissao;
    }

    public Long getFuncionarioResponsavelId() {
        return funcionarioResponsavelId;
    }

    public void setFuncionarioResponsavelId(Long funcionarioResponsavelId) {
        this.funcionarioResponsavelId = funcionarioResponsavelId;
    }
}
