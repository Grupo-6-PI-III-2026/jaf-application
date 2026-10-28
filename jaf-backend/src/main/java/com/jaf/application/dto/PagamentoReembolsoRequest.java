package com.jaf.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoReembolsoRequest(
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 17, fraction = 2) BigDecimal valor,
        @NotNull @PastOrPresent LocalDate dataPagamento,
        @Size(max = 1000) String observacao) {}