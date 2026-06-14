UPDATE funcionario
SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO'
WHERE cargo_global IN ('GESTOR_OBRA', 'MESTRE_DE_OBRAS', 'OPERADOR_LANCAMENTO');

UPDATE funcionario
SET cargo_global = 'ENGENHEIRO'
WHERE cargo_global IN ('ARQUITETO', 'PEDREIRO');

UPDATE funcionario
SET cargo_global = 'RESPONSAVEL_ADMINISTRATIVO'
WHERE email = 'rafael.pereira@jaf.com';

UPDATE funcionario
SET cargo_global = 'ENGENHEIRO'
WHERE email = 'gabriel.junior@jaf.com';