package com.jaf.application.controller;

import com.jaf.application.armazenamento.ArmazenamentoLocal;
import com.jaf.application.armazenamento.TipoArquivo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

/** Público por design: a autorização está na assinatura gerada pelo backend para quem pode ver o arquivo. */
@RestController
@ConditionalOnProperty(name = "app.armazenamento.tipo", havingValue = "local", matchIfMissing = true)
public class ArquivoLocalController {
    private final ArmazenamentoLocal armazenamento;

    public ArquivoLocalController(ArmazenamentoLocal armazenamento) {
        this.armazenamento = armazenamento;
    }

    @GetMapping("/arquivos")
    public ResponseEntity<Resource> baixar(@RequestParam String chave,
                                           @RequestParam long expira,
                                           @RequestParam String assinatura) {
        return armazenamento.abrir(chave, expira, assinatura)
                .<ResponseEntity<Resource>>map(arquivo -> {
                    long restante = Math.max(0, expira - Instant.now().getEpochSecond());
                    ResponseEntity.BodyBuilder resposta = ResponseEntity.ok()
                            .cacheControl(CacheControl.maxAge(Duration.ofSeconds(restante)).cachePrivate())
                            .header("X-Content-Type-Options", "nosniff");
                    TipoArquivo.porExtensao(chave).ifPresentOrElse(
                            tipo -> resposta.contentType(MediaType.parseMediaType(tipo.getContentType()))
                                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline"),
                            () -> resposta.contentType(MediaType.APPLICATION_OCTET_STREAM)
                                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment"));
                    return resposta.body(new FileSystemResource(arquivo));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
