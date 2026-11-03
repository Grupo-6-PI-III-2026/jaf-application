package com.jaf.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EstornoPagamentoRequest(@NotBlank @Size(max = 500) String motivo) {}
