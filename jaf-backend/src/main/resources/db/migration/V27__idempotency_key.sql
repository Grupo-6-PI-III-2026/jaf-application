CREATE TABLE idempotency_key (
    id BIGSERIAL PRIMARY KEY,
    chave UUID NOT NULL,
    usuario_id BIGINT NOT NULL,
    metodo VARCHAR(10) NOT NULL,
    caminho VARCHAR(255) NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    estado VARCHAR(12) NOT NULL,
    status INTEGER,
    corpo TEXT,
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_idempotency_chave_usuario UNIQUE (chave, usuario_id),
    CONSTRAINT ck_idempotency_estado CHECK (estado IN ('PENDENTE', 'CONCLUIDA'))
);

CREATE INDEX idx_idempotency_criado_em ON idempotency_key (criado_em);
