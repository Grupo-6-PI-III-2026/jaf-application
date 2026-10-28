package com.jaf.application.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.jaf.application.config.ObraSerializer.class)
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    private String orcamento;
    private String status;

    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal totalRecebido = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getTotalRecebido() { return totalRecebido; }
    public void setTotalRecebido(java.math.BigDecimal totalRecebido) { this.totalRecebido = totalRecebido; }

    private LocalDate dtInicio;
    private LocalDate dtTerminoPrevisto;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
