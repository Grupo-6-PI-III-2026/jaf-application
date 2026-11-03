package com.jaf.application.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nota_fiscal")
public class NotaFiscal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gasto_id", nullable = false)
    @JsonIgnore
    private Gasto gasto;

    private String numero;
    @Column(name = "cnpj_emitente")
    private String cnpjEmitente;
    private BigDecimal valor;
    @Column(name = "data_emissao")
    private LocalDate dataEmissao;
    @Column(name = "arquivo_chave", nullable = false)
    @JsonIgnore
    private String arquivoChave;
    @Column(name = "arquivo_nome", nullable = false)
    private String arquivoNome;
    @Column(name = "content_type", nullable = false)
    @JsonIgnore
    private String contentType;
    @Column(name = "tamanho_bytes", nullable = false)
    @JsonIgnore
    private long tamanhoBytes;
    @Column(nullable = false, length = 64)
    @JsonIgnore
    private String sha256;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por", nullable = false)
    @JsonIgnore
    private Funcionario criadoPor;
    @Column(name = "criado_em", nullable = false)
    @JsonIgnore
    private LocalDateTime criadoEm;

    @PrePersist
    void onCreate() {
        if (criadoEm == null) criadoEm = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Gasto getGasto() { return gasto; }
    public void setGasto(Gasto gasto) { this.gasto = gasto; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public String getCnpjEmitente() { return cnpjEmitente; }
    public void setCnpjEmitente(String cnpjEmitente) { this.cnpjEmitente = cnpjEmitente; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public LocalDate getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDate dataEmissao) { this.dataEmissao = dataEmissao; }
    public String getArquivoChave() { return arquivoChave; }
    public void setArquivoChave(String arquivoChave) { this.arquivoChave = arquivoChave; }
    public String getArquivoNome() { return arquivoNome; }
    public void setArquivoNome(String arquivoNome) { this.arquivoNome = arquivoNome; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getTamanhoBytes() { return tamanhoBytes; }
    public void setTamanhoBytes(long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public Funcionario getCriadoPor() { return criadoPor; }
    public void setCriadoPor(Funcionario criadoPor) { this.criadoPor = criadoPor; }
}
