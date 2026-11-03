package com.jaf.application.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcluirContaDto(@NotBlank(message = "Senha atual é obrigatória.") String senhaAtual) {}
