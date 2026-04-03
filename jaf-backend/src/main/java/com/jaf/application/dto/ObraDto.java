package com.jaf.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Date;

public class ObraDto {

    @NotBlank(message = "O título da obra é obrigatório")
    @Size(min = 5, max = 150, message = "O título deve ter entre 5 e 150 caracteres")
    private String titulo;

    @NotBlank(message = "O orçamento é obrigatório")
    private String orcamento;

    @NotBlank(message = "O status é obrigatório")
    private String status;

    @NotNull(message = "A data de início é obrigatória")
    private Date dtInicio;

    @NotNull(message = "A data de término prevista é obrigatória")
    @FutureOrPresent(message = "A data de término prevista não pode estar no passado")
    private Date dtTerminoPrevisto;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(String orcamento) {
        this.orcamento = orcamento;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getDtInicio() {
        return dtInicio;
    }

    public void setDtInicio(Date dtInicio) {
        this.dtInicio = dtInicio;
    }

    public Date getDtTerminoPrevisto() {
        return dtTerminoPrevisto;
    }

    public void setDtTerminoPrevisto(Date dtTerminoPrevisto) {
        this.dtTerminoPrevisto = dtTerminoPrevisto;
    }
}
