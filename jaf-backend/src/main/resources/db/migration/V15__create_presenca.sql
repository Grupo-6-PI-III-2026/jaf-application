CREATE TABLE IF NOT EXISTS presenca (
    id BIGSERIAL PRIMARY KEY,
    funcionario_id BIGINT REFERENCES funcionario(id),
    obra_id BIGINT REFERENCES obra(id),
    data DATE,
    presente BOOLEAN,
    horario_entrada TIME,
    horario_saida TIME
);
