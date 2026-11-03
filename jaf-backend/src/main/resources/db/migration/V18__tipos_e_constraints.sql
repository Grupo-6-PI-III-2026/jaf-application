DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM obra WHERE orcamento IS NULL OR orcamento !~ '^\d+(\.\d{1,2})?$') THEN
        RAISE EXCEPTION 'obra.orcamento com valor invalido; corrija antes da V18';
    END IF;
END $$;

ALTER TABLE obra ALTER COLUMN orcamento TYPE NUMERIC(19,2) USING trim(orcamento)::numeric;
ALTER TABLE obra ALTER COLUMN orcamento SET NOT NULL;
ALTER TABLE obra ADD CONSTRAINT ck_obra_orcamento CHECK (orcamento >= 0);

ALTER TABLE gasto ADD CONSTRAINT ck_gasto_valor CHECK (valor > 0) NOT VALID;
ALTER TABLE gasto VALIDATE CONSTRAINT ck_gasto_valor;
ALTER TABLE gasto ALTER COLUMN obra_id SET NOT NULL;
ALTER TABLE gasto ALTER COLUMN funcionario_id SET NOT NULL;
ALTER TABLE gasto ALTER COLUMN dt_gasto SET NOT NULL;
ALTER TABLE alocacao_obra ALTER COLUMN funcionario_id SET NOT NULL;
ALTER TABLE alocacao_obra ALTER COLUMN obra_id SET NOT NULL;
UPDATE funcionario SET cargo_global = 'ENGENHEIRO' WHERE cargo_global IS NULL;
ALTER TABLE funcionario ALTER COLUMN cargo_global SET NOT NULL;
