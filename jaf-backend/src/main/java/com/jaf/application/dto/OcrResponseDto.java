package com.jaf.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de resposta do endpoint POST /ocr/nota-fiscal.
 *
 * Retornado ao frontend após o processamento da imagem pela OCR.space.
 * Os campos extraídos devem ser usados para pré-preencher o formulário
 * de criação de gasto (GastoDto) na tela de "Lançamento de Gastos".
 *
 * === CAMPOS DA TELA vs CAMPOS EXTRAÍDOS ===
 *
 *  Tela (frontend)          → Campo neste DTO          → Campo em GastoDto
 *  ─────────────────────────────────────────────────────────────────────────
 *  Valor (R$)               → valor            ✅       → valor
 *  Data                     → dtGasto          ✅       → dtGasto
 *  Tipo de Movimentação     → metodoPagamento  ✅       → metodoPagamento
 *  Etapa da Obra            → etapa            ⚠️ *    → etapa
 *  Material / Insumo        → materialInsumo   ✅       → descricao
 *  Descrição Adicional      → descricaoAdicional ✅     → (campo livre, frontend concatena se quiser)
 *
 *  * Etapa não é extraída do OCR pois não aparece em notas fiscais — usuário preenche manualmente.
 *
 * === INTEGRAÇÃO FRONTEND ===
 * Endpoint  : POST /ocr/nota-fiscal
 * Método    : multipart/form-data
 * Parâmetro : "arquivo" (MultipartFile — JPG, PNG ou PDF, máx 5MB)
 *
 * Exemplo TypeScript:
 *
 *   const formData = new FormData();
 *   formData.append("arquivo", file);
 *
 *   const { data } = await api.post<OcrResponseDto>("/ocr/nota-fiscal", formData, {
 *     headers: { "Content-Type": "multipart/form-data" }
 *   });
 *
 *   if (data.sucesso) {
 *     setValor(data.valor ?? "");
 *     setDtGasto(data.dtGasto ?? "");
 *     setMetodoPagamento(data.metodoPagamento ?? "");  // ex: "Cartão", "Dinheiro", "Pix"
 *     setMaterialInsumo(data.materialInsumo ?? "");    // → campo "Material / Insumo" na tela
 *     setDescricaoAdicional(data.descricaoAdicional ?? "");
 *     // etapa NÃO é preenchida automaticamente — usuário deve selecionar manualmente
 *   }
 */
public class OcrResponseDto {

    /**
     * Indica se o OCR conseguiu extrair ao menos um campo com sucesso.
     * Use para decidir se exibe os dados pré-preenchidos ou uma mensagem de aviso.
     */
    private boolean sucesso;

    /**
     * Mensagem de feedback para exibir ao usuário (sucesso ou motivo do erro).
     * Ex: "Nota processada com sucesso. Revise os dados antes de salvar."
     */
    private String mensagem;

    /**
     * Texto bruto extraído da imagem pela OCR.space.
     * Útil para debug. Não precisa ser exibido na UI de produção.
     */
    private String textoBruto;

    // -------------------------------------------------------------------------
    // Campos extraídos — mapeamento direto para o formulário da tela
    // -------------------------------------------------------------------------

    /**
     * CAMPO: "Valor (R$)"
     * Valor total identificado na nota fiscal.
     * Mapeia para: GastoDto.valor
     * Null se não encontrado — deixar campo em branco no frontend.
     */
    private BigDecimal valor;

    /**
     * CAMPO: "Data"
     * Data de emissão/compra extraída da nota (formato YYYY-MM-DD).
     * Mapeia para: GastoDto.dtGasto
     * Null se não encontrada — deixar campo em branco no frontend.
     */
    private LocalDate dtGasto;

    /**
     * CAMPO: "Tipo de Movimentação" (dropdown na tela)
     * Forma de pagamento identificada na nota.
     * Valores possíveis retornados: "Cartão", "Dinheiro", "Pix", "Boleto", "Transferência"
     * Mapeia para: GastoDto.metodoPagamento
     *
     * ATENÇÃO FRONTEND: use este valor para tentar selecionar a opção correspondente
     * no dropdown. Se o valor retornado não corresponder a nenhuma opção, deixe em branco.
     * Null se não identificado — usuário seleciona manualmente.
     */
    private String metodoPagamento;

    /**
     * CAMPO: "Material / Insumo"
     * Nome do produto, material ou serviço identificado na nota.
     * Exemplos: "Cimento CP-II 50kg", "Tinta Látex 18L", "Mão de obra elétrica"
     * Mapeia para: GastoDto.descricao
     * Null se não identificado.
     */
    private String materialInsumo;

    /**
     * CAMPO: "Descrição Adicional"
     * Informações complementares extraídas da nota:
     * nome do estabelecimento, CNPJ, endereço, observações.
     * Este campo NÃO existe em GastoDto — é para uso livre no frontend.
     * O frontend pode exibir como leitura ou concatenar ao salvar o gasto se necessário.
     * Null se não houver informação adicional relevante.
     */
    private String descricaoAdicional;

    /**
     * CAMPO: "Etapa da Obra" (dropdown na tela)
     * NÃO é extraído do OCR — notas fiscais não contêm essa informação.
     * Este campo sempre será null. Usuário deve selecionar manualmente.
     * Mantido aqui apenas para documentar que a decisão foi intencional.
     */
    private String etapa = null;

    // -------------------------------------------------------------------------
    // Getters e Setters
    // -------------------------------------------------------------------------

    public boolean isSucesso() { return sucesso; }
    public void setSucesso(boolean sucesso) { this.sucesso = sucesso; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public String getTextoBruto() { return textoBruto; }
    public void setTextoBruto(String textoBruto) { this.textoBruto = textoBruto; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }

    public LocalDate getDtGasto() { return dtGasto; }
    public void setDtGasto(LocalDate dtGasto) { this.dtGasto = dtGasto; }

    public String getMetodoPagamento() { return metodoPagamento; }
    public void setMetodoPagamento(String metodoPagamento) { this.metodoPagamento = metodoPagamento; }

    public String getMaterialInsumo() { return materialInsumo; }
    public void setMaterialInsumo(String materialInsumo) { this.materialInsumo = materialInsumo; }

    public String getDescricaoAdicional() { return descricaoAdicional; }
    public void setDescricaoAdicional(String descricaoAdicional) { this.descricaoAdicional = descricaoAdicional; }

    public String getEtapa() { return etapa; }
    public void setEtapa(String etapa) { this.etapa = etapa; }
}
