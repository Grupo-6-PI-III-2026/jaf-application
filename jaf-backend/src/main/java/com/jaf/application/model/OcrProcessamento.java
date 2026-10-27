package com.jaf.application.model;

import com.jaf.application.enums.StatusOcr;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ocr_processamento")
public class OcrProcessamento {
    @Id
    @Column(length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusOcr status;

    @Column(nullable = false)
    private String solicitante;

    @Lob
    @Column(name = "texto_bruto")
    private String textoBruto;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected OcrProcessamento() {
    }

    public OcrProcessamento(String id, String solicitante) {
        this.id = id;
        this.solicitante = solicitante;
        this.status = StatusOcr.PROCESSANDO;
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    public void marcarConcluido(String textoBruto) {
        this.status = StatusOcr.CONCLUIDO;
        this.textoBruto = textoBruto;
        this.mensagemErro = null;
        this.atualizadoEm = LocalDateTime.now();
    }

    public void marcarErro(String mensagemErro) {
        this.status = StatusOcr.ERRO;
        this.mensagemErro = mensagemErro != null && mensagemErro.length() > 1000
                ? mensagemErro.substring(0, 997) + "..."
                : mensagemErro;
        this.atualizadoEm = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public StatusOcr getStatus() {
        return status;
    }

    public String getSolicitante() {
        return solicitante;
    }

    public String getTextoBruto() {
        return textoBruto;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
