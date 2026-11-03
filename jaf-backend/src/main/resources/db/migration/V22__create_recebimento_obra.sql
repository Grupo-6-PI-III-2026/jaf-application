CREATE TABLE recebimento_obra (
    id BIGSERIAL PRIMARY KEY,
    obra_id BIGINT NOT NULL REFERENCES obra(id),
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0),
    data DATE NOT NULL,
    descricao VARCHAR(255),
    criado_por BIGINT REFERENCES funcionario(id),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_recebimento_obra ON recebimento_obra (obra_id, data);
INSERT INTO recebimento_obra (obra_id, valor, data, descricao)
SELECT id, total_recebido, CURRENT_DATE, 'Saldo inicial migrado'
FROM obra
WHERE total_recebido > 0;
