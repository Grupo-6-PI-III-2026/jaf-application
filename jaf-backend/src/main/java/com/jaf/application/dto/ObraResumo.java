package com.jaf.application.dto;

import com.jaf.application.model.Obra;

public record ObraResumo(Long id, String titulo) {
    public static ObraResumo de(Obra obra) {
        return new ObraResumo(obra.getId(), obra.getTitulo());
    }
}
