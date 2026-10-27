CREATE TABLE funcionario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255),
    email VARCHAR(255),
    senha VARCHAR(255),
    cargo_global VARCHAR(50)
);

CREATE TABLE obra (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255),
    orcamento VARCHAR(255),
    status VARCHAR(255),
    dt_inicio DATE,
    dt_termino_previsto DATE
);

CREATE TABLE alocacao_obra (
    id BIGSERIAL PRIMARY KEY,
    cargo VARCHAR(50),
    funcionario_id BIGINT,
    obra_id BIGINT,
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    FOREIGN KEY (obra_id) REFERENCES obra(id)
);

CREATE TABLE gasto (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255),
    categoria VARCHAR(255),
    metodo_pagamento VARCHAR(255),
    etapa VARCHAR(255),
    valor DECIMAL(19, 2),
    dt_gasto DATE,
    funcionario_id BIGINT,
    obra_id BIGINT,
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    FOREIGN KEY (obra_id) REFERENCES obra(id)
);

CREATE TABLE relatorio (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255),
    dt_emissao DATE,
    funcionario_id BIGINT,
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id)
);
