ALTER TABLE funcionario ADD COLUMN permissoes_personalizadas BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE funcionario f SET permissoes_personalizadas = TRUE
WHERE EXISTS (SELECT 1 FROM funcionario_permissao fp WHERE fp.funcionario_id = f.id);
