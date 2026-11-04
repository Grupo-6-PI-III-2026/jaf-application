package com.jaf.application.dto;

import com.jaf.application.enums.FuncaoObra;

/** Pessoa que aparece no filtro de gastos: alocada na obra ou autora de algum gasto dela. */
public record PessoaDaObraDto(Long id, String nome, FuncaoObra funcaoNaObra) {}