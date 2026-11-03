package com.jaf.application.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jaf.application.model.Obra;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

/** Aplica a mesma proteção a obras diretas, paginadas e aninhadas em outras entidades. */
public class ObraSerializer extends JsonSerializer<Obra> {
    @Override
    public void serialize(Obra obra, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("id", obra.getId());
        gen.writeStringField("titulo", obra.getTitulo());
        gen.writeStringField("status", obra.getStatus());
        gen.writeObjectField("dtInicio", obra.getDtInicio());
        gen.writeObjectField("dtTerminoPrevisto", obra.getDtTerminoPrevisto());
        boolean admin = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            gen.writeStringField("orcamento", obra.getOrcamento().toPlainString());
            gen.writeObjectField("totalRecebido", obra.getTotalRecebido());
        }
        gen.writeEndObject();
    }
}