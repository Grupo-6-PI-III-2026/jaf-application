package com.jaf.application.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

class ArmazenamentoMigrationTest {
    @Test
    void migraFotosAntigasParaChavesEDescartaUrlsExternas() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:armazenamento;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init.sql"),
                new ClassPathResource("db/migration/V4__add_foto_url.sql"),
                new ClassPathResource("db/migration/V12__create_ocr_processamento.sql")).execute(ds);
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO funcionario (nome, email, foto_url) VALUES "
                + "('A', 'a@jaf.com', '/uploads/fotos/1111-aaaa.png'), "
                + "('B', 'b@jaf.com', '/assets/Geral/avatar-gestor.svg'), "
                + "('C', 'c@jaf.com', 'https://externo.com/x.png'), "
                + "('D', 'd@jaf.com', NULL)");

        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V14__armazenamento_arquivos.sql")).execute(ds);

        assertEquals("fotos/1111-aaaa.png", jdbc.queryForObject("SELECT foto_chave FROM funcionario WHERE nome='A'", String.class));
        assertNull(jdbc.queryForObject("SELECT foto_url FROM funcionario WHERE nome='A'", String.class));
        assertEquals("/assets/Geral/avatar-gestor.svg", jdbc.queryForObject("SELECT foto_url FROM funcionario WHERE nome='B'", String.class));
        assertNull(jdbc.queryForObject("SELECT foto_url FROM funcionario WHERE nome='C'", String.class));
        assertNull(jdbc.queryForObject("SELECT foto_chave FROM funcionario WHERE nome='C'", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM obra WHERE foto_chave IS NOT NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM gasto WHERE nota_fiscal_chave IS NOT NULL", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM ocr_processamento WHERE arquivo_chave IS NOT NULL", Integer.class));
    }
}
