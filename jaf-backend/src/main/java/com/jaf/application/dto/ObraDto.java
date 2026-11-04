package com.jaf.application.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;


public class ObraDto {

    @NotBlank(message = "O título da obra é obrigatório")
    @Size(min = 5, message = "O título da obra deve ter no mínimo 5 caracteres")
    @Size(max = 150, message = "O título da obra deve ter no máximo 150 caracteres")
    @Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-.,()/'&]+$", message = "O título da obra contém caracteres inválidos")
    private String titulo;

    @NotBlank(message = "O orçamento é obrigatório")
    @Pattern(regexp = "^(?=.*[1-9])\\d+(\\.\\d{1,2})?$", message = "O orçamento deve ser maior que zero")
    private String orcamento;

    @NotBlank(message = "O status é obrigatório")
    @Pattern(regexp = "^(EM_ANDAMENTO|CONCLUIDA|CANCELADA|PLANEJADA)$", message = "Status inválido. Use: EM_ANDAMENTO, CONCLUIDA, CANCELADA ou PLANEJADA")
    private String status;

    @NotNull(message = "A data de início é obrigatória")
    private LocalDate dtInicio;

    @NotNull(message = "A data de término prevista é obrigatória")
    private LocalDate dtTerminoPrevisto;

    private Long responsavelId;

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    private Long versao;

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

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
