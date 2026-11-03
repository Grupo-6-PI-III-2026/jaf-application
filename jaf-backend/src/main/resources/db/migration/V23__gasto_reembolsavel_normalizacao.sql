ALTER TABLE gasto ADD COLUMN reembolsavel BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE gasto SET reembolsavel = TRUE WHERE metodo_pagamento = 'REEMBOLSO';
UPDATE gasto SET metodo_pagamento = 'CARTAO_DEBITO'
WHERE metodo_pagamento IN ('Débito', 'Debito');
UPDATE gasto SET metodo_pagamento = 'CARTAO_CREDITO'
WHERE metodo_pagamento IN ('Crédito', 'Credito');
