package com.jaf.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jaf.application.model.Obra;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ObraResponse(Long id, String titulo, String status, LocalDate dtInicio,
                           LocalDate dtTerminoPrevisto, @JsonInclude(JsonInclude.Include.NON_NULL) String orcamento,
                           @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal totalRecebido, Long versao) {
    public static ObraResponse de(Obra obra, boolean admin) {
        return new ObraResponse(obra.getId(), obra.getTitulo(), obra.getStatus(), obra.getDtInicio(),
                obra.getDtTerminoPrevisto(), admin && obra.getOrcamento() != null ? obra.getOrcamento().toPlainString() : null,
                admin ? obra.getTotalRecebido() : null, obra.getVersao());
    }
}
