package com.jaf.application.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;


public class ObraDto {

    @NotBlank(message = "O título da obra é obrigatório")
    @Size(min = 5, max = 150, message = "O título deve ter entre 5 e 150 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9À-ÿ\\s\\-]+$", message = "O título deve conter apenas letras, números, espaços e hífens")
    private String titulo;

    @NotBlank(message = "O orçamento é obrigatório")
    @Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "O orçamento deve ser um valor numérico válido")
    private String orcamento;

    @NotBlank(message = "O status é obrigatório")
    @Pattern(regexp = "^(EM_ANDAMENTO|CONCLUIDA|CANCELADA|PLANEJADA)$", message = "Status inválido. Use: EM_ANDAMENTO, CONCLUIDA, CANCELADA ou PLANEJADA")
    private String status;

    @NotNull(message = "A data de início é obrigatória")
    @FutureOrPresent(message = "A data de início deve ser hoje ou no futuro")
    private LocalDate dtInicio;

    @NotNull(message = "A data de término prevista é obrigatória")
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
