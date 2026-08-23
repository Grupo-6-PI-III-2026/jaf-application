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
    
    // CORREÇÃO DE SEGURANÇA A02: Senha padrão já está em hash BCrypt
    // Hash BCrypt da senha "senha123" (todos os usuários criados no seed usam esta senha)
    // Recomendação: Em produção, cada usuário deve ter senha única ou ser forçado a trocar no primeiro login
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
        inserirFuncionario("Carlos Silva", "carlos.silva@jaf.com", "ENGENHEIRO");
        inserirFuncionario("Marina Costa", "marina.costa@jaf.com", "RESPONSAVEL_ADMINISTRATIVO");
        inserirFuncionario("Pedro Santos", "pedro.santos@jaf.com", "ENGENHEIRO");
        inserirFuncionario("Lucia Ferreira", "lucia.ferreira@jaf.com", "ENGENHEIRO");
        inserirFuncionario("Roberto Almeida", "roberto.almeida@jaf.com", "RESPONSAVEL_ADMINISTRATIVO");
    }

    private void inserirFuncionario(String nome, String email, String cargo) {
        jdbcTemplate.update("""
                INSERT INTO funcionario (nome, email, senha, cargo_global)
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (SELECT 1 FROM funcionario WHERE email = ?)
                """, nome, email, SENHA_PADRAO_HASH, cargo, email);
    }

    private void seedObras() {
        inserirObra("Obra Alphaville", "65000", "EM_ANDAMENTO", "2026-03-01", "2026-07-01");
        inserirObra("Obra Osasco", "25000", "EM_ANDAMENTO", "2026-04-15", "2026-09-15");
        inserirObra("Residencial Morumbi", "120000", "EM_ANDAMENTO", "2026-02-01", "2026-08-30");
        inserirObra("Comercial Pinheiros", "85000", "EM_ANDAMENTO", "2026-01-15", "2026-10-15");
        inserirObra("Galeria Shopping", "200000", "EM_ANDAMENTO", "2026-05-01", "2026-12-01");
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
        inserirAlocacao("carlos.silva@jaf.com", "Residencial Morumbi", "ENGENHEIRO");
        inserirAlocacao("marina.costa@jaf.com", "Residencial Morumbi", "RESPONSAVEL_ADMINISTRATIVO");
        inserirAlocacao("pedro.santos@jaf.com", "Comercial Pinheiros", "ENGENHEIRO");
        inserirAlocacao("lucia.ferreira@jaf.com", "Comercial Pinheiros", "ARQUITETO");
        inserirAlocacao("roberto.almeida@jaf.com", "Galeria Shopping", "RESPONSAVEL_ADMINISTRATIVO");
        inserirAlocacao("gabriel.junior@jaf.com", "Galeria Shopping", "ENGENHEIRO");
        inserirAlocacao("ana.souza@jaf.com", "Residencial Morumbi", "ENGENHEIRO");
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
        jdbcTemplate.update("UPDATE obra SET orcamento = '120000' WHERE titulo = 'Residencial Morumbi'");
        jdbcTemplate.update("UPDATE obra SET orcamento = '85000' WHERE titulo = 'Comercial Pinheiros'");
        jdbcTemplate.update("UPDATE obra SET orcamento = '200000' WHERE titulo = 'Galeria Shopping'");
        jdbcTemplate.update("UPDATE gasto SET etapa = 'ETAPA 1' WHERE etapa IN ('Pintura', 'Estrutura', 'Fundação')");
        jdbcTemplate.update("UPDATE gasto SET etapa = 'ETAPA 2' WHERE etapa IN ('Instalações', 'Acabamento')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Alvenaria' WHERE categoria IN ('Cimento', 'Silicone')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Equipamentos' WHERE categoria IN ('Eletrica', 'Hidraulica')");
        jdbcTemplate.update("UPDATE gasto SET categoria = 'Pintura' WHERE categoria = 'Ceramica'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE cargo_global IN ('GESTOR_OBRA', 'MESTRE_DE_OBRAS', 'OPERADOR_LANCAMENTO')");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE cargo_global IN ('ARQUITETO', 'PEDREIRO')");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE email = 'rafael.pereira@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE email = 'marina.costa@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO' WHERE email = 'roberto.almeida@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE email = 'gabriel.junior@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE email = 'carlos.silva@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE email = 'pedro.santos@jaf.com'");
        jdbcTemplate.update("UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE email = 'lucia.ferreira@jaf.com'");
        jdbcTemplate.update("UPDATE gasto SET reembolso_concluido = NULL WHERE metodo_pagamento <> 'REEMBOLSO' AND reembolso_concluido = false");
    }

    private void seedGastos() {
        // Gastos Obra Alphaville
        inserirGasto("Pagamento mão de obra", "Alvenaria", "Débito", "ETAPA 1", "1000.00", "2026-03-12", "isac.newton@jaf.com", "Obra Alphaville", null);
        inserirGasto("Silicone e acabamentos", "Alvenaria", "Débito", "ETAPA 1", "450.00", "2026-03-10", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Compra de insumos", "Alvenaria", "Débito", "ETAPA 1", "2300.00", "2026-03-08", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Material elétrico", "Equipamentos", "Crédito", "ETAPA 2", "1850.00", "2026-04-20", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Piso cerâmico", "Pintura", "Débito", "ETAPA 2", "3200.00", "2026-05-05", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Reembolso material extra", "Alvenaria", "REEMBOLSO", "ETAPA 1", "1200.00", "2026-04-15", "rafael.pereira@jaf.com", "Obra Alphaville", false);
        inserirGasto("Reembolso ferramentas", "Equipamentos", "REEMBOLSO", "ETAPA 1", "800.00", "2026-05-10", "gabriel.junior@jaf.com", "Obra Alphaville", true);
        inserirGasto("Reembolso transporte", "Mão de Obra", "REEMBOLSO", "ETAPA 2", "650.00", "2026-05-25", "ana.souza@jaf.com", "Obra Alphaville", false);
        inserirGasto("Reembolso EPI", "Mão de Obra", "REEMBOLSO", "ETAPA 2", "450.00", "2026-06-05", "gabriel.junior@jaf.com", "Obra Alphaville", true);
        inserirGasto("Reparo imprevisto estrutural", "Custos extras", "Débito", "ETAPA 1", "3200.00", "2026-03-20", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Material adicional urgente", "Custos extras", "Crédito", "ETAPA 1", "1800.00", "2026-04-12", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Ajuste de planta", "Custos extras", "Débito", "ETAPA 2", "2500.00", "2026-05-08", "ana.souza@jaf.com", "Obra Alphaville", null);
        inserirGasto("Correção elétrica imprevista", "Custos extras", "Débito", "ETAPA 2", "1100.00", "2026-06-10", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Reforço de laje", "Custos extras", "Crédito", "ETAPA 1", "4200.00", "2026-06-12", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Troca de tubulação", "Custos extras", "Débito", "ETAPA 2", "950.00", "2026-06-14", "ana.souza@jaf.com", "Obra Alphaville", null);
        inserirGasto("Demolição de paredes internas", "Demolição", "Débito", "ETAPA 1", "8500.00", "2026-03-25", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Mão de obra estrutura", "Mão de Obra", "Débito", "ETAPA 1", "7200.00", "2026-04-05", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Pintura externa completa", "Pintura", "Crédito", "ETAPA 2", "4800.00", "2026-05-20", "ana.souza@jaf.com", "Obra Alphaville", null);
        inserirGasto("Locação de equipamentos", "Equipamentos", "Débito", "ETAPA 1", "3100.00", "2026-04-28", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Reforço de fundação", "Alvenaria", "Débito", "ETAPA 1", "5500.00", "2026-05-30", "gabriel.junior@jaf.com", "Obra Alphaville", null);
        inserirGasto("Instalação ar condicionado", "Equipamentos", "Crédito", "ETAPA 2", "2800.00", "2026-06-08", "rafael.pereira@jaf.com", "Obra Alphaville", null);
        inserirGasto("Acabamento gourmet", "Alvenaria", "Débito", "ETAPA 2", "1900.00", "2026-06-12", "gabriel.junior@jaf.com", "Obra Alphaville", null);

        // Gastos Obra Osasco
        inserirGasto("Fundação e estrutura", "Alvenaria", "Débito", "ETAPA 1", "5500.00", "2026-05-20", "gabriel.junior@jaf.com", "Obra Osasco", null);
        inserirGasto("Material hidráulico", "Equipamentos", "Débito", "ETAPA 2", "2100.00", "2026-06-05", "rafael.pereira@jaf.com", "Obra Osasco", null);
        inserirGasto("Tinta e pintura", "Pintura", "Crédito", "ETAPA 2", "1200.00", "2026-06-13", "isac.newton@jaf.com", "Obra Osasco", null);
        inserirGasto("Escavação inicial", "Demolição", "Débito", "ETAPA 1", "4200.00", "2026-05-01", "rafael.pereira@jaf.com", "Obra Osasco", null);
        inserirGasto("Concretagem", "Alvenaria", "Débito", "ETAPA 1", "3800.00", "2026-05-15", "gabriel.junior@jaf.com", "Obra Osasco", null);
        inserirGasto("Reembolso aluguel equipamento", "Equipamentos", "REEMBOLSO", "ETAPA 1", "950.00", "2026-05-28", "isac.newton@jaf.com", "Obra Osasco", false);
        inserirGasto("Reembolso material construção", "Alvenaria", "REEMBOLSO", "ETAPA 1", "1800.00", "2026-06-02", "rafael.pereira@jaf.com", "Obra Osasco", true);
        inserirGasto("Vergalhão e aço", "Alvenaria", "Débito", "ETAPA 1", "2900.00", "2026-05-25", "gabriel.junior@jaf.com", "Obra Osasco", null);
        inserirGasto("Imprevisto fundação", "Custos extras", "Débito", "ETAPA 1", "1500.00", "2026-05-30", "rafael.pereira@jaf.com", "Obra Osasco", null);
        inserirGasto("Telhado e cobertura", "Alvenaria", "Crédito", "ETAPA 2", "3400.00", "2026-06-08", "isac.newton@jaf.com", "Obra Osasco", null);
        inserirGasto("Vidros e esquadrias", "Equipamentos", "Débito", "ETAPA 2", "2600.00", "2026-06-12", "gabriel.junior@jaf.com", "Obra Osasco", null);

        // Gastos Residencial Morumbi
        inserirGasto("Terraplanagem", "Demolição", "Débito", "ETAPA 1", "12000.00", "2026-02-15", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Estrutura metálica", "Alvenaria", "Crédito", "ETAPA 1", "15000.00", "2026-03-01", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Alvenaria externa", "Alvenaria", "Débito", "ETAPA 1", "8900.00", "2026-03-20", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Instalação elétrica", "Equipamentos", "Débito", "ETAPA 2", "6500.00", "2026-04-10", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Hidráulica completa", "Equipamentos", "Crédito", "ETAPA 2", "7200.00", "2026-04-25", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Piso porcelanato", "Pintura", "Débito", "ETAPA 2", "9800.00", "2026-05-15", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Revestimento premium", "Pintura", "Crédito", "ETAPA 2", "5400.00", "2026-05-28", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Pintura interna", "Pintura", "Débito", "ETAPA 2", "3200.00", "2026-06-05", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Acabamentos gourmet", "Alvenaria", "Crédito", "ETAPA 2", "4500.00", "2026-06-10", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Reembolso transporte materiais", "Mão de Obra", "REEMBOLSO", "ETAPA 1", "1200.00", "2026-03-25", "carlos.silva@jaf.com", "Residencial Morumbi", true);
        inserirGasto("Reembolso ferramentas especiais", "Equipamentos", "REEMBOLSO", "ETAPA 1", "1800.00", "2026-04-05", "ana.souza@jaf.com", "Residencial Morumbi", false);
        inserirGasto("Imprevisto solo", "Custos extras", "Débito", "ETAPA 1", "2800.00", "2026-02-28", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Ajuste projeto arquitetônico", "Custos extras", "Crédito", "ETAPA 1", "2200.00", "2026-03-15", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Mão de obra especializada", "Mão de Obra", "Débito", "ETAPA 1", "8500.00", "2026-04-01", "carlos.silva@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Ar condicionado central", "Equipamentos", "Crédito", "ETAPA 2", "12000.00", "2026-06-01", "ana.souza@jaf.com", "Residencial Morumbi", null);
        inserirGasto("Automatização residencial", "Equipamentos", "Débito", "ETAPA 2", "7500.00", "2026-06-12", "carlos.silva@jaf.com", "Residencial Morumbi", null);

        // Gastos Comercial Pinheiros
        inserirGasto("Demolição antiga", "Demolição", "Débito", "ETAPA 1", "6500.00", "2026-02-01", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Estrutura concreto", "Alvenaria", "Crédito", "ETAPA 1", "18000.00", "2026-02-20", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Fachada comercial", "Alvenaria", "Débito", "ETAPA 1", "9500.00", "2026-03-15", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Elétrica comercial", "Equipamentos", "Débito", "ETAPA 2", "8400.00", "2026-04-05", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Hidráulica industrial", "Equipamentos", "Crédito", "ETAPA 2", "6200.00", "2026-04-20", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Piso industrial", "Pintura", "Débito", "ETAPA 2", "7800.00", "2026-05-10", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Forro acústico", "Pintura", "Crédito", "ETAPA 2", "4200.00", "2026-05-25", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Iluminação LED", "Equipamentos", "Débito", "ETAPA 2", "5600.00", "2026-06-05", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Portas automáticas", "Equipamentos", "Crédito", "ETAPA 2", "3400.00", "2026-06-12", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Reembolso logística", "Mão de Obra", "REEMBOLSO", "ETAPA 1", "1500.00", "2026-03-01", "pedro.santos@jaf.com", "Comercial Pinheiros", true);
        inserirGasto("Reembolso materiais importados", "Alvenaria", "REEMBOLSO", "ETAPA 1", "2500.00", "2026-03-20", "lucia.ferreira@jaf.com", "Comercial Pinheiros", false);
        inserirGasto("Licenças e alvarás", "Custos extras", "Débito", "ETAPA 1", "3200.00", "2026-02-10", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Projetos complementares", "Custos extras", "Crédito", "ETAPA 1", "4800.00", "2026-02-28", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Segurança obra", "Mão de Obra", "Débito", "ETAPA 1", "1800.00", "2026-04-01", "pedro.santos@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Elevador comercial", "Equipamentos", "Crédito", "ETAPA 2", "15000.00", "2026-06-08", "lucia.ferreira@jaf.com", "Comercial Pinheiros", null);
        inserirGasto("Sistema incêndio", "Equipamentos", "Débito", "ETAPA 2", "6800.00", "2026-06-13", "pedro.santos@jaf.com", "Comercial Pinheiros", null);

        // Gastos Galeria Shopping
        inserirGasto("Preparação terreno", "Demolição", "Débito", "ETAPA 1", "25000.00", "2026-05-10", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Fundações profundas", "Alvenaria", "Crédito", "ETAPA 1", "35000.00", "2026-05-20", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Estrutura metálica principal", "Alvenaria", "Débito", "ETAPA 1", "42000.00", "2026-06-01", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Cobertura metálica", "Alvenaria", "Crédito", "ETAPA 1", "28000.00", "2026-06-10", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Elétrica de alta tensão", "Equipamentos", "Débito", "ETAPA 2", "15000.00", "2026-06-15", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Hidráulica de grande porte", "Equipamentos", "Crédito", "ETAPA 2", "12000.00", "2026-06-20", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Ar condicionado central", "Equipamentos", "Débito", "ETAPA 2", "25000.00", "2026-06-25", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Reembolso estacionamento", "Mão de Obra", "REEMBOLSO", "ETAPA 1", "3200.00", "2026-05-25", "roberto.almeida@jaf.com", "Galeria Shopping", true);
        inserirGasto("Reembolso sinalização", "Alvenaria", "REEMBOLSO", "ETAPA 1", "1800.00", "2026-06-05", "gabriel.junior@jaf.com", "Galeria Shopping", false);
        inserirGasto("Licenças ambientais", "Custos extras", "Débito", "ETAPA 1", "8500.00", "2026-05-15", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Estudos geotécnicos", "Custos extras", "Crédito", "ETAPA 1", "6200.00", "2026-05-18", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Consultoria estrutural", "Custos extras", "Débito", "ETAPA 1", "12000.00", "2026-05-22", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Logística de materiais", "Mão de Obra", "Crédito", "ETAPA 1", "7800.00", "2026-06-02", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Segurança industrial", "Mão de Obra", "Débito", "ETAPA 1", "4500.00", "2026-06-08", "roberto.almeida@jaf.com", "Galeria Shopping", null);
        inserirGasto("Sistema firewall", "Equipamentos", "Crédito", "ETAPA 2", "9200.00", "2026-06-28", "gabriel.junior@jaf.com", "Galeria Shopping", null);
        inserirGasto("Escadas rolantes", "Equipamentos", "Débito", "ETAPA 2", "35000.00", "2026-07-01", "roberto.almeida@jaf.com", "Galeria Shopping", null);
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
