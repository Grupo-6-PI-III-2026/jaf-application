CREATE INDEX IF NOT EXISTS idx_gasto_obra_data ON gasto (obra_id, dt_gasto);
CREATE INDEX IF NOT EXISTS idx_gasto_funcionario ON gasto (funcionario_id);
CREATE INDEX IF NOT EXISTS idx_alocacao_obra ON alocacao_obra (obra_id);
CREATE INDEX IF NOT EXISTS idx_presenca_obra_data ON presenca (obra_id, data);
