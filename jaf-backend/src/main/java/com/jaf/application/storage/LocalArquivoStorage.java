package com.jaf.application.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@Component
public class LocalArquivoStorage implements ArquivoStorage {
    private final Path raiz;

    public LocalArquivoStorage(@Value("${app.storage.local-dir:uploads}") String diretorio) {
        this.raiz = Paths.get(diretorio).toAbsolutePath().normalize();
    }

    @Override
    public void salvar(String chave, InputStream conteudo) throws IOException {
        Path destino = resolver(chave);
        Files.createDirectories(destino.getParent());
        Files.copy(conteudo, destino, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public InputStream abrir(String chave) throws IOException {
        return Files.newInputStream(resolver(chave), StandardOpenOption.READ);
    }

    @Override
    public void remover(String chave) throws IOException {
        Files.deleteIfExists(resolver(chave));
    }

    private Path resolver(String chave) {
        Path destino = raiz.resolve(chave).normalize();
        if (!destino.startsWith(raiz)) {
            throw new IllegalArgumentException("Chave de arquivo inválida.");
        }
        return destino;
    }
}
