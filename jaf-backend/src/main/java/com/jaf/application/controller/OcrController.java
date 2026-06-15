package com.jaf.application.controller;

import com.jaf.application.dto.OcrResponseDto;
import com.jaf.application.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller responsável pelos endpoints de OCR (leitura óptica de notas fiscais).
 *
 * === INTEGRAÇÃO FRONTEND ===
 *
 * Base URL : /ocr
 * Auth     : Bearer JWT (mesmo token usado nos demais endpoints)
 *
 * Fluxo esperado na tela de inserção de gastos:
 *
 *   1. Usuário clica em "Anexar Nota Fiscal" e seleciona uma imagem/PDF
 *   2. Frontend envia o arquivo via POST /ocr/nota-fiscal (multipart)
 *   3. Backend retorna OcrResponseDto com campos pré-preenchidos
 *   4. Frontend preenche o formulário de gasto com os dados extraídos
 *   5. Usuário revisa/corrige os dados e confirma via POST /gastos
 *
 * Tipos de arquivo aceitos: JPEG, PNG, GIF, BMP, TIFF, PDF (máx. 10 MB)
 * Limite configurado em: spring.servlet.multipart.max-file-size=10MB
 */
@RestController
@RequestMapping("/ocr")
@SecurityRequirement(name = "Bearer")
public class OcrController {

    private final OcrService ocrService;

    public OcrController(OcrService ocrService) {
        this.ocrService = ocrService;
    }

    /**
     * Processa uma imagem ou PDF de nota fiscal via OCR.space
     * e retorna os dados extraídos para pré-preencher o formulário de gasto.
     *
     * === CHAMADA DO FRONTEND ===
     *
     *   POST /ocr/nota-fiscal
     *   Content-Type: multipart/form-data
     *   Authorization: Bearer {token}
     *
     *   FormData:
     *     arquivo: File  ← imagem ou PDF da nota fiscal
     *
     * === RESPOSTA (OcrResponseDto) ===
     *
     *   {
     *     "sucesso": true,
     *     "mensagem": "Nota processada com sucesso. Revise os dados antes de salvar.",
     *     "textoBruto": "SUPERMERCADO XYZ\nData: 12/06/2025\nTOTAL R$ 89,90\n...",
     *     "descricao": "SUPERMERCADO XYZ",          → pré-preenche campo "descricao"
     *     "valor": 89.90,                           → pré-preenche campo "valor"
     *     "dtGasto": "2025-06-12",                  → pré-preenche campo "dtGasto"
     *     "categoriaSugerida": "Alimentação"        → pré-preenche campo "categoria"
     *   }
     *
     * Campos que NÃO vêm do OCR (usuário preenche manualmente):
     *   - funcionarioId, obraId, metodoPagamento, etapa
     *
     * Em caso de erro ou campo não reconhecido, o valor retornado é null.
     * O frontend deve tratar campos null deixando-os em branco no formulário.
     *
     * @param arquivo Imagem ou PDF da nota fiscal enviado pelo frontend
     * @return OcrResponseDto com os dados extraídos
     */
    @PostMapping(value = "/nota-fiscal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('CRIAR_GASTO')")
    @Operation(
            summary = "Processa nota fiscal via OCR",
            description = "Envia uma imagem/PDF para a OCR.space e retorna os campos extraídos " +
                          "para pré-preencher o formulário de inserção de gasto.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "OCR processado com sucesso",
                            content = @Content(schema = @Schema(implementation = OcrResponseDto.class))),
                    @ApiResponse(responseCode = "400", description = "Arquivo inválido ou não enviado"),
                    @ApiResponse(responseCode = "401", description = "Token JWT ausente ou inválido"),
                    @ApiResponse(responseCode = "403", description = "Sem permissão CRIAR_GASTO")
            }
    )
    public ResponseEntity<OcrResponseDto> processarNota(
            @RequestParam("arquivo") MultipartFile arquivo) {

        OcrResponseDto resposta = ocrService.processarNota(arquivo);
        return ResponseEntity.ok(resposta);
    }
}
