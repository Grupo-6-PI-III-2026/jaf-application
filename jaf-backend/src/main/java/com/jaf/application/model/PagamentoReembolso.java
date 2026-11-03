package com.jaf.application.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "pagamento_reembolso")
public class PagamentoReembolso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gasto_id", nullable = false)
    private Gasto gasto;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private LocalDate dataPagamento;
    @Column(length = 1000)
    private String observacao;
    @Column(name = "cancelado_em")
    private java.time.LocalDateTime canceladoEm;
    @Column(name = "cancelado_por")
    private Long canceladoPor;
    @Column(name = "motivo_cancelamento", length = 500)
    private String motivoCancelamento;
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonIgnore
    private java.time.LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private java.time.LocalDateTime updatedAt;
    @Column(name = "created_by")
    @JsonIgnore
    private Long createdBy;
    @Column(name = "updated_by")
    @JsonIgnore
    private Long updatedBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Gasto getGasto() { return gasto; }
    public void setGasto(Gasto gasto) { this.gasto = gasto; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataPagamento() { return dataPagamento; }
    public void setDataPagamento(LocalDate dataPagamento) { this.dataPagamento = dataPagamento; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public java.time.LocalDateTime getCanceladoEm() { return canceladoEm; }
    public void setCanceladoEm(java.time.LocalDateTime canceladoEm) { this.canceladoEm = canceladoEm; }
    public Long getCanceladoPor() { return canceladoPor; }
    public void setCanceladoPor(Long canceladoPor) { this.canceladoPor = canceladoPor; }
    public String getMotivoCancelamento() { return motivoCancelamento; }
    public void setMotivoCancelamento(String motivoCancelamento) { this.motivoCancelamento = motivoCancelamento; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    public java.time.LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }

    @PrePersist
    protected void onCreate() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = java.time.LocalDateTime.now(); }
}