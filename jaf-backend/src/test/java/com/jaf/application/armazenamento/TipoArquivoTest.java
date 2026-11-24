package com.jaf.application.armazenamento;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TipoArquivoTest {

    private static byte[] bytes(int... valores) {
        byte[] resultado = new byte[valores.length];
        for (int i = 0; i < valores.length; i++) resultado[i] = (byte) valores[i];
        return resultado;
    }

    @Test
    void detectaPelosBytesIniciais() {
        assertEquals(Optional.of(TipoArquivo.JPEG), TipoArquivo.detectar(bytes(0xFF, 0xD8, 0xFF, 0xE0)));
        assertEquals(Optional.of(TipoArquivo.PNG), TipoArquivo.detectar(bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)));
        assertEquals(Optional.of(TipoArquivo.GIF), TipoArquivo.detectar("GIF89a...".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.of(TipoArquivo.WEBP), TipoArquivo.detectar("RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.of(TipoArquivo.PDF), TipoArquivo.detectar("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)));
        assertEquals(Optional.of(TipoArquivo.TIFF), TipoArquivo.detectar(bytes(0x49, 0x49, 0x2A, 0x00)));
    }

    @Test
    void rejeitaHtmlSvgEConteudoVazio() {
        assertTrue(TipoArquivo.detectar("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(TipoArquivo.detectar("<svg onload=alert(1)>".getBytes(StandardCharsets.UTF_8)).isEmpty());
        assertTrue(TipoArquivo.detectar(new byte[0]).isEmpty());
        assertTrue(TipoArquivo.detectar(null).isEmpty());
    }

    @Test
    void identificaPelaExtensaoDaChave() {
        assertEquals(Optional.of(TipoArquivo.JPEG), TipoArquivo.porExtensao("fotos/a.jpeg"));
        assertEquals(Optional.of(TipoArquivo.PDF), TipoArquivo.porExtensao("notas/a.pdf"));
        assertTrue(TipoArquivo.porExtensao("fotos/a.html").isEmpty());
        assertFalse(TipoArquivo.IMAGENS.contains(TipoArquivo.PDF));
    }
}
