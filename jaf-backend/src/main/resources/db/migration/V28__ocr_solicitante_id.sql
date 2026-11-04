ALTER TABLE ocr_processamento ADD COLUMN solicitante_id BIGINT;

UPDATE ocr_processamento o
SET solicitante_id = (SELECT f.id FROM funcionario f WHERE lower(f.email) = lower(o.solicitante));

CREATE INDEX idx_ocr_processamento_solicitante_id ON ocr_processamento (solicitante_id);