UPDATE gasto
SET reembolso_concluido = NULL
WHERE reembolso_concluido = false
  AND metodo_pagamento <> 'REEMBOLSO';
