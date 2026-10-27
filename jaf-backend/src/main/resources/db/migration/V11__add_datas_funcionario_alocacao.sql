ALTER TABLE funcionario
    ADD COLUMN IF NOT EXISTS created_at DATE;

UPDATE funcionario
SET created_at = CURRENT_DATE
WHERE created_at IS NULL;

ALTER TABLE funcionario
    ALTER COLUMN created_at SET DEFAULT CURRENT_DATE;

ALTER TABLE alocacao_obra
    ADD COLUMN IF NOT EXISTS dt_inicio DATE;

ALTER TABLE alocacao_obra
    ADD COLUMN IF NOT EXISTS dt_fim DATE;

UPDATE alocacao_obra
SET dt_inicio = CURRENT_DATE
WHERE dt_inicio IS NULL;

ALTER TABLE alocacao_obra
    ALTER COLUMN dt_inicio SET DEFAULT CURRENT_DATE;
