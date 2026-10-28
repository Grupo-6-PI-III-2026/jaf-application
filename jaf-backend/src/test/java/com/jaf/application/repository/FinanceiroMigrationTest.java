package com.jaf.application.repository;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class FinanceiroMigrationTest {
    @Test void migrationPreservaFlagEConverteQuitadosEmUmPagamento() {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V1__init.sql"),
                new ClassPathResource("db/migration/V5__add_reembolso_concluido.sql")).execute(ds);
        var jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO obra (titulo, orcamento) VALUES ('Obra', '5000')");
        jdbc.update("INSERT INTO gasto (metodo_pagamento, valor, dt_gasto, reembolso_concluido) VALUES ('REEMBOLSO', 100, CURRENT_DATE, TRUE), ('REEMBOLSO', 200, CURRENT_DATE, FALSE), ('PIX', 300, CURRENT_DATE, NULL)");
        new ResourceDatabasePopulator(new ClassPathResource("db/migration/V13__financeiro_e_pagamentos_reembolso.sql")).execute(ds);
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pagamento_reembolso", Integer.class));
        assertEquals(new BigDecimal("100.00"), jdbc.queryForObject("SELECT SUM(valor) FROM pagamento_reembolso", BigDecimal.class));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT reembolso_concluido FROM gasto WHERE id=1", Boolean.class));
        assertEquals(new BigDecimal("0.00"), jdbc.queryForObject("SELECT total_recebido FROM obra", BigDecimal.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
                () -> jdbc.update("DELETE FROM gasto WHERE id=1"));
    }
}