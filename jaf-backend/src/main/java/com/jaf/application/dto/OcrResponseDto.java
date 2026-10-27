package com.jaf.application.dto;

import com.jaf.application.enums.StatusOcr;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OcrResponseDto {
    private String jobId;
    private StatusOcr status;
    private boolean sucesso;
    private String mensagem;
    private String textoBruto;
    private BigDecimal valor;
    private LocalDate dtGasto;
    private String metodoPagamento;
    private String materialInsumo;
    private String descricaoAdicional;
    private String etapa;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public StatusOcr getStatus() {
        return status;
    }

    public void setStatus(StatusOcr status) {
        this.status = status;
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public void setSucesso(boolean sucesso) {
        this.sucesso = sucesso;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getTextoBruto() {
        return textoBruto;
    }

    public void setTextoBruto(String textoBruto) {
        this.textoBruto = textoBruto;
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

    public String getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(String metodoPagamento) {
        this.metodoPagamento = metodoPagamento;
    }

    public String getMaterialInsumo() {
        return materialInsumo;
    }

    public void setMaterialInsumo(String materialInsumo) {
        this.materialInsumo = materialInsumo;
    }

    public String getDescricaoAdicional() {
        return descricaoAdicional;
    }

    public void setDescricaoAdicional(String descricaoAdicional) {
        this.descricaoAdicional = descricaoAdicional;
    }

    public String getEtapa() {
        return etapa;
    }

    public void setEtapa(String etapa) {
        this.etapa = etapa;
    }
}