package com.jaf.application.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseSeeder.class);
    private static final String SENHA_PADRAO_HASH = "$2a$10$yPsrDB3hR4NRGdCKaTxrz.CNjwgz2LjEZnUMw9ttY00GHSDfhLcdW";

    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedFuncionarios();
        seedObras();
        seedAlocacoes();
        normalizarDadosBase();
        seedGastos();
        registrarResumoSeed();
    }

    private void seedFuncionarios() {
        inserirFuncionario("Administrador", "admin@gmail.com", "ADMIN");
        inserirFuncionario("Rafael Pereira", "rafael.pereira@jaf.com", "RESPONSAVEL_ADMINISTRATIVO");
        inserirFuncionario("Gabriel Junior", "gabriel.junior@jaf.com", "ENGENHEIRO");
        inserirFuncionario("Ana Souza", "ana.souza@jaf.com", "ENGENHEIRO");
        inserirFuncionario("Isac Newton", "isac.newton@jaf.com", "ENGENHEIRO");
    }

    private void inserirFuncionario(String nome, String email, String cargo) {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, email, senha, cargo_global)
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM funcionario WHERE email = ?)
                """, nome, email, SENHA_PADRAO_HASH, cargo, email);
    }

    private void seedObras() {
        inserirObra("Obra Alphaville", "65000", "EM_ANDAMENTO", "2026-01-01", "2026-06-01");
        inserirObra("Obra Osasco", "25000", "EM_ANDAMENTO", "2026-02-15", "2026-08-15");
    }

    private void inserirObra(String titulo, String orcamento, String status, String inicio, String terminoPrevisto) {
        jdbcTemplate.update("""
                INSERT INTO obra (titulo, orcamento, status, dt_inicio, dt_termino_previsto)
                SELECT ?, ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM obra WHERE titulo = ?)
                """, titulo, orcamento, status, inicio, terminoPrevisto, titulo);
    }

    private void seedAlocacoes() {
        inserirAlocacao("rafael.pereira@jaf.com", "Obra Alphaville", "MESTRE_DE_OBRAS");
        inserirAlocacao("gabriel.junior@jaf.com", "Obra Alphaville", "ENGENHEIRO");
        inserirAlocacao("ana.souza@jaf.com", "Obra Alphaville", "ARQUITETO");
        inserirAlocacao("rafael.pereira@jaf.com", "Obra Osasco", "ENGENHEIRO");
        inserirAlocacao("isac.newton@jaf.com", "Obra Osasco", "PEDREIRO");
    }

    private void inserirAlocacao(String funcionarioEmail, String obraTitulo, String cargo) {
        jdbcTemplate.update("""
                INSERT INTO alocacao_obra (funcionario_id, obra_id, cargo)
                SELECT funcionario.id, obra.id, ?
                FROM funcionario, obra
                WHERE funcionario.email = ?
                  AND obra.titulo = ?
                  AND NOT EXISTS (
                    SELECT 1
                    FROM alocacao_obra alocacao
                    WHERE alocacao.funcionario_id = funcionario.id
                      AND alocacao.obra_id = obra.id
                  )
                """, cargo, funcionarioEmail, obraTitulo);
    }

    private void normalizarDadosBase() {
        jdbcTemplate.update("UPDATE obra SET orcamento = '65000' WHERE titulo = 'Obra Alphaville'");
        jdbcTemplate.update("UPDATE obra SET orcamento = '25000' WHERE titulo = 'Obra Osasco'");
        jdbcTemplate.update("UPDATE gasto SET etapa = 'ETAPA 1' WHERE etapa IN ('Pintura', 'Estrutura', 'Fundação')");
        jdbcTemplate.update("UPDATE gasto SET etapa = 'ETAPA 2' WHERE etapa IN ('Instalações', 'Acabamento')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Alvenaria' WHERE categoria IN ('Cimento', 'Silicone')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Equipamentos' WHERE categoria IN ('Eletrica', 'Hidraulica')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Pintura' WHERE categoria = 'Ceramica'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE cargo_global IN ('GESTOR_OBRA', 'MESTRE_DE_OBRAS', 'OPERADOR_LANCAMENTO')");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE cargo_global IN ('ARQUITETO', 'PEDREIRO')");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE email = 'rafael.pereira@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE email = 'gabriel.junior@jaf.com'");
        jdbcTemplate.update("UPDATE gasto SET reembolso_concluido = NULL WHERE metodo_pagamento <> 'REEMBOLSO' AND reembolso_concluido = false");
    }

    private void seedGastos() {
        inserirGasto("Pagamento mão de obra", "Alvenaria", "Débito", "ETAPA 1", "1000.00", "2026-01-12", "isac.newton@jaf.com", "Obra Alphaville", null);
        inserirGasto("Silicone e acabamentos", "Alvenaria", "Débito", "ETAPA 1", "450.00", "2026-01-10", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Compra de insumos", "Alvenaria", "Débito", "ETAPA 1", "2300.00", "2026-01-08", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Material elétrico", "Equipamentos", "Crédito", "ETAPA 2", "1850.00", "2026-01-20", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Piso cerâmico", "Pintura", "Débito", "ETAPA 2", "3200.00", "2026-02-05", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Fundação e estrutura", "Alvenaria", "Débito", "ETAPA 1", "5500.00", "2026-02-20", "gabriel.junior@jaf.com", "Obra Osasco", null);
        inserirGasto("Material hidráulico", "Equipamentos", "Débito", "ETAPA 2", "2100.00", "2026-03-05", "rafael.pereira@jaf.com", "Obra Osasco", null);
        inserirGasto("Tinta e pintura", "Pintura", "Crédito", "ETAPA 2", "1200.00", "2026-03-15", "isac.newton@jaf.com", "Obra Osasco", null);

        inserirGasto("Reembolso material extra", "Alvenaria", "REEMBOLSO", "ETAPA 1", "1200.00", "2026-01-15", "rafael.pereira@jaf.com", "Obra Alphaville", false);
        inserirGasto("Reembolso ferramentas", "Equipamentos", "REEMBOLSO", "ETAPA 1", "800.00", "2026-02-10", "gabriel.junior@jaf.com", "Obra Alphaville", true);
        inserirGasto("Reembolso transporte", "Mão de Obra", "REEMBOLSO", "ETAPA 2", "650.00", "2026-02-25", "ana.souza@jaf.com", "Obra Alphaville", false);
        inserirGasto("Reembolso EPI", "Mão de Obra", "REEMBOLSO", "ETAPA 2", "450.00", "2026-03-05", "gabriel.junior@jaf.com", "Obra Alphaville", true);

        inserirGasto("Reparo imprevisto estrutural", "Custos extras", "Débito", "ETAPA 1", "3200.00", "2026-01-20", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Material adicional urgente", "Custos extras", "Crédito", "ETAPA 1", "1800.00", "2026-02-12", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Ajuste de planta", "Custos extras", "Débito", "ETAPA 2", "2500.00", "2026-03-08", "ana.souza@jaf.com", "Obra Alphaville", null);
        inserirGasto("Correção elétrica imprevista", "Custos extras", "Débito", "ETAPA 2", "1100.00", "2026-04-15", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Reforço de laje", "Custos extras", "Crédito", "ETAPA 1", "4200.00", "2026-05-03", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Troca de tubulação", "Custos extras", "Débito", "ETAPA 2", "950.00", "2026-06-10", "ana.souza@jaf.com", "Obra Alphaville", null);

        inserirGasto("Demolição de paredes internas", "Demolição", "Débito", "ETAPA 1", "8500.00", "2026-01-25", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Mão de obra estrutura", "Mão de Obra", "Débito", "ETAPA 1", "7200.00", "2026-02-05", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Pintura externa completa", "Pintura", "Crédito", "ETAPA 2", "4800.00", "2026-03-20", "ana.souza@jaf.com", "Obra Alphaville", null);
        inserirGasto("Locação de equipamentos", "Equipamentos", "Débito", "ETAPA 1", "3100.00", "2026-02-28", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Reforço de fundação", "Alvenaria", "Débito", "ETAPA 1", "5500.00", "2026-01-30", "gabriel.junior@jaf.com", "Obra Alphaville", null);
    }

    private void inserirGasto(
            String descricao,
            String categoria,
            String metodoPagamento,
            String etapa,
            String valor,
            String dataGasto,
            String funcionarioEmail,
            String obraTitulo,
            Boolean reembolsoConcluido
    ) {
        jdbcTemplate.update("""
                INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id, reembolso_concluido)
                SELECT ?, ?, ?, ?, ?, ?, funcionario.id, obra.id, ?
                FROM funcionario, obra
                WHERE funcionario.email = ?
                  AND obra.titulo = ?
                  AND NOT EXISTS (
                    SELECT 1
                    FROM gasto gasto_existente
                    WHERE gasto_existente.descricao = ?
                      AND gasto_existente.obra_id = obra.id
                  )
                """, descricao, categoria, metodoPagamento, etapa, valor, dataGasto, reembolsoConcluido, funcionarioEmail, obraTitulo, descricao);
    }

    private void registrarResumoSeed() {
        Integer funcionarios = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM funcionario", Integer.class);
        Integer obras = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM obra", Integer.class);
        Integer alocacoes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM alocacao_obra", Integer.class);
        Integer gastos = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM gasto", Integer.class);

        LOGGER.info(
                "Seed de dados verificado: funcionarios={}, obras={}, alocacoes={}, gastos={}",
                funcionarios,
                obras,
                alocacoes,
                gastos
        );
    }
}
