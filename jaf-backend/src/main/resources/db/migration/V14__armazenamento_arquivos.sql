ALTER TABLE funcionario ADD COLUMN foto_chave VARCHAR(255);
ALTER TABLE obra ADD COLUMN foto_chave VARCHAR(255);
ALTER TABLE gasto ADD COLUMN nota_fiscal_chave VARCHAR(255);
ALTER TABLE ocr_processamento ADD COLUMN arquivo_chave VARCHAR(255);

-- Fotos antigas ficavam em uploads/fotos e eram servidas publicamente em /uploads/fotos.
UPDATE funcionario SET foto_chave = SUBSTRING(foto_url, 10) WHERE foto_url LIKE '/uploads/fotos/%';

-- foto_url passa a guardar somente os avatares padrão do frontend.
UPDATE funcionario SET foto_url = NULL
WHERE foto_url IS NOT NULL AND foto_url NOT LIKE '/assets/Geral/avatar-%.svg';
