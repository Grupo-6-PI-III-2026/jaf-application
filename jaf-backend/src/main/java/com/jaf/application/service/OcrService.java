package com.jaf.application.service;

import com.jaf.application.dto.OcrResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço responsável por enviar imagens/PDFs à API da OCR.space
 * e extrair dados estruturados para pré-preencher o formulário
 * "Lançamento de Gastos".
 *
 * === CAMPOS EXTRAÍDOS ===
 *   ✅ valor            — Valor total da nota
 *   ✅ dtGasto          — Data de emissão/compra
 *   ✅ metodoPagamento  — Forma de pagamento (Cartão, Dinheiro, Pix etc.)
 *   ✅ materialInsumo   — Nome do produto/material/serviço
 *   ✅ descricaoAdicional — Estabelecimento + informações complementares
 *   ⛔ etapa            — Não extraído (não consta em notas fiscais)
 *
 * === CONFIGURAÇÃO ===
 * Adicione ao application.properties (ou variável de ambiente):
 *   ocr.space.api-key=SUA_CHAVE_AQUI
 * Obtenha em: https://ocr.space/ocrapi (plano gratuito: 25.000 req/mês)
 */
@Service
public class OcrService {

    @Value("${ocr.space.api-key}")
    private String ocrApiKey;

    private static final String OCR_SPACE_URL = "https://api.ocr.space/parse/image";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Ponto de entrada principal.
     * Recebe o arquivo do frontend, chama a OCR.space e retorna os dados extraídos.
     */
    public OcrResponseDto processarNota(MultipartFile arquivo) {
        OcrResponseDto resposta = new OcrResponseDto();

        if (arquivo == null || arquivo.isEmpty()) {
            resposta.setSucesso(false);
            resposta.setMensagem("Nenhum arquivo foi enviado.");
            return resposta;
        }

        String textoBruto;
        try {
            textoBruto = chamarOcrSpace(arquivo);
        } catch (Exception e) {
            resposta.setSucesso(false);
            resposta.setMensagem("Erro ao processar a imagem: " + e.getMessage());
            return resposta;
        }

        if (textoBruto == null || textoBruto.isBlank()) {
            resposta.setSucesso(false);
            resposta.setMensagem("Não foi possível extrair texto da imagem. Tente com uma foto mais nítida.");
            return resposta;
        }

        resposta.setTextoBruto(textoBruto);
        extrairCampos(textoBruto, resposta);

        resposta.setSucesso(true);
        resposta.setMensagem("Nota processada. Revise os campos antes de salvar.");
        return resposta;
    }

    // =========================================================================
    // Comunicação com a API OCR.space
    // =========================================================================

    /**
     * Envia o arquivo para a OCR.space e retorna o texto extraído.
     *
     * Parâmetros usados:
     *   language=por      → Otimizado para português (notas brasileiras)
     *   OCREngine=2       → Melhor precisão para documentos complexos/inclinados
     *   isTable=true      → Preserva estrutura tabular (útil para notas com itens)
     *   scale=true        → Escala automática para melhorar qualidade
     */
    @SuppressWarnings("unchecked")
    private String chamarOcrSpace(MultipartFile arquivo) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("apikey", ocrApiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(arquivo.getBytes()) {
            @Override
            public String getFilename() {
                return arquivo.getOriginalFilename() != null
                        ? arquivo.getOriginalFilename()
                        : "nota.jpg";
            }
        });
        body.add("language", "por");
        body.add("isOverlayRequired", "false");
        body.add("OCREngine", "2");
        body.add("isTable", "true");
        body.add("scale", "true");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(OCR_SPACE_URL, request, Map.class);

        if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
            throw new RuntimeException("OCR.space retornou status inesperado: " + response.getStatusCode());
        }

        Map<String, Object> payload = response.getBody();

        Boolean isError = (Boolean) payload.get("IsErroredOnProcessing");
        if (Boolean.TRUE.equals(isError)) {
            throw new RuntimeException("OCR.space erro: " + payload.get("ErrorMessage"));
        }

        List<Map<String, Object>> parsedResults = (List<Map<String, Object>>) payload.get("ParsedResults");
        if (parsedResults == null || parsedResults.isEmpty()) {
            return "";
        }

        StringBuilder textoTotal = new StringBuilder();
        for (Map<String, Object> resultado : parsedResults) {
            Object parsedText = resultado.get("ParsedText");
            if (parsedText instanceof String texto && !texto.isBlank()) {
                textoTotal.append(texto.trim()).append("\n");
            }
        }
        return textoTotal.toString().trim();
    }

    // =========================================================================
    // Extração de campos — mapeamento para os inputs da tela
    // =========================================================================

    /**
     * Coordena a extração de todos os campos do formulário a partir do texto bruto.
     * Cada método abaixo corresponde a um campo específico da tela "Lançamento de Gastos".
     */
    private void extrairCampos(String texto, OcrResponseDto resposta) {
        // CAMPO: "Valor (R$)"
        resposta.setValor(extrairValor(texto));

        // CAMPO: "Data"
        resposta.setDtGasto(extrairData(texto));

        // CAMPO: "Tipo de Movimentação" (dropdown: Cartão, Dinheiro, Pix, Boleto, Transferência)
        resposta.setMetodoPagamento(extrairMetodoPagamento(texto));

        // CAMPO: "Material / Insumo" — produto/serviço principal da nota
        resposta.setMaterialInsumo(extrairMaterialInsumo(texto));

        // CAMPO: "Descrição Adicional" — estabelecimento e informações complementares
        resposta.setDescricaoAdicional(extrairDescricaoAdicional(texto));

        // CAMPO: "Etapa da Obra" — NÃO extraído (não consta em notas fiscais)
        // etapa permanece null — usuário seleciona manualmente no dropdown
    }

    // =========================================================================
    // Extração: Valor (R$)
    // =========================================================================

    /**
     * Extrai o valor total da nota.
     * Prioriza padrões que indicam total (mais confiáveis),
     * depois cai para qualquer valor monetário encontrado.
     *
     * Padrões reconhecidos:
     *   "TOTAL R$ 150,00"  |  "Valor Total: 1.250,50"  |  "VL TOTAL 89.90"
     */
    private BigDecimal extrairValor(String texto) {
        List<Pattern> padroes = List.of(
                Pattern.compile("(?i)(?:total\\s*a\\s*pagar|total\\s*geral)[\\s:R$]*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})"),
                Pattern.compile("(?i)(?:vl\\.?\\s*total|valor\\s*total|total)[\\s:R$]*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})"),
                Pattern.compile("R\\$\\s*([\\d]{1,3}(?:[.,]\\d{3})*[.,]\\d{2})")
        );

        for (Pattern padrao : padroes) {
            Matcher m = padrao.matcher(texto);
            if (m.find()) {
                String valorStr = m.group(1).replace(".", "").replace(",", ".");
                try {
                    return new BigDecimal(valorStr);
                } catch (NumberFormatException ignored) { }
            }
        }
        return null; // usuário preenche manualmente
    }

    // =========================================================================
    // Extração: Data
    // =========================================================================

    /**
     * Extrai a data de emissão/compra da nota.
     * Suporta os formatos mais comuns em documentos fiscais brasileiros.
     */
    private LocalDate extrairData(String texto) {
        List<String[]> tentativas = List.of(
                new String[]{"(?i)(?:data|emissao|emissão|dt\\.?\\s*emis)[\\s:./]*([\\d]{2}/[\\d]{2}/[\\d]{4})", "dd/MM/yyyy"},
                new String[]{"([\\d]{2}/[\\d]{2}/[\\d]{4})", "dd/MM/yyyy"},
                new String[]{"([\\d]{2}-[\\d]{2}-[\\d]{4})", "dd-MM-yyyy"},
                new String[]{"([\\d]{4}-[\\d]{2}-[\\d]{2})", "yyyy-MM-dd"}
        );

        for (String[] tentativa : tentativas) {
            Matcher m = Pattern.compile(tentativa[0]).matcher(texto);
            if (m.find()) {
                try {
                    return LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern(tentativa[1]));
                } catch (DateTimeParseException ignored) { }
            }
        }
        return null; // usuário preenche manualmente
    }

    // =========================================================================
    // Extração: Tipo de Movimentação (metodoPagamento)
    // =========================================================================

    /**
     * Identifica a forma de pagamento mencionada na nota.
     *
     * ATENÇÃO FRONTEND: os valores retornados ("Cartão", "Dinheiro", "Pix",
     * "Boleto", "Transferência") devem corresponder exatamente às opções
     * do dropdown "Tipo de Movimentação" na tela. Ajuste as strings aqui
     * se os valores do dropdown forem diferentes.
     *
     * Exemplos de texto que ativam cada opção:
     *   "Cartão"       → "cartao de credito", "debito", "visa", "mastercard", "elo"
     *   "Pix"          → "pix", "chave pix"
     *   "Boleto"       → "boleto", "codigo de barras"
     *   "Transferência"→ "ted", "doc", "transferencia"
     *   "Dinheiro"     → "dinheiro", "especie", "à vista em dinheiro"
     */
    private String extrairMetodoPagamento(String texto) {
        String textoLower = texto.toLowerCase()
                .replace("ã", "a").replace("é", "e").replace("ê", "e")
                .replace("ç", "c").replace("á", "a").replace("ó", "o");

        if (textoLower.contains("cartao") || textoLower.contains("credito")
                || textoLower.contains("debito") || textoLower.contains("visa")
                || textoLower.contains("mastercard") || textoLower.contains("elo")
                || textoLower.contains("hipercard")) {
            return "Cartão";
        }
        if (textoLower.contains("pix") || textoLower.contains("chave pix")) {
            return "Pix";
        }
        if (textoLower.contains("boleto") || textoLower.contains("codigo de barras")) {
            return "Boleto";
        }
        if (textoLower.contains("ted") || textoLower.contains("doc")
                || textoLower.contains("transferencia")) {
            return "Transferência";
        }
        if (textoLower.contains("dinheiro") || textoLower.contains("especie")
                || textoLower.contains("a vista em dinheiro")) {
            return "Dinheiro";
        }

        return null; // usuário seleciona manualmente no dropdown
    }

    // =========================================================================
    // Extração: Material / Insumo
    // =========================================================================

    /**
     * Extrai o nome do produto, material ou serviço principal da nota.
     * Mapeia para o campo "Material / Insumo" na tela.
     * Em notas com múltiplos itens, retorna o primeiro item identificado.
     *
     * Exemplos de saída: "Cimento CP-II 50kg", "Tinta Látex 18L", "Mão de obra"
     */
    private String extrairMaterialInsumo(String texto) {
        // Padrões comuns em cupons/notas: "ITEM: X", "PRODUTO: X", "DESCRICAO: X"
        List<Pattern> padroesLabel = List.of(
                Pattern.compile("(?i)(?:item|produto|descricao|descri..o|material|insumo)[:\\s]+(.{5,80})"),
                Pattern.compile("(?i)(?:servico|servi..o)[:\\s]+(.{5,80})")
        );

        for (Pattern p : padroesLabel) {
            Matcher m = p.matcher(texto);
            if (m.find()) {
                String candidato = m.group(1).trim().split("\\r?\\n")[0].trim();
                if (candidato.length() >= 5) {
                    return candidato.length() > 255 ? candidato.substring(0, 252) + "..." : candidato;
                }
            }
        }

        // Fallback: busca por padrão "TEXTO Quantidade Unidade" comum em cupons fiscais
        // Ex: "CIMENTO CPII 50KG   1 UN"
        Matcher mCupom = Pattern.compile("(?m)^([A-ZÀ-Ú][A-ZÀ-Ú0-9 ]{4,50})\\s+\\d+\\s+(?:UN|KG|MT|PC|CX|SC|LT|GL)").matcher(texto.toUpperCase());
        if (mCupom.find()) {
            String item = mCupom.group(1).trim();
            return item.length() > 255 ? item.substring(0, 252) + "..." : item;
        }

        return null; // usuário preenche manualmente
    }

    // =========================================================================
    // Extração: Descrição Adicional
    // =========================================================================

    /**
     * Extrai informações complementares para o campo "Descrição Adicional".
     * Prioridade: nome do estabelecimento → CNPJ → endereço.
     *
     * FRONTEND: exibir no textarea "Descrição Adicional" como contexto extra.
     * O usuário pode editar livremente antes de salvar.
     */
    private String extrairDescricaoAdicional(String texto) {
        StringBuilder info = new StringBuilder();

        // Nome do estabelecimento (geralmente nas primeiras linhas não numéricas)
        String[] linhas = texto.split("\\r?\\n");
        for (String linha : linhas) {
            String limpa = linha.trim();
            if (limpa.length() > 5
                    && !limpa.matches("[\\d\\s.,/*:R$()-]+")
                    && !limpa.toUpperCase().matches(".*(CNPJ|CPF|IE|CEP|TEL|FAX|HTTP|WWW).*")) {
                info.append("Estabelecimento: ").append(limpa).append("\n");
                break;
            }
        }

        // CNPJ — sempre útil como referência
        Matcher mCnpj = Pattern.compile("\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}").matcher(texto);
        if (mCnpj.find()) {
            info.append("CNPJ: ").append(mCnpj.group()).append("\n");
        }

        // Número da nota fiscal (NF-e, NFCe, NFe, NF)
        Matcher mNf = Pattern.compile("(?i)(?:nf-?e?|nota\\s*fiscal)[\\s:nº#]*([\\d]{1,9})").matcher(texto);
        if (mNf.find()) {
            info.append("NF: ").append(mNf.group(1)).append("\n");
        }

        String resultado = info.toString().trim();
        if (resultado.isEmpty()) return null;
        return resultado.length() > 500 ? resultado.substring(0, 497) + "..." : resultado;
    }
}
