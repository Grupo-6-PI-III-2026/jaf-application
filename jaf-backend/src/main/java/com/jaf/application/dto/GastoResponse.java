package com.jaf.application.dto;

import com.jaf.application.enums.StatusReembolso;
import com.jaf.application.model.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record GastoResponse(Long id, String descricao, String categoria, String metodoPagamento, String etapa,
                            BigDecimal valor, LocalDate dtGasto, Boolean reembolsoConcluido, boolean reembolsavel,
                            StatusReembolso statusReembolso, BigDecimal totalPago, BigDecimal saldoPendente,
                            Long versao, FuncionarioResumo funcionario, ObraResumo obra) {
    public static GastoResponse de(Gasto gasto, boolean admin) {
        return de(gasto, admin, Map.of());
    }

    public static GastoResponse de(Gasto gasto, boolean admin, Map<Long, BigDecimal> totaisPagos) {
        BigDecimal pago = null;
        BigDecimal saldo = null;
        StatusReembolso status = null;
        if (gasto.isReembolsavel()) {
            pago = totaisPagos.getOrDefault(gasto.getId(), BigDecimal.ZERO);
            saldo = gasto.getValor().subtract(pago).max(BigDecimal.ZERO);
            status = pago.signum() == 0 ? StatusReembolso.PENDENTE
                    : pago.compareTo(gasto.getValor()) >= 0 ? StatusReembolso.PAGO : StatusReembolso.PARCIAL;
        }
        return new GastoResponse(gasto.getId(), gasto.getDescricao(), gasto.getCategoria(), gasto.getMetodoPagamento(),
                gasto.getEtapa(), gasto.getValor(), gasto.getDtGasto(),
                gasto.isReembolsavel() ? gasto.getReembolsoConcluido() : null, gasto.isReembolsavel(),
                status, pago, saldo,
                gasto.getVersao(), FuncionarioResumo.de(gasto.getFuncionario()).semFoto(),
                ObraResumo.de(gasto.getObra()));
    }
}
