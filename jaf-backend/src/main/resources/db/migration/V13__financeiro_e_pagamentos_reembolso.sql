ALTER TABLE obra ADD COLUMN total_recebido NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE obra ADD CONSTRAINT ck_obra_total_recebido CHECK (total_recebido >= 0);

CREATE TABLE pagamento_reembolso (
    id BIGSERIAL PRIMARY KEY,
    gasto_id BIGINT NOT NULL REFERENCES gasto(id),
    valor NUMERIC(19,2) NOT NULL CHECK (valor > 0),
    data_pagamento DATE NOT NULL,
    observacao VARCHAR(1000)
);
CREATE INDEX idx_pagamento_reembolso_gasto ON pagamento_reembolso(gasto_id);

-- O legado não tem data de quitação: usa-se a data do gasto, com origem explícita.
INSERT INTO pagamento_reembolso (gasto_id, valor, data_pagamento, observacao)
SELECT id, valor, COALESCE(dt_gasto, CURRENT_DATE), 'Quitação migrada da flag legada; data original do pagamento indisponível.'
FROM gasto
WHERE metodo_pagamento = 'REEMBOLSO' AND reembolso_concluido = TRUE AND valor > 0;