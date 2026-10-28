package com.jaf.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ObraFinanceiroRequest(
        @NotNull @DecimalMin("0.00") @Digits(integer = 17, fraction = 2) BigDecimal totalRecebido) {}