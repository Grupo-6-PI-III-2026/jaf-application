package com.jaf.application.service;

import com.jaf.application.armazenamento.ArquivoService;
import com.jaf.application.armazenamento.TipoArquivo;
import com.jaf.application.config.RabbitMQConfig;
import com.jaf.application.dto.OcrResponseDto;
import com.jaf.application.dto.OcrResultadoMensagem;
import com.jaf.application.dto.OcrSolicitacaoMensagem;
import com.jaf.application.enums.StatusOcr;
import com.jaf.application.exceptions.BadRequest;
import com.jaf.application.exceptions.NotFoundException;
import com.jaf.application.model.OcrProcessamento;
import com.jaf.application.repository.OcrProcessamentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrServiceTest {

    private static final String SOLICITANTE = "admin@gmail.com";

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OcrProcessamentoRepository processamentoRepository;

    @Mock
    private ArquivoService arquivos;

    private OcrService ocrService;

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2};

    private final MockMultipartFile notaValida =
            new MockMultipartFile("arquivo", "nota.png", "image/png", PNG);

    @BeforeEach
    void setUp() {
        ocrService = new OcrService(rabbitTemplate, processamentoRepository, arquivos, "http://ocr.invalid/api/ocr");
    }

    @Test
    void process_arquivoVazio_lancaErroSemChamarServicoExterno() {
        MockMultipartFile vazio = new MockMultipartFile("file", "vazio.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> ocrService.process(vazio));
        assertThrows(IllegalArgumentException.class, () -> ocrService.process(null));
        verifyNoInteractions(rabbitTemplate, processamentoRepository);
    }

    @Test
    void processarNota_publicaSolicitacaoERetornaProcessando() {
        when(arquivos.armazenar(PNG, "notas", TipoArquivo.NOTAS_FISCAIS)).thenReturn("notas/nf.png");
        when(processamentoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));

        OcrResponseDto resposta = ocrService.processarNota(notaValida, SOLICITANTE);

        ArgumentCaptor<OcrSolicitacaoMensagem> mensagem = ArgumentCaptor.forClass(OcrSolicitacaoMensagem.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq(RabbitMQConfig.RK_SOLICITAR), mensagem.capture());
        ArgumentCaptor<OcrProcessamento> salvo = ArgumentCaptor.forClass(OcrProcessamento.class);
        verify(processamentoRepository).save(salvo.capture());

        assertEquals(StatusOcr.PROCESSANDO, resposta.getStatus());
        assertTrue(resposta.isSucesso());
        assertNotNull(resposta.getJobId());
        assertEquals("notas/nf.png", salvo.getValue().getArquivoChave());
        assertEquals(resposta.getJobId(), mensagem.getValue().jobId());
        assertEquals("nota.png", mensagem.getValue().filename());
        assertArrayEquals(PNG, Base64.getDecoder().decode(mensagem.getValue().imagemBase64()));
    }

    @Test
    void processarNota_conteudoDisfarcado_naoPublicaNemPersiste() {
        MockMultipartFile html = new MockMultipartFile("arquivo", "nota.png", "image/png", "<script>".getBytes());
        when(arquivos.armazenar(any(byte[].class), eq("notas"), any()))
                .thenThrow(new BadRequest("Formato não suportado."));

        OcrResponseDto resposta = ocrService.processarNota(html, SOLICITANTE);

        assertEquals(StatusOcr.ERRO, resposta.getStatus());
        assertEquals("Formato não suportado.", resposta.getMensagem());
        verifyNoInteractions(rabbitTemplate, processamentoRepository);
    }

    @Test
    void processarNota_armazenamentoIndisponivel_naoPublica() {
        when(arquivos.armazenar(any(byte[].class), eq("notas"), any()))
                .thenThrow(new IllegalStateException("S3 fora do ar"));

        OcrResponseDto resposta = ocrService.processarNota(notaValida, SOLICITANTE);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusOcr.ERRO, resposta.getStatus());
        verifyNoInteractions(rabbitTemplate, processamentoRepository);
    }

    @Test
    void processarNota_formatoInvalido_naoPublicaNemPersiste() {
        MockMultipartFile texto = new MockMultipartFile("arquivo", "nota.txt", "text/plain", new byte[]{1});

        OcrResponseDto resposta = ocrService.processarNota(texto, SOLICITANTE);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusOcr.ERRO, resposta.getStatus());
        verifyNoInteractions(rabbitTemplate, processamentoRepository);
    }

    @Test
    void processarNota_brokerIndisponivel_marcaErro() {
        when(arquivos.armazenar(PNG, "notas", TipoArquivo.NOTAS_FISCAIS)).thenReturn("notas/nf.png");
        when(processamentoRepository.save(any())).thenAnswer(invocacao -> invocacao.getArgument(0));
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        OcrResponseDto resposta = ocrService.processarNota(notaValida, SOLICITANTE);

        assertFalse(resposta.isSucesso());
        assertEquals(StatusOcr.ERRO, resposta.getStatus());
        verify(processamentoRepository, times(2)).save(any());
    }

    @Test
    void receberResultado_concluido_extraiCamposNaConsulta() {
        OcrProcessamento processamento = new OcrProcessamento("job-1", SOLICITANTE);
        when(processamentoRepository.findById("job-1")).thenReturn(Optional.of(processamento));

        ocrService.receberResultado(new OcrResultadoMensagem(
                "job-1", StatusOcr.CONCLUIDO,
                "LOJA DE MATERIAIS LTDA\nData: 10/10/2026\nTOTAL A PAGAR R$ 1.250,90\nPagamento: PIX",
                92.5, null));
        OcrResponseDto resposta = ocrService.consultar("job-1", SOLICITANTE);

        assertEquals(StatusOcr.CONCLUIDO, resposta.getStatus());
        assertTrue(resposta.isSucesso());
        assertEquals(new BigDecimal("1250.90"), resposta.getValor());
        assertEquals(LocalDate.of(2026, 10, 10), resposta.getDtGasto());
        assertEquals("PIX", resposta.getMetodoPagamento());
        verify(processamentoRepository).save(processamento);
    }

    @Test
    void receberResultado_erroDoWorker_marcaErroComMensagem() {
        OcrProcessamento processamento = new OcrProcessamento("job-2", SOLICITANTE);
        when(processamentoRepository.findById("job-2")).thenReturn(Optional.of(processamento));

        ocrService.receberResultado(new OcrResultadoMensagem("job-2", StatusOcr.ERRO, null, null, "timeout"));

        assertEquals(StatusOcr.ERRO, processamento.getStatus());
        assertTrue(processamento.getMensagemErro().contains("timeout"));
    }

    @Test
    void receberResultado_textoVazio_marcaErro() {
        OcrProcessamento processamento = new OcrProcessamento("job-3", SOLICITANTE);
        when(processamentoRepository.findById("job-3")).thenReturn(Optional.of(processamento));

        ocrService.receberResultado(new OcrResultadoMensagem("job-3", StatusOcr.CONCLUIDO, "  ", null, null));

        assertEquals(StatusOcr.ERRO, processamento.getStatus());
    }

    @Test
    void receberResultado_jobDesconhecido_descartaSemFalhar() {
        when(processamentoRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> ocrService.receberResultado(
                new OcrResultadoMensagem("inexistente", StatusOcr.CONCLUIDO, "texto", null, null)));
        verify(processamentoRepository, never()).save(any());
    }

    @Test
    void consultar_jobDeOutroUsuario_retornaNaoEncontrado() {
        when(processamentoRepository.findById("job-4"))
                .thenReturn(Optional.of(new OcrProcessamento("job-4", "outro@jaf.com")));

        assertThrows(NotFoundException.class, () -> ocrService.consultar("job-4", SOLICITANTE));
    }
}
