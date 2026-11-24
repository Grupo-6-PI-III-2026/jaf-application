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

    // Sem @Lob: no PostgreSQL o Hibernate mapearia String @Lob para oid (large object), não TEXT.
    @Column(name = "texto_bruto", columnDefinition = "TEXT")
    private String textoBruto;

    @Column(name = "mensagem_erro", length = 1000)
    private String mensagemErro;

    @Column(name = "arquivo_chave")
    private String arquivoChave;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    protected OcrProcessamento() {
    }

    public OcrProcessamento(String id, String solicitante) {
        this(id, solicitante, null);
    }

    public OcrProcessamento(String id, String solicitante, String arquivoChave) {
        this.id = id;
        this.solicitante = solicitante;
        this.arquivoChave = arquivoChave;
        this.status = StatusOcr.PROCESSANDO;
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = this.criadoEm;
    }

    /** Transfere o arquivo para um gasto; o mesmo arquivo nunca fica ligado a dois gastos. */
    public String liberarArquivo() {
        String chave = arquivoChave;
        arquivoChave = null;
        atualizadoEm = LocalDateTime.now();
        return chave;
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

    public String getArquivoChave() {
        return arquivoChave;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }
}
