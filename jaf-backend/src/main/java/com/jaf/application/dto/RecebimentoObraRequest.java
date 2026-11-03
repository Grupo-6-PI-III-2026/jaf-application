package com.jaf.application.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecebimentoObraRequest(@NotNull @Digits(integer = 17, fraction = 2) @DecimalMin(value = "0.00", inclusive = false) BigDecimal valor,
                                     @NotNull @PastOrPresent LocalDate data,
                                     @Size(max = 255) String descricao) {}
