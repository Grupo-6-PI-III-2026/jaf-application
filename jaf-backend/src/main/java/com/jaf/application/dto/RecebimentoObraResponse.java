package com.jaf.application.dto;

import com.jaf.application.model.RecebimentoObra;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecebimentoObraResponse(
        Long id,
        Long obraId,
        BigDecimal valor,
        LocalDate dataRecebimento,
        String descricao,
        boolean estornado) {

    public static RecebimentoObraResponse de(RecebimentoObra recebimento) {
        return new RecebimentoObraResponse(
                recebimento.getId(),
                recebimento.getObra().getId(),
                recebimento.getValor(),
                recebimento.getData(),
                recebimento.getDescricao(),
                false);
    }
}
