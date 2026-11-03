CREATE TABLE nota_fiscal (
    id BIGSERIAL PRIMARY KEY,
    gasto_id BIGINT NOT NULL REFERENCES gasto(id),
    numero VARCHAR(60),
    cnpj_emitente VARCHAR(18),
    valor NUMERIC(19,2),
    data_emissao DATE,
    arquivo_chave VARCHAR(255) NOT NULL,
    arquivo_nome VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL CHECK (tamanho_bytes > 0),
    sha256 CHAR(64) NOT NULL,
    criado_por BIGINT NOT NULL REFERENCES funcionario(id),
    criado_em TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_nota_fiscal_gasto ON nota_fiscal (gasto_id);
CREATE UNIQUE INDEX uk_nota_fiscal_gasto_sha ON nota_fiscal (gasto_id, sha256);
CREATE UNIQUE INDEX uk_nota_fiscal_emitente_numero ON nota_fiscal (cnpj_emitente, numero)
    WHERE cnpj_emitente IS NOT NULL AND numero IS NOT NULL;
