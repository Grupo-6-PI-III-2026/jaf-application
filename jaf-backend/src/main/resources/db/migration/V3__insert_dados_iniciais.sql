-- Inserir funcionários
INSERT INTO funcionario (nome, email, senha, cargo_global) VALUES
('Rafael Pereira', 'rafael.pereira@jaf.com', '$2a$10$yPsrDB3hR4NRGdCKaTxrz.CNjwgz2LjEZnUMw9ttY00GHSDfhLcdW', 'MESTRE_DE_OBRAS'),
('Gabriel Junior', 'gabriel.junior@jaf.com', '$2a$10$yPsrDB3hR4NRGdCKaTxrz.CNjwgz2LjEZnUMw9ttY00GHSDfhLcdW', 'ENGENHEIRO'),
('Ana Souza', 'ana.souza@jaf.com', '$2a$10$yPsrDB3hR4NRGdCKaTxrz.CNjwgz2LjEZnUMw9ttY00GHSDfhLcdW', 'ARQUITETO'),
('Isac Newton', 'isac.newton@jaf.com', '$2a$10$yPsrDB3hR4NRGdCKaTxrz.CNjwgz2LjEZnUMw9ttY00GHSDfhLcdW', 'PEDREIRO');

-- Inserir obras
INSERT INTO obra (titulo, orcamento, status, dt_inicio, dt_termino_previsto) VALUES
('Obra Alphaville', '250000', 'EM_ANDAMENTO', '2026-01-01', '2026-06-01'),
('Obra Osasco', '180000', 'EM_ANDAMENTO', '2026-02-15', '2026-08-15');

-- Alocar funcionários nas obras
INSERT INTO alocacao_obra (funcionario_id, obra_id, cargo) VALUES
(2, 1, 'MESTRE_DE_OBRAS'),
(3, 1, 'ENGENHEIRO'),
(4, 1, 'ARQUITETO'),
(2, 2, 'ENGENHEIRO'),
(5, 2, 'PEDREIRO');

-- Inserir gastos para Obra Alphaville
INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id) VALUES
('Pagamento mão de obra', 'Cimento', 'Débito', 'Pintura', 1000.00, '2026-01-12', 5, 1),
('Silicone e acabamentos', 'Silicone', 'Débito', 'Pintura', 450.00, '2026-01-10', 2, 1),
('Compra de insumos', 'Cimento', 'Débito', 'Estrutura', 2300.00, '2026-01-08', 3, 1),
('Material elétrico', 'Eletrica', 'Crédito', 'Instalações', 1850.00, '2026-01-20', 3, 1),
('Piso cerâmico', 'Ceramica', 'Débito', 'Acabamento', 3200.00, '2026-02-05', 2, 1);

-- Inserir gastos para Obra Osasco
INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id) VALUES
('Fundação e estrutura', 'Cimento', 'Débito', 'Fundação', 5500.00, '2026-02-20', 3, 2),
('Material hidráulico', 'Hidraulica', 'Débito', 'Instalações', 2100.00, '2026-03-05', 2, 2),
('Tinta e pintura', 'Pintura', 'Crédito', 'Acabamento', 1200.00, '2026-03-15', 5, 2);
