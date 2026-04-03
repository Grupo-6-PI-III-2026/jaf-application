package com.jaf.application.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "obra_id")
    private Long id;

    private String titulo;
    private String orcamento;
    private String status;

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
