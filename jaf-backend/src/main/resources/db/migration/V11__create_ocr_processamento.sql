CREATE TABLE ocr_processamento (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    solicitante VARCHAR(255) NOT NULL,
    texto_bruto LONGTEXT,
    mensagem_erro VARCHAR(1000),
    criado_em DATETIME(6) NOT NULL,
    atualizado_em DATETIME(6) NOT NULL
);
