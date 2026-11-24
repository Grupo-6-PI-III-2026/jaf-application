package com.jaf.application.armazenamento;

import com.jaf.application.exceptions.BadRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
public class ArquivoService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArquivoService.class);
    private static final long MEGABYTE = 1024L * 1024L;

    private final ArmazenamentoArquivos armazenamento;
    private final Duration validadeUrl;

    public ArquivoService(ArmazenamentoArquivos armazenamento,
                          @Value("${app.armazenamento.url-validade-minutos:15}") long validadeMinutos) {
        this.armazenamento = armazenamento;
        this.validadeUrl = Duration.ofMinutes(validadeMinutos);
    }

    public String armazenar(MultipartFile arquivo, String pasta, Set<TipoArquivo> permitidos, long tamanhoMaximo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new BadRequest("Selecione um arquivo.");
        }
        if (arquivo.getSize() > tamanhoMaximo) {
            throw new BadRequest("O arquivo deve ter no máximo " + tamanhoMaximo / MEGABYTE + " MB.");
        }
        try {
            return armazenar(arquivo.getBytes(), pasta, permitidos);
        } catch (IOException exception) {
            throw new BadRequest("Não foi possível ler o arquivo enviado.");
        }
    }

    /** A extensão e o Content-Type gravados vêm do conteúdo real, nunca do nome enviado pelo usuário. */
    public String armazenar(byte[] conteudo, String pasta, Set<TipoArquivo> permitidos) {
        TipoArquivo tipo = TipoArquivo.detectar(conteudo)
                .filter(permitidos::contains)
                .orElseThrow(() -> new BadRequest(
                        "Formato não suportado. Envie um arquivo " + TipoArquivo.descrever(permitidos) + "."));
        String chave = pasta + "/" + UUID.randomUUID() + "." + tipo.getExtensao();
        armazenamento.salvar(chave, conteudo, tipo.getContentType());
        return chave;
    }

    public String gerarUrl(String chave) {
        return chave == null || chave.isBlank() ? null : armazenamento.gerarUrl(chave, validadeUrl);
    }

    /** Remove só depois do commit para não apagar um arquivo ainda referenciado se a transação falhar. */
    public void removerAposCommit(String chave) {
        if (chave == null || chave.isBlank()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removerSilenciosamente(chave);
                }
            });
        } else {
            removerSilenciosamente(chave);
        }
    }

    private void removerSilenciosamente(String chave) {
        try {
            armazenamento.remover(chave);
        } catch (RuntimeException exception) {
            LOGGER.warn("Não foi possível remover o arquivo {}", chave, exception);
        }
    }
}
