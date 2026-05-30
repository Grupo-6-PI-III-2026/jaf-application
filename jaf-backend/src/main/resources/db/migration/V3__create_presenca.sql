CREATE TABLE presenca (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data DATE,
    presente BOOLEAN,
    horario_entrada TIME,
    horario_saida TIME,
    funcionario_id BIGINT,
    obra_id BIGINT,
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    FOREIGN KEY (obra_id) REFERENCES obra(id)
);
