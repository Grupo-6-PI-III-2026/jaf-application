package com.jaf.application.dto;

import com.jaf.application.model.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoResponse(Long id, String descricao, String categoria, String metodoPagamento, String etapa,
                            BigDecimal valor, LocalDate dtGasto, Boolean reembolsoConcluido, boolean reembolsavel,
                            Long versao, FuncionarioResumo funcionario, ObraResumo obra) {
    public static GastoResponse de(Gasto gasto, boolean admin) {
        return new GastoResponse(gasto.getId(), gasto.getDescricao(), gasto.getCategoria(), gasto.getMetodoPagamento(),
                gasto.getEtapa(), gasto.getValor(), gasto.getDtGasto(),
                gasto.isReembolsavel() ? gasto.getReembolsoConcluido() : null, gasto.isReembolsavel(),
                gasto.getVersao(), FuncionarioResumo.de(gasto.getFuncionario()).semFoto(),
                ObraResumo.de(gasto.getObra()));
    }
}
