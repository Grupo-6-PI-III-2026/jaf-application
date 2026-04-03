package com.jaf.application.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public class ObraDto {

    @NotBlank(message = "O título da obra é obrigatório")
    @Size(min = 5, max = 150, message = "O título deve ter entre 5 e 150 caracteres")
    private String titulo;

    @NotBlank(message = "O orçamento é obrigatório")
    private String orcamento;

    @NotBlank(message = "O status é obrigatório")
    private String status;

    @NotNull(message = "A data de início é obrigatória")
    private LocalDate dtInicio;

    @NotNull(message = "A data de término prevista é obrigatória")
    @FutureOrPresent(message = "A data de término prevista não pode estar no passado")
    private LocalDate dtTerminoPrevisto;

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

    public LocalDate getDtInicio() {
        return dtInicio;
    }

    public void setDtInicio(LocalDate dtInicio) {
        this.dtInicio = dtInicio;
    }

    public LocalDate getDtTerminoPrevisto() {
        return dtTerminoPrevisto;
    }

    public void setDtTerminoPrevisto(LocalDate dtTerminoPrevisto) {
        this.dtTerminoPrevisto = dtTerminoPrevisto;
    }
}
