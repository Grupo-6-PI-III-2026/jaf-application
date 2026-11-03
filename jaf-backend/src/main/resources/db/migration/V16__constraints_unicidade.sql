UPDATE funcionario
SET email = lower(trim(email))
WHERE email <> lower(trim(email));

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM funcionario GROUP BY lower(email) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'E-mails duplicados em funcionario; resolva manualmente antes da V16';
    END IF;
    IF EXISTS (SELECT 1 FROM presenca GROUP BY funcionario_id, obra_id, data HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Presencas duplicadas; resolva manualmente antes da V16';
    END IF;
    IF EXISTS (
        SELECT 1
        FROM alocacao_obra a
        JOIN alocacao_obra b
          ON a.funcionario_id = b.funcionario_id
         AND a.obra_id = b.obra_id
         AND a.id < b.id
         AND daterange(COALESCE(a.dt_inicio, '-infinity'::date), COALESCE(a.dt_fim, 'infinity'::date), '[]')
             && daterange(COALESCE(b.dt_inicio, '-infinity'::date), COALESCE(b.dt_fim, 'infinity'::date), '[]')
    ) THEN
        RAISE EXCEPTION 'Alocacoes com vigencia sobreposta; resolva manualmente antes da V16';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_funcionario_email_lower ON funcionario (lower(email));
CREATE UNIQUE INDEX uk_obra_titulo_lower ON obra (lower(titulo));
CREATE UNIQUE INDEX uk_presenca_func_obra_data ON presenca (funcionario_id, obra_id, data);

CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE alocacao_obra
    ADD CONSTRAINT ex_alocacao_vigencia EXCLUDE USING gist (
        funcionario_id WITH =,
        obra_id WITH =,
        daterange(COALESCE(dt_inicio, '-infinity'::date), COALESCE(dt_fim, 'infinity'::date), '[]') WITH &&
    );
