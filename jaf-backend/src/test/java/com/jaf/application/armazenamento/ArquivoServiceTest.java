package com.jaf.application.armazenamento;

import com.jaf.application.exceptions.BadRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ArquivoServiceTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0};

    private final ArmazenamentoArquivos armazenamento = mock(ArmazenamentoArquivos.class);
    private final ArquivoService service = new ArquivoService(armazenamento, 15);

    @Test
    void extensaoEContentTypeVemDoConteudoENaoDoNome() {
        var arquivo = new MockMultipartFile("file", "foto.html", "text/html", PNG);

        String chave = service.armazenar(arquivo, "perfil", TipoArquivo.IMAGENS, 1024);

        assertTrue(chave.matches("perfil/[0-9a-f-]{36}\\.png"), chave);
        verify(armazenamento).salvar(chave, PNG, "image/png");
    }

    @Test
    void htmlDeclaradoComoImagemERejeitado() {
        var arquivo = new MockMultipartFile("file", "foto.png", "image/png",
                "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8));

        assertThrows(BadRequest.class, () -> service.armazenar(arquivo, "perfil", TipoArquivo.IMAGENS, 1024));
        verifyNoInteractions(armazenamento);
    }

    @Test
    void pdfNaoEAceitoComoFoto() {
        var arquivo = new MockMultipartFile("file", "foto.pdf", "application/pdf", "%PDF-1.7".getBytes());

        assertThrows(BadRequest.class, () -> service.armazenar(arquivo, "perfil", TipoArquivo.IMAGENS, 1024));
        verifyNoInteractions(armazenamento);
    }

    @Test
    void respeitaTamanhoMaximoEArquivoVazio() {
        assertThrows(BadRequest.class, () -> service.armazenar(
                new MockMultipartFile("file", "a.png", "image/png", PNG), "perfil", TipoArquivo.IMAGENS, 4));
        assertThrows(BadRequest.class, () -> service.armazenar(
                new MockMultipartFile("file", "a.png", "image/png", new byte[0]), "perfil", TipoArquivo.IMAGENS, 1024));
        verifyNoInteractions(armazenamento);
    }

    @Test
    void urlNulaParaChaveAusente() {
        assertNull(service.gerarUrl(null));
        verifyNoInteractions(armazenamento);
    }

    @Test
    void remocaoEmTransacaoAconteceSomenteAposCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.removerAposCommit("notas/a.pdf");
            verify(armazenamento, never()).remover(anyString());

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(armazenamento).remover("notas/a.pdf");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void falhaAoRemoverNaoPropagaErro() {
        doThrow(new IllegalStateException("fora do ar")).when(armazenamento).remover(any());

        assertDoesNotThrow(() -> service.removerAposCommit("notas/a.pdf"));
        ArgumentCaptor<String> chave = ArgumentCaptor.forClass(String.class);
        verify(armazenamento).remover(chave.capture());
        assertEquals("notas/a.pdf", chave.getValue());
    }
}
