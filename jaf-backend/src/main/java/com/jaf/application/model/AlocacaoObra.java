package com.jaf.application.model;

import com.jaf.application.enums.Cargo;
import com.jaf.application.enums.FuncaoObra;
import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;

@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
@Entity
public class AlocacaoObra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    private Funcionario funcionario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "obra_id")
    private Obra obra;

    @Enumerated(EnumType.STRING)
    private FuncaoObra cargo;

    private LocalDate dtInicio;
    private LocalDate dtFim;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public FuncaoObra getCargo() {
        return cargo;
    }

    public void setCargo(FuncaoObra cargo) {
        this.cargo = cargo;
    }

    @Deprecated
    public void setCargo(Cargo cargo) {
        this.cargo = cargo == null ? null : switch (cargo) {
            case GESTOR_OBRA -> FuncaoObra.GESTOR_OBRA;
            case ENGENHEIRO -> FuncaoObra.ENGENHEIRO;
            case ARQUITETO -> FuncaoObra.ARQUITETO;
            case MESTRE_DE_OBRAS -> FuncaoObra.MESTRE_DE_OBRAS;
            case OPERADOR_LANCAMENTO -> FuncaoObra.OPERADOR_LANCAMENTO;
            case PEDREIRO -> FuncaoObra.PEDREIRO;
            case PINTOR -> FuncaoObra.PINTOR;
            case MARCENEIRO -> FuncaoObra.MARCENEIRO;
            case GESSEIRO -> FuncaoObra.GESSEIRO;
            default -> FuncaoObra.ENGENHEIRO;
        };
    }

    public LocalDate getDtInicio() {
        return dtInicio;
    }

    public void setDtInicio(LocalDate dtInicio) {
        this.dtInicio = dtInicio;
    }

    public LocalDate getDtFim() {
        return dtFim;
    }

    public void setDtFim(LocalDate dtFim) {
        this.dtFim = dtFim;
    }

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
        if (dtInicio == null) {
            dtInicio = LocalDate.now();
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = java.time.LocalDateTime.now(); }
}
