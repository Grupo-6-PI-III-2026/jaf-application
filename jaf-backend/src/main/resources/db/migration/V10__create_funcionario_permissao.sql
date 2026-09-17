CREATE TABLE funcionario_permissao (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    funcionario_id BIGINT NOT NULL,
    permissao VARCHAR(50) NOT NULL,
    CONSTRAINT uk_funcionario_permissao UNIQUE (funcionario_id, permissao),
    CONSTRAINT fk_funcionario_permissao_funcionario FOREIGN KEY (funcionario_id) REFERENCES funcionario(id)
);