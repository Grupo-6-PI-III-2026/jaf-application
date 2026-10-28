package com.jaf.application.model;

import jakarta.persistence.*;
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
}