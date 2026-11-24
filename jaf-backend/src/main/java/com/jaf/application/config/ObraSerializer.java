package com.jaf.application.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.jaf.application.armazenamento.ArquivoService;
import com.jaf.application.model.Obra;
import com.jaf.application.service.AcessoObraService;
import org.springframework.beans.factory.ObjectProvider;
import java.io.IOException;

/** Aplica a mesma proteção a obras diretas, paginadas e aninhadas em outras entidades. */
public class ObraSerializer extends JsonSerializer<Obra> {
    private final AcessoObraService acesso;
    private final ObjectProvider<ArquivoService> arquivos;

    public ObraSerializer(AcessoObraService acesso, ObjectProvider<ArquivoService> arquivos) {
        this.acesso = acesso;
        this.arquivos = arquivos;
    }

    @Override
    public void serialize(Obra obra, JsonGenerator gen, SerializerProvider provider) throws IOException {
        ArquivoService arquivoService = arquivos.getIfAvailable();
        gen.writeStartObject();
        gen.writeObjectField("id", obra.getId());
        gen.writeStringField("titulo", obra.getTitulo());
        gen.writeStringField("status", obra.getStatus());
        gen.writeObjectField("dtInicio", obra.getDtInicio());
        gen.writeObjectField("dtTerminoPrevisto", obra.getDtTerminoPrevisto());
        gen.writeStringField("fotoUrl", arquivoService == null ? null : arquivoService.gerarUrl(obra.getFotoChave()));
        if (acesso.isAdmin()) {
            gen.writeStringField("orcamento", obra.getOrcamento());
            gen.writeObjectField("totalRecebido", obra.getTotalRecebido());
        }
        gen.writeEndObject();
    }
}