package com.jaf.application.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.time.LocalDate;

@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Entity
public class Gasto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;
    private String categoria;
    private String metodoPagamento;
    private String etapa;
    private BigDecimal valor;
    private LocalDate dtGasto;

    private Boolean reembolsoConcluido;
    private boolean reembolsavel;

    @Version
    @JsonIgnore
    @Column(nullable = false)
    private Long versao;

    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonIgnore
    private java.time.LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    @JsonIgnore
    private java.time.LocalDateTime updatedAt;
    @Column(name = "created_by")
    @JsonIgnore
    @org.springframework.data.annotation.CreatedBy
    private Long createdBy;
    @Column(name = "updated_by")
    @JsonIgnore
    @org.springframework.data.annotation.LastModifiedBy
    private Long updatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }

    public LocalDate getDtGasto() {
        return dtGasto;
    }

    public void setDtGasto(LocalDate dtGasto) {
        this.dtGasto = dtGasto;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public Obra getObra() {
        return obra;
    }

    public void setObra(Obra obra) {
        this.obra = obra;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Boolean getReembolsoConcluido() {
        return reembolsoConcluido;
    }

    public void setReembolsoConcluido(Boolean reembolsoConcluido) {
        this.reembolsoConcluido = reembolsoConcluido;
    }

    public boolean isReembolsavel() {
        return reembolsavel || (metodoPagamento != null && "REEMBOLSO".equalsIgnoreCase(metodoPagamento.trim()));
    }
    public void setReembolsavel(boolean reembolsavel) { this.reembolsavel = reembolsavel; }
    public Long getVersao() { return versao; }
    public void setVersao(Long versao) { this.versao = versao; }
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
