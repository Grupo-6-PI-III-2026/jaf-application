package com.jaf.application.armazenamento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ArmazenamentoLocalTest {

    @TempDir
    Path raiz;

    private Map<String, String> parametros(String url) {
        Map<String, String> resultado = new HashMap<>();
        for (String par : url.substring(url.indexOf('?') + 1).split("&")) {
            String[] partes = par.split("=", 2);
            resultado.put(partes[0], URLDecoder.decode(partes[1], StandardCharsets.UTF_8));
        }
        return resultado;
    }

    @Test
    void urlAssinadaAbreSomenteOArquivoAssinadoDentroDoPrazo() throws Exception {
        var armazenamento = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-de-teste");
        armazenamento.salvar("notas/abc-123.pdf", "%PDF-1.7".getBytes(), "application/pdf");

        String url = armazenamento.gerarUrl("notas/abc-123.pdf", Duration.ofMinutes(5));
        var p = parametros(url);
        long expira = Long.parseLong(p.get("expira"));

        assertTrue(url.startsWith("/api/arquivos?"));
        Path arquivo = armazenamento.abrir(p.get("chave"), expira, p.get("assinatura")).orElseThrow();
        assertEquals("%PDF-1.7", Files.readString(arquivo));

        assertTrue(armazenamento.abrir("notas/outro.pdf", expira, p.get("assinatura")).isEmpty());
        assertTrue(armazenamento.abrir(p.get("chave"), expira + 60, p.get("assinatura")).isEmpty());
        assertTrue(armazenamento.abrir(p.get("chave"), expira, p.get("assinatura") + "x").isEmpty());
    }

    @Test
    void urlExpiradaNaoAbre() {
        var armazenamento = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-de-teste");
        armazenamento.salvar("perfil/foto.png", new byte[]{1}, "image/png");
        var p = parametros(armazenamento.gerarUrl("perfil/foto.png", Duration.ofSeconds(-1)));

        assertTrue(armazenamento.abrir(p.get("chave"), Long.parseLong(p.get("expira")), p.get("assinatura")).isEmpty());
    }

    @Test
    void assinaturaDeOutroSegredoNaoVale() {
        var a = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-a");
        var b = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-b");
        a.salvar("perfil/foto.png", new byte[]{1}, "image/png");
        var p = parametros(a.gerarUrl("perfil/foto.png", Duration.ofMinutes(5)));

        assertTrue(b.abrir(p.get("chave"), Long.parseLong(p.get("expira")), p.get("assinatura")).isEmpty());
    }

    @Test
    void rejeitaChavesForaDoPadrao() {
        var armazenamento = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-de-teste");
        long futuro = Instant.now().plusSeconds(60).getEpochSecond();

        assertThrows(IllegalArgumentException.class,
                () -> armazenamento.salvar("../fora.png", new byte[]{1}, "image/png"));
        assertThrows(IllegalArgumentException.class,
                () -> armazenamento.salvar("notas/../../fora.png", new byte[]{1}, "image/png"));
        assertTrue(armazenamento.abrir("../../etc/passwd", futuro, "qualquer").isEmpty());
    }

    @Test
    void removerApagaOArquivo() {
        var armazenamento = new ArmazenamentoLocal(raiz.toString(), "/api", "segredo-de-teste");
        armazenamento.salvar("obras/foto.jpg", new byte[]{1}, "image/jpeg");

        armazenamento.remover("obras/foto.jpg");

        assertFalse(Files.exists(raiz.resolve("obras/foto.jpg")));
    }
}
