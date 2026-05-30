-- Add location and responsible fields to obra table
ALTER TABLE obra ADD COLUMN responsavel VARCHAR(255);
ALTER TABLE obra ADD COLUMN endereco VARCHAR(255);
ALTER TABLE obra ADD COLUMN cidade VARCHAR(100);
ALTER TABLE obra ADD COLUMN estado VARCHAR(2);