-- Add fields for external employees support
ALTER TABLE funcionario ADD COLUMN documento VARCHAR(100);
ALTER TABLE funcionario ADD COLUMN tipo_funcionario VARCHAR(20) DEFAULT 'INTERNO';
ALTER TABLE funcionario MODIFY COLUMN senha VARCHAR(255) NULL;