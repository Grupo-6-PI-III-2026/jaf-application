package com.jaf.application.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Entity
public class Obra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titulo;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal orcamento = BigDecimal.ZERO;
    private String status;

    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal totalRecebido = java.math.BigDecimal.ZERO;

    public java.math.BigDecimal getTotalRecebido() { return totalRecebido; }
    public void setTotalRecebido(java.math.BigDecimal totalRecebido) { this.totalRecebido = totalRecebido; }

    private LocalDate dtInicio;
    private LocalDate dtTerminoPrevisto;

    @Version
    @Column(nullable = false)
    @JsonIgnore
    private Long versao;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonIgnore
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    @JsonIgnore
    @org.springframework.data.annotation.CreatedBy
    private Long createdBy;
    @Column(name = "updated_by")
    @JsonIgnore
    @org.springframework.data.annotation.LastModifiedBy
    private Long updatedBy;

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

    public BigDecimal getOrcamento() {
        return orcamento;
    }

    public void setOrcamento(BigDecimal orcamento) {
        this.orcamento = orcamento;
    }

    public void setOrcamento(String orcamento) {
        this.orcamento = orcamento == null ? null : new BigDecimal(orcamento);
    }

    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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
