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

    // O e-mail pode mudar; o id identifica o dono do job. Registros antigos podem não ter o id.
    @Column(name = "solicitante_id")
    private Long solicitanteId;

    // Sem @Lob: no PostgreSQL o Hibernate mapearia String @Lob para oid (large object), não TEXT.
    @Column(name = "texto_bruto", columnDefinition = "TEXT")
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
        this(id, solicitante, null);
    }

    public OcrProcessamento(String id, String solicitante, Long solicitanteId) {
        this.id = id;
        this.solicitante = solicitante;
        this.solicitanteId = solicitanteId;
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

    public Long getSolicitanteId() {
        return solicitanteId;
    }

    public boolean pertenceA(Long idSolicitante, String emailSolicitante) {
        if (solicitanteId != null && idSolicitante != null) return solicitanteId.equals(idSolicitante);
        return solicitante != null && solicitante.equalsIgnoreCase(emailSolicitante);
    }

    public String getTextoBruto() {
        return textoBruto;
    }

    public void limparTextoBruto() {
        this.textoBruto = null;
        this.atualizadoEm = LocalDateTime.now();
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
