package com.jaf.application.dto;

import com.jaf.application.enums.StatusReembolso;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReembolsoResponse(Long gastoId, BigDecimal valorTotal, BigDecimal totalPago,
        BigDecimal saldoPendente, StatusReembolso status, List<Pagamento> pagamentos) {
    public record Pagamento(Long id, BigDecimal valor, LocalDate dataPagamento, String observacao) {}
}