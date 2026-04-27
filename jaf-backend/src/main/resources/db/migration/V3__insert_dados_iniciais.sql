INSERT INTO obra (titulo, orcamento, status, dt_inicio, dt_termino_previsto) VALUES
  ('Obra Central', '1000000', 'EM_ANDAMENTO', '2026-01-01', '2026-12-31'),
  ('Obra Leste', '500000', 'PLANEJADA', '2026-05-01', '2026-11-30');

INSERT INTO alocacao_obra (cargo, funcionario_id, obra_id) VALUES
  ('ADMIN', 1, 1);

INSERT INTO gasto (descricao, categoria, metodo_pagamento, etapa, valor, dt_gasto, funcionario_id, obra_id) VALUES
  ('Compra de cimento', 'Material', 'Cartão', 'Fundação', 15000.00, '2026-02-15', 1, 1);

INSERT INTO relatorio (titulo, dt_emissao, funcionario_id) VALUES
  ('Relatório Inicial', '2026-03-01', 1);
