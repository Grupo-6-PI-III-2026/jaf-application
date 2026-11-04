package com.jaf.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Linha da lista de obras da Home: já traz responsável e (só para ADMIN) o total gasto agregado no banco. */
public record ObraResumoDto(Long id, String titulo, String status, LocalDate dtInicio, LocalDate dtTerminoPrevisto,
                            @JsonInclude(JsonInclude.Include.NON_NULL) Responsavel responsavel,
                            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal totalGasto) {
    public record Responsavel(Long id, String nome) {}
}