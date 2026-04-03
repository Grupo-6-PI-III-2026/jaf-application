package com.jaf.application.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GastoDto {
    @NotBlank(message = "A descrição não pode estar vazia")
    @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
    private String descricao;

    @NotBlank(message = "A categoria é obrigatória")
    private String categoria;

    @NotBlank(message = "O método de pagamento é obrigatório")
    private String metodoPagamento;

    private String etapa;

    @NotNull(message = "O valor do gasto é obrigatório")
    @Positive(message = "O valor deve ser maior que zero")
    private BigDecimal valor;

    @NotNull(message = "A data do gasto é obrigatória")
    @PastOrPresent(message = "A data do gasto não pode ser no futuro")
    private LocalDate dtGasto;

    @NotNull(message = "O funcionário responsável é obrigatório")
    private Long funcionarioId;

    @NotNull(message = "A obra é obrigatória")
    private Long obraId;

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

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDate getDtGasto() {
        return dtGasto;
    }

    public void setDtGasto(LocalDate dtGasto) {
        this.dtGasto = dtGasto;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public Long getObraId() {
        return obraId;
    }

    public void setObraId(Long obraId) {
        this.obraId = obraId;
    }
}
