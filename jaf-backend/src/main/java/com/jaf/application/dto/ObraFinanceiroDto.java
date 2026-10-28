package com.jaf.application.dto;

import java.math.BigDecimal;

public record ObraFinanceiroDto(BigDecimal totalRecebido, BigDecimal totalGasto, BigDecimal saldo) {}