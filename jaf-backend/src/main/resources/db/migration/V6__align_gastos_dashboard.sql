-- Ajustar orçamento para visualização representativa no dashboard
UPDATE obra SET orcamento = '65000' WHERE id = 1;
UPDATE obra SET orcamento = '25000' WHERE id = 2;

-- Normalizar etapas para o padrão do dashboard
UPDATE gasto SET etapa = 'ETAPA 1' WHERE etapa IN ('Pintura', 'Estrutura', 'Fundação');
UPDATE gasto SET etapa = 'ETAPA 2' WHERE etapa IN ('Instalações', 'Acabamento');

-- Normalizar categorias para o padrão do gráfico de barras
UPDATE gasto SET categoria = 'Alvenaria'    WHERE categoria IN ('Cimento', 'Silicone');
UPDATE gasto SET categoria = 'Equipamentos' WHERE categoria IN ('Eletrica', 'Hidraulica');
UPDATE gasto SET categoria = 'Pintura'      WHERE categoria = 'Ceramica';

-- Reembolsos para obra 1 (2 pendentes, 2 concluídos → pizza 50/50)
INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id, reembolso_concluido) VALUES
('Reembolso material extra',  'Alvenaria',    'REEMBOLSO', 'ETAPA 1', 1200.00, '2026-01-15', 2, 1, false),
('Reembolso ferramentas',     'Equipamentos', 'REEMBOLSO', 'ETAPA 1',  800.00, '2026-02-10', 3, 1, true),
('Reembolso transporte',      'Mão de Obra',  'REEMBOLSO', 'ETAPA 2',  650.00, '2026-02-25', 4, 1, false),
('Reembolso EPI',             'Mão de Obra',  'REEMBOLSO', 'ETAPA 2',  450.00, '2026-03-05', 3, 1, true);

-- Gastos imprevistos (categoria = Custos extras) — 6 meses para o gráfico de linha
INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id) VALUES
('Reparo imprevisto estrutural', 'Custos extras', 'Débito',  'ETAPA 1', 3200.00, '2026-01-20', 2, 1),
('Material adicional urgente',   'Custos extras', 'Crédito', 'ETAPA 1', 1800.00, '2026-02-12', 3, 1),
('Ajuste de planta',             'Custos extras', 'Débito',  'ETAPA 2', 2500.00, '2026-03-08', 4, 1),
('Correção elétrica imprevista', 'Custos extras', 'Débito',  'ETAPA 2', 1100.00, '2026-04-15', 2, 1),
('Reforço de laje',              'Custos extras', 'Crédito', 'ETAPA 1', 4200.00, '2026-05-03', 3, 1),
('Troca de tubulação',           'Custos extras', 'Débito',  'ETAPA 2',  950.00, '2026-06-10', 4, 1);

-- Gastos regulares adicionais para métricas representativas
INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id) VALUES
('Demolição de paredes internas', 'Demolição',    'Débito',  'ETAPA 1',  8500.00, '2026-01-25', 2, 1),
('Mão de obra estrutura',         'Mão de Obra',  'Débito',  'ETAPA 1',  7200.00, '2026-02-05', 3, 1),
('Pintura externa completa',      'Pintura',      'Crédito', 'ETAPA 2',  4800.00, '2026-03-20', 4, 1),
('Locação de equipamentos',       'Equipamentos', 'Débito',  'ETAPA 1',  3100.00, '2026-02-28', 2, 1),
('Reforço de fundação',           'Alvenaria',    'Débito',  'ETAPA 1',  5500.00, '2026-01-30', 3, 1);
