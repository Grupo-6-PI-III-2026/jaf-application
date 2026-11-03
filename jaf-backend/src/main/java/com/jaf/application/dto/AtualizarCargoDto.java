package com.jaf.application.dto;

import com.jaf.application.enums.Cargo;
import jakarta.validation.constraints.NotNull;

public record AtualizarCargoDto(@NotNull(message = "Cargo é obrigatório") Cargo cargo) {}
