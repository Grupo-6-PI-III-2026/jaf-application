package com.jaf.application.dto;

import com.jaf.application.model.NotaFiscal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record NotaFiscalResponse(Long id, String numero, String cnpjEmitente,
                                 BigDecimal valor, LocalDate dataEmissao,
                                 String nomeArquivo) {
    public static NotaFiscalResponse from(NotaFiscal nota) {
        return new NotaFiscalResponse(nota.getId(), nota.getNumero(), nota.getCnpjEmitente(),
                nota.getValor(), nota.getDataEmissao(), nota.getArquivoNome());
    }
}
