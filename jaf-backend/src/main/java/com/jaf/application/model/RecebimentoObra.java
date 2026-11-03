package com.jaf.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "recebimento_obra")
public class RecebimentoObra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obra_id", nullable = false)
    @JsonIgnore
    private Obra obra;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    @Column(name = "data", nullable = false)
    private LocalDate data;
    private String descricao;
    @Column(name = "criado_por")
    private Long criadoPor;
    @Column(name = "criado_em", nullable = false)
    @JsonIgnore
    private LocalDateTime criadoEm;
    public Long getId() { return id; }
    public Obra getObra() { return obra; }
    public void setObra(Obra obra) { this.obra = obra; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public Long getCriadoPor() { return criadoPor; }
    public void setCriadoPor(Long criadoPor) { this.criadoPor = criadoPor; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    @PrePersist void onCreate() { if (criadoEm == null) criadoEm = LocalDateTime.now(); }
}
