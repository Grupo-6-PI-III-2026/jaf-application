package com.jaf.application.dto;

import com.jaf.application.enums.FuncaoObra;
import com.jaf.application.model.AlocacaoObra;

import java.time.LocalDate;

public record AlocacaoResponse(Long id, FuncaoObra cargo, LocalDate dtInicio, LocalDate dtFim,
                               FuncionarioResumo funcionario, ObraResponse obra) {
    public static AlocacaoResponse de(AlocacaoObra alocacao, boolean admin) {
        return new AlocacaoResponse(alocacao.getId(), alocacao.getCargo(), alocacao.getDtInicio(), alocacao.getDtFim(),
                FuncionarioResumo.de(alocacao.getFuncionario()), ObraResponse.de(alocacao.getObra(), admin));
    }
}
