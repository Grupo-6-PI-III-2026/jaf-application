package com.jaf.application.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jaf.application.model.Obra;
import com.jaf.application.service.AcessoObraService;
import java.io.IOException;

/** Aplica a mesma proteção a obras diretas, paginadas e aninhadas em outras entidades. */
public class ObraSerializer extends JsonSerializer<Obra> {
    private final AcessoObraService acesso;

    public ObraSerializer(AcessoObraService acesso) { this.acesso = acesso; }

    @Override
    public void serialize(Obra obra, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("id", obra.getId());
        gen.writeStringField("titulo", obra.getTitulo());
        gen.writeStringField("status", obra.getStatus());
        gen.writeObjectField("dtInicio", obra.getDtInicio());
        gen.writeObjectField("dtTerminoPrevisto", obra.getDtTerminoPrevisto());
        if (acesso.isAdmin()) {
            gen.writeStringField("orcamento", obra.getOrcamento());
            gen.writeObjectField("totalRecebido", obra.getTotalRecebido());
        }
        gen.writeEndObject();
    }
}