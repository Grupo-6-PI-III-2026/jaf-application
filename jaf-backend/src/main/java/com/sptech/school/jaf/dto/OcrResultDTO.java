package com.sptech.school.jaf.dto;

public class OcrResultDTO {

    private String cnpjEmitente;
    private String dataEmissao;
    private String numeroNf;
    private String valorTotal;
    private String destinatario;
    private String descricaoItens;
    private String textoOriginalOcr; // texto bruto retornado pelo Filestack, útil para debug

    public OcrResultDTO() {
    }

    public String getCnpjEmitente() { return cnpjEmitente; }
    public void setCnpjEmitente(String cnpjEmitente) { this.cnpjEmitente = cnpjEmitente; }

    public String getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(String dataEmissao) { this.dataEmissao = dataEmissao; }

    public String getNumeroNf() { return numeroNf; }
    public void setNumeroNf(String numeroNf) { this.numeroNf = numeroNf; }

    public String getValorTotal() { return valorTotal; }
    public void setValorTotal(String valorTotal) { this.valorTotal = valorTotal; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getDescricaoItens() { return descricaoItens; }
    public void setDescricaoItens(String descricaoItens) { this.descricaoItens = descricaoItens; }

    public String getTextoOriginalOcr() { return textoOriginalOcr; }
    public void setTextoOriginalOcr(String textoOriginalOcr) { this.textoOriginalOcr = textoOriginalOcr; }
}
