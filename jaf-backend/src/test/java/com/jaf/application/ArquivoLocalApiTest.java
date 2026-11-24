package com.jaf.application;

import com.jaf.application.armazenamento.ArquivoService;
import com.jaf.application.armazenamento.TipoArquivo;
import com.jaf.application.config.GerenciadorTokenJwt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:arquivos;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop", "app.seed.enabled=false",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "app.armazenamento.local.url-publica=", "app.armazenamento.local.segredo=segredo-de-teste"
})
@AutoConfigureMockMvc
class ArquivoLocalApiTest {
    @TempDir
    static Path raiz;

    @DynamicPropertySource
    static void diretorio(DynamicPropertyRegistry registro) {
        registro.add("app.armazenamento.local.diretorio", () -> raiz.toString());
    }

    @MockitoBean GerenciadorTokenJwt tokenJwt;
    @Autowired MockMvc mvc;
    @Autowired ArquivoService arquivos;

    private static final byte[] PDF = "%PDF-1.7 conteudo".getBytes();

    @Test
    void urlAssinadaServeComTipoCorretoSemAutenticacao() throws Exception {
        String chave = arquivos.armazenar(PDF, "notas", TipoArquivo.NOTAS_FISCAIS);

        mvc.perform(get(URI.create(arquivos.gerarUrl(chave))))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PDF));
    }

    @Test
    void semAssinaturaValidaNaoServe() throws Exception {
        String chave = arquivos.armazenar(PDF, "notas", TipoArquivo.NOTAS_FISCAIS);

        mvc.perform(get("/arquivos").param("chave", chave).param("expira", "9999999999").param("assinatura", "falsa"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pastaUploadsNaoEMaisPublica() throws Exception {
        Files.createDirectories(raiz.resolve("fotos"));
        Files.write(raiz.resolve("fotos/antiga.png"), new byte[]{1});

        mvc.perform(get("/uploads/fotos/antiga.png")).andExpect(status().isForbidden());
        mvc.perform(get("/uploads/notas/qualquer.pdf")).andExpect(status().isForbidden());
    }
}
