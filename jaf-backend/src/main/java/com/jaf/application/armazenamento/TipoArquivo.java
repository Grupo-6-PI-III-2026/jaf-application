package com.jaf.application.armazenamento;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tipos aceitos, identificados pelos bytes iniciais e nunca pelo nome ou Content-Type enviados pelo cliente.
 */
public enum TipoArquivo {
    JPEG("image/jpeg", "jpg", "JPG"),
    PNG("image/png", "png", "PNG"),
    GIF("image/gif", "gif", "GIF"),
    WEBP("image/webp", "webp", "WEBP"),
    BMP("image/bmp", "bmp", "BMP"),
    TIFF("image/tiff", "tiff", "TIFF"),
    PDF("application/pdf", "pdf", "PDF");

    public static final Set<TipoArquivo> IMAGENS = EnumSet.of(JPEG, PNG, GIF, WEBP);
    public static final Set<TipoArquivo> NOTAS_FISCAIS = EnumSet.allOf(TipoArquivo.class);

    private final String contentType;
    private final String extensao;
    private final String rotulo;

    TipoArquivo(String contentType, String extensao, String rotulo) {
        this.contentType = contentType;
        this.extensao = extensao;
        this.rotulo = rotulo;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtensao() {
        return extensao;
    }

    public static Optional<TipoArquivo> detectar(byte[] conteudo) {
        if (conteudo == null) {
            return Optional.empty();
        }
        if (comeca(conteudo, 0, 0xFF, 0xD8, 0xFF)) return Optional.of(JPEG);
        if (comeca(conteudo, 0, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return Optional.of(PNG);
        if (comecaTexto(conteudo, 0, "GIF87a") || comecaTexto(conteudo, 0, "GIF89a")) return Optional.of(GIF);
        if (comecaTexto(conteudo, 0, "RIFF") && comecaTexto(conteudo, 8, "WEBP")) return Optional.of(WEBP);
        if (comecaTexto(conteudo, 0, "%PDF-")) return Optional.of(PDF);
        if (comeca(conteudo, 0, 0x49, 0x49, 0x2A, 0x00) || comeca(conteudo, 0, 0x4D, 0x4D, 0x00, 0x2A)) {
            return Optional.of(TIFF);
        }
        if (comecaTexto(conteudo, 0, "BM") && conteudo.length > 14) return Optional.of(BMP);
        return Optional.empty();
    }

    public static Optional<TipoArquivo> porExtensao(String nomeOuChave) {
        if (nomeOuChave == null || !nomeOuChave.contains(".")) {
            return Optional.empty();
        }
        String extensao = nomeOuChave.substring(nomeOuChave.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if ("jpeg".equals(extensao)) return Optional.of(JPEG);
        if ("tif".equals(extensao)) return Optional.of(TIFF);
        return Arrays.stream(values()).filter(tipo -> tipo.extensao.equals(extensao)).findFirst();
    }

    public static String descrever(Set<TipoArquivo> tipos) {
        return tipos.stream().map(tipo -> tipo.rotulo).collect(Collectors.joining(", "));
    }

    private static boolean comeca(byte[] conteudo, int deslocamento, int... assinatura) {
        if (conteudo.length < deslocamento + assinatura.length) {
            return false;
        }
        for (int i = 0; i < assinatura.length; i++) {
            if ((conteudo[deslocamento + i] & 0xFF) != assinatura[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean comecaTexto(byte[] conteudo, int deslocamento, String assinatura) {
        byte[] bytes = assinatura.getBytes(StandardCharsets.US_ASCII);
        if (conteudo.length < deslocamento + bytes.length) {
            return false;
        }
        return Arrays.equals(conteudo, deslocamento, deslocamento + bytes.length, bytes, 0, bytes.length);
    }
}
