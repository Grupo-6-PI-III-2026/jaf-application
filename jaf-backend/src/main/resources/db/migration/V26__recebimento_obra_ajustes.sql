ALTER TABLE recebimento_obra DROP CONSTRAINT IF EXISTS recebimento_obra_valor_check;
ALTER TABLE recebimento_obra ADD CONSTRAINT recebimento_obra_valor_check CHECK (valor <> 0);
