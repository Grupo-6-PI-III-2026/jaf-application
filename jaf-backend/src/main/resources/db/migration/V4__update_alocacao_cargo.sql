-- Update the cargo column in alocacao_obra table to support new construction-specific roles
ALTER TABLE alocacao_obra MODIFY COLUMN cargo VARCHAR(100);