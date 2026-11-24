package com.jaf.application.armazenamento;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Pattern;

/** Disco local para desenvolvimento; os arquivos só são servidos por URL assinada com prazo. */
@Component
@ConditionalOnProperty(name = "app.armazenamento.tipo", havingValue = "local", matchIfMissing = true)
public class ArmazenamentoLocal implements ArmazenamentoArquivos {
    private static final Pattern CHAVE_VALIDA = Pattern.compile("^[a-z]+/[A-Za-z0-9-]+\\.[a-z0-9]{2,5}$");
    private static final String ALGORITMO = "HmacSHA256";

    private final Path raiz;
    private final String urlPublica;
    private final byte[] segredo;

    public ArmazenamentoLocal(@Value("${app.armazenamento.local.diretorio:uploads}") String diretorio,
                              @Value("${app.armazenamento.local.url-publica:/api}") String urlPublica,
                              @Value("${app.armazenamento.local.segredo:}") String segredo) {
        this.raiz = Path.of(diretorio).toAbsolutePath().normalize();
        this.urlPublica = urlPublica.endsWith("/") ? urlPublica.substring(0, urlPublica.length() - 1) : urlPublica;
        // Sem segredo configurado, as URLs deixam de valer ao reiniciar o backend.
        this.segredo = segredo.isBlank() ? segredoAleatorio() : segredo.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void salvar(String chave, byte[] conteudo, String contentType) {
        Path destino = resolver(chave);
        try {
            Files.createDirectories(destino.getParent());
            Files.write(destino, conteudo, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao gravar o arquivo " + chave, exception);
        }
    }

    @Override
    public String gerarUrl(String chave, Duration validade) {
        long expira = Instant.now().plus(validade).getEpochSecond();
        return urlPublica + "/arquivos?chave=" + URLEncoder.encode(chave, StandardCharsets.UTF_8)
                + "&expira=" + expira + "&assinatura=" + assinar(chave, expira);
    }

    @Override
    public void remover(String chave) {
        try {
            Files.deleteIfExists(resolver(chave));
        } catch (IOException exception) {
            throw new UncheckedIOException("Falha ao remover o arquivo " + chave, exception);
        }
    }

    public Optional<Path> abrir(String chave, long expira, String assinatura) {
        if (chave == null || assinatura == null || expira < Instant.now().getEpochSecond()
                || !CHAVE_VALIDA.matcher(chave).matches()) {
            return Optional.empty();
        }
        byte[] esperada = assinar(chave, expira).getBytes(StandardCharsets.US_ASCII);
        if (!MessageDigest.isEqual(esperada, assinatura.getBytes(StandardCharsets.US_ASCII))) {
            return Optional.empty();
        }
        Path arquivo = resolver(chave);
        return Files.isRegularFile(arquivo) ? Optional.of(arquivo) : Optional.empty();
    }

    private Path resolver(String chave) {
        if (chave == null || !CHAVE_VALIDA.matcher(chave).matches()) {
            throw new IllegalArgumentException("Chave de arquivo inválida.");
        }
        Path caminho = raiz.resolve(chave).normalize();
        if (!caminho.startsWith(raiz)) {
            throw new IllegalArgumentException("Chave de arquivo inválida.");
        }
        return caminho;
    }

    private String assinar(String chave, long expira) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(segredo, ALGORITMO));
            byte[] assinatura = mac.doFinal((chave + "\n" + expira).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 indisponível.", exception);
        }
    }

    private static byte[] segredoAleatorio() {
        byte[] segredo = new byte[32];
        new SecureRandom().nextBytes(segredo);
        return segredo;
    }
}
