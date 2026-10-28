package com.jaf.application.dto;

import java.math.BigDecimal;

public record ReembolsoResumo(Long funcionarioId, String funcionarioNome, BigDecimal valorTotal,
        BigDecimal totalPago, BigDecimal saldoPendente) {}