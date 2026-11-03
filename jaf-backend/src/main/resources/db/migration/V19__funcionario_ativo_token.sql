ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS desativado_em TIMESTAMP;
ALTER TABLE funcionario ADD COLUMN IF NOT EXISTS token_version INTEGER NOT NULL DEFAULT 0;

DO $$
DECLARE constraint_name TEXT;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'funcionario_permissao'::regclass
          AND contype = 'f'
          AND confrelid = 'funcionario'::regclass
    LOOP
        EXECUTE format('ALTER TABLE funcionario_permissao DROP CONSTRAINT %I', constraint_name);
    END LOOP;
END $$;

ALTER TABLE funcionario_permissao
    ADD CONSTRAINT fk_funcionario_permissao_funcionario
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id) ON DELETE CASCADE;
