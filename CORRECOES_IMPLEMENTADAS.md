# Correções Implementadas

## Fase 1 — Correções DB-01 a DB-09 (esta execução)

### DB-01

- **Problema Corrigido:** O Hibernate podia alterar o schema automaticamente e a tabela `presenca` não era versionada pelo Flyway.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/application.properties`; `jaf-backend/pom.xml`; `jaf-backend/src/main/resources/db/migration/V15__create_presenca.sql`.
- **Solução Aplicada:** O `ddl-auto` passou para `validate`, o baseline automático do Flyway foi desativado, a tabela `presenca` foi criada em migration e foram adicionadas dependências de Testcontainers para a futura validação PostgreSQL.

### DB-02

- **Problema Corrigido:** Unicidade de e-mail, título, presença e vigência de alocações dependia apenas de verificações de aplicação sujeitas a corrida.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V16__constraints_unicidade.sql`; `jaf-backend/src/main/java/com/jaf/application/repository/FuncionarioRepository.java`; `jaf-backend/src/main/java/com/jaf/application/service/FuncionarioService.java`.
- **Solução Aplicada:** E-mails são normalizados no serviço e consultados com `lower`; atualização rejeita e-mail de outro funcionário; a migration valida duplicidades existentes e cria índice funcional de e-mail, índices únicos de título/presença e constraint de exclusão para alocações sobrepostas.

### DB-03

- **Problema Corrigido:** Consultas por obra, data e funcionário não possuíam índices dedicados.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V17__indices.sql`.
- **Solução Aplicada:** Foram criados índices para gastos por obra/data, gastos por funcionário, alocações por obra e presenças por obra/data.

### DB-04

- **Problema Corrigido:** Orçamento era armazenado como texto, havia conversão silenciosa para zero e faltavam constraints financeiras e referências obrigatórias.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V18__tipos_e_constraints.sql`; `jaf-backend/src/main/java/com/jaf/application/model/Obra.java`; `jaf-backend/src/main/java/com/jaf/application/service/ObraService.java`; `jaf-backend/src/main/java/com/jaf/application/service/DashboardService.java`; `jaf-backend/src/main/java/com/jaf/application/config/ObraSerializer.java`.
- **Solução Aplicada:** O orçamento passou a `BigDecimal` persistido como `NUMERIC(19,2)`, com validação prévia e `CHECK`; gastos, alocações e cargos receberam restrições de nulidade; o serializer mantém o contrato JSON como string e o dashboard deixou de converter valores inválidos silenciosamente.

### DB-05

- **Problema Corrigido:** Funcionários não tinham campos de desativação e versionamento de token, e permissões não tinham política de exclusão.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V19__funcionario_ativo_token.sql`; `jaf-backend/src/main/java/com/jaf/application/model/Funcionario.java`.
- **Solução Aplicada:** Foram adicionados `desativado_em` e `token_version` (preservando a `V14` já existente de `ativo`) e a FK de permissões passou a usar cascata apenas nessa tabela, sem cascata no histórico financeiro.

### DB-06

- **Problema Corrigido:** Atualizações concorrentes podiam sobrescrever dados e não havia metadados de auditoria.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V20__versao_e_auditoria.sql`; `jaf-backend/src/main/java/com/jaf/application/model/Funcionario.java`; `jaf-backend/src/main/java/com/jaf/application/model/Gasto.java`; `jaf-backend/src/main/java/com/jaf/application/model/Obra.java`; `jaf-backend/src/main/java/com/jaf/application/model/AlocacaoObra.java`; `jaf-backend/src/main/java/com/jaf/application/model/Presenca.java`; `jaf-backend/src/main/java/com/jaf/application/model/PagamentoReembolso.java`.
- **Solução Aplicada:** Entidades financeiras receberam `@Version` onde previsto, além de timestamps e referências de criação/atualização ignoradas no JSON até a estabilização do contrato. Callbacks garantem timestamps para novas entidades.

### DB-07

- **Problema Corrigido:** Não existia estrutura persistente para anexar e auditar notas fiscais.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V21__create_nota_fiscal.sql`.
- **Solução Aplicada:** Criada tabela de metadados com chave externa do storage, hash SHA-256, metadados do arquivo, índices por gasto e unicidade contra duplicidade.

### DB-08

- **Problema Corrigido:** `obra.total_recebido` era sobrescrito sem histórico de lançamentos.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V22__create_recebimento_obra.sql`.
- **Solução Aplicada:** Criada tabela de recebimentos com valor, data, descrição e autor; o saldo atual positivo é migrado como lançamento inicial, mantendo o campo legado para compatibilidade.

### DB-09

- **Problema Corrigido:** O método de pagamento misturava meio de pagamento com o fato de o gasto ser reembolsável e continha valores legados incompatíveis.
- **Arquivos Afetados:** `jaf-backend/src/main/resources/db/migration/V23__gasto_reembolsavel_normalizacao.sql`; `jaf-backend/src/main/java/com/jaf/application/model/Gasto.java`; `jaf-backend/src/main/java/com/jaf/application/service/GastoService.java`.
- **Solução Aplicada:** Adicionado `reembolsavel`, preenchido a partir do legado, normalizados débito/crédito e sincronizado o novo campo ao criar/editar gastos sem remover ainda o literal legado.

### Validação da Fase 1

- Testes direcionados `FinanceiroApiTest`, `ReembolsoPersistenciaTest` e `AcessoFinanceiroTest`: **passaram**.
- Compilação de produção e testes: **passou**.
- A suíte completa ainda possui uma falha preexistente em `FuncionarioAutorizacaoApiTest` (regra de alteração de permissões/cargo de ADMIN, fora da Fase 1); não foi alterada para evitar escopo indevido.

> Escopo desta rodada: problemas **P1 a P10**. Os itens do `CORRECOES.md` **não foram aplicados** (adiados por decisão do solicitante).
> Validação feita apenas por leitura de código; **testes, build e lint não foram executados** (sem JDK e sem dependências npm instaladas no ambiente).

## Implementado

## Estabilização da suíte backend — Fase 1 prioritária

### Suíte de testes backend

- **Problema Corrigido:** O contexto Spring falhava ao instanciar `FuncionarioService`, os testes de autorização enviavam requests mutáveis sem CSRF e usavam usuários simulados sem as authorities persistidas. Também havia expectativas legadas incompatíveis com a política atual de cargo ADMIN, com os grupos do Actuator e com o tratamento seguro de erros do OCR.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/service/FuncionarioService.java`; `jaf-backend/src/main/java/com/jaf/application/service/AcessoObraService.java`; `jaf-backend/src/main/resources/application.properties`; `jaf-backend/src/test/java/com/jaf/application/FuncionarioAutorizacaoApiTest.java`; `jaf-backend/src/test/java/com/jaf/application/HealthCheckApiTest.java`; `jaf-backend/src/test/java/com/jaf/application/repository/ReembolsoPersistenciaTest.java`; `jaf-backend/src/test/java/com/jaf/application/service/AcessoFinanceiroTest.java`; `jaf-backend/src/test/java/com/jaf/application/service/GastoServiceTest.java`; `jaf-backend/src/test/java/com/jaf/application/service/ObraServiceTest.java`; `jaf-backend/src/test/java/com/jaf/application/service/OcrServiceTest.java`.
- **Solução Aplicada:** A injeção principal de `FuncionarioService` foi explicitada com `@Autowired`; os testes de API passaram a enviar CSRF e authorities carregadas pelo serviço real; o healthcheck passou a declarar explicitamente os grupos `liveness` e `readiness`; mocks de alocação foram atualizados para `existsAtiva`; fixtures financeiras passaram a preencher `reembolsavel`; e a expectativa OCR passou a respeitar a mensagem sanitizada do contrato atual.

### Validação

- **Backend:** `mvnw.cmd test` passou com **105 testes, 0 falhas, 0 erros e 1 teste ignorado** (`MigracoesPostgresTest`, pois Docker não está disponível nesta máquina).
- **Build:** `mvnw.cmd -DskipTests package` passou.

| # | Problema | Arquivos principais |
|---|---|---|
| P3 | Excluir obra com alocações retorna 409 com mensagem clara; sem cascata. Frontend exibe mensagem amigável. | `ObraService.java`, `AlocacaoObraRepository.java`, `DetalhamentoObras.tsx`, `ObraServiceTest.java` |
| P1 | Filtro de status lista todos os status do sistema; mensagem "Nenhuma obra encontrada com esse status."; dropdown arredondado. Busca por nome intacta. | `Home.tsx`, `Home.module.css`, `Home.test.tsx` |
| P2 | Mensagens de validação específicas por campo (backend junta as mensagens dos campos em `message`; frontend as exibe). | `ObraDto.java`, `FuncionarioDto.java`, `GlobalExceptionHandler.java`, `NovaObra.tsx`, `NovoFuncionario.tsx` |
| P4 | Texto de Gestão de Presença reescrito sem termos técnicos. | `GestaoPresenca.tsx` |
| P5 | Removido o filtro de período da tela de perfil de acesso; mantida só a busca. Componente compartilhado não alterado. | `Permissoes.tsx`, `Permissoes.module.css` |
| P6 | Aviso de bloqueio (`avisoBloqueio`) não tinha estilo; criado com contraste e padding adequados. | `Permissoes.module.css` |
| P7 | Removida a checagem de unicidade de nome (`existsByNome`). Não havia constraint no banco (V1–V13 verificadas). E-mail continua único. | `FuncionarioService.java`, `FuncionarioRepository.java`, `FuncionarioServiceTest.java` |
| P8 | Backend: alterar cargo de ADMIN retorna 403 (`PATCH /cargo` e `PUT /funcionarios/{id}`). Frontend: seletor de cargo desabilitado para ADMIN. | `AcessoObraService.java`, `FuncionarioService.java`, `Permissoes.tsx`, `FuncionarioAutorizacaoTest.java` |
| P9 | Removido o botão "Esqueceu a senha?", seu estilo e o teste correspondente. | `Login.tsx`, `Login.module.css`, `Login.test.tsx` |

## Fase 2 — Segurança e autenticação (revisada)

### SEG-01, SEG-03 e SEG-04

- **Problema Corrigido:** A configuração JWT usava propriedades sem unidade explícita, identificava o usuário por dados mutáveis e não revogava tokens emitidos antes de alterações de conta.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/config/JwtProperties.java`; `jaf-backend/src/main/java/com/jaf/application/config/GerenciadorTokenJwt.java`; `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoFilter.java`; `jaf-backend/src/main/java/com/jaf/application/config/AutenticacaoEntryPoint.java`; `jaf-backend/src/main/java/com/jaf/application/dto/FuncionarioDetalhesDto.java`; `jaf-backend/src/main/java/com/jaf/application/service/AutenticacaoService.java`.
- **Solução Aplicada:** A expiração passou a usar duração em segundos validada, o `sub` passou a conter o ID do funcionário, a versão do token é validada contra o banco e respostas de autenticação inválida passaram a usar JSON 401 sem expor detalhes sensíveis.

### SEG-02

- **Problema Corrigido:** O seed de produção podia criar credenciais conhecidas e o bootstrap administrativo não possuía uma entrada operacional segura.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/config/DatabaseSeeder.java`; `jaf-backend/src/main/java/com/jaf/application/config/BootstrapAdminRunner.java`; `jaf-backend/src/main/resources/application-dev.properties`; `docker-compose.yml`; `terraform/variables.tf`; `terraform/main.tf`; `terraform/modules/ec2/variables.tf`; `terraform/modules/ec2/main.tf`; `terraform/scripts/backend.sh`.
- **Solução Aplicada:** O seed ficou restrito ao profile `dev`; o primeiro ADMIN pode ser criado somente quando credenciais fornecidas por ambiente existem, com senha validada e armazenada com hash; Terraform e o script de inicialização repassam os valores como variáveis sensíveis, sem valores padrão reais.

### SEG-06, SEG-09, SEG-11 e SEG-12

- **Problema Corrigido:** Cookies JWT, CORS e cabeçalhos de segurança tinham valores permissivos ou configuração espalhada; Swagger e migrations de desenvolvimento não estavam isolados por profile.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/config/CookieProperties.java`; `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`; `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java`; `jaf-backend/src/main/resources/application.properties`; `jaf-backend/src/main/resources/application-dev.properties`; `terraform/variables.tf`; `terraform/modules/ec2/variables.tf`.
- **Solução Aplicada:** O cookie usa `HttpOnly`, `Secure`, `SameSite`, `Path` e duração configuráveis; CORS aceita apenas origens e cabeçalhos explícitos; foram adicionados CSP, `frame-ancestors`, `Referrer-Policy` e `frameOptions`; Swagger e dados de desenvolvimento ficaram limitados ao profile `dev`.

### SEG-10

- **Problema Corrigido:** Requisições mutáveis não tinham proteção CSRF compatível com o uso de cookie JWT.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`; `jaf-backend/src/main/java/com/jaf/application/config/CsrfCookieFilter.java`.
- **Solução Aplicada:** Foi configurado `CookieCsrfTokenRepository` com cookie `XSRF-TOKEN` legível pelo frontend, `CsrfTokenRequestAttributeHandler` e filtro emissor; somente `POST /funcionarios/login` é excluído da validação.

### SEG-05, SEG-07 e SEG-08

- **Problema Corrigido:** HTTPS/cookie não tinham integração operacional segura e o login não possuía proteção contra força bruta ou tratamento uniforme de falhas.
- **Arquivos Afetados:** `terraform/variables.tf`; `terraform/modules/alb/main.tf`; `terraform/modules/ec2/variables.tf`; `terraform/modules/ec2/main.tf`; `terraform/scripts/backend.sh`; `jaf-backend/src/main/java/com/jaf/application/config/ClientIpResolver.java`; `jaf-backend/src/main/java/com/jaf/application/service/LoginAttemptService.java`; `jaf-backend/src/main/java/com/jaf/application/exceptions/TooManyRequests.java`; `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioController.java`; `jaf-backend/src/main/java/com/jaf/application/config/GlobalExceptionHandler.java`.
- **Solução Aplicada:** HTTPS exige certificado quando habilitado, o ALB descarta headers inválidos e o backend recebe a política de cookie e a quantidade de proxies confiáveis; falhas de login são limitadas por IP/e-mail por janela de 15 minutos, com 429 e `Retry-After`, sem distinguir usuário inexistente de senha inválida.

### SEG-13 e SEG-14

- **Problema Corrigido:** Endpoints de alocação permitiam leitura e alterações fora do escopo de obra e a paginação de obras aceitava propriedades arbitrárias e tamanhos sem teto.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/service/AcessoObraService.java`; `jaf-backend/src/main/java/com/jaf/application/service/AlocacaoObraService.java`; `jaf-backend/src/main/java/com/jaf/application/service/RelatorioService.java`; `jaf-backend/src/main/java/com/jaf/application/service/PaginacaoUtil.java`; `jaf-backend/src/main/java/com/jaf/application/service/ObraService.java`.
- **Solução Aplicada:** Consultas e mutações de alocações verificam a obra; consulta por funcionário é limitada ao próprio usuário ou ADMIN; relatórios são filtrados pelo responsável; paginação valida página, tamanho, direção e whitelist de ordenação.

### SEG-15, SEG-16 e SEG-17

- **Problema Corrigido:** Upload aceitava conteúdo com tipo/extensão forjáveis, arquivos eram públicos, erros e credenciais tinham defaults inseguros e a exclusão de conta não exigia reautenticação nem protegia o último ADMIN.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/controller/FuncionarioPerfilController.java`; `jaf-backend/src/main/java/com/jaf/application/config/StaticResourceConfig.java`; `jaf-backend/src/main/java/com/jaf/application/config/SecurityConfiguracao.java`; `jaf-backend/src/main/java/com/jaf/application/dto/FuncionarioPerfilUpdateDto.java`; `jaf-backend/src/main/java/com/jaf/application/dto/ExcluirContaDto.java`; `jaf-backend/src/main/java/com/jaf/application/service/FuncionarioService.java`; `jaf-backend/src/main/java/com/jaf/application/repository/FuncionarioRepository.java`; `jaf-backend/src/main/resources/application.properties`; `jaf-frontend/react/src/Pages/Profile/Profile.tsx`; `jaf-frontend/react/src/Service/Funcionarios/funcionarioService.ts`.
- **Solução Aplicada:** Imagens são identificadas por magic bytes e recebem extensão UUID; `/uploads` exige autenticação; URLs de foto são validadas; erros 4xx não são logados como erro e defaults de banco/RabbitMQ foram removidos; exclusão virou desativação com senha atual, revogação de tokens e bloqueio do último ADMIN.

### SEG-18, SEG-19, SEG-20 e SEG-21

- **Problema Corrigido:** Artefatos de banco/log poderiam ser versionados, containers rodavam como root, o build Docker ignorava testes e o pipeline não tinha validação mínima.
- **Arquivos Afetados:** `.gitignore`; `jaf-backend/Dockerfile`; `docker-compose.yml`; `jaf-backend/src/main/java/com/jaf/application/dto/AlterarSenhaDto.java`; `terraform/scripts/backend.sh`; `terraform/modules/alb/main.tf`; `jaf-backend/pom.xml`; `jaf-backend/src/main/resources/application-dev.properties`.
- **Solução Aplicada:** Artefatos H2 e logs de crash foram ignorados, o container usa UID não-root, healthcheck e limite de memória, portas locais foram vinculadas a loopback, H2 passou ao escopo de teste, a política de senha foi unificada e o ALB recebeu proteção adicional de headers. O script de backend mantém login GHCR sem `set -x` e profile de produção explícito.

### Validação da Fase 2

- Compilação de produção: **passou**.
- `ApplicationTests`: **passou**.
- `FuncionarioAutorizacaoTest`: 17/19 testes passaram; 2 expectativas legadas ainda esperam exclusão física, enquanto a regra SEG-17 implementada desativa a conta para preservar histórico.
- A auditoria confirmou que o JWT usa cookie `HttpOnly`, CSRF está habilitado, o login possui rate limit, tokens são revogados por `tokenVersion` e o OpenAPI foi ajustado para `cookieAuth`.
- Foi removida a sobrescrita global de beans e a configuração de segurança passou a usar injeção por construtor.
- `FinanceiroApiTest`: **passou** após atualizar requisições mutáveis com `.with(csrf())`.
- `terraform validate`: **passou**.
- Frontend: `npm run lint` e `npm run build` (inclui `tsc -b`): **passaram**.
- A revisão identificou duas expectativas legadas em `FuncionarioAutorizacaoTest` que ainda esperam exclusão física; a implementação vigente desativa contas para preservar histórico. Esse teste precisa ser alinhado à regra SEG-17 antes de considerar a Fase 2 totalmente verde.
| P10 | Exclusão verifica alocações, presenças, gastos e relatórios; retorna 409 com a mensagem pedida. Novos endpoints `PATCH /funcionarios/{id}/inativar` e `/reativar`. Campo `ativo` (migration **V14**); usuário inativo não faz login (provider e filtro JWT). UI de inativar/reativar/excluir. | `FuncionarioService.java`, `FuncionarioController.java`, `Funcionario.java`, `V14__add_ativo_funcionario.sql`, `AutenticacaoProvider.java`, `AutenticacaoFilter.java`, `FuncionarioDetalhesDto.java`, DTOs, `Permissoes.tsx`, `funcionarioService.ts`, `authService.tsx` |

## Observações

- **P10 – local da UI:** não existia tela para excluir outros usuários; os botões foram colocados no painel de "Configurações de Acesso" (`Permissoes`), visíveis conforme `EDITAR_FUNCIONARIO` / `DELETAR_FUNCIONARIO`.
- **P10 – exclusão da própria conta** (`DELETE /funcionarios/me`) também passou a retornar 409 quando há registros vinculados (mensagem própria), em vez de erro de FK.
- **P2:** o mapa `fields` continua sendo retornado; o `message` agora contém as mensagens específicas.
- **P1:** o dropdown nativo só aceita bordas arredondadas na lista aberta em navegadores com `appearance: base-select`; nos demais, o campo fechado segue o padrão arredondado.
- Frontend de usuário inativo no login: tela mostra "Usuário inativo..." quando o backend responde 401 com essa mensagem.

## Não implementado / pendente

- **Itens ainda não implementados:** permanecem os requisitos explicitamente listados nas seções de pendências das Fases 3, 4 e 5.
- Testes de UI para os novos botões de inativar/excluir em `Permissoes` não foram criados.

## Fase 3 — Backend, regras de negócio e API (em andamento)

### BE-01, BE-02, BE-03, BE-05, BE-08, BE-09, BE-10 e BE-12 (parcial)

- **Problema Corrigido:** O escopo de obras ignorava a vigência das alocações, listagens de gastos não aceitavam período, alguns serviços de escrita não delimitavam transações e alocações não possuíam fluxo explícito de encerramento.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/repository/AlocacaoObraRepository.java`; `jaf-backend/src/main/java/com/jaf/application/service/AcessoObraService.java`; `jaf-backend/src/main/java/com/jaf/application/service/GastoService.java`; `jaf-backend/src/main/java/com/jaf/application/repository/GastoFiltro.java`; `jaf-backend/src/main/java/com/jaf/application/controller/GastoController.java`; `jaf-backend/src/main/java/com/jaf/application/dto/AlocacaoObraDto.java`; `jaf-backend/src/main/java/com/jaf/application/service/AlocacaoObraService.java`; `jaf-backend/src/main/java/com/jaf/application/controller/AlocacaoObraController.java`; `jaf-backend/src/main/java/com/jaf/application/service/RelatorioService.java`; `jaf-backend/src/main/java/com/jaf/application/service/ObraService.java`; entidades financeiras e `ObraSerializer.java`.
- **Solução Aplicada:** A autorização consulta alocações ativas na data corrente; filtros de gastos por `from`/`to` são inclusivos; serviços principais receberam transações Spring; DTOs de alocação aceitam início/fim; sobreposição é rejeitada; alterações não podem trocar funcionário ou obra; foi criado `PATCH /alocacoes/{id}/encerrar`; relações `ManyToOne` passaram a LAZY e a serialização de obra usa o papel autenticado sem dependência de injeção do Jackson.

### BE-08 — Gestão de notas fiscais

- **Problema Corrigido:** A tabela de metadados de notas fiscais existia, mas não havia API para anexar, consultar, baixar ou excluir os documentos.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/model/NotaFiscal.java`; `jaf-backend/src/main/java/com/jaf/application/repository/NotaFiscalRepository.java`; `jaf-backend/src/main/java/com/jaf/application/storage/ArquivoStorage.java`; `jaf-backend/src/main/java/com/jaf/application/storage/LocalArquivoStorage.java`; `jaf-backend/src/main/java/com/jaf/application/service/NotaFiscalService.java`; `jaf-backend/src/main/java/com/jaf/application/controller/NotaFiscalController.java`; `jaf-backend/src/main/java/com/jaf/application/dto/NotaFiscalResponse.java`.
- **Solução Aplicada:** Criada abstração de storage com implementação local, validação por magic bytes para PDF/JPEG/PNG, limite de 10 MB, SHA-256, prevenção de duplicidade, validação de CNPJ, escopo pela obra do gasto e download autenticado. Exclusão ficou restrita a ADMIN.

### BE-10 — Exportação contábil de gastos

- **Problema Corrigido:** Não existia exportação estruturada de gastos para a contabilidade.
- **Arquivos Afetados:** `jaf-backend/pom.xml`; `jaf-backend/src/main/java/com/jaf/application/service/ExportacaoGastosService.java`; `jaf-backend/src/main/java/com/jaf/application/controller/GastoExportController.java`.
- **Solução Aplicada:** Criado `GET /gastos/export` com filtros de obra, período, etapa, categoria, método, funcionário e status de reembolso. CSV é UTF-8 com BOM, separador `;`, decimal com vírgula e proteção contra CSV injection; XLSX é gerado com `SXSSFWorkbook`; exportações são limitadas a 50.000 linhas e registradas como `Relatorio`.

### Validação da Fase 3

- Compilação/package do backend sem testes: **passou**.
- `git diff --check`: **passou**.

### FE-02 — Interface de notas fiscais

- **Problema Corrigido:** O OCR preenchia os campos do gasto, mas o arquivo da nota era descartado; não havia interface para anexar, consultar, baixar ou excluir comprovantes.
- **Arquivos Afetados:** `jaf-frontend/react/src/Service/NotasFiscais/notaFiscalService.ts`; `jaf-frontend/react/src/Components/NotasFiscais/NotasFiscaisGasto.tsx`; `jaf-frontend/react/src/Components/NotasFiscais/NotasFiscaisGasto.module.css`; `jaf-frontend/react/src/Pages/Obras/DetalhamentoObras.tsx`.
- **Solução Aplicada:** O arquivo selecionado para OCR permanece disponível e é anexado ao gasto salvo, sem recriar o lançamento quando o anexo falha. O formulário valida tipo e limite de 10 MB e aceita número/CNPJ opcionais. Cada gasto consulta e exibe suas notas, oferece download autenticado pelo cookie e restringe exclusão aos administradores. Em falha posterior ao salvamento, o formulário permanece aberto para nova tentativa.

### Validação do FE-02

- `npm run build`: **passou**.
- `npm run lint`: **passou**.
- `npx vitest run`: **19 arquivos e 126 testes passaram**.
- `git diff --check`: **passou**.

## Continuação solicitada — INT-01 e FE-07

### INT-01 — Migração inicial dos controllers para DTOs de resposta

- **Problema Corrigido:** Controllers de gastos, obras, alocações e relatórios devolviam entidades JPA diretamente, acoplando o JSON ao modelo de persistência e permitindo exposição acidental de campos internos.
- **Arquivos Afetados:** `jaf-backend/src/main/java/com/jaf/application/dto/FuncionarioResumo.java`; `ObraResponse.java`; `GastoResponse.java`; `AlocacaoResponse.java`; `RelatorioResponse.java`; `PageResponse.java`; `jaf-backend/src/main/java/com/jaf/application/controller/GastoController.java`; `ObraController.java`; `AlocacaoObraController.java`; `RelatorioController.java`.
- **Solução Aplicada:** Criados records explícitos para respostas, com orçamento serializado como string, campos administrativos condicionais, funcionário resumido sem senha e paginação estável. Os quatro controllers passaram a mapear entidades para DTOs antes de responder.
- **Limitação:** O desligamento de `spring.jpa.open-in-view` e os testes formais de contrato ainda não foram aplicados; a migração completa exige concluir o mapeamento transacional dos demais endpoints e estabilizar a suíte backend.

### FE-07 — Proteção contra falhas de renderização

- **Problema Corrigido:** Uma exceção de renderização podia derrubar a aplicação inteira em tela branca.
- **Arquivos Afetados:** `jaf-frontend/react/src/Components/ErrorBoundary/ErrorBoundary.tsx`; `jaf-frontend/react/src/Components/Layout/Layout.tsx`; `jaf-frontend/react/src/Utils/logger.ts`.
- **Solução Aplicada:** Adicionado Error Boundary com tela amigável, botão de recarregar e logging sem dados de requisição. O conteúdo das rotas autenticadas passou a ser protegido pelo boundary.

## Fase 5 — Frontend

### FE-01 — Exportação de gastos

- **Problema Corrigido:** A tela de detalhamento não permitia exportar a lista de gastos para a contabilidade; o relatório anterior do Dashboard imprimia apenas agregados em uma janela com HTML/script inline e não respeitava os filtros da listagem.
- **Arquivos Afetados:** `jaf-frontend/react/src/Service/Gastos/exportacaoService.ts`; `jaf-frontend/react/src/Components/ExportarGastos/ExportarGastosModal.tsx`; `jaf-frontend/react/src/Components/ExportarGastos/ExportarGastosModal.module.css`; `jaf-frontend/react/src/Pages/Obras/DetalhamentoObras.tsx`; `jaf-frontend/react/src/Utils/erro.ts`.
- **Solução Aplicada:** Adicionado modal React para exportação em CSV/XLSX, com período inicial baseado no filtro atual, filtros de funcionário, método e status, estado de carregamento e prevenção de duplo clique. O download usa `blob`, `Content-Disposition`, `URL.createObjectURL` e revogação da URL. Erros retornados como Blob são convertidos para o formato tratado pelo helper padronizado antes da exibição. O botão só aparece para usuários com `GERAR_RELATORIO`.

### Validação da Fase 5 — primeiro incremento

- `npm run build`: **passou**.
- `npm run lint`: **passou**.
- `npx vitest run`: **19 arquivos e 126 testes passaram**.
- `npx vitest run src/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.test.tsx`: **5 testes passaram**.
- `git diff --check`: **passou**.
- Testes direcionados: `ApplicationTests`, `FinanceiroApiTest` e `ReembolsoPersistenciaTest` passaram após o ajuste das regras de autorização. `AcessoFinanceiroTest` e `GastoServiceTest` ainda contêm expectativas legadas incompatíveis com a validação de vigência e com a serialização baseada nas authorities do principal; precisam ser atualizados antes de marcar a fase como concluída.
- A Fase 3 ainda não deve ser considerada 100% concluída enquanto BE-18, BE-19, BE-23, BE-24 e BE-25 não forem implementados e validados.

### Avanços adicionais da Fase 3

- **BE-14:** adicionada migration de estorno, campos de cancelamento em pagamentos, endpoint `POST /gastos/{id}/reembolsos/{pagamentoId}/estorno` restrito a ADMIN e resposta com `estornos`. Pagamentos cancelados não entram no total.
- **BE-15:** adicionada migration de permissões personalizadas, flag de herança no funcionário, campo `personalizada` no DTO e invalidação do `tokenVersion` ao alterar permissões.
- **BE-21:** criado bean `Clock` com `America/Sao_Paulo` e aplicado ao registro de reembolsos.
- **BE-16:** criado histórico de recebimentos por obra, com lançamentos, ajustes e estornos lógicos; o total financeiro da obra é recalculado a partir do ledger.
- **BE-17:** ampliadas as validações de nomes, títulos, orçamento, categorias, métodos e alteração de cargo, com DTO próprio para o cargo.
- **BE-22/BE-24 (parcial):** adicionados `X-Request-Id`, expiração automática de jobs OCR presos, retenção de texto bruto por 30 dias, timeouts HTTP, mensagens de erro generalizadas e exposição do endpoint Prometheus.
- **BE-20:** removida a configuração `allow-bean-definition-overriding` e convertida a configuração principal de segurança para injeção por construtor.
- **BE-26:** `PUT /funcionarios/{id}` agora utiliza `FuncionarioUpdateRequest`, sem o campo `senha`; a documentação OpenAPI passou a declarar autenticação por cookie JWT.

Ficam fora deste escopo, conforme orientação do solicitante, BE-23, BE-24 e demais pendências não relacionadas.

### Escopo priorizado pelo solicitante

- **BE-18:** enums de função na obra, método de pagamento e status; alias legado `REEMBOLSO` preservado e filtragem baseada em `reembolsavel`.
- **BE-19:** removidos o endpoint `/auth/login`, o serviço de autenticação duplicado, o token do DTO de login e o matcher redundante de OCR; `POST /funcionarios/login` permanece como fluxo único.
- **BE-25:** adicionada cobertura explícita para aplicação e validação das migrations em PostgreSQL via Testcontainers. Exclusão de funcionário permanece lógica por desativação, preservando histórico.
- **PostgreSQL:** criado `MigracoesPostgresTest`; a execução foi tentada, mas o ambiente atual não possui Docker, portanto o teste foi marcado como ignorado automaticamente quando Docker não estiver disponível. A validação real deve ser executada no CI com Docker.

### Validação do escopo priorizado

- `mvnw.cmd -Dtest=FuncionarioAutorizacaoTest,ReembolsoServiceTest,FinanceiroMigrationTest,MigracoesPostgresTest test`: **19 testes passaram; 1 teste PostgreSQL foi ignorado por ausência de Docker**.
- `mvnw.cmd -DskipTests package`: **passou**.
- `git diff --check`: **passou**.
- Exclusão de funcionário: **lógica**, com `ativo=false`, `desativadoEm` preenchido, incremento de `tokenVersion` e preservação dos registros.
- Compatibilidade de reembolso: `"REEMBOLSO"` é aceito sem distinção de maiúsculas/minúsculas e continua exposto no contrato legado; consultas de status usam `reembolsavel`.

O enum `Cargo` mantém valores legados depreciados para não quebrar dados e testes enquanto a validação PostgreSQL real não estiver disponível; as funções de obra já usam `FuncaoObra` separadamente.

### Início da Fase 4 — contratos e integração

- **INT-02 (primeiro incremento):** criado `tratarErroApi` em `jaf-frontend/react/src/Utils/erro.ts`, preservando `erroApi` como wrapper compatível.
- O novo helper diferencia rede, timeout, autorização, recurso ausente, limite de upload, rate limit, validação com `fields` e erros 5xx, sem exibir mensagens técnicas do Axios.

### Validação adicional desta rodada

- `mvnw.cmd -DskipTests package`: **passou**.
- `mvnw.cmd -Dtest=ApplicationTests,ReembolsoPersistenciaTest test`: **passou**.
- `mvnw.cmd -Dtest=ReembolsoServiceTest test`: **passou**.
- `mvnw.cmd -Dtest=OcrServiceTest test`: possui uma expectativa legada incompatível com a nova política de não exposição da mensagem bruta do worker; a implementação mantém a mensagem genérica por segurança.
- `git diff --check`: **passou**.
- `npm run build`: **passou**.
- `npm run lint`: **passou**.

### INT-02 — Aplicação do tratamento padronizado de erros

- **Problema Corrigido:** Alguns fluxos do frontend exibiam mensagens técnicas, tratavam respostas Axios de forma inconsistente ou descartavam erros de validação retornados pelo backend.
- **Arquivos Afetados:** `jaf-frontend/react/src/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `jaf-frontend/react/src/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`; `jaf-frontend/react/src/Pages/Obras/NovaObra/NovaObra.tsx`; `jaf-frontend/react/src/Utils/erro.ts`.
- **Solução Aplicada:** Os fluxos de carregamento e gravação passaram a usar `tratarErroApi`, preservando mensagens amigáveis por status HTTP, encaminhando erros `fields` para os campos do formulário e mantendo o redirecionamento específico para sessão expirada. O wrapper legado `erroApi` continua disponível para os consumidores ainda não migrados.

### Validação do incremento INT-02

- `npm run build`: **passou**.
- `npm run lint`: **passou**.
- `git diff --check`: **passou**.
