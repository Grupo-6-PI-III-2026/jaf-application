package com.jaf.application.dto;

import com.jaf.application.model.Relatorio;

import java.time.LocalDate;

public record RelatorioResponse(Long id, String titulo, LocalDate dtEmissao, FuncionarioResumo funcionarioResponsavel) {
    public static RelatorioResponse de(Relatorio relatorio) {
        return new RelatorioResponse(relatorio.getId(), relatorio.getTitulo(), relatorio.getDtEmissao(),
                relatorio.getFuncionarioResponsavel() == null ? null : FuncionarioResumo.de(relatorio.getFuncionarioResponsavel()).semFoto());
    }
}
