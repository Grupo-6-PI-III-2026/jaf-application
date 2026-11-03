# CORRECOES.md

> **Documento de execução para o GitHub Copilot.** Consolida `docs/ANALISE_CRITICA_BACKEND.md` (66 achados), `docs/ANALISE_CRITICA_FRONTEND.md` (56 achados + 11 inconsistências doc×código) e os problemas **novos** encontrados na análise de integração (marcados com **[NOVO]**). O código foi relido para confirmar cada item; onde a documentação anterior estava imprecisa, isto está indicado.
>
> **Como usar:** execute os itens **na ordem da seção "Ordem de Execução"** (um item por branch/commit). Cada item é autocontido, mas respeite as dependências declaradas em **Restrições**.

## Convenções de caminho

| Alias | Caminho real (a partir da raiz do repositório) |
|---|---|
| `{BE}` | `jaf-backend/src/main/java/com/jaf/application` |
| `{BE-RES}` | `jaf-backend/src/main/resources` |
| `{BE-TEST}` | `jaf-backend/src/test/java/com/jaf/application` |
| `{FE}` | `jaf-frontend/react/src` |
| `{FE-ROOT}` | `jaf-frontend/react` |
| `{TF}` | `terraform` |
| `{OCR}` | `POC-API` |

## Regras gerais (valem para TODOS os itens)

1. **Nunca edite uma migration Flyway já aplicada** (`V1`…`V13`): o checksum quebra a subida. Toda mudança de schema é uma **nova** migration (`V14`, `V15`…), idempotente quando possível e com verificação prévia de dados (falhar com mensagem clara em vez de "consertar" dado silenciosamente).
2. **Contrato do frontend é protegido.** Antes de mudar qualquer JSON, consulte a tabela "Contrato mínimo consumido pelo frontend". Mudanças de contrato seguem **expandir → migrar → contrair**: o backend publica o novo campo mantendo o antigo; o frontend migra; só então o campo antigo é removido (em item próprio e posterior).
3. **Um item por commit/PR**, com testes. Comandos de verificação:
   - Backend: `cd jaf-backend; ./mvnw test` (Windows: `mvnw.cmd test`).
   - Frontend: `cd jaf-frontend/react; npm run lint; npx tsc -b; npx vitest run`.
4. **Não altere código fora do escopo do item.** Não corrija "de passagem" outros achados; eles têm item próprio.
5. **Segredos nunca entram no Git** (senhas, `JWT_SECRET`, tokens do GHCR). Valores só por variável de ambiente.
6. **Compatibilidade de implantação:** o backend roda em 2 instâncias (ASG) atrás de ALB interno; o frontend (nginx) fala com ele em HTTP interno. Mudanças de sessão/cookie precisam funcionar nesse caminho: `Navegador → ALB público (80/443) → nginx (/api → rewrite) → ALB interno (8080) → Spring Boot`.
7. Textos de erro e UI em **português do Brasil, com acentuação correta**.

---

## Visão Geral da Aplicação

### Estado atual

O JAF é um monorepo para gestão de obras, gastos, reembolsos, presença e alocação de funcionários de uma construtora:

- **Backend** Spring Boot 3.4.5 / Java 21 / PostgreSQL (Flyway + Hibernate) / RabbitMQ (OCR assíncrono) — `jaf-backend/`.
- **Frontend** React 19 + Vite + TypeScript + axios + react-router 7 — `jaf-frontend/react/`, servido por nginx com proxy `/api` e `/uploads`.
- **OCR** worker Python (RabbitMQ) — `POC-API/`.
- **Infra** Terraform (VPC, ALB público + ALB interno, ASG de frontend/backend/OCR, RDS, RabbitMQ) e `docker-compose.yml` para execução local.
- **Autenticação:** `POST /funcionarios/login` grava um JWT no cookie `jwt` (HttpOnly). O frontend usa `withCredentials` e decide "logado" por `localStorage.userEmail`; o cargo/permissões vêm de `GET /funcionarios/me` e `/me/permissoes`.

A aplicação funciona de ponta a ponta em ambiente de desenvolvimento, mas **não está pronta para produção**. Os três requisitos que o `contexto.txt` classifica como essenciais — **Notas Fiscais**, **exportação de relatório para a contabilidade** e **filtro de data nos gastos** — **não existem** (R1/4.1).

### Principais riscos identificados

1. **Vazamento de hash de senha** em `GET /gastos`, `/alocacoes*`, `/obras/{id}/gastos|alocacoes`, `/relatorios*` (entidade `Funcionario` serializada com `senha`).
2. **ADMIN com senha conhecida** criado por padrão (seed ligado por padrão + migrations V2/V3/V6), com a senha exibida no Swagger público.
3. **Sessão insegura e inconsistente:** cookie sem `Secure`/`SameSite`, CSRF desligado, tráfego HTTP, CORS com `*.elb.amazonaws.com` + credenciais; **[NOVO]** sessão expirada responde **403** (o frontend só trata 401 e fica preso); **[NOVO]** `JWT_EXPIRATION` tem unidade diferente no Terraform (ms) e no código (s) — o token vale ~1000 dias na AWS.
4. **Schema não confiável:** `ddl-auto=update` convivendo com Flyway; tabela `presenca` sem migration; sem `UNIQUE` e sem índices; dinheiro em `VARCHAR`.
5. **Contrato de API frágil:** entidades JPA como resposta, três formatos de erro, enums como strings livres (inclusive `"REEMBOLSO"` como "método de pagamento"), cargo do sistema misturado com função na obra.
6. **Autorização por obra incompleta** (IDOR em alocações/relatórios, auto-alocação, `dtFim` ignorada) e sem trilha de auditoria num sistema financeiro.
7. **Requisitos essenciais ausentes** (NF, exportação, filtro de data) e fluxo de reembolso sem estorno.
8. **Operação:** uploads em disco efêmero atrás de ASG, fuso UTC no AWS, OCR sem timeout/DLQ de resultado, token do GHCR impresso no log do cloud-init (`frontend.sh`), imagens `:latest` rodando como root.

### Impacto geral da dívida técnica

Somando os dois relatórios e a análise de integração, são **122 achados originais** (66 backend + 56 frontend), 11 inconsistências de documentação e **11 itens novos de integração** (marcados **[NOVO]** no título, além de agravantes cruzados incorporados aos itens existentes), consolidados em **95 itens de correção** neste documento (totais exatos no "Resumo Final"). A dívida está concentrada em: (a) segurança de sessão e exposição de dados; (b) ausência de constraints/auditoria no banco; (c) contratos implícitos entre front e back que só funcionam "por coincidência" (strings mágicas, formatos de erro, fuso, default de `etapa`); (d) funcionalidades essenciais não entregues. Sem tratar (a) e (b) antes das demais, qualquer nova funcionalidade herda os furos.

### Avaliação por camada

| Camada | Situação | Risco | Pronta para produção? | Motivos principais |
|---|---|---|---|---|
| **Banco de Dados** | Schema parcialmente gerado pelo Hibernate | **Alto** | Não | `ddl-auto=update`, `presenca` sem migration, sem `UNIQUE`/índices/`CHECK`, `orcamento` VARCHAR, exclusão física sem política, sem auditoria/versão, dados de demo nas migrations |
| **Backend** | Código organizado, regras de segurança/negócio incompletas | **Alto** | Não | Hash de senha exposto, seed com ADMIN conhecido, sem rate limit, IDOR, N+1/OSIV, sem NF/exportação/filtro de data, fuso UTC |
| **Integração** | Contratos implícitos e divergentes | **Alto** | Não | 403 vs 401, entidades JPA no JSON, 3 formatos de erro, validações divergentes, enums por string, `etapa` default, unidade do JWT, `cargoNaObra` legado, permissões "congeladas" |
| **Frontend** | Funcional, com bons cuidados pontuais (reembolso, `valorDecimal`) | **Médio-Alto** | Não | Sessão por `localStorage`, tratamento de erro só de 401, sem exportação, sem cancelamento de requests, monólito de 1149 linhas, sem `ErrorBoundary` |
| **Infra (transversal)** | HTTP-only, defaults inseguros | **Alto** | Não | ALB sem HTTPS por padrão, token do GHCR no log, `:latest`, containers como root, uploads locais |

---

## Ordem de Execução das Correções

A ordem segue a cadeia de dependência **Banco → Segurança → Backend → Integração → Frontend**. Cada fase só começa quando a anterior estiver verde (testes passando).

### Justificativa das fases

| Fase | Por que nesta posição |
|---|---|
| **1. Banco de Dados e Modelagem (DB-01…DB-09)** | Tudo que vem depois assume um schema confiável: constraints (para devolver 409 em vez de 500), colunas novas (`ativo`, `token_version`, `versao`, auditoria), tabelas novas (`nota_fiscal`, `recebimento_obra`) e `ddl-auto=validate`. As migrations são **aditivas** (expandir), então o código atual continua funcionando. Também habilita Testcontainers/Postgres para testar as fases seguintes. |
| **2. Segurança e Autenticação (SEG-01…SEG-21)** | São bloqueadores de produção e mexem no fluxo de login que **todo** item de frontend usa. A resposta **401 JSON** e o formato de cookie precisam estar definidos antes de qualquer ajuste de sessão no frontend. O vazamento de hash tem um *stop-gap* imediato (`@JsonIgnore`) antes da troca definitiva por DTOs (INT-01). |
| **3. Backend (BE-01…BE-26)** | Regras de negócio, desempenho e funcionalidades essenciais, **mantendo o contrato JSON atual** (apenas adicionando campos/endpoints). Depende de DB (colunas/tabelas) e SEG (autorização centralizada, token). |
| **4. Integração (INT-01…INT-19)** | Itens que exigem mudança coordenada nos dois lados (DTOs de resposta, envelope de erro, enums, validações, permissões, fuso). Só começam depois que o backend publicou os novos campos/endpoints (fase 3), para o frontend poder migrar sem quebra. |
| **5. Frontend (FE-01…FE-19 e DOC-01)** | Consome os contratos já estabilizados, remove workarounds que mascaravam problemas do backend (ex.: logout ao trocar e-mail, tabela fixa de permissões) e entrega a UI dos requisitos essenciais (exportação). |

### Dependências explícitas (não inverter)

| Item | Depende de | Motivo |
|---|---|---|
| DB-01 | — | Primeiro: sem `validate` e sem `presenca` versionada, o resto não é confiável |
| DB-02 | DB-01 | `UNIQUE` em `presenca` exige a tabela versionada |
| SEG-09, SEG-17, BE-05 | DB-05 | Precisam de `ativo` e `token_version` |
| BE-06, BE-13, BE-24 | DB-06 | Precisam de `versao` e colunas de auditoria |
| BE-08 | DB-07 | Tabela `nota_fiscal` |
| BE-16 | DB-08 | Tabela `recebimento_obra` |
| BE-18, INT-04 | DB-09 | Coluna `reembolsavel` e normalização de métodos |
| INT-01 | SEG-01 | `@JsonIgnore` é o *stop-gap*; DTOs são a correção definitiva |
| SEG-06 (cookie `Secure`) | SEG-04, SEG-05 | `Secure=true` sem HTTPS faz o login "funcionar" e o cookie ser descartado |
| SEG-10 (CSRF) | SEG-06 | CSRF por cookie duplo depende do cookie/same-origin estabilizados |
| SEG-13 | (inclui auto-alocação do criador) | Sem isso, SEG-13 quebraria o fluxo "criar obra → alocar responsável" do `RESPONSAVEL_ADMINISTRATIVO` |
| BE-10 | BE-09 | A exportação reutiliza o filtro de data |
| FE-01 | BE-09, BE-10 | Botão "Exportar" consome o endpoint |
| FE-02 | DB-07, BE-08, INT-12 | UI de notas fiscais consome o endpoint e usa chave de idempotência |
| FE-03 | SEG-03, SEG-09 | Remover o workaround de logout ao trocar e-mail só depois de `sub = id` |
| INT-12 | BE-06 | `versao` e `Idempotency-Key` |
| INT-17 | BE-14, INT-04 | Status do reembolso na listagem e contração de `reembolso_concluido` |
| INT-18 (CSP bloqueante) | FE-01, FE-12 | Remover script inline do relatório e imagens externas antes de bloquear |
| INT-02 | BE-02 | Envelope único de erro |
| INT-07 | BE-11 | `etapa` default e `TODAS` |
| INT-09 | SEG-13, BE-12 | Criação de obra com responsável atômico |

### Sequência numerada (1 → 95)

A numeração definitiva (coluna **Ordem** do "Resumo Final") é: **DB-01…DB-09 (1–9) → SEG-01…SEG-21 (10–30) → BE-01…BE-26 (31–56) → INT-01…INT-19 (57–75) → FE-01…FE-19 (76–94) → DOC-01 (95)**.

---

## Contrato mínimo consumido pelo frontend (NÃO QUEBRAR)

Levantado em `{FE}/Service/**` e nas páginas. Qualquer item que mude esses formatos deve seguir "expandir → migrar → contrair".

| Recurso | Campos que o frontend lê/envia |
|---|---|
| `POST /funcionarios/login` | envia `{email, senha}`; lê `{id, email, nome, cargo}` (o `token` é ignorado) |
| `POST /funcionarios/logout` | sem corpo |
| `GET /funcionarios/me` | `{id, email, nome, cargo, fotoUrl}` |
| `GET /funcionarios/me/permissoes` | `{funcionarioId, cargo, permissoes[]}` (validado contra `/me`) |
| `GET /funcionarios` | lista com `id, nome, email, cargo` (+ campos de `FuncionarioPermissoes`) |
| `PUT /funcionarios/me` | envia `{nome, email, fotoUrl}`; `fotoUrl` pode ser `null`, `/uploads/fotos/<uuid>.<ext>` **ou um avatar estático** `/assets/Geral/avatar-*.svg` |
| `PATCH /funcionarios/me/senha` | envia `{senhaAtual, novaSenha, confirmacaoSenha}` |
| `POST /funcionarios/me/foto` | multipart, parte **`file`**; lê `{url}` |
| Obra (`/obras`, `/obras/{id}`) | `id, titulo, status, dtInicio, dtTerminoPrevisto, orcamento?` (**string**, só ADMIN), `totalRecebido?` (number, só ADMIN) |
| `POST/PUT /obras` | envia `titulo, orcamento (string), status, dtInicio, dtTerminoPrevisto` |
| Gasto (`/gastos`) | `id, descricao, categoria, metodoPagamento, etapa, valor (number), dtGasto, reembolsoConcluido, funcionario{id,nome,email,cargoGlobal}, obra{id,titulo}` |
| `POST/PUT /gastos` | envia `descricao, categoria, metodoPagamento, etapa, valor (number), dtGasto, funcionarioId, obraId` |
| Alocação (`/alocacoes`, `/alocacoes/obra/{id}`) | `id, cargo, dtInicio, dtFim, funcionario{id,nome,email,fotoUrl,cargoGlobal}, obra{id,titulo,status,dtInicio,dtTerminoPrevisto,orcamento?,totalRecebido?}` |
| `POST /alocacoes` | envia `{funcionarioId, obraId, cargoNaObra}` (**resposta usa `cargo`, requisição usa `cargoNaObra`**) |
| Presença | lista `{id?, funcionarioId, funcionarioNome, funcionarioCargo, data, presente, desabilitado}`; envia `{funcionarioId, obraId, data, presente, horarioEntrada?, horarioSaida?}`; `PATCH /presencas/{id}/alternar` → 204 |
| `GET /dashboard/stats?obraId&etapa&from&to` | `gastosEtapa, progressoEtapa, reembolsosPendentes, saldoRestante, progressoSaldo, reembolsosPizza[{name,value}], gastosImprevistos[{mes,valor}], gastosPorCategoria[{categoria,valor}]`; envia `etapa="TODAS"` por padrão |
| Financeiro | `GET/PUT /obras/{id}/financeiro` → `{totalRecebido,totalGasto,saldo}` |
| Reembolso | `GET/POST /gastos/{id}/reembolsos` → `{gastoId,valorTotal,totalPago,saldoPendente,status,pagamentos[{id,valor,dataPagamento,observacao}]}`; `GET /gastos/reembolsos/resumo?obraId` → `[{funcionarioId,funcionarioNome,valorTotal,totalPago,saldoPendente}]` |
| OCR | `POST /ocr/nota-fiscal` (multipart, parte **`arquivo`**) → `{jobId,status,sucesso,mensagem,textoBruto,valor,dtGasto,metodoPagamento,materialInsumo,descricaoAdicional,etapa}`; polling `GET /ocr/nota-fiscal/{jobId}` a cada 2 s por até 2 min |
| Permissões | `GET/PUT /permissoes/cargos(/{cargo})` → `{cargo,permissoes[]}`; `GET/PUT /funcionarios/{id}/permissoes`; `PATCH /funcionarios/{id}/cargo` body `{cargo}` |
| Erros | lê `message` / `mensagem` / `detail`; `Profile.tsx` lê `fields` (mapa campo→mensagem). **Status 401** → limpa sessão e redireciona a `/` |
| Valores literais | cargos `ADMIN`, `RESPONSAVEL_ADMINISTRATIVO`, `ENGENHEIRO`; métodos `PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO, REEMBOLSO`; categorias `MATERIAL, MAO_DE_OBRA, TRANSPORTE, EQUIPAMENTO, IMPREVISTO`; etapas `ETAPA 1, ETAPA 2, TODAS`; status de obra `PLANEJADA, EM_ANDAMENTO, CONCLUIDA, CANCELADA`; status de reembolso `PENDENTE, PARCIAL, PAGO`; função na obra enviada: `GESTOR_OBRA` |

## Decisões em aberto (com padrão adotado se ninguém responder)

| # | Pergunta | Padrão que o Copilot deve assumir |
|---|---|---|
| D-1 | O deploy final terá TLS (ACM + domínio)? | Sim. `enable_https=true` em produção; cookie `Secure=true` por propriedade. Se não houver certificado (ex.: Learner Lab), manter `app.security.cookie.secure=false` **apenas** nesse ambiente e registrar o risco no PR |
| D-2 | Formato e permissão da exportação para a contabilidade | CSV (UTF-8 com BOM, separador `;`) e XLSX; PDF fica para depois. Quem tem `GERAR_RELATORIO` (ADMIN e RESPONSAVEL_ADMINISTRATIVO), respeitando escopo por obra |
| D-3 | Regex de nome (só letras) e de título de obra (sem pontuação) é intencional? | Não. Relaxar: nome aceita letras, espaço, `'`, `-`, `.`; título aceita letras, números, espaço e `- . , ( ) / ' &` |
| D-4 | Mudança de permissão vale na hora ou só no próximo login? | Vale **imediatamente no servidor** (já é assim: permissões são lidas do banco a cada request). A UI do usuário afetado atualiza ao recarregar/`refreshUser` |
| D-5 | `banco.json`/`endpoint.js` podem ser removidos? Alguma senha é real? | Remover; tratar **qualquer** senha ali como comprometida e rotacionar onde existir |
| D-6 | Dashboard é só para ADMIN? | Sim por ora (o service já exige ADMIN). Alinhar o `@PreAuthorize` do controller a isso (BE-11) |
| D-7 | Cargo do sistema × função na obra | Separar: `Cargo` (ADMIN, RESPONSAVEL_ADMINISTRATIVO, ENGENHEIRO) e `FuncaoObra` (os valores legados usados em `cargoNaObra`) — INT-05 |

---

## Lista Completa de Correções

> Legenda de origem: **BE x.y** = `ANALISE_CRITICA_BACKEND.md`; **FE Xn** = `ANALISE_CRITICA_FRONTEND.md`; **FE Dn** = inconsistência doc×código; **[NOVO]** = encontrado na análise de integração.

## FASE 1 — Banco de Dados e Modelagem

### [DB-01] [CRÍTICO] Schema gerado pelo Hibernate (`ddl-auto=update`) e tabela `presenca` sem migration

**Camada:** Banco de Dados (configuração do Backend)  
**Arquivo(s):** `{BE-RES}/application.properties`; novo `{BE-RES}/db/migration/V14__create_presenca.sql`; `{BE}/model/Presenca.java` (referência); `jaf-backend/pom.xml`; `{BE-TEST}/repository/FinanceiroMigrationTest.java`; `{BE-TEST}/config/DatabaseSeederTest.java`  
**Descrição:** *(BE 3.1)* O schema real é a soma do Flyway com o que o Hibernate decide criar/alterar na subida (`spring.jpa.hibernate.ddl-auto=update`). A entidade `Presenca` **não tem migration** em `V1`…`V13`: em banco novo a tabela só nasce porque o Hibernate a cria, sem índices nem constraints versionados. `spring.flyway.baseline-on-migrate=true` aceita bancos que já têm tabelas e apenas marca a baseline. Os testes que tocam o banco usam H2 (`DatabaseSeederTest` cria tabela à mão), que não reproduz PostgreSQL.  
**Impacto:** schema divergente entre dev/CI/RDS; qualquer mudança de entidade vira `ALTER TABLE` automático em produção; impossível auditar/reverter; testes verdes que não garantem produção.  
**Instrução para o Copilot:**
1. Criar `V14__create_presenca.sql` com `CREATE TABLE IF NOT EXISTS presenca` reproduzindo o que o Hibernate gera hoje (nomes `snake_case` da `CamelCaseToUnderscoresNamingStrategy`: `funcionario_id`, `obra_id`, `data`, `presente`, `horario_entrada`, `horario_saida`; `id BIGSERIAL`). **Sem** `NOT NULL`/`UNIQUE` extras aqui (o RDS existente pode ter linhas); isso é DB-02/DB-04.
2. Em `application.properties` trocar `spring.jpa.hibernate.ddl-auto=update` por `validate` e `spring.flyway.baseline-on-migrate=true` por `false`.
3. Adicionar `org.testcontainers:postgresql` e `org.testcontainers:junit-jupiter` (escopo `test`) e criar um teste `@SpringBootTest` com PostgreSQL real que aplique todas as migrations e suba com `validate`. Corrigir divergências entre entidade e schema **na entidade ou em nova migration**, nunca voltando para `update`.
4. Runbook (descrever no PR): antes de subir em um ambiente já existente, confirmar que `flyway_schema_history` existe. Se o banco foi criado só pelo Hibernate, **primeiro reconciliar o schema**: gerar um banco novo só com as migrations (`V1`…`V13`) e comparar `pg_dump --schema-only` com o do ambiente (nomes de constraints/FKs e colunas podem divergir porque o Hibernate os nomeia sozinho); somente depois executar uma vez `flyway baseline -baselineVersion=13` manualmente e subir com `baseline-on-migrate=false`. As migrations seguintes (DB-02…) **não podem supor nomes de constraints** definidos nas migrations antigas (usar `pg_constraint`, ver DB-05).

**Restrições:** não editar `V1`…`V13`; não alterar `Presenca.java`; não adicionar `UNIQUE`/`NOT NULL` em `presenca` neste item; não ligar `validate` em produção sem validar em staging. DB-02 depende deste item.  
**Exemplo de correção:**
```sql
-- {BE-RES}/db/migration/V14__create_presenca.sql
CREATE TABLE IF NOT EXISTS presenca (
    id              BIGSERIAL PRIMARY KEY,
    funcionario_id  BIGINT REFERENCES funcionario(id),
    obra_id         BIGINT REFERENCES obra(id),
    data            DATE,
    presente        BOOLEAN,
    horario_entrada TIME,
    horario_saida   TIME
);
```
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.flyway.baseline-on-migrate=false
```

---

### [DB-02] [CRÍTICO] Unicidade garantida só por código (*check-then-insert*), sem `UNIQUE` no banco

**Camada:** Banco de Dados + Backend  
**Arquivo(s):** novo `{BE-RES}/db/migration/V15__constraints_unicidade.sql`; `{BE}/service/FuncionarioService.java`; `{BE}/repository/FuncionarioRepository.java`; `{BE}/service/AlocacaoObraService.java`; `{BE}/service/PresencaService.java`; `{BE}/service/ObraService.java`; `{BE}/service/AutenticacaoService.java`  
**Descrição:** *(BE 3.2, BE 6.5 parte banco)* Não há `UNIQUE` em `funcionario.email`, `obra.titulo`, `alocacao_obra(funcionario_id, obra_id)` nem `presenca(funcionario_id, obra_id, data)`. As verificações `existsBy…` seguidas de `save` perdem a corrida (duplo clique no celular, retry do ALB). `FuncionarioService.atualizar` não verifica e-mail duplicado e `criar` não normaliza (`Joao@x.com` ≠ `joao@x.com` no banco, mas `findByEmailIgnoreCase` os trata como iguais): com dois registros, `findByEmailIgnoreCase` (retorno `Optional`) lança `IncorrectResultSizeDataAccessException` e **nenhum dos dois consegue logar**. `existsByNome` impede homônimos legítimos. **[NOVO]** Esse `Conflict("Usuário já existe.")` chega ao frontend como 409 e `NovoFuncionario.tsx` o exibe como "Já existe um funcionário cadastrado com este **e-mail**", mensagem errada para a colisão de nome.  
**Impacto:** usuários bloqueados no login, presenças/alocações duplicadas distorcendo relatórios, 500 em endpoints que antes funcionavam, mensagem enganosa ao administrador.  
**Instrução para o Copilot:**
1. Criar `V15` que (a) normaliza e-mails (`lower(trim(email))`), (b) **falha com mensagem listando as linhas** se existirem duplicidades (para resolução manual antes de aplicar), (c) cria os índices/constraints abaixo. Para alocação use uma **constraint de exclusão por vigência** (`EXCLUDE USING gist` com `btree_gist`): impede períodos sobrepostos do mesmo funcionário na mesma obra (inclusive "uma com `dt_fim` futura e outra sem `dt_fim`"), mas permite histórico de alocações encerradas e re-alocação posterior (BE-12). **Fallback** se a extensão `btree_gist` não puder ser criada no RDS: índice único parcial `WHERE dt_fim IS NULL` **mais** verificação de sobreposição no `AlocacaoObraService` (BE-12).
2. No código: normalizar e-mail com `trim().toLowerCase()` em `FuncionarioService.criar` e `atualizar`, e no login. `atualizar` deve rejeitar e-mail já usado por **outro** id (409).
3. Trocar `FuncionarioRepository.findByEmailIgnoreCase` por consulta com `lower(f.email) = lower(:email)` (usa o índice funcional; o derivado gera `upper(...)`, que não usa). Manter o nome do método para não alterar chamadores.
4. Remover `existsByNome` e o `Conflict("Usuário já existe.")` em `FuncionarioService.criar` (homônimos são legítimos).
5. Mapear `DataIntegrityViolationException` desses índices para 409 — isso é concluído em BE-02; aqui apenas garantir que `save` propaga a exceção.

**Restrições:** depende de DB-01 (tabela `presenca` versionada). Não editar `V1`…`V14`. Não criar `UNIQUE (funcionario_id, obra_id)` **total** em `alocacao_obra` (impediria re-alocação histórica). Não alterar o contrato de resposta de nenhum endpoint. Para encerrar e depois re-alocar, o serviço deve **reabrir** a linha (limpar `dt_fim`) ou criar nova linha com `dt_inicio` posterior ao `dt_fim` anterior (senão a constraint rejeita).  
**Exemplo de correção:**
```sql
-- V15__constraints_unicidade.sql
UPDATE funcionario SET email = lower(trim(email)) WHERE email <> lower(trim(email));

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM funcionario GROUP BY lower(email) HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'E-mails duplicados em funcionario; resolva manualmente antes da V15';
  END IF;
  IF EXISTS (SELECT 1 FROM presenca GROUP BY funcionario_id, obra_id, data HAVING count(*) > 1) THEN
    RAISE EXCEPTION 'Presencas duplicadas; resolva manualmente antes da V15';
  END IF;
  IF EXISTS (SELECT 1 FROM alocacao_obra a JOIN alocacao_obra b
               ON a.funcionario_id = b.funcionario_id AND a.obra_id = b.obra_id AND a.id < b.id
              AND daterange(COALESCE(a.dt_inicio, '-infinity'::date), COALESCE(a.dt_fim, 'infinity'::date), '[]')
               && daterange(COALESCE(b.dt_inicio, '-infinity'::date), COALESCE(b.dt_fim, 'infinity'::date), '[]')) THEN
    RAISE EXCEPTION 'Alocacoes com vigencia sobreposta; resolva manualmente antes da V15';
  END IF;
END $$;

CREATE UNIQUE INDEX uk_funcionario_email_lower ON funcionario (lower(email));
CREATE EXTENSION IF NOT EXISTS btree_gist;
ALTER TABLE alocacao_obra ADD CONSTRAINT ex_alocacao_vigencia
  EXCLUDE USING gist (funcionario_id WITH =, obra_id WITH =,
    daterange(COALESCE(dt_inicio, '-infinity'::date), COALESCE(dt_fim, 'infinity'::date), '[]') WITH &&);
-- fallback sem btree_gist: CREATE UNIQUE INDEX uk_alocacao_ativa ON alocacao_obra (funcionario_id, obra_id) WHERE dt_fim IS NULL;
ALTER TABLE presenca ADD CONSTRAINT uk_presenca_func_obra_data UNIQUE (funcionario_id, obra_id, data);
CREATE UNIQUE INDEX uk_obra_titulo_lower ON obra (lower(titulo));
```
```java
// FuncionarioRepository
@Query("select f from Funcionario f where lower(f.email) = lower(:email)")
Optional<Funcionario> findByEmailIgnoreCase(@Param("email") String email);
```

---

### [DB-03] [MÉDIO] Falta de índices em colunas críticas

**Camada:** Banco de Dados  
**Arquivo(s):** novo `{BE-RES}/db/migration/V16__indices.sql`  
**Descrição:** *(BE 3.3)* O PostgreSQL não indexa FKs automaticamente. Faltam índices em `gasto(obra_id, dt_gasto)`, `gasto(funcionario_id)`, `alocacao_obra(obra_id)` e `presenca(obra_id, data)`. A consulta de e-mail (executada ≥ 3 vezes por request autenticado) só passa a usar índice com o `UNIQUE INDEX lower(email)` de DB-02 **e** a consulta `lower(...)`.  
**Impacto:** *seq scans* crescentes; o custo de autenticação cresce com o número de usuários.  
**Instrução para o Copilot:** criar `V16__indices.sql` com `CREATE INDEX IF NOT EXISTS` para os índices abaixo. Não recriar `idx_pagamento_reembolso_gasto` (já existe na V13) nem índices cobertos pelos `UNIQUE` da V15.  
**Restrições:** depende de DB-02 (índice de e-mail e consulta `lower`). Em tabelas grandes, usar `CREATE INDEX CONCURRENTLY` fora de transação só se o volume justificar (Flyway exige `-- flyway:executeInTransaction=false` nesse caso).  
**Exemplo de correção:**
```sql
CREATE INDEX IF NOT EXISTS idx_gasto_obra_data   ON gasto (obra_id, dt_gasto);
CREATE INDEX IF NOT EXISTS idx_gasto_funcionario ON gasto (funcionario_id);
CREATE INDEX IF NOT EXISTS idx_alocacao_obra     ON alocacao_obra (obra_id);
CREATE INDEX IF NOT EXISTS idx_presenca_obra_data ON presenca (obra_id, data);
```

---

### [DB-04] [MÉDIO] Dinheiro como `VARCHAR`, colunas sem `NOT NULL`/`CHECK` e migrations sem constraints

**Camada:** Banco de Dados + Backend  
**Arquivo(s):** novo `{BE-RES}/db/migration/V17__tipos_e_constraints.sql`; `{BE}/model/Obra.java`; `{BE}/config/ObraSerializer.java`; `{BE}/service/ObraService.java`; `{BE}/service/DashboardService.java`; `{BE}/service/ObraFinanceiroService.java`; `{BE}/dto/ObraDto.java`  
**Descrição:** *(BE 3.5 parte `orcamento`, BE 3.9, BE 4.7 parte `cargo_global`)* `obra.orcamento` é `VARCHAR(255)` com regex no DTO; `parseOrcamento` captura erro e devolve 0 (dado sujo vira orçamento 0 sem aviso). A V1 não tem `NOT NULL`, `CHECK (valor > 0)` em `gasto` nem `CHECK (orcamento >= 0)`; só a V13 adiciona `CHECK` (pagamentos e `total_recebido`). `funcionario.cargo_global` aceita `NULL` e o código converte `null` em `ENGENHEIRO` silenciosamente.  
**Impacto:** cálculos financeiros sensíveis a dado sujo; privilégio padrão concedido a usuário sem cargo.  
**Instrução para o Copilot:**
1. `V17`: verificar que todo `obra.orcamento` casa `^\d+(\.\d{1,2})?$` (falhar com exceção se não casar); converter para `NUMERIC(19,2) NOT NULL` com `CHECK (orcamento >= 0)`.
2. `V17`: `ALTER TABLE gasto ADD CONSTRAINT ck_gasto_valor CHECK (valor > 0) NOT VALID;` seguido de `VALIDATE CONSTRAINT` (após conferir dados). `NOT NULL` em `gasto.obra_id`, `gasto.funcionario_id`, `gasto.dt_gasto`, `alocacao_obra.funcionario_id`, `alocacao_obra.obra_id`, `funcionario.cargo_global` (corrigir antes eventuais `NULL` para o cargo de menor privilégio, `ENGENHEIRO`, **registrando no PR** quais linhas foram alteradas).
3. Entidade `Obra.orcamento` passa a `BigDecimal` (`@Column(precision = 19, scale = 2, nullable = false)`). **Manter o JSON atual:** `ObraSerializer` continua escrevendo `orcamento` como **string** (`toPlainString()`), e `ObraDto.orcamento` continua `String` com o mesmo `@Pattern` (a mudança de contrato é INT-19). Remover o `catch` silencioso de `parseOrcamento`.

**Restrições:** não mudar o formato JSON de `orcamento` (frontend lê `string`); não tocar em `reembolso_concluido` (BE-14/DB-09); não alterar `totalRecebido` (já `NUMERIC`).  
**Exemplo de correção:**
```sql
DO $$ BEGIN
  IF EXISTS (SELECT 1 FROM obra WHERE orcamento !~ '^\d+(\.\d{1,2})?$') THEN
    RAISE EXCEPTION 'obra.orcamento com valor invalido; corrija antes da V17';
  END IF;
END $$;
ALTER TABLE obra ALTER COLUMN orcamento TYPE NUMERIC(19,2) USING orcamento::numeric;
ALTER TABLE obra ALTER COLUMN orcamento SET NOT NULL;
ALTER TABLE obra ADD CONSTRAINT ck_obra_orcamento CHECK (orcamento >= 0);
ALTER TABLE gasto ADD CONSTRAINT ck_gasto_valor CHECK (valor > 0) NOT VALID;
ALTER TABLE gasto VALIDATE CONSTRAINT ck_gasto_valor;
```
```java
gen.writeStringField("orcamento", obra.getOrcamento().toPlainString());
```

---

### [DB-05] [MÉDIO] Exclusão física sem política, sem desativação de funcionário e sem versão de token

**Camada:** Banco de Dados  
**Arquivo(s):** novo `{BE-RES}/db/migration/V18__funcionario_ativo_token.sql`; `{BE}/model/Funcionario.java`; `{BE-RES}/db/migration/V10__create_funcionario_permissao.sql` (referência)  
**Descrição:** *(BE 3.7, BE 4.8; prepara BE 2.8 e 2.15)* Não existe `ativo` em `funcionario`; `FuncionarioDetalhesDto.isEnabled()` retorna `true` fixo. `funcionario` é referenciado por `gasto`, `alocacao_obra`, `relatorio`, `presenca` e `funcionario_permissao` sem política de exclusão: excluir quem tem histórico gera `DataIntegrityViolationException` → 500; o caso raro que passa não limpa `funcionario_permissao`. Não há campo para invalidar tokens quando senha/cargo mudam.  
**Impacto:** impossível desligar quem saiu da empresa (ex-funcionário continua logando e vendo dados financeiros); se alguém "resolver" com `ON DELETE CASCADE`, perde-se histórico contábil; tokens roubados não têm como ser revogados.  
**Instrução para o Copilot:** criar `V18` adicionando `funcionario.ativo BOOLEAN NOT NULL DEFAULT TRUE`, `funcionario.desativado_em TIMESTAMP NULL` e `funcionario.token_version INTEGER NOT NULL DEFAULT 0`; mudar a FK de `funcionario_permissao.funcionario_id` para `ON DELETE CASCADE` (é configuração, não histórico); manter as demais FKs sem cascade (comportamento `NO ACTION`/restrito). Mapear os três campos em `Funcionario.java` (getters/setters; `ativo` padrão `true`). **Não** implementar regras (login, filtro, endpoints) aqui: SEG-09, SEG-17 e BE-05 usam estas colunas.  
**Restrições:** não usar `ON DELETE CASCADE` em `gasto`, `alocacao_obra`, `presenca`, `relatorio`; não expor os novos campos em nenhuma resposta JSON ainda; não editar V10.  
**Exemplo de correção:**
```sql
ALTER TABLE funcionario ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE funcionario ADD COLUMN desativado_em TIMESTAMP;
ALTER TABLE funcionario ADD COLUMN token_version INTEGER NOT NULL DEFAULT 0;

DO $$
DECLARE r record;
BEGIN
  -- O nome da FK depende de como o schema nasceu (V10 ou Hibernate): localizar via catálogo.
  FOR r IN SELECT conname FROM pg_constraint
            WHERE conrelid = 'funcionario_permissao'::regclass AND contype = 'f'
              AND confrelid = 'funcionario'::regclass LOOP
    EXECUTE format('ALTER TABLE funcionario_permissao DROP CONSTRAINT %I', r.conname);
  END LOOP;
END $$;
ALTER TABLE funcionario_permissao
  ADD CONSTRAINT fk_funcionario_permissao_funcionario FOREIGN KEY (funcionario_id)
  REFERENCES funcionario(id) ON DELETE CASCADE;
```

---

### [DB-06] [MÉDIO] Sem controle de concorrência (`@Version`) nem colunas de auditoria

**Camada:** Banco de Dados  
**Arquivo(s):** novo `{BE-RES}/db/migration/V19__versao_e_auditoria.sql`; `{BE}/model/Gasto.java`, `Obra.java`, `Funcionario.java`, `AlocacaoObra.java`, `Presenca.java`  
**Descrição:** *(BE 3.8, BE 3.9 parte auditoria, BE 4.5 parte auditoria, BE 6.7 parte)* Nenhuma entidade tem `@Version`; `gasto`, `obra` e `alocacao_obra` não têm `created_at/updated_at/created_by/updated_by`. Dois gestores editando o mesmo gasto/obra fazem *last write wins*; não dá para responder "quem alterou este valor?".  
**Impacto:** perda de atualização em dados financeiros; auditoria contábil impossível.  
**Instrução para o Copilot:** criar `V19` com `versao BIGINT NOT NULL DEFAULT 0` em `gasto`, `obra`, `funcionario`; e `created_at TIMESTAMP NOT NULL DEFAULT now()`, `updated_at TIMESTAMP NOT NULL DEFAULT now()`, `created_by BIGINT NULL REFERENCES funcionario(id)`, `updated_by BIGINT NULL REFERENCES funcionario(id)` em `gasto`, `obra`, `alocacao_obra`, `pagamento_reembolso` (onde ainda não existirem). Mapear `@Version Long versao` e as colunas de auditoria nas entidades **sem** expor no JSON (`@JsonIgnore`) até BE-06/INT-01 decidirem o contrato. Não popular `created_by` retroativamente (deixar `NULL`).  
**Restrições:** não adicionar `@Version` em `Presenca` (a regra de presença é "último estado vale"); não alterar DTOs; o preenchimento automático (`@EnableJpaAuditing`) é feito em BE-13/BE-24.  
**Exemplo de correção:**
```sql
ALTER TABLE gasto ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE obra  ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE funcionario ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE gasto ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE gasto ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT now();
ALTER TABLE gasto ADD COLUMN created_by BIGINT REFERENCES funcionario(id);
ALTER TABLE gasto ADD COLUMN updated_by BIGINT REFERENCES funcionario(id);
-- repetir para obra, alocacao_obra e pagamento_reembolso (se faltarem)
```
```java
@Version
@JsonIgnore
private Long versao;
```

---

### [DB-07] [CRÍTICO] Tabela de Notas Fiscais inexistente (requisito essencial)

**Camada:** Banco de Dados  
**Arquivo(s):** novo `{BE-RES}/db/migration/V20__create_nota_fiscal.sql`  
**Descrição:** *(BE 4.1 parte modelagem)* `contexto.txt` lista "Inserção e gestão de Notas Fiscais" como funcionalidade **essencial**. Não existe entidade/tabela: o OCR (`/ocr/nota-fiscal`) lê o arquivo, devolve campos sugeridos e **descarta o arquivo**; `Gasto` não guarda número da NF, CNPJ nem anexo.  
**Impacto:** a principal dor do cliente (organizar NFs e enviar números à contabilidade) não é atendida; sem o arquivo não há comprovação em auditoria.  
**Instrução para o Copilot:** criar `V20` com a tabela `nota_fiscal` ligada a `gasto`, guardando metadados e a **chave do arquivo** em storage (não o binário no banco). Incluir hash SHA-256 para detectar duplicidade e `UNIQUE` parcial (`cnpj_emitente`, `numero`) quando ambos existirem. A implementação Java é BE-08.  
**Restrições:** não armazenar o arquivo em coluna `bytea`; não alterar a tabela `gasto` neste item; não criar endpoints.  
**Exemplo de correção:**
```sql
CREATE TABLE nota_fiscal (
    id             BIGSERIAL PRIMARY KEY,
    gasto_id       BIGINT NOT NULL REFERENCES gasto(id),
    numero         VARCHAR(60),
    cnpj_emitente  VARCHAR(18),
    valor          NUMERIC(19,2),
    data_emissao   DATE,
    arquivo_chave  VARCHAR(255) NOT NULL,
    arquivo_nome   VARCHAR(255) NOT NULL,
    content_type   VARCHAR(100) NOT NULL,
    tamanho_bytes  BIGINT NOT NULL CHECK (tamanho_bytes > 0),
    sha256         CHAR(64) NOT NULL,
    criado_por     BIGINT NOT NULL REFERENCES funcionario(id),
    criado_em      TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_nota_fiscal_gasto ON nota_fiscal (gasto_id);
CREATE UNIQUE INDEX uk_nota_fiscal_gasto_sha ON nota_fiscal (gasto_id, sha256);
CREATE UNIQUE INDEX uk_nota_fiscal_emitente_numero ON nota_fiscal (cnpj_emitente, numero)
  WHERE cnpj_emitente IS NOT NULL AND numero IS NOT NULL;
```

---

### [DB-08] [MÉDIO] `totalRecebido` é um número sobrescrito, sem histórico de entradas

**Camada:** Banco de Dados  
**Arquivo(s):** novo `{BE-RES}/db/migration/V21__create_recebimento_obra.sql`  
**Descrição:** *(BE 4.9 parte modelagem)* `obra.total_recebido` é editado por `PUT`; não há registro de cada recebimento (data, valor, origem, quem lançou). Um erro de digitação apaga o valor anterior sem rastro; o requisito desejável "monitoramento de entradas/saídas anuais" é inviável.  
**Impacto:** sem trilha de auditoria e sem relatório anual.  
**Instrução para o Copilot:** criar `recebimento_obra` e **migrar o saldo atual** como um lançamento inicial por obra (`descricao = 'Saldo inicial migrado'`, `valor = total_recebido` quando > 0). **Manter** `obra.total_recebido` por enquanto (BE-16 passa a recalculá-lo/derivá-lo).  
**Restrições:** não remover `obra.total_recebido`; não mudar o contrato de `/obras/{id}/financeiro` aqui.  
**Exemplo de correção:**
```sql
CREATE TABLE recebimento_obra (
    id          BIGSERIAL PRIMARY KEY,
    obra_id     BIGINT NOT NULL REFERENCES obra(id),
    valor       NUMERIC(19,2) NOT NULL CHECK (valor <> 0), -- negativo = estorno/ajuste
    data        DATE NOT NULL,
    descricao   VARCHAR(255),
    criado_por  BIGINT REFERENCES funcionario(id),
    criado_em   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_recebimento_obra ON recebimento_obra (obra_id, data);
INSERT INTO recebimento_obra (obra_id, valor, data, descricao)
SELECT id, total_recebido, CURRENT_DATE, 'Saldo inicial migrado' FROM obra WHERE total_recebido > 0;
```

---

### [DB-09] [MÉDIO] `"REEMBOLSO"` usado como método de pagamento e valores de domínio inconsistentes **[NOVO]**

**Camada:** Banco de Dados (expandir) — contrai em BE-18/INT-04  
**Arquivo(s):** novo `{BE-RES}/db/migration/V22__gasto_reembolsavel_normalizacao.sql`; `{BE}/model/Gasto.java`; `{BE}/config/DatabaseSeeder.java`; `{BE-RES}/db/migration/V3`, `V6`, `V7` (referência)  
**Descrição:** *(BE 1.4, BE 3.5 parte `reembolso_concluido`, [NOVO])* `gasto.metodo_pagamento` mistura dois conceitos: o **meio de pagamento** (`PIX`, `CARTAO_*`, `DINHEIRO`) e o **fato de ser reembolsável**, codificado como o literal `"REEMBOLSO"` (usado em `GastoFiltro`, `ReembolsoService`, `GastoService`, `DashboardService`). Quando o usuário marca "Reembolso ao funcionário", o frontend **troca** o método por `"REEMBOLSO"` e, ao editar, mostra `PIX` no lugar (`DetalhamentoObras.tsx`), perdendo como foi pago. Os dados legados (V3/V6/seeder) usam `Débito`/`Crédito`, que **não casam** com os valores do frontend/OCR (`CARTAO_DEBITO`, `CARTAO_CREDITO`): filtro por método exclui os legados. O estado do reembolso também está duplicado em `gasto.reembolso_concluido` e `pagamento_reembolso`.  
**Impacto:** filtros e relatórios incompletos; perda de informação ao editar; dupla fonte de verdade para status de reembolso.  
**Instrução para o Copilot (fase "expandir" — não quebra o código atual):**
1. `V22`: adicionar `gasto.reembolsavel BOOLEAN NOT NULL DEFAULT FALSE` e preenchê-la com `metodo_pagamento = 'REEMBOLSO'`.
2. `V22`: normalizar legados: `Débito|Debito` → `CARTAO_DEBITO`; `Crédito|Credito` → `CARTAO_CREDITO`. **Manter** `metodo_pagamento = 'REEMBOLSO'` nas linhas reembolsáveis por enquanto (o código atual depende disso).
3. Mapear `Gasto.reembolsavel` na entidade (getter/setter) e mantê-lo sincronizado em `GastoService.criar/atualizar` (`reembolsavel = "REEMBOLSO".equals(metodoPagamento)`), **sem expor** no JSON ainda.
4. O valor de método `NAO_INFORMADO` será usado na fase "contrair" (BE-18/INT-04) para gastos reembolsáveis legados.

**Restrições:** não remover `reembolso_concluido` nem o literal `"REEMBOLSO"` neste item (BE-14/BE-18 e INT-04 fazem a contração depois de o frontend migrar); não editar V3/V6/V7; o `DatabaseSeeder` pode passar a gravar `CARTAO_DEBITO/CARTAO_CREDITO` (apenas dev).  
**Exemplo de correção:**
```sql
ALTER TABLE gasto ADD COLUMN reembolsavel BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE gasto SET reembolsavel = TRUE WHERE metodo_pagamento = 'REEMBOLSO';
UPDATE gasto SET metodo_pagamento = 'CARTAO_DEBITO'  WHERE metodo_pagamento IN ('Débito', 'Debito');
UPDATE gasto SET metodo_pagamento = 'CARTAO_CREDITO' WHERE metodo_pagamento IN ('Crédito', 'Credito');
```

## FASE 2 — Segurança e Autenticação

### [SEG-01] [CRÍTICO] Hash BCrypt das senhas exposto por vários endpoints

**Camada:** Backend (stop-gap imediato; correção definitiva em INT-01)  
**Arquivo(s):** `{BE}/model/Funcionario.java`; `{BE}/controller/GastoController.java`, `AlocacaoObraController.java`, `RelatorioController.java`, `ObraController.java`; novo teste `{BE-TEST}/SenhaNaoVazaApiTest.java`  
**Descrição:** *(BE 2.1)* `Gasto.funcionario`, `AlocacaoObra.funcionario` e `Relatorio.funcionarioResponsavel` são `@ManyToOne` para a entidade `Funcionario`, que expõe `getSenha()` sem anotação Jackson. Os controllers devolvem as entidades: `GET /gastos`, `/gastos/{id}`, `/gastos/paginado`, `/obras/{id}/gastos`, `/alocacoes*`, `/obras/{id}/alocacoes` e `/relatorios*` entregam `"funcionario": {…, "senha": "$2a$10$…"}`. Os testes atuais só verificam `$.senha` na raiz dos endpoints de funcionário. Como os hashes do seed são **iguais** (mesma senha conhecida, ver SEG-02), o vazamento é imediatamente explorável por qualquer usuário com `VISUALIZAR_GASTOS`/`VISUALIZAR_ALOCACOES`.  
**Impacto:** qualquer funcionário (inclusive `ENGENHEIRO`) baixa os hashes de todos os colegas e do ADMIN.  
**Instrução para o Copilot:**
1. Em `Funcionario.senha` adicionar `@JsonIgnore` (campo; manter a coluna e os getters/setters).
2. Criar teste que, com dados reais (usuário ADMIN autenticado e ao menos um gasto/alocação/relatório), percorra **todos** os endpoints que retornam entidades e falhe se a resposta contiver `"senha"` ou `$2a$` em **qualquer nível**.
3. Registrar no PR o **runbook de incidente**: trocar `JWT_SECRET` (invalida sessões) e forçar redefinição de senha de todos os usuários.

**Restrições:** não remover `senha` de `FuncionarioDto` (entrada de criação); confirmar por busca que nenhum `@RequestBody` recebe a entidade `Funcionario` (se houvesse, `@JsonIgnore` impediria o bind); **manter o `@JsonIgnore` mesmo depois de INT-01** (defesa em profundidade); não criar DTOs aqui.  
**Exemplo de correção:**
```java
// {BE}/model/Funcionario.java
@JsonIgnore
private String senha;
```
```java
mockMvc.perform(get("/gastos").with(user(admin)))
       .andExpect(content().string(not(containsString("\"senha\""))))
       .andExpect(content().string(not(containsString("$2a$"))));
```

---

### [SEG-02] [CRÍTICO] Credenciais padrão conhecidas, seed ligado por padrão e dados de demonstração em produção

**Camada:** Backend + Infra (Terraform)  
**Arquivo(s):** `{BE}/config/DatabaseSeeder.java`; `{BE-RES}/application.properties`; novo `{BE-RES}/application-dev.properties`; `{BE-RES}/db/migration/V2__insert_user.sql`, `V3__insert_dados_iniciais.sql`, `V6__align_gastos_dashboard.sql`; `{BE}/dto/FuncionarioLoginDto.java`; `{TF}/scripts/backend.sh`; `{TF}/variables.tf`; `{TF}/modules/ec2/main.tf`; `docker-compose.yml`  
**Descrição:** *(BE 2.2, BE 3.9)* O seed roda por padrão (`matchIfMissing = true`, `app.seed.enabled=${APP_SEED_ENABLED:true}`) e `terraform/scripts/backend.sh` não define `APP_SEED_ENABLED` nem `SPRING_PROFILES_ACTIVE`: em produção o backend cria `admin@gmail.com` (ADMIN) e outros 9 usuários, todos com a mesma senha padrão (documentada no comentário do seeder e no `@Schema(example=…)` do login, **visível no Swagger público**). As migrations V2/V3/V6 inserem os mesmos usuários, obras e gastos fictícios em qualquer banco novo (com IDs fixos). O `NOT EXISTS` do seeder ressuscita usuário excluído no próximo restart.  
**Impacto:** takeover imediato do ADMIN em produção por qualquer pessoa que leia o repositório; dados falsos misturados aos reais corrompem relatórios enviados à contabilidade.  
**Instrução para o Copilot:**
1. `DatabaseSeeder`: anotar com `@Profile("dev")` e `@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")` (**sem** `matchIfMissing`). `application.properties`: `app.seed.enabled=${APP_SEED_ENABLED:false}`; criar `application-dev.properties` com `app.seed.enabled=true`.
2. Mover (`git mv`) `V2`, `V3`, `V6` para `{BE-RES}/db/dev/` **sem alterar o conteúdo** (checksum preservado). Em `application.properties`: `spring.flyway.locations=classpath:db/migration`; em `application-dev.properties`: `spring.flyway.locations=classpath:db/migration,classpath:db/dev`; para ambientes que já aplicaram V2/V3/V6, `spring.flyway.ignore-migration-patterns=*:missing`.
3. Criar `BootstrapAdminRunner` (`ApplicationRunner`): se `APP_BOOTSTRAP_ADMIN_EMAIL` e `APP_BOOTSTRAP_ADMIN_PASSWORD` existirem **e** não houver nenhum ADMIN, cria o administrador (senha validada pela política de SEG-20, hash BCrypt, **nunca** logar a senha).
4. Terraform: em `backend.sh` passar `-e SPRING_PROFILES_ACTIVE=prod -e APP_SEED_ENABLED=false -e APP_BOOTSTRAP_ADMIN_EMAIL=… -e APP_BOOTSTRAP_ADMIN_PASSWORD=…`; declarar as duas variáveis como `sensitive` em `variables.tf` e repassá-las em `modules/ec2`.
5. Remover o exemplo de senha de `@Schema(example=…)` em `FuncionarioLoginDto`.
6. `docker-compose.yml` (uso local): `SPRING_PROFILES_ACTIVE: dev` e `APP_SEED_ENABLED: "true"` no serviço `backend`.
7. Runbook (no PR, **executado manualmente** em cada ambiente existente, nunca por migration): trocar a senha de `admin@gmail.com`, desativar (`ativo=false`, após DB-05) ou trocar a senha dos demais usuários de demonstração.

**Restrições:** não editar o conteúdo de V2/V3/V6; não excluir usuários por migration; os testes que dependem do seed (`DatabaseSeederTest`, `FinanceiroMigrationTest`) devem usar `@ActiveProfiles("dev")`; não commitar valores reais de senha/e-mail do admin; `APP_BOOTSTRAP_*` só cria usuário quando **não existe ADMIN**. Depende de DB-01.  
**Exemplo de correção:**
```java
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DatabaseSeeder implements ApplicationRunner { /* ... */ }
```
```properties
# application.properties
app.seed.enabled=${APP_SEED_ENABLED:false}
spring.flyway.locations=classpath:db/migration
# application-dev.properties
app.seed.enabled=true
spring.flyway.locations=classpath:db/migration,classpath:db/dev
```

---

### [SEG-03] [CRÍTICO] Sessão ausente/expirada responde 403 (e não 401): o frontend nunca se recupera **[NOVO]**

**Camada:** Backend + Integração (nenhuma mudança obrigatória no frontend)  
**Arquivo(s):** `{BE}/config/AutenticacaoEntryPoint.java`; `{BE}/config/SecurityConfiguracao.java`; `{BE}/config/GlobalExceptionHandler.java` (reutilizar `ErrorResponse`); `{BE}/config/AutenticacaoFilter.java`; `{FE}/Service/Auth/Login/Api/Api.ts` e `{FE}/Context/UserContext.tsx` (apenas referência)  
**Descrição:** *(BE 5.2 parte; [NOVO] efeito cruzado)* `AutenticacaoEntryPoint` só devolve 401 para `BadCredentialsException`; para qualquer outra falha (o caso normal: cookie ausente/expirado → `InsufficientAuthenticationException`) faz `sendError(403)`. Já o frontend só reage a **401**: o interceptor de `Api.ts` limpa a sessão e redireciona apenas em 401, e `UserContext.refreshUser` só chama `clearSession()` em 401 (o comentário no código afirma "Cookie expirado/encerrado… a rota privada volta ao login"). Com o cookie de 1 h expirado, `GET /funcionarios/me` volta 403 → o `UserContext` mantém `localStorage.userEmail` (logo `isAuthenticated()` continua verdadeiro) e mostra "Nao foi possivel carregar os dados do usuario"; todas as telas passam a exibir "sem permissão". Não há como o usuário perceber que precisa logar de novo, exceto clicar em *Sair*.  
**Impacto:** depois de 1 hora, a aplicação fica inutilizável até limpar o `localStorage`/clicar em Sair; o mesmo vale para token inválido, usuário excluído em outra sessão e troca de e-mail (ver SEG-09). Toda a lógica de expiração de sessão do frontend é, na prática, código morto.  
**Instrução para o Copilot:**
1. `AutenticacaoEntryPoint`: responder **sempre 401** com corpo JSON no mesmo formato de `ErrorResponse` (`timestamp,status,error,message,path`), `Content-Type: application/json;charset=UTF-8` e mensagem "Sessão expirada ou ausente. Faça login novamente.". Não usar `sendError`.
2. Registrar um `AccessDeniedHandler` que responda **403** (JSON, mesmo formato) apenas para usuário autenticado sem permissão.
3. Em `AutenticacaoFilter`, capturar `UsernameNotFoundException` (usuário excluído com token válido) e tratá-la como token inválido (não autentica → 401), em vez de propagar 500.
4. Testes MockMvc: sem cookie → 401 JSON; cookie com JWT expirado → 401; JWT de usuário inexistente → 401; autenticado sem permissão → 403.

**Restrições:** manter `/funcionarios/login` com 401 para credenciais erradas (já é 401 via `BadCredentialsException`); não alterar o interceptor do frontend neste item (o frontend já trata 401 e passa a funcionar sozinho; melhorias de UX estão em FE-03); não retornar detalhes do JWT na mensagem.  
**Exemplo de correção:**
```java
@Component
public class AutenticacaoEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    public AutenticacaoEntryPoint(ObjectMapper mapper) { this.mapper = mapper; }

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex) throws IOException {
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // GlobalExceptionHandler.ErrorResponse é um record de pacote (mesmo pacote `config`); extrair para classe pública se preferir.
        mapper.writeValue(res.getOutputStream(), new GlobalExceptionHandler.ErrorResponse(
                LocalDateTime.now(), 401, "Unauthorized",
                "Sessão expirada ou ausente. Faça login novamente.", req.getRequestURI()));
    }
}
```

---

### [SEG-04] [CRÍTICO] Unidade de `JWT_EXPIRATION` diverge entre Terraform e código; segredo sem validação na subida **[NOVO]**

**Camada:** Backend + Infra  
**Arquivo(s):** `{BE}/config/GerenciadorTokenJwt.java`; novo `{BE}/config/JwtProperties.java`; `{BE-RES}/application.properties`; `{TF}/variables.tf` (`jwt_expiration`); `{TF}/modules/ec2/variables.tf`; `.env.example`; `{BE}/config/SecurityConfiguracao.java`  
**Descrição:** *(BE 2.8 parte, [NOVO])* `terraform/variables.tf` declara `jwt_expiration` "em milissegundos" com default `"86400000"` (24 h). O código faz `setExpiration(now + jwtTokenValidity * 1_000)`, ou seja, trata o valor como **segundos**: na AWS o JWT dura 86.400.000 s ≈ **1000 dias**, enquanto o cookie expira em 3600 s (`Max-Age` fixo, SEG-06) e o `.env` local usa `3600`. Além disso `JWT_SECRET`/`JWT_EXPIRATION` não têm validação: um segredo Base64 com menos de 256 bits lança `WeakKeyException` **no primeiro login**, e o app sobe "saudável".  
**Impacto:** token roubado (ou enviado pelo header `Authorization`, que ignora o cookie) vale por ~3 anos e o logout não o revoga; configuração errada só aparece em runtime.  
**Instrução para o Copilot:**
1. Criar `JwtProperties` (`@ConfigurationProperties("app.security.jwt")`, `@Validated`): `@NotBlank String secret` e `@NotNull @DurationUnit(ChronoUnit.SECONDS) Duration expiration` (validar mínimo 5 min e máximo 24 h). **Registrar o bean**: `@ConfigurationPropertiesScan` na classe `Application` (ou `@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})` em uma `@Configuration`) — `@ConfigurationProperties` sozinho não cria o bean e a aplicação não sobe. Em `application.properties`: `app.security.jwt.secret=${JWT_SECRET}` e `app.security.jwt.expiration=${JWT_EXPIRATION:3600}` (valores numéricos = segundos).
2. Na subida (`@PostConstruct`/`InitializingBean`), decodificar o segredo Base64 e **falhar** se tiver menos de 32 bytes.
3. `GerenciadorTokenJwt` usa `JwtProperties` (remover `@Value`); `expiration().toMillis()` na emissão.
4. Terraform: `jwt_expiration` passa a descrever **segundos** com default `"3600"`; corrigir a descrição. `.env.example`: documentar `JWT_EXPIRATION=3600 (segundos)`.
5. Runbook: ao implantar, **rotacionar `JWT_SECRET`** para invalidar tokens emitidos com a validade de ~1000 dias.

**Restrições:** manter os nomes das variáveis de ambiente (`JWT_SECRET`, `JWT_EXPIRATION`); não alterar claims nem `sub` aqui (SEG-09); o `Max-Age` do cookie passa a usar esta mesma propriedade em SEG-06. Sem `@DurationUnit`, o número puro seria lido como **milissegundos** pelo Spring: manter a anotação.  
**Exemplo de correção:**
```java
@Validated
@ConfigurationProperties("app.security.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotNull @DurationUnit(ChronoUnit.SECONDS) Duration expiration) {
    public JwtProperties {
        if (expiration != null && (expiration.compareTo(Duration.ofMinutes(5)) < 0
                || expiration.compareTo(Duration.ofHours(24)) > 0)) {
            throw new IllegalArgumentException("app.security.jwt.expiration deve estar entre 5 min e 24 h");
        }
    }
}
```
```hcl
variable "jwt_expiration" {
  description = "Tempo de expiração do JWT em SEGUNDOS"
  type        = string
  default     = "3600"
}
```

---

### [SEG-05] [CRÍTICO] Tráfego em HTTP: HTTPS opcional no ALB, sem HSTS

**Camada:** Integração (infra: Terraform + nginx)  
**Arquivo(s):** `{TF}/variables.tf` (`enable_https`, `acm_certificate_arn`); `{TF}/main.tf`; `{TF}/modules/alb/main.tf`; `{TF}/profiles/*.tfvars.example`; `{TF}/USO.md`; `{TF}/scripts/backend.sh`; `{FE-ROOT}/nginx.conf`  
**Descrição:** *(BE 2.3 parte, FE S1 parte)* O ALB só habilita o listener 443 quando `enable_https=true` **e** há certificado (`acm_certificate_arn`), e o padrão é `false`. O nginx só escuta na porta 80 e não envia HSTS. O cookie `jwt` e as credenciais do login trafegam em texto claro (uso *mobile first* em obra, Wi-Fi público).  
**Impacto:** captura de sessão e senha em rede compartilhada; sem HTTPS não é possível ligar `Secure` no cookie (SEG-06).  
**Instrução para o Copilot:**
1. Em `variables.tf`, adicionar `validation` em `enable_https` exigindo `acm_certificate_arn` quando verdadeiro; documentar em `USO.md` e nos `*.tfvars.example` o fluxo (certificado ACM + domínio).
2. No módulo ALB manter o redirect 80→443 já existente quando `enable_https=true`; adicionar `drop_invalid_header_fields = true` no `aws_lb.main`.
3. Criar variável `cookie_secure` (padrão = `enable_https`) e repassá-la ao backend como `-e APP_COOKIE_SECURE=…` em `backend.sh` (consumida em SEG-06).
4. HSTS e `X-Forwarded-Proto` correto: ver INT-18.

**Restrições:** se não houver certificado disponível (decisão D-1, ex.: Learner Lab), **não forçar** HTTPS: manter `cookie_secure=false` e registrar o risco no PR; não alterar o ALB interno (HTTP dentro da VPC é aceitável). Este item precisa estar implantado **antes** de ligar `Secure=true` em produção.  
**Exemplo de correção:**
```hcl
variable "enable_https" {
  type    = bool
  default = false
  validation {
    condition     = !var.enable_https || var.acm_certificate_arn != null
    error_message = "enable_https=true exige acm_certificate_arn."
  }
}
```
```bash
# backend.sh
-e APP_COOKIE_SECURE="${cookie_secure}" \
```

---

### [SEG-06] [CRÍTICO] Cookie JWT sem `Secure`/`SameSite` e com `Max-Age` fixo

**Camada:** Backend  
**Arquivo(s):** `{BE}/controller/FuncionarioController.java` (`login`, `logout`); novo `{BE}/config/CookieProperties.java` e `{BE}/config/SessionCookieFactory.java`; `{BE-RES}/application.properties`; `docker-compose.yml`; `{TF}/scripts/backend.sh`  
**Descrição:** *(BE 2.3, FE S1, FE D2)* O cookie é criado com `jakarta.servlet.http.Cookie`: `setSecure(false)` fixo ("ajustado para desenvolvimento"), sem `SameSite` (a classe não suporta; vale o padrão do navegador) e `Max-Age=3600` fixo, desacoplado de `jwt.validity`. A documentação interna (`RELATORIO_SEGURANCA_OWASP.md`) afirma "HttpOnly e Secure" — o que é falso. O mesmo código roda no deploy AWS.  
**Impacto:** token em texto claro em HTTP; sem `SameSite` explícito e com CSRF desligado, navegadores sem *Lax by default* ficam sujeitos a requisições forjadas (criar/excluir gastos, trocar cargos).  
**Instrução para o Copilot:**
1. Criar `CookieProperties` (`app.security.cookie.name=jwt`, `secure` **padrão `true`**, `same-site` padrão `Strict`) lendo `APP_COOKIE_SECURE`/`APP_COOKIE_SAMESITE`; registrado pelo mesmo `@ConfigurationPropertiesScan` de SEG-04.
2. Criar `SessionCookieFactory` que gera o cookie de login e o de logout com `ResponseCookie` (mesmos atributos `Path=/`, `Secure`, `SameSite`; logout com `Max-Age=0`) e `maxAge` = `JwtProperties.expiration()` (SEG-04). `FuncionarioController` usa a factory e `response.addHeader(HttpHeaders.SET_COOKIE, …)`.
3. `docker-compose.yml` (desenvolvimento local, HTTP): `APP_COOKIE_SECURE: "false"`. `backend.sh` já recebe `APP_COOKIE_SECURE` (SEG-05).
4. Testes: `Set-Cookie` do login contém `HttpOnly`, `Secure` (quando configurado), `SameSite=Strict`, `Path=/` e `Max-Age` igual à expiração do JWT; o logout expira o cookie com os **mesmos** atributos.

**Restrições:** **não** habilitar `Secure=true` em ambiente sem HTTPS: o login "funciona" e o navegador descarta o cookie (após SEG-03 isso aparece como 401 em `/me`). Manter o nome `jwt`. `Strict` é seguro porque o navegador fala sempre com o mesmo site (`/api` via nginx); se algum dia houver navegação cross-site autenticada, usar `Lax`. Não devolver o token no corpo. Depende de SEG-04 e SEG-05.  
**Exemplo de correção:**
```java
ResponseCookie cookie = ResponseCookie.from(props.name(), token)
        .httpOnly(true)
        .secure(props.secure())
        .sameSite(props.sameSite())
        .path("/")
        .maxAge(jwt.expiration())
        .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```
```properties
app.security.cookie.name=jwt
app.security.cookie.secure=${APP_COOKIE_SECURE:true}
app.security.cookie.same-site=${APP_COOKIE_SAMESITE:Strict}
```

---

### [SEG-07] [CRÍTICO] Sem proteção contra força bruta no login

**Camada:** Backend (+ Infra opcional)  
**Arquivo(s):** `{BE}/controller/FuncionarioController.java`; `{BE}/config/AutenticacaoProvider.java`; novos `{BE}/service/LoginAttemptService.java` e `{BE}/config/ClientIpResolver.java`; `{BE}/exceptions/TooManyRequests.java`; `{BE}/config/GlobalExceptionHandler.java`; `jaf-backend/pom.xml` (Caffeine); `{TF}/scripts/backend.sh`  
**Descrição:** *(BE 2.4)* Não há rate limit, contador de falhas, bloqueio, CAPTCHA nem atraso progressivo; `isAccountNonLocked()` sempre retorna `true`. **[NOVO]** Um limitador por IP *ingênuo* usaria `request.getRemoteAddr()`, que no deploy é o IP do **ALB interno** (todos os usuários compartilham o mesmo IP): bloquearia todos ou nenhum. A cadeia real é `cliente → ALB público → nginx → ALB interno → Spring`, e o `X-Forwarded-For` visto pelo backend é `cliente, IP-ALB-público, IP-nginx` (o primeiro elemento é forjável pelo cliente).  
**Impacto:** ataque de dicionário trivial contra o ADMIN.  
**Instrução para o Copilot:**
1. Adicionar `com.github.ben-manes.caffeine:caffeine` (gerenciado pelo BOM do Spring Boot).
2. `ClientIpResolver`: obter o IP do cliente a partir do `X-Forwarded-For` contando `app.security.trusted-proxy-hops` **a partir da direita** (padrão `1` para `docker-compose`: nginx → backend; **`3` na AWS**: ALB público, nginx, ALB interno); se o cabeçalho for mais curto, usar `getRemoteAddr()`.
3. `LoginAttemptService` (Caffeine, expira em 15 min): chaves `ip|email-normalizado` e `email-normalizado`; limite 5 falhas/15 min. `FuncionarioController.login` consulta antes de autenticar, registra falha em `BadCredentialsException` e limpa em sucesso. Bloqueado → `TooManyRequests` → **429** com `Retry-After` e a mesma mensagem genérica (sem revelar se o e-mail existe).
4. `backend.sh`: `-e APP_TRUSTED_PROXY_HOPS=3`.
5. (Opcional, recomendado) regra *rate-based* no AWS WAF do ALB público.

**Restrições:** contadores em memória são por instância (2 instâncias ⇒ limite efetivo ×2): aceitável; registrar a limitação no PR. Não aplicar limite aos demais endpoints. O frontend ainda não trata 429 (ver INT-02; até lá cai na mensagem genérica de erro do login).  
**Exemplo de correção:**
```java
String email = loginDto.getEmail().trim().toLowerCase();
String ip = clientIp.resolve(request);
if (attempts.bloqueado(ip, email)) {
    throw new TooManyRequests("Muitas tentativas. Aguarde alguns minutos e tente novamente.");
}
try {
    Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(email, loginDto.getSenha()));
    attempts.limpar(ip, email);
} catch (BadCredentialsException e) {
    attempts.registrarFalha(ip, email);
    throw e;
}
```

---

### [SEG-08] [MÉDIO] Login devolve 500 para e-mail inexistente e 401 para senha errada (enumeração de usuários)

**Camada:** Backend  
**Arquivo(s):** `{BE}/config/AutenticacaoProvider.java`; `{BE}/config/SecurityConfiguracao.java` (`authManager`); `{BE}/service/AutenticacaoService.java`; `{BE}/config/GlobalExceptionHandler.java`  
**Descrição:** *(BE 2.5)* `loadUserByUsername` lança `UsernameNotFoundException`; o `AutenticacaoProvider` customizado não a converte em `BadCredentialsException`; o handler genérico responde 500, enquanto senha errada responde 401. Há também diferença de tempo (sem `BCrypt.matches` para usuário inexistente). **[NOVO]** O frontend mapeia 401 → "e-mail ou senha inválidos" e qualquer outro status → mensagem genérica: a UI revela quais e-mails existem.  
**Impacto:** enumeração de usuários; 500 de login polui o monitoramento.  
**Instrução para o Copilot:** substituir o `AutenticacaoProvider` por `DaoAuthenticationProvider` (mantém `hideUserNotFoundExceptions=true` e mitiga ataque de tempo) usando `AutenticacaoService` e o `PasswordEncoder`, expor via `ProviderManager`. Mapear `DisabledException`/`LockedException` para a mesma resposta 401 "Credenciais inválidas" em `GlobalExceptionHandler`. Testes: e-mail inexistente e senha errada ⇒ **mesmo** status (401), mesma mensagem.  
**Restrições:** não remover o `PasswordEncoder` BCrypt; não mudar a mensagem "Credenciais inválidas"; o frontend não precisa de mudança (já trata 401).  
**Exemplo de correção:**
```java
@Bean
public AuthenticationManager authManager(AutenticacaoService uds, PasswordEncoder encoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(uds);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
}
```

---

### [SEG-09] [MÉDIO] JWT sem revogação, `sub` mutável (e-mail) e cookie inválido ignora o header

**Camada:** Backend (+ efeito no frontend em FE-03)  
**Arquivo(s):** `{BE}/config/GerenciadorTokenJwt.java`; `{BE}/config/AutenticacaoFilter.java`; `{BE}/service/AutenticacaoService.java`; `{BE}/dto/FuncionarioDetalhesDto.java`; `{BE}/controller/FuncionarioController.java`; `{BE}/service/FuncionarioService.java` (`alterarSenha`, `atualizarCargo`); `{FE}/Pages/Profile/Profile.tsx` (referência)  
**Descrição:** *(BE 2.8, FE U7 parte)* O logout só apaga o cookie; o token continua válido até expirar (e o header `Authorization` também é aceito). Trocar senha, rebaixar ou desativar o usuário não invalida tokens. O `sub` é o **e-mail**, mutável via `PUT /funcionarios/me`: o token antigo deixa de funcionar e o job OCR perde o dono (`solicitante` é e-mail). As claims `authorities`/`cargo` são geradas e nunca lidas. **[NOVO]** O frontend esconde o problema: `Profile.tsx` força `clearUser(); authService.logout()` quando o e-mail muda (workaround). Se o cookie estiver presente mas inválido, o header `Authorization` não é tentado.  
**Impacto:** token roubado vale até expirar; trocar e-mail desloga sem aviso (U7); sessão não reflete desativação de usuário.  
**Instrução para o Copilot (depende de DB-05):**
1. Emitir o token com `sub = id` do funcionário e a claim `tv` = `token_version`. Remover as claims `authorities`/`cargo`. Adicionar `AutenticacaoService.loadUserById(Long)`.
2. **Substituir `GerenciadorTokenJwt.validateToken`**: hoje ele compara o `sub` com `userDetails.getUsername()` (e-mail) e, com `sub = id`, **nenhuma requisição seria autenticada**. A nova validação é: verificar assinatura e expiração (o parser já lança em caso de falha), ler `sub` e `tv`, carregar o usuário por id e comparar apenas `tv` com `token_version`. `getUsernameFromToken` passa a se chamar `getSubjectFromToken`.
3. `AutenticacaoFilter`: carregar o usuário por id (`loadUserById`), **rejeitar** (não autenticar) se `!ativo` ou se `tv` do token ≠ `token_version` do banco; capturar `UsernameNotFoundException` (SEG-03).
4. Incrementar `token_version` em: troca de senha, troca de cargo/permissões do usuário, desativação/exclusão e logout (`POST /funcionarios/logout` incrementa a versão do usuário autenticado quando houver — efeito: encerra as sessões do usuário em **todos os dispositivos**; aceitável para o porte do sistema, registrar no PR).
5. Definir precedência: usar o cookie; usar `Authorization: Bearer` **somente se não houver cookie**.
6. `FuncionarioController.login`: reutilizar o principal autenticado (id/nome/cargo) em vez de reconsultar o funcionário.
7. (Opcional, em commit separado) migrar jjwt `0.11.5` → `0.12.x` (`Jwts.parser().verifyWith(key)`), mantendo HS256.

**Restrições:** `UserDetails.getUsername()` continua sendo o **e-mail** (o `AcessoObraService` usa `auth.getName()`); a troca da semântica do `sub` invalida todas as sessões existentes (um logout geral) — avisar no PR; não remover o workaround do `Profile.tsx` neste item (FE-03 o remove depois). Depende de DB-05 e SEG-04.  
**Exemplo de correção:**
```java
String token = Jwts.builder()
        .setSubject(String.valueOf(usuario.getId()))
        .claim("tv", usuario.getTokenVersion())
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + props.expiration().toMillis()))
        .signWith(parseSecret())
        .compact();
```
```java
// GerenciadorTokenJwt — substitui validateToken(token, userDetails)
public boolean tokenVigente(String token, FuncionarioDetalhesDto usuario) {
    Claims c = getAllClaimsFromToken(token); // lança em assinatura inválida/expirado
    Integer tv = c.get("tv", Integer.class);
    return usuario.isEnabled() && tv != null && tv == usuario.getTokenVersion();
}
```
```java
// AutenticacaoFilter
Long id = Long.valueOf(jwt.getSubjectFromToken(token));
FuncionarioDetalhesDto user = (FuncionarioDetalhesDto) autenticacaoService.loadUserById(id);
if (!jwt.tokenVigente(token, user)) return; // não autentica → 401 (SEG-03)
```

---

### [SEG-10] [MÉDIO] CSRF desabilitado com autenticação por cookie

**Camada:** Backend (+ comportamento já suportado pelo axios no frontend)  
**Arquivo(s):** `{BE}/config/SecurityConfiguracao.java`; novo `{BE}/config/CsrfCookieFilter.java`; `{BE-TEST}/**` (testes de API passam a usar `.with(csrf())`); `{FE}/Service/Auth/Login/Api/Api.ts` (referência)  
**Descrição:** *(BE 2.3, FE S1)* `csrf.disable()` só é seguro com token em header; aqui a autenticação é por **cookie** e o filtro dá prioridade ao cookie. Após SEG-06 (`SameSite`) o risco cai, mas CSRF continua sendo defesa em profundidade necessária (mudança de cargo, exclusão de gasto, etc.).  
**Impacto:** requisições forjadas autenticadas em navegadores/cenários que não aplicam `SameSite` da forma esperada.  
**Instrução para o Copilot:** habilitar CSRF com `CookieCsrfTokenRepository.withHttpOnlyFalse()` + `CsrfTokenRequestAttributeHandler` (padrão SPA do Spring Security 6) e um `CsrfCookieFilter` que força a emissão do cookie `XSRF-TOKEN` em toda resposta. Ignorar CSRF **apenas** em `POST /funcionarios/login`. Configurar o cookie do token com `Secure` e `SameSite` vindos de `CookieProperties`. O axios do frontend já envia `X-XSRF-TOKEN` automaticamente em requisições **same-origin** (a `baseURL` é `/api`), sem mudança de código. Atualizar os testes de API para `.with(csrf())` e adicionar teste: `POST /gastos` sem token ⇒ 403; com token ⇒ sucesso.  
**Restrições:** depende de SEG-06. A `baseURL` do frontend **deve continuar relativa** (`/api`); com URL absoluta de outra origem o axios não envia o header. Se o CORS de desenvolvimento permanecer, incluir `X-XSRF-TOKEN` em `allowedHeaders` (SEG-11). Não desabilitar CSRF para outros endpoints; o Swagger ("Try it out") deixa de funcionar em ambientes com CSRF — aceitável (SEG-12 o desliga em produção).  
**Exemplo de correção:**
```java
.csrf(csrf -> csrf
    .csrfTokenRepository(cookieRepo)
    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
    .ignoringRequestMatchers("/funcionarios/login"))
.addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
```
```java
CookieCsrfTokenRepository cookieRepo = CookieCsrfTokenRepository.withHttpOnlyFalse();
cookieRepo.setCookieCustomizer(c -> c.secure(props.secure()).sameSite(props.sameSite()).path("/"));
```

---

### [SEG-11] [MÉDIO] CORS com curinga `*.elb.amazonaws.com` e credenciais; `TRACE` e `*` em headers

**Camada:** Backend  
**Arquivo(s):** `{BE}/config/SecurityConfiguracao.java` (`corsConfigurationSource`); `{BE-RES}/application.properties` (`app.cors.allowed-origins`); `.env.example`  
**Descrição:** *(BE 2.7, FE S1)* `allowedOriginPatterns` inclui `http://*.elb.amazonaws.com` e `https://*.elb.amazonaws.com`: domínio compartilhado por **todas as contas AWS**; com `allowCredentials(true)` qualquer ALB de terceiros é uma origem aceita. Também permite `TRACE` e `allowedHeaders=*`. A propriedade `app.cors.allowed-origins` e `CORS_ALLOWED_ORIGINS` existem, mas **nada as lê** (lista hardcoded). **Verificado:** o frontend sempre chama `/api` relativo (proxy do Vite em dev e nginx em produção), logo **não há requisição cross-origin** em nenhum ambiente suportado.  
**Impacto:** furo de design que se agrava com `SameSite=None` ou navegadores sem o padrão *Lax*.  
**Instrução para o Copilot:** ler `app.cors.allowed-origins` (`${CORS_ALLOWED_ORIGINS:}`) via `@Value`/`@ConfigurationProperties` e usar **lista exata** (sem curingas); lista vazia ⇒ CORS desabilitado (não registrar configuração). Remover `TRACE` e `allowedHeaders="*"` (permitir `Content-Type`, `Accept`, `X-XSRF-TOKEN`). Remover os padrões `*.elb.amazonaws.com`.  
**Restrições:** não quebrar o desenvolvimento local (`http://localhost:5173` continua via `CORS_ALLOWED_ORIGINS` no `.env`, e o Vite já faz proxy same-origin); não alterar `allowCredentials` para origens fora da lista.  
**Exemplo de correção:**
```java
List<String> origens = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim).filter(s -> !s.isEmpty()).toList();
if (origens.isEmpty()) { return new UrlBasedCorsConfigurationSource(); } // CORS desligado
configuracao.setAllowedOrigins(origens);
configuracao.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN"));
```

---

### [SEG-12] [MÉDIO] Swagger, H2 console e `frameOptions` desabilitado em produção; sem cabeçalhos de segurança no backend

**Camada:** Backend  
**Arquivo(s):** `{BE}/config/SecurityConfiguracao.java` (`URLS_PERMITIDAS`, `headers`); `{BE}/controller/HomeController.java`; `{BE}/config/swagger/OpenApiConfig.java`; `jaf-backend/pom.xml` (dependência `h2`); `{BE-RES}/application.properties`  
**Descrição:** *(BE 2.6)* `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**` e `/` (redireciona ao Swagger) são `permitAll`. `headers.frameOptions().disable()` vale para **toda** a aplicação (só seria necessário para o console H2). Não há CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`. O H2 está com escopo `runtime` (vai no jar de produção).  
**Impacto:** mapa completo da API para qualquer visitante; clickjacking; MIME sniffing em `/uploads`.  
**Instrução para o Copilot:** (1) `springdoc.api-docs.enabled` e `springdoc.swagger-ui.enabled` controlados por `${SWAGGER_ENABLED:false}` (ligado só no profile `dev`); remover do `permitAll` as rotas `/swagger*`, `/v3/api-docs/**`, `/webjars/**`, `/h2-console/**` quando desligado e remover o redirect de `/` em produção (`@Profile("dev")` no `HomeController`). (2) H2 passa a `<scope>test</scope>`; remover `/h2-console/**`. (3) `frameOptions.deny()`, `contentTypeOptions` (padrão), `referrerPolicy(NO_REFERRER)`, `httpStrictTransportSecurity` (efetivo somente sob HTTPS) e CSP `default-src 'none'; frame-ancestors 'none'` para respostas da API.  
**Restrições:** não remover `/actuator/health/**` (usado pelo health check do ALB); manter o Swagger acessível no profile `dev`; verificar que nenhum teste dependa do console H2.  
**Exemplo de correção:**
```java
http.headers(h -> h
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
    .contentSecurityPolicy(c -> c.policyDirectives("default-src 'none'; frame-ancestors 'none'")));
```
```properties
springdoc.api-docs.enabled=${SWAGGER_ENABLED:false}
springdoc.swagger-ui.enabled=${SWAGGER_ENABLED:false}
```

---

### [SEG-13] [MÉDIO] IDOR de leitura em alocações/relatórios e escalada por auto-alocação

**Camada:** Backend  
**Arquivo(s):** `{BE}/controller/AlocacaoObraController.java`; `{BE}/service/AlocacaoObraService.java`; `{BE}/controller/RelatorioController.java`; `{BE}/service/RelatorioService.java`; `{BE}/service/ObraService.java` (`criar`); `{BE}/service/AcessoObraService.java`  
**Descrição:** *(BE 2.10, BE 2.11, BE 4.3 parte backend)* O documentado (e implementado em `/obras`, `/gastos`, `/presencas`) é que o não-ADMIN só vê as obras em que está alocado. Em `/alocacoes/obra/{id}`, `/alocacoes/funcionario/{id}`, `/alocacoes/filtro?obraId=`, `/alocacoes/{id}`, `/relatorios` e `/relatorios/{id}` basta ter `VISUALIZAR_ALOCACOES`/`VISUALIZAR_RELATORIO` para ler **qualquer** obra/funcionário. `RESPONSAVEL_ADMINISTRATIVO` tem `CRIAR/EDITAR/DELETAR_ALOCACAO` sem verificação de escopo nem de "não alterar a si mesmo": pode se alocar em qualquer obra. **[NOVO]** Corrigir só isso quebraria o fluxo existente em que o RA cria uma obra e em seguida aloca o responsável (`NovaObra.tsx`), porque `ObraService.criar` não aloca o criador (BE 4.3: "cria e a obra some").  
**Impacto:** quebra do isolamento por obra; ex-funcionários/obras confidenciais visíveis a todos; qualquer gestor contorna o controle por obra.  
**Instrução para o Copilot:**
1. `AlocacaoObraService`: `listarPorObra`, `filtro(obraId)`, `buscarPorId` e `criar/atualizar/deletar` chamam `acesso.exigirObra(obraId)`; `listarPorFuncionario(id)` só para ADMIN ou o próprio usuário; `filtro` sem `obraId` filtra pelo escopo do usuário.
2. Não-ADMIN não pode criar/editar alocação **para si mesmo** nem em obra à qual não pertence.
3. `RelatorioService`: `listar/buscar` somente ADMIN ou o responsável (`funcionarioResponsavel`) do relatório.
4. `ObraService.criar`: se o criador **não** for ADMIN, criar a alocação do criador na obra (cargo `GESTOR_OBRA`, `dtInicio` = hoje) **na mesma transação**.
5. `POST /alocacoes` passa a ser **idempotente**: se já existir alocação **ativa** do mesmo funcionário/obra **com o mesmo cargo**, devolver `200` com a existente (em vez de `409`); com cargo diferente continua `409`. Isso evita que a auto-alocação de (4) faça o frontend atual (`NovaObra.tsx`) exibir "não foi possível alocar" quando o responsável escolhido é o próprio criador.
6. Testes por papel (ADMIN, RA, ENGENHEIRO) × obras alocadas/não alocadas para cada rota acima (200/403).

**Restrições:** não alterar o formato das respostas; não alterar permissões do enum `Cargo`; `exigirObra` já existe em `AcessoObraService` — reutilizar, não duplicar (a centralização total é BE-01). Não mudar `AlocacaoObra.atualizar` além de impedir troca de funcionário/obra (detalhes em BE-12).  
**Exemplo de correção:**
```java
public List<AlocacaoObra> listarPorObra(Long obraId) {
    acesso.exigirObra(obraId);
    return repo.findByObraId(obraId);
}
public List<AlocacaoObra> listarPorFuncionario(Long funcionarioId) {
    if (!acesso.isAdmin() && !acesso.atual().getId().equals(funcionarioId)) {
        throw new Forbidden("Operação restrita ao próprio usuário ou ADMIN.");
    }
    return repo.findByFuncionarioId(funcionarioId);
}
```

---

### [SEG-14] [MÉDIO] `/obras/paginado` sem whitelist de ordenação nem teto de tamanho (vazamento do orçamento por ordenação)

**Camada:** Backend  
**Arquivo(s):** `{BE}/controller/ObraController.java` (`listarPaginado`); `{BE}/service/ObraService.java` (`listarPaginadoPorUsuario`); `{BE}/service/GastoService.java` (`listarPaginadoFiltrado`); novo `{BE}/service/PaginacaoUtil.java`  
**Descrição:** *(BE 2.14)* `sortBy` vai direto para `Sort.by(sortBy)`. Um não-ADMIN usa `sortBy=orcamento` (campo que o `ObraSerializer` esconde dele) e deduz a ordem de grandeza dos orçamentos; `sortBy=xyz` vira `PropertyReferenceException` → 500; `size` não tem teto. `/gastos/paginado` tem whitelist e teto 200, o que prova a inconsistência.  
**Impacto:** vazamento de informação financeira e DoS por consulta enorme.  
**Instrução para o Copilot:** extrair para `PaginacaoUtil` a validação já existente em `GastoService` (whitelist, `size` 1..100, `page >= 0`, `sortDir` `asc|desc`) e usá-la em `/obras/paginado` com whitelist `id, titulo, status, dtInicio, dtTerminoPrevisto`; valor inválido ⇒ 400 com mensagem clara. Manter o teto de `/gastos/paginado` em 200 se o frontend futuro (INT-11) precisar, apenas centralizado.  
**Restrições:** o frontend **não** usa `/obras/paginado` hoje (verificado), então não há impacto de contrato; não mudar o shape de `Page` aqui (INT-11).  
**Exemplo de correção:**
```java
public static Pageable validar(int page, int size, String sortBy, String sortDir, Set<String> permitidos, int maxSize) {
    if (page < 0 || size < 1 || size > maxSize || !permitidos.contains(sortBy)
            || !Set.of("asc", "desc").contains(sortDir.toLowerCase())) {
        throw new BadRequest("Parâmetros de paginação inválidos.");
    }
    return PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
}
```

---

### [SEG-15] [MÉDIO] Upload de foto de perfil inseguro e `/uploads` público; `fotoUrl` arbitrária

**Camada:** Backend (+ nginx em INT-18)  
**Arquivo(s):** `{BE}/controller/FuncionarioPerfilController.java` (`uploadFoto`, `getExtensao`); `{BE}/config/StaticResourceConfig.java`; `{BE}/config/SecurityConfiguracao.java` (`/uploads/**`); `{BE}/dto/FuncionarioPerfilUpdateDto.java`; `{BE}/service/FuncionarioService.java` (`atualizarPerfil`)  
**Descrição:** *(BE 2.9, FE S8)* O tipo é validado só por `file.getContentType()` (cabeçalho do cliente, forjável); a extensão vem do **nome original** (`foto.html`, `foto.svg`); não há verificação de *magic bytes*. O arquivo é servido com o Content-Type deduzido da extensão e **sem autenticação** (`/uploads/**` é `permitAll`). `PUT /funcionarios/me` aceita qualquer string de até 512 caracteres em `fotoUrl` (`javascript:`, pixels de rastreamento). O upload não atualiza o perfil (duas chamadas), sem limpeza de órfãos.  
**Impacto:** XSS armazenado (HTML/SVG servido como `text/html`/`image/svg+xml`) na origem da API; hospedagem de conteúdo arbitrário; fotos de funcionários públicas (LGPD).  
**Instrução para o Copilot:**
1. Validar o conteúdo por *magic bytes* (JPEG `FF D8 FF`, PNG `89 50 4E 47`, GIF `47 49 46 38`, WEBP `RIFF….WEBP`), **ignorar** `Content-Type` e nome do cliente e derivar a extensão do tipo detectado. Rejeitar SVG/HTML. Manter limite de 5 MB.
2. Exigir autenticação em `/uploads/**` (remover do `permitAll`). O frontend usa `<img src="/uploads/...">` same-origin, então o cookie é enviado.
3. `StaticResourceConfig`: `Cache-Control: private` e `X-Content-Type-Options: nosniff`.
4. `FuncionarioPerfilUpdateDto.fotoUrl`: aceitar `null`/vazio, `/uploads/fotos/<uuid>.(jpg|png|gif|webp)` **ou** os avatares estáticos do frontend `/assets/Geral/avatar-*.svg`; qualquer outro valor ⇒ 400.

**Restrições:** **não** restringir `fotoUrl` apenas a `/uploads/…`: `Types/user.ts` oferece 5 avatares `/assets/Geral/avatar-*.svg` que o `Profile.tsx` grava em `fotoUrl`; o `null` (remover foto) deve continuar válido. Não alterar o contrato `POST /funcionarios/me/foto → {url}`. A migração para S3/EFS é BE-23.  
**Exemplo de correção:**
```java
@Size(max = 512)
@Pattern(regexp = "^$|^/uploads/fotos/[0-9a-f\\-]{36}\\.(jpg|png|gif|webp)$|^/assets/Geral/avatar-[a-z]+\\.svg$",
         message = "URL de foto inválida")
private String fotoUrl;
```

---

### [SEG-16] [MÉDIO] Detalhes de erro, SQL e PII em logs e respostas

**Camada:** Backend  
**Arquivo(s):** `{BE-RES}/application.properties`; `{BE}/config/AutenticacaoFilter.java`; `{BE}/service/FuncionarioService.java`; `{BE}/config/GlobalExceptionHandler.java`  
**Descrição:** *(BE 2.13)* `server.error.include-message=always` e `include-binding-errors=always` expõem mensagens internas no `/error` padrão; `spring.jpa.show-sql=true` imprime todo SQL (com PII) no stdout em produção; logs com e-mails em `INFO` e mensagens do JJWT no `WARN` do filtro; erros de validação logados como `ERROR`.  
**Impacto:** fuga de detalhes internos, logs gigantes, risco LGPD.  
**Instrução para o Copilot:** `server.error.include-message=never`, `server.error.include-binding-errors=never`, `spring.jpa.show-sql=false` (SQL só com `logging.level.org.hibernate.SQL=debug` no profile `dev`); criar utilitário `LogMask.email(String)` (`jo***@dominio.com`) e usá-lo em todos os `logger.*` que imprimem e-mail; mensagens do JJWT apenas em `DEBUG`; erros 4xx de negócio em `WARN`/`INFO`, nunca `ERROR`.  
**Restrições:** as respostas de erro **da aplicação** (JSON de `GlobalExceptionHandler` e do `AutenticacaoEntryPoint` de SEG-03) não dependem de `server.error.*`, logo o frontend não é afetado; não remover o log de segurança de tentativas de login (apenas mascarar).  
**Exemplo de correção:**
```properties
server.error.include-message=never
server.error.include-binding-errors=never
spring.jpa.show-sql=false
```

---

### [SEG-17] [MÉDIO] Exclusão da própria conta sem reautenticação e sem proteção do último ADMIN

**Camada:** Ambos (mudança coordenada, **no mesmo PR**)  
**Arquivo(s):** `{BE}/controller/FuncionarioPerfilController.java` (`excluirConta`); `{BE}/service/FuncionarioService.java` (`excluirConta`, `deletar`); `{BE}/service/AcessoObraService.java`; `{FE}/Pages/Profile/Profile.tsx` (`handleExcluirConta`); `{FE}/Service/Funcionarios/funcionarioService.ts`  
**Descrição:** *(BE 2.15, FE S7)* `DELETE /funcionarios/me` apaga a conta apenas com o token (a confirmação é digitar "EXCLUIR" no cliente). Não há bloqueio do **último ADMIN** (o único administrador pode se excluir ou ser excluído por outro ADMIN e o sistema fica sem gestão). Alterar senha também não derruba outras sessões. A exclusão física ainda falha por FK para quem tem histórico (500).  
**Impacto:** sessão aberta/token roubado apaga a conta; sistema sem ADMIN só se recupera com acesso ao banco.  
**Instrução para o Copilot (depende de DB-05 e SEG-09):**
1. Backend: `DELETE /funcionarios/me` passa a exigir corpo `{ "senhaAtual": "…" }` (validar com `PasswordEncoder.matches`; falha ⇒ 400 "Senha atual incorreta."). Em vez de exclusão física, fazer **desativação** (`ativo=false`, `desativado_em=now()`, `token_version++`); `DELETE /funcionarios/{id}` (ADMIN) idem.
2. Bloquear desativação/exclusão/rebaixamento do **último ADMIN ativo** (409 "Não é possível remover o último administrador.").
3. Frontend (mesmo PR): `funcionarioService.excluirConta(senhaAtual)` envia `api.delete("/funcionarios/me", { data: { senhaAtual } })` e `Profile.tsx` pede a senha no modal de exclusão; tratar 400 exibindo a mensagem do servidor.

**Restrições:** não implementar `DELETE` físico; não apagar histórico de gastos/presença do usuário; não alterar `DELETE /funcionarios/{id}` permissões (`DELETAR_FUNCIONARIO`); manter a confirmação "EXCLUIR" na UI além da senha.  
**Exemplo de correção:**
```java
@DeleteMapping
public ResponseEntity<Void> excluirConta(@AuthenticationPrincipal UserDetails u,
                                         @Valid @RequestBody ExcluirContaDto dto) {
    funcionarioService.desativarPropriaConta(u.getUsername(), dto.senhaAtual());
    return ResponseEntity.noContent().build();
}
```
```ts
excluirConta: async (senhaAtual: string): Promise<void> => {
  await api.delete("/funcionarios/me", { data: { senhaAtual } });
},
```

---

### [SEG-18] [MÉDIO] Artefatos sensíveis e protótipo legado versionados no Git

**Camada:** Ambos (repositório)  
**Arquivo(s):** `data/jafdb.mv.db`; `jaf-backend/data/jafdb.mv.db`; `jaf-backend/data/jafdb.trace.db`; `hs_err_pid36652.log`; `jaf-frontend/banco.json`; `jaf-frontend/endpoint.js`; `jaf-frontend/index.html`; `jaf-frontend/index.css`; `.gitignore`  
**Descrição:** *(BE 2.16, FE S6, FE Q7 parte)* Bancos H2 (que podem ter usuários, hashes e gastos) e um log de crash da JVM estão no Git (`git ls-files` confirma). `jaf-frontend/banco.json` contém e-mails e senhas em texto puro (algumas triviais e uma de aparência real); `endpoint.js` guarda token em `sessionStorage`, concatena e-mail em URLs e usa `innerHTML`. O `.gitignore` não cobre `*.db`, `data/` nem `hs_err_pid*`.  
**Impacto:** dados e informações do ambiente ficam no histórico para sempre; se alguma senha existir/for reutilizada, o risco é imediato.  
**Instrução para o Copilot:** (1) criar a tag `prototipo-legado` no commit atual para preservar o protótipo; (2) `git rm --cached` nos seis primeiros caminhos e `git rm` em `banco.json`, `endpoint.js`, `index.html`, `index.css` de `jaf-frontend/` (a aplicação real está em `jaf-frontend/react/`); (3) acrescentar ao `.gitignore`: `*.mv.db`, `*.trace.db`, `hs_err_pid*.log`, `data/`, `jaf-backend/data/`, `.env.*` com exceção de `.env.example`; (4) adicionar scanner de segredos (gitleaks) como job no `.github/workflows` (SEG-21); (5) no PR, **listar** que qualquer senha de `banco.json` deve ser tratada como comprometida e rotacionada (D-5) e que remover do histórico (`git filter-repo --invert-paths`) exige combinação com o time (reescreve histórico).  
**Restrições:** não rodar `filter-repo` automaticamente; não apagar `data/uploads` de ambientes reais; não remover `.env.example`.  
**Exemplo de correção:**
```gitignore
*.mv.db
*.trace.db
hs_err_pid*.log
data/
jaf-backend/data/
.env.*
!.env.example
```

---

### [SEG-19] [MÉDIO] `frontend.sh` usa `set -x` e imprime o token do GHCR no log do cloud-init **[NOVO]**

**Camada:** Integração (infra)  
**Arquivo(s):** `{TF}/scripts/frontend.sh`; (referência) `{TF}/scripts/backend.sh`, `ocr.sh`, `rabbitmq.sh`  
**Descrição:** `frontend.sh` começa com `set -eux`. Com `-x` o shell imprime cada comando **já expandido** (o Terraform substitui `${ghcr_token}` antes de gerar o script), então `echo "<token>" | docker login …` fica em texto claro em `/var/log/cloud-init-output.log`. Os demais scripts já evitam `-x` com comentário explícito.  
**Impacto:** qualquer pessoa com acesso ao log/instância obtém o token do registry (permite *pull* — e potencialmente *push* — de imagens).  
**Instrução para o Copilot:** trocar `set -eux` por `set -eu` (como em `backend.sh`) e adicionar o mesmo comentário. No PR, instruir a **rotacionar o token do GHCR** usado e a reciclar as instâncias de frontend (o log antigo permanece nelas).  
**Restrições:** não alterar a lógica de retry de login/pull; não imprimir variáveis sensíveis em nenhum `echo`.  
**Exemplo de correção:**
```bash
#!/bin/bash
# Sem "set -x": evita gravar tokens no log do cloud-init.
set -eu
```

---

### [SEG-20] [BAIXO] Política de senha inconsistente entre criação, troca e frontend

**Camada:** Backend (frontend alinhado em INT-03)  
**Arquivo(s):** `{BE}/dto/FuncionarioDto.java`; `{BE}/dto/AlterarSenhaDto.java`; novos `{BE}/validation/SenhaForte.java` e `SenhaForteValidator.java`; `{BE}/service/FuncionarioService.java` (`alterarSenha`)  
**Descrição:** *(BE 2.17, FE R2 parte)* Criação exige 8+ caracteres com maiúscula/minúscula/dígito/especial **e** só aceita os especiais `@$!%*?&` e `[A-Za-z\d@$!%*?&]`; a **troca** aceita 6+ sem especial (`@Size(min=6,max=100)`); o frontend de cadastro aceita qualquer caractere não alfanumérico e o de perfil usa outra regex. Senha acima de 72 bytes é truncada pelo BCrypt. Senha atual errada retorna **403**.  
**Impacto:** a troca enfraquece a política; senhas válidas na tela (ex.: `Senha#2026`) são recusadas pela API sem explicação.  
**Instrução para o Copilot:** criar `@SenhaForte` única: mínimo 8, **máximo 72 bytes UTF-8**, ao menos 1 maiúscula, 1 minúscula, 1 dígito e 1 caractere não alfanumérico **qualquer**; aplicar em `FuncionarioDto.senha` e `AlterarSenhaDto.novaSenha` (e no bootstrap do admin, SEG-02). Em `alterarSenha`, senha atual incorreta ⇒ `BadRequest` (400) "Senha atual incorreta.".  
**Restrições:** não rebaixar o mínimo de 8; hashes existentes continuam válidos; mensagem de validação em português com a regra completa (o frontend a exibe via `fields`, ver INT-02).  
**Exemplo de correção:**
```java
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({FIELD}) @Retention(RUNTIME)
public @interface SenhaForte { String message() default "Senha deve ter 8 a 72 bytes, com maiúscula, minúscula, número e símbolo."; Class<?>[] groups() default {}; Class<? extends Payload>[] payload() default {}; }
// validator: ^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$  e  getBytes(UTF_8).length <= 72
```

---

### [SEG-21] [BAIXO] Dependências, imagens, defaults de credenciais, portas expostas e pipeline

**Camada:** Backend + Infra  
**Arquivo(s):** `jaf-backend/pom.xml`; `jaf-backend/Dockerfile`; `{BE-RES}/application.properties`; `docker-compose.yml`; `.github/workflows/deploy.yml`; `{TF}/scripts/*.sh`; `{TF}/modules/ec2/main.tf`  
**Descrição:** *(BE 2.18, BE 2.19, BE 6.9)* Spring Boot 3.4.5 já saiu do suporte open source; o Dockerfile roda como **root**, sem `HEALTHCHECK` e sem flags de memória, e constrói com `-DskipTests`; o único workflow (`deploy.yml`) faz *build* e *push* **sem rodar testes** e com a tag `:latest` (e o Terraform faz `docker pull …:latest`: deploys não reproduzíveis, rollback difícil). `application.properties` tem defaults de credencial (`postgres`, `guest`): se a variável faltar, o app tenta conectar com credencial padrão em vez de falhar. O `docker-compose` publica `5432`, `5672`, `15672` e `8000` no host e usa `restart: on-failure:5`.  
**Impacto:** CVEs sem correção, escalada em caso de RCE, serviços internos acessíveis, imagens sem teste.  
**Instrução para o Copilot:** (1) atualizar o `spring-boot-starter-parent` para a última 3.5.x e rodar a suíte; (2) Dockerfile: criar usuário não-root (`USER 10001`), `HEALTHCHECK` em `/actuator/health/readiness`, `-XX:MaxRAMPercentage=75` e **remover** `-DskipTests` (testes passam a rodar no CI); (3) `application.properties`: remover defaults de `SPRING_DATASOURCE_USERNAME` e `RABBITMQ_USER`/senha (falhar se ausentes; `.env` local já fornece); (4) `docker-compose.yml`: publicar portas internas apenas em `127.0.0.1` e usar `restart: unless-stopped`; (5) `deploy.yml`: job `test` (`./mvnw verify` e `npm ci && npm run lint && npx tsc -b && npx vitest run`) como pré-requisito do build; tag das imagens com `${{ github.sha }}` além de `latest`; job de gitleaks; Terraform passa a receber `image_tag` explícito.  
**Restrições:** não trocar a versão major do Java; atualizar o Boot em commit separado; as imagens `latest` continuam publicadas durante a transição para não quebrar instâncias existentes.  
**Exemplo de correção:**
```dockerfile
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S -u 10001 -G app app
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER 10001
HEALTHCHECK --interval=30s --timeout=5s --retries=5 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
```

## FASE 3 — Backend (regras, API e lógica)

### [BE-01] [MÉDIO] Regras de autorização/escopo duplicadas e inconsistentes

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/AcessoObraService.java`; `{BE}/service/ObraService.java`, `GastoService.java`, `AlocacaoObraService.java`, `RelatorioService.java`; `{BE}/controller/ReembolsoController.java`; `{BE}/controller/ObraFinanceiroController.java`; `{BE}/controller/AlocacaoObraController.java`; `{BE}/controller/DashboardController.java`  
**Descrição:** *(BE 1.1)* "O usuário pode ver esta obra?" é reimplementado em ≥5 lugares (`ObraService.buscarPorIdComEscopo`, `GastoService.buscarPorIdComEscopo`/`filtro()`, `AcessoObraService.exigirObra`, `ObraService.listarPorUsuario`, `AlocacaoObraService.listarPorUsuario`), cada um ligeiramente diferente. A autorização é dividida entre `@PreAuthorize` (controller) e `exigirX()` (service); `ReembolsoController` e `ObraFinanceiroController` não têm `@PreAuthorize`, então a segurança depende de lembrar de chamar `acesso.exigirX()`. É a causa raiz de SEG-13.  
**Impacto:** cada endpoint novo é um risco de esquecer a checagem.  
**Instrução para o Copilot:** (1) em `AcessoObraService` expor `podeVerObra(Long obraId)`, `podeRegistrarReembolso(Long obraId)` e `isAdmin()` baseados no **principal já autenticado** (`FuncionarioDetalhesDto`) — evitar reconsultar o funcionário em cada chamada; (2) declarar a regra no controller com `@PreAuthorize("hasAuthority('X') and @acessoObraService.podeVerObra(#obraId)")` onde houver `obraId`; (3) adicionar `@PreAuthorize` aos controllers de reembolso (`VISUALIZAR_GASTOS`; registro: `@acessoObraService.podeRegistrarReembolso(#id)`) e financeiro (`@acessoObraService.isAdmin()`); (4) manter as checagens dos services como segunda barreira; (5) teste que percorre **todos** os `@RequestMapping` (via `RequestMappingHandlerMapping`) e falha se algum endpoint fora da lista pública não responder 401 sem autenticação.  
**Restrições:** não alterar mensagens/códigos já devolvidos; depende de SEG-13 (que já aplicou `exigirObra`) e SEG-03; não mudar permissões do enum `Cargo`.  
**Exemplo de correção:**
```java
@GetMapping("/obra/{obraId}")
@PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES') and @acessoObraService.podeVerObra(#obraId)")
public List<AlocacaoObra> listarPorObra(@PathVariable Long obraId) { /* ... */ }
```

---

### [BE-02] [MÉDIO] `GlobalExceptionHandler` transforma erros do framework em 500 e há 3 formatos de erro

**Camada:** Backend (+ Integração em INT-02)  
**Arquivo(s):** `{BE}/config/GlobalExceptionHandler.java`; `{BE}/exceptions/NoContent.java`; `{BE}/controller/ReembolsoController.java`; `{BE}/service/FuncionarioService.java` (`listar`), `{BE}/service/RelatorioService.java`  
**Descrição:** *(BE 1.2, BE 1.8, BE 5.2, BE 5.3)* O handler `Exception.class` captura tudo e responde 500, inclusive `HttpRequestMethodNotSupportedException` (405), `HttpMediaTypeNotSupportedException` (415), `NoResourceFoundException` (404), `MaxUploadSizeExceededException` (413), `MissingServletRequestPartException` (400), `IllegalArgumentException` (cargo inválido em `FuncionarioService.atualizarCargo`), `IllegalStateException` (OCR fora do ar, deveria ser 503), `DataIntegrityViolationException` (exclusão com histórico/duplicidade: 409), `PropertyReferenceException` (400), `DisabledException/LockedException` (401). Existem 3 formatos de erro (`ErrorResponse`, `ValidationError` com `fields`, JSON padrão do Spring em `/error`), sem `code` estável; `NoContent` responde 204 **com corpo**; `handleValidationExceptions` faz cast `(FieldError)` (falha com erros de objeto) e, com vários erros no mesmo campo, só guarda o último; erros de validação são logados como `ERROR`; `POST /gastos/{id}/reembolsos` devolve 201 sem `Location`; mensagens ora com, ora sem acento (`"Usuario nao encontrado."`).  
**Impacto:** alertas 5xx falsos, o frontend não distingue erros previsíveis, ruído nos logs.  
**Instrução para o Copilot:** estender `ResponseEntityExceptionHandler` e acrescentar handlers para as exceções acima, todos devolvendo o **mesmo envelope**: `{timestamp, status, error, code, message, path}` e, em validação, `fields` (mapa `campo → mensagem`, juntando mensagens com `; `). **Expandir sem remover**: manter `message` e `fields` exatamente como hoje (o frontend os lê) e apenas **adicionar** `code` (ex.: `VALIDATION_ERROR`, `CONFLICT`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `RATE_LIMITED`, `PAYLOAD_TOO_LARGE`, `INTERNAL`). O `AutenticacaoEntryPoint`/`AccessDeniedHandler` (SEG-03) devem usar o mesmo construtor. Remover `NoContent` (lista vazia = `200 []`). Corrigir acentuação das mensagens. Logar 4xx em `WARN`. Devolver `Location` no 201 do reembolso.  
**Restrições:** não trocar para `ProblemDetail`/RFC 7807 (quebraria `message`/`fields` do frontend); manter 401 para `BadCredentialsException` e 403 para `AccessDeniedException`; mensagens existentes testadas no frontend (ex.: "Credenciais inválidas") não devem mudar de significado. Depende de DB-02 (para o 409 de integridade) e SEG-03.  
**Exemplo de correção:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflito(DataIntegrityViolationException ex, HttpServletRequest req) {
        return ApiError.of(HttpStatus.CONFLICT, "CONFLICT", "Registro duplicado ou em uso.", req);
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> versao(OptimisticLockingFailureException ex, HttpServletRequest req) {
        return ApiError.of(HttpStatus.CONFLICT, "STALE_VERSION",
            "O registro foi alterado por outra pessoa. Recarregue e tente novamente.", req);
    }
}
```

---

### [BE-03] [MÉDIO] Transações ausentes e inconsistentes

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/ObraService.java`, `AlocacaoObraService.java`, `FuncionarioService.java`, `PresencaService.java`, `RelatorioService.java`; `{BE}/service/FuncionarioPermissaoService.java`, `CargoPermissaoService.java`  
**Descrição:** *(BE 3.6)* Operações de leitura+escrita (`FuncionarioService.atualizar`, `PresencaService.atualizar`, `AlocacaoObraService.atualizar`) rodam sem transação; cada `save` é uma transação isolada. `FuncionarioPermissaoService`/`CargoPermissaoService` usam `jakarta.transaction.Transactional` (ignora `readOnly`/`rollbackFor`) enquanto o resto usa o do Spring.  
**Impacto:** checagem de duplicidade e `save` em transações diferentes; rollback diferente do esperado.  
**Instrução para o Copilot:** anotar os services com `@Transactional` do Spring (`org.springframework.transaction.annotation.Transactional`) no nível da classe e `@Transactional(readOnly = true)` nos métodos de leitura; trocar os imports `jakarta.transaction.Transactional`.  
**Restrições:** não mudar a lógica; fazer **antes** de BE-04/INT-01 (o mapeamento para DTO precisa ocorrer dentro da transação quando o OSIV for desligado); `ReembolsoService` já tem bloqueio pessimista — não alterar.  
**Exemplo de correção:**
```java
@Service
@Transactional
public class AlocacaoObraService {
    @Transactional(readOnly = true)
    public List<AlocacaoObra> listarPorObra(Long obraId) { /* ... */ }
}
```

---

### [BE-04] [MÉDIO] N+1, `@ManyToOne` EAGER, consultas repetidas por request, `ObraSerializer` consultando o banco e pool sem tuning

**Camada:** Backend  
**Arquivo(s):** `{BE}/model/Gasto.java`, `AlocacaoObra.java`, `Presenca.java`, `Relatorio.java`; `{BE}/config/ObraSerializer.java`; `{BE}/config/AutenticacaoFilter.java`; `{BE}/service/AcessoObraService.java`; `{BE}/repository/*Repository.java`; `{BE-RES}/application.properties`  
**Descrição:** *(BE 3.4, BE 1.6, BE 6.3)* `@ManyToOne` sem `fetch = LAZY` é EAGER: cada listagem dispara 1 query por funcionário/obra distinto. `spring.jpa.open-in-view` não está definido (padrão `true`): a conexão fica presa até o fim da serialização. `ObraSerializer.serialize` chama `acesso.isAdmin()` (→ `findByEmailIgnoreCase`) **por obra serializada**, e `Gasto`/`AlocacaoObra`/`Presenca` embutem `Obra`: 500 gastos ⇒ até 500 queries. Cada request autenticado faz 5–10 queries só para autenticar/autorizar. Hikari com padrão (10 conexões, sem `leak-detection-threshold`) e `RestTemplate` sem timeout.  
**Impacto:** latência cresce linearmente com o tamanho da resposta; esgotamento do pool em picos (início de expediente).  
**Instrução para o Copilot:** (1) `ObraSerializer`: decidir ADMIN lendo `SecurityContextHolder` (authority `ROLE_ADMIN`, já emitida por `FuncionarioDetalhesDto`) — sem acessar o banco; (2) `fetch = FetchType.LAZY` nos `@ManyToOne` e `@EntityGraph(attributePaths = {"funcionario","obra"})`/`JOIN FETCH` nas consultas de listagem usadas pelos controllers; (3) `AcessoObraService.atual()`/`isAdmin()` leem o principal (sem nova query); (4) `application.properties`: `spring.jpa.open-in-view=true` **explícito** por ora (silencia o aviso) — desligar em INT-01; `spring.datasource.hikari.maximum-pool-size=20`, `connection-timeout=5000`, `leak-detection-threshold=20000`; (5) substituir `new RestTemplate()` por cliente com *connect/read timeout* (ver BE-22).  
**Restrições:** **não desligar o OSIV agora**: enquanto as entidades forem serializadas direto, proxies LAZY quebrariam o Jackson fora da transação (isso só é seguro após INT-01); não alterar o JSON; depende de BE-03.  
**Exemplo de correção:**
```java
@Override
public void serialize(Obra obra, JsonGenerator gen, SerializerProvider p) throws IOException {
    boolean admin = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    /* ... idem ao atual ... */
}
```
```java
@EntityGraph(attributePaths = {"funcionario", "obra"})
List<Gasto> findByObraId(Long obraId);
```

---

### [BE-05] [MÉDIO] Desativação de funcionário e exclusão com histórico devolvendo 500

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/FuncionarioService.java` (`deletar`, `excluirConta`); `{BE}/controller/FuncionarioController.java`; `{BE}/dto/FuncionarioDetalhesDto.java` (`isEnabled`); `{BE}/service/ObraService.java` (`deletar`); `{BE}/service/FuncionarioPermissaoService.java`  
**Descrição:** *(BE 3.7, BE 4.8)* Não existe desativação; `FuncionarioDetalhesDto.isEnabled()` retorna `true` fixo. Excluir funcionário com gastos/alocações/presenças viola FK → 500. Excluir obra com gastos idem. Quando a exclusão passa, não limpa `funcionario_permissao`.  
**Impacto:** ex-funcionários continuam logando e vendo dados financeiros; mensagem de erro genérica.  
**Instrução para o Copilot (depende de DB-05, SEG-09 e SEG-17):** (1) `isEnabled()` devolve `funcionario.isAtivo()`; (2) novo `PATCH /funcionarios/{id}/ativo` (`{"ativo": boolean}`, ADMIN, não permite desativar a si mesmo nem o último ADMIN): grava `ativo`, `desativado_em` e incrementa `token_version`; (3) `FuncionarioService.deletar`: se existir histórico (gasto, alocação, presença, relatório) ⇒ `Conflict` 409 "Funcionário possui histórico; desative em vez de excluir."; sem histórico ⇒ exclusão física (permissões saem por `ON DELETE CASCADE`); (4) `ObraService.deletar`: 409 se existirem gastos ou presenças; senão excluir alocações e a obra na mesma transação.  
**Restrições:** nunca usar `ON DELETE CASCADE` em histórico financeiro; o frontend ainda **não tem UI** para desativar (fora do escopo; ver DOC-01); manter `DELETE /funcionarios/{id}` e `DELETE /obras/{id}` com as mesmas permissões.  
**Exemplo de correção:**
```java
@PatchMapping("/{id}/ativo")
@PreAuthorize("@acessoObraService.isAdmin()")
public ResponseEntity<FuncionarioResponseDto> ativar(@PathVariable Long id, @Valid @RequestBody AtivoDto dto) {
    return ResponseEntity.ok(funcionarioService.definirAtivo(id, dto.ativo()));
}
```

---

### [BE-06] [MÉDIO] Sem controle otimista de concorrência

**Camada:** Backend (+ Integração em INT-12)  
**Arquivo(s):** `{BE}/model/Gasto.java`, `Obra.java`, `Funcionario.java`; `{BE}/dto/GastoDto.java`, `ObraDto.java`; `{BE}/service/GastoService.java`, `ObraService.java`, `ObraFinanceiroService.java`; `{BE}/service/FuncionarioPermissaoService.java`; `{BE}/config/GlobalExceptionHandler.java`  
**Descrição:** *(BE 3.8)* Dois gestores editando o mesmo gasto/obra ou ajustando `totalRecebido` fazem *last write wins* silencioso. `FuncionarioPermissaoService.atualizar` faz `deleteByFuncionarioId` + `saveAll`: duas chamadas concorrentes violam `uk_funcionario_permissao` (500). O bloqueio pessimista de `Gasto` (`buscarParaAtualizar`) só cobre gasto/reembolso.  
**Impacto:** perda de atualização em dados financeiros.  
**Instrução para o Copilot (depende de DB-06):** ativar `@Version` (já mapeado em DB-06) em `Gasto`, `Obra`, `Funcionario`; aceitar `versao` **opcional** em `GastoDto`/`ObraDto` — se vier e divergir da atual ⇒ 409 (`code=STALE_VERSION`, BE-02); se não vier, comportar-se como hoje (compatibilidade até INT-12). Em `FuncionarioPermissaoService.atualizar`, `flush()` entre o `delete` e o `saveAll` e tratar `OptimisticLockingFailureException`.  
**Restrições:** não tornar `versao` obrigatória enquanto o frontend não a enviar; não expor `versao` na resposta antes de INT-01 (DTOs).  
**Exemplo de correção:**
```java
if (dto.getVersao() != null && !dto.getVersao().equals(existente.getVersao())) {
    throw new Conflict("O registro foi alterado por outra pessoa. Recarregue e tente novamente.");
}
```

---

### [BE-07] [MÉDIO] Agregações feitas em memória e listas sem paginação

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/DashboardService.java`; `{BE}/service/ObraFinanceiroService.java`; `{BE}/service/PresencaService.java` (`listarPorObraEData`); `{BE}/repository/GastoRepository.java`; `{BE}/repository/PresencaRepository.java`  
**Descrição:** *(BE 3.10, BE 6.6)* O dashboard carrega **todos** os gastos da obra (`findByObraId`) e filtra/soma com streams; `ObraFinanceiroService` carrega todos os gastos só para somar; `listarPorObraEData` faz *stream* aninhado O(alocações × presenças) e `Collectors.toMap` sem validar chaves duplicadas. Com poucos anos de dados as respostas passam de MB e `-Xmx512m` fica no limite.  
**Impacto:** latência e memória crescem linearmente com o histórico da obra.  
**Instrução para o Copilot:** substituir por consultas agregadas (`select sum(...) … group by categoria/etapa/mês`, projeções) mantendo **exatamente** os mesmos números do `DashboardResponseDto`/`ObraFinanceiroDto`; indexar presença por `funcionarioId` num `Map` antes do cruzamento. Cobrir com testes comparando o resultado novo com o antigo em dados de exemplo. A paginação obrigatória das listas é tratada em INT-11 (depende do frontend).  
**Restrições:** não alterar nomes/formatos dos campos do dashboard; arredondamentos e pizza ficam em BE-11; não adicionar paginação aos endpoints de lista antes de INT-11 (o frontend os consome como array).  
**Exemplo de correção:**
```java
@Query("select coalesce(sum(g.valor), 0) from Gasto g where g.obra.id = :obraId")
BigDecimal somarPorObra(@Param("obraId") Long obraId);
```

---

### [BE-08] [CRÍTICO] Gestão de Notas Fiscais inexistente no backend (requisito essencial)

**Camada:** Backend  
**Arquivo(s):** novos `{BE}/model/NotaFiscal.java`, `{BE}/repository/NotaFiscalRepository.java`, `{BE}/service/NotaFiscalService.java`, `{BE}/controller/NotaFiscalController.java`, `{BE}/storage/ArquivoStorage.java` (+ `LocalArquivoStorage`); `{BE}/service/OcrService.java`; `{BE}/service/GastoService.java`  
**Descrição:** *(BE 4.1)* "Inserção e gestão de Notas Fiscais" é requisito **essencial** (`contexto.txt`), mas o OCR descarta o arquivo e `Gasto` não guarda número da NF, CNPJ ou anexo. A tabela é criada em DB-07.  
**Impacto:** não é possível organizar nem comprovar NFs para a contabilidade.  
**Instrução para o Copilot (depende de DB-07):** implementar a entidade e os endpoints: `POST /gastos/{gastoId}/notas-fiscais` (multipart: parte `arquivo` + campos opcionais `numero`, `cnpjEmitente`, `valor`, `dataEmissao`), `GET /gastos/{gastoId}/notas-fiscais`, `GET /notas-fiscais/{id}/arquivo` (download autenticado, `Content-Disposition: attachment`) e `DELETE /notas-fiscais/{id}` (ADMIN). Permissão: `CRIAR_GASTO`/`EDITAR_GASTO` + `exigirObra(obra do gasto)`. Validar por *magic bytes* (PDF, JPEG, PNG), tamanho ≤ 10 MB, calcular SHA-256 (duplicidade ⇒ 409), validar CNPJ (dígitos verificadores) e gravar via `ArquivoStorage` (chave `notas/{obraId}/{gastoId}/{sha256}.{ext}`); implementação local agora, S3 em BE-23. Incluir `notasFiscais` (id, numero, cnpjEmitente, nomeArquivo) na exportação (BE-10).  
**Restrições:** não guardar binário no banco; não alterar o contrato do OCR (`/ocr/nota-fiscal`); reutilizar a validação de arquivo de SEG-15; a interface `ArquivoStorage` deve permitir trocar a implementação sem mudar controllers.  
**Exemplo de correção:**
```java
@PostMapping(value = "/gastos/{gastoId}/notas-fiscais", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAnyAuthority('CRIAR_GASTO','EDITAR_GASTO')")
public ResponseEntity<NotaFiscalResponse> anexar(@PathVariable Long gastoId,
        @RequestParam("arquivo") MultipartFile arquivo,
        @RequestParam(required = false) String numero,
        @RequestParam(required = false) String cnpjEmitente) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.anexar(gastoId, arquivo, numero, cnpjEmitente));
}
```

---

### [BE-09] [CRÍTICO] Listagem de gastos sem filtro de data (requisito essencial)

**Camada:** Backend  
**Arquivo(s):** `{BE}/controller/GastoController.java`; `{BE}/service/GastoService.java`; `{BE}/repository/GastoFiltro.java`  
**Descrição:** *(BE 4.1, FE Q8 parte)* "Filtro de data relacionado aos gastos" é essencial, mas `GET /gastos` e `/gastos/paginado` não aceitam `from/to`; só o dashboard filtra por período. O frontend filtra o período **no cliente**, então o total "Mostrando X de Y" reflete só o que foi baixado.  
**Impacto:** requisito não atendido; base para a exportação (BE-10) e para a paginação no servidor (INT-11).  
**Instrução para o Copilot:** adicionar os parâmetros opcionais `from` e `to` (`@DateTimeFormat(iso = ISO.DATE)`, inclusivos) a `GET /gastos` e `GET /gastos/paginado`, aplicados sobre `dtGasto` em `GastoFiltro`; `from > to` ⇒ 400. Semântica idêntica à do dashboard (`from`/`to` inclusivos).  
**Restrições:** **apenas adicionar parâmetros opcionais** (o frontend atual não os envia); não mudar o formato da resposta; manter o teto/whitelist de `/gastos/paginado`.  
**Exemplo de correção:**
```java
if (from != null) preds.add(cb.greaterThanOrEqualTo(root.get("dtGasto"), from));
if (to != null)   preds.add(cb.lessThanOrEqualTo(root.get("dtGasto"), to));
```

---

### [BE-10] [CRÍTICO] Exportação de relatório de gastos para a contabilidade inexistente

**Camada:** Backend  
**Arquivo(s):** novo `{BE}/controller/GastoExportController.java`; novo `{BE}/service/ExportacaoGastosService.java`; `{BE}/service/RelatorioService.java`; `{BE}/model/Relatorio.java`; `jaf-backend/pom.xml` (Apache POI ou equivalente)  
**Descrição:** *(BE 4.1, FE R1, FE D6)* "Exportação de relatório de gastos" é requisito **essencial** e o envio à contabilidade é restrição do projeto. `Relatorio` é só `titulo + dtEmissao + funcionario`; não há geração de CSV/XLSX/PDF nem dependência no `pom.xml`. O que existe no frontend é um popup que imprime agregados do dashboard.  
**Impacto:** a dor central do cliente (consolidar dados para a contabilidade) não é atendida.  
**Instrução para o Copilot (depende de BE-09; decisão D-2):** criar `GET /gastos/export?obraId&from&to&etapa&categoria&metodoPagamento&funcionarioId&statusReembolso&formato=csv|xlsx` (`obraId` obrigatório). Permissão `GERAR_RELATORIO` + `exigirObra(obraId)`. Colunas: Data, Obra, Etapa, Categoria, Descrição, Método, Valor, Responsável, Reembolso (status/pago/saldo), NF (número/CNPJ) e linha de total. CSV em UTF-8 **com BOM**, separador `;`, valores decimais com vírgula; XLSX por streaming (SXSSF). **Neutralizar *CSV injection***: células de texto que comecem com `=`, `+`, `-` ou `@` recebem um apóstrofo inicial. Limite de 50 000 linhas (excedeu ⇒ 400 pedindo filtro). Resposta com `Content-Disposition: attachment; filename*=UTF-8''gastos-obra-{id}.csv` (o header já está exposto no CORS). Registrar cada exportação como `Relatorio` (quem, quando, filtros).  
**Restrições:** respeitar o escopo por obra do usuário; não carregar todos os gastos em memória (usar *stream*/paginação interna); PDF fica fora deste item; não alterar `GET /gastos`. O botão no frontend é FE-01.  
**Exemplo de correção:**
```java
@GetMapping("/gastos/export")
@PreAuthorize("hasAuthority('GERAR_RELATORIO')")
public ResponseEntity<StreamingResponseBody> exportar(@RequestParam Long obraId, /* filtros */ @RequestParam(defaultValue = "csv") String formato) {
    acesso.exigirObra(obraId);
    return service.exportar(obraId, filtros, formato); // ContentType + Content-Disposition
}
static String protegerCsv(String v) { return v != null && v.matches("^[=+\\-@].*") ? "'" + v : v; }
```

---

### [BE-11] [MÉDIO] Dashboard: contrato de acesso contraditório e números/rótulos imprecisos

**Camada:** Backend (+ Integração em INT-07)  
**Arquivo(s):** `{BE}/controller/DashboardController.java`; `{BE}/service/DashboardService.java`; `{BE}/dto/DashboardResponseDto.java`  
**Descrição:** *(BE 4.2, BE 4.11)* O `@PreAuthorize` libera quem tem `VISUALIZAR_GASTOS` (inclusive ENGENHEIRO e RA), mas o service chama `acesso.exigirAdmin()` e lança 403 depois de passar pelo controller; o frontend esconde o menu só para não-ADMIN, o que **mascara** a contradição. Percentuais inteiros podem passar de 100; "Pendentes" = `100 − Concluídos` (soma sempre 100 mesmo com arredondamento); a série mensal agrupa por `YearMonth` mas rotula só o nome do mês (jan/2025 e jan/2026 se confundem); `new Locale("pt","BR")` está deprecated; categorias de imprevistos fixas no código (`CATEGORIAS_IMPREVISTOS`).  
**Impacto:** 403 tardio; gráficos confusos em obras de mais de um ano.  
**Instrução para o Copilot (decisão D-6):** (1) expressar a regra em um só lugar: `@PreAuthorize("@acessoObraService.isAdmin()")` no controller (mantendo `exigirObra`); (2) rótulo do mês com ano (`"jan/2026"`, `DateTimeFormatter.ofPattern("MMM/yyyy", Locale.forLanguageTag("pt-BR"))`) mantendo o nome do campo `mes`; (3) adicionar a cada item de `reembolsosPizza` o campo **novo** `tipo` (`CONCLUIDO`|`PENDENTE`) mantendo `name`/`value`; (4) calcular os percentuais pela mesma fórmula e garantir que `Concluídos + Pendentes` some 100 pelo método do maior resto; **não** limitar `progressoSaldo` a 100 (valores > 100 indicam gasto acima do recebido; o frontend deve exibir alerta, FE-11).  
**Restrições:** não renomear campos existentes (`gastosEtapa`, `progressoEtapa`, `reembolsosPendentes`, `saldoRestante`, `progressoSaldo`, `reembolsosPizza[name,value]`, `gastosImprevistos[mes,valor]`, `gastosPorCategoria[categoria,valor]`); não mudar o default de `etapa` aqui (INT-07).  
**Exemplo de correção:**
```java
private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM/yyyy", Locale.forLanguageTag("pt-BR"));
```
```java
@GetMapping("/stats")
@PreAuthorize("@acessoObraService.isAdmin()")
```

---

### [BE-12] [MÉDIO] Alocação: `dtFim` ignorada nas permissões, sem como encerrar, `atualizar` troca funcionário/obra

**Camada:** Backend  
**Arquivo(s):** `{BE}/repository/AlocacaoObraRepository.java`; `{BE}/service/AcessoObraService.java`; `{BE}/service/GastoService.java`; `{BE}/service/ObraService.java`; `{BE}/service/AlocacaoObraService.java`; `{BE}/dto/AlocacaoObraDto.java`; `{BE}/controller/AlocacaoObraController.java`  
**Descrição:** *(BE 4.4)* A V11 introduziu `dt_fim` e a presença a respeita (`alocadoNaData`), mas o **acesso à obra** (`exigirObra`, `ObraService.listarPorUsuario`, `GastoService`) usa `existsByFuncionarioIdAndObraId` e ignora a vigência: quem foi desalocado continua vendo e lançando gastos. Nenhum endpoint define `dtFim` (o DTO só tem funcionário/obra/cargo). `atualizar` pode trocar funcionário e obra de uma alocação existente sem validar duplicidade nem presenças/gastos vinculados.  
**Impacto:** ex-funcionário com acesso a dados financeiros; histórico de presença incoerente.  
**Instrução para o Copilot:** (1) criar `existsAtiva(funcionarioId, obraId, hoje)` = alocação com `(dtInicio is null or dtInicio <= hoje) and (dtFim is null or dtFim >= hoje)` e usá-la em **todos** os pontos de escopo; (2) `AlocacaoObraDto` ganha `dtInicio`/`dtFim` **opcionais** (padrão: `dtInicio = hoje`); (3) novo `PATCH /alocacoes/{id}/encerrar` (`{"dtFim": "yyyy-MM-dd"}`, padrão hoje; `EDITAR_ALOCACAO` + `exigirObra`); reativar = limpar `dtFim` na mesma linha (o índice parcial de DB-02 impede duas ativas); (4) `atualizar` só altera `cargo`, `dtInicio`, `dtFim` — trocar `funcionarioId`/`obraId` ⇒ 400.  
**Restrições:** antes de implantar, listar alocações com `dt_fim` já preenchida (perderão acesso); o frontend atual não envia datas (usa o padrão); "hoje" vem de um bean `Clock` (`America/Sao_Paulo`) — **criar o bean neste item se ainda não existir** (BE-21 o reutiliza e estende o uso aos demais serviços); não alterar a regra de presença por data; a constraint `ex_alocacao_vigencia` (DB-02) rejeita períodos sobrepostos: `criar`/`reativar` devem checar sobreposição antes e responder 409 com mensagem clara.  
**Exemplo de correção:**
```java
@Query("select count(a) > 0 from AlocacaoObra a where a.funcionario.id = :f and a.obra.id = :o "
     + "and (a.dtInicio is null or a.dtInicio <= :hoje) and (a.dtFim is null or a.dtFim >= :hoje)")
boolean existsAtiva(@Param("f") Long f, @Param("o") Long o, @Param("hoje") LocalDate hoje);
```

---

### [BE-13] [MÉDIO] Gasto: autoria forjável, edição por qualquer alocado, sem auditoria nem validação de status da obra

**Camada:** Backend (+ Integração em INT-08)  
**Arquivo(s):** `{BE}/service/GastoService.java`; `{BE}/dto/GastoDto.java`; `{BE}/model/Gasto.java`, `Obra.java`, `AlocacaoObra.java`, `PagamentoReembolso.java`; novos `{BE}/config/JpaAuditingConfig.java`; `{BE-RES}/application.properties`  
**Descrição:** *(BE 4.5, BE 6.7 parte)* O "responsável" do gasto vem do corpo (`funcionarioId`): quem tem `CRIAR_GASTO` registra gasto em nome de **qualquer** colega alocado. `EDITAR_GASTO` altera valor/descrição de gastos de outras pessoas. Não há `criado_por/atualizado_por` nem histórico. Gasto em obra `CANCELADA`/`CONCLUIDA` é aceito. **[NOVO]** O frontend pré-seleciona como responsável a **primeira alocação** da lista (`alocacoes[0]`), não o usuário logado, então por padrão o gasto/reembolso é lançado em nome de outra pessoa.  
**Impacto:** impossível atribuir responsabilidade (ponto fraco para auditoria contábil); lançar "reembolso" em nome de terceiro cria dívida para outra pessoa.  
**Instrução para o Copilot (depende de DB-06):** (1) `@EnableJpaAuditing` + `AuditorAware<Long>` (id do usuário autenticado) e `@CreatedBy/@LastModifiedBy/@CreatedDate/@LastModifiedDate` nas entidades com colunas de DB-06; (2) regra de autoria controlada por `app.gasto.restringir-autor` (**padrão `false`** para não quebrar o frontend atual; INT-08 muda para `true`): quando `true`, quem não é ADMIN/RA só pode lançar/editar gastos com `funcionarioId` = próprio id (senão 403) e `funcionarioId` ausente assume o usuário; (3) obra `CANCELADA` ⇒ 409; obra `CONCLUIDA` ⇒ só ADMIN.  
**Restrições:** manter `funcionarioId` obrigatório no DTO enquanto a flag for `false`; não expor colunas de auditoria no JSON (INT-01 decide); o gasto continua exigindo funcionário alocado na obra (agora vigente, BE-12).  
**Exemplo de correção:**
```java
if (props.restringirAutor() && !acesso.isAdminOuRa()
        && !dto.getFuncionarioId().equals(acesso.atual().getId())) {
    throw new Forbidden("Você só pode lançar gastos em seu próprio nome.");
}
```

---

### [BE-14] [MÉDIO] Reembolso: sem estorno, dupla fonte de verdade e flag legada aceita

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/ReembolsoService.java`; `{BE}/controller/ReembolsoController.java`; `{BE}/model/PagamentoReembolso.java`; `{BE}/model/Gasto.java` (`reembolsoConcluido`); `{BE}/service/GastoService.java`; `{BE}/dto/GastoDto.java`; novo `{BE-RES}/db/migration/V23__estorno_pagamento_reembolso.sql`  
**Descrição:** *(BE 4.6, BE 3.5 parte)* Só existe `POST /gastos/{id}/reembolsos`; um pagamento lançado com valor/data errados é permanente e `GastoService.deletar` bloqueia gastos com pagamentos. O DTO ainda aceita `reembolsoConcluido` (400 só quando `true`) e o estado do reembolso fica duplicado em `gasto.reembolso_concluido` e na soma de `pagamento_reembolso`, recalculado à mão em `GastoService.atualizar` e `ReembolsoService.registrar`.  
**Impacto:** erro operacional vira dado contábil errado sem correção; as duas fontes podem divergir.  
**Instrução para o Copilot:** (1) `V23`: `pagamento_reembolso.cancelado_em TIMESTAMP`, `cancelado_por BIGINT REFERENCES funcionario(id)`, `motivo_cancelamento VARCHAR(500)`; (2) `POST /gastos/{id}/reembolsos/{pagamentoId}/estorno` (ADMIN, corpo `{motivo}` obrigatório) marca o pagamento como cancelado e recalcula; cancelados **não** entram em `totalPago`/`pagamentos[]` (adicionar `estornos[]` à resposta); (3) deixar de **gravar** `gasto.reembolso_concluido` — `getReembolsoConcluido()` passa a ser derivado do status (`PAGO`) para manter o JSON atual; (4) `GastoDto.reembolsoConcluido`: aceitar e **ignorar** (log em `DEBUG`) em vez de responder 400.  
**Restrições:** **não** dropar a coluna `reembolso_concluido` ainda (a contração ocorre em INT-17 depois que o frontend migrar); o bloqueio pessimista existente deve ser preservado; não editar pagamentos existentes (apenas estornar).  
**Exemplo de correção:**
```sql
ALTER TABLE pagamento_reembolso ADD COLUMN cancelado_em TIMESTAMP;
ALTER TABLE pagamento_reembolso ADD COLUMN cancelado_por BIGINT REFERENCES funcionario(id);
ALTER TABLE pagamento_reembolso ADD COLUMN motivo_cancelamento VARCHAR(500);
```
```java
@Transient
public Boolean getReembolsoConcluido() { return reembolsavel ? statusAtual == StatusReembolso.PAGO : null; }
```

---

### [BE-15] [MÉDIO] Permissões: lista personalizada substitui, lista vazia volta ao padrão, cargo nulo vira ENGENHEIRO

**Camada:** Backend (+ Integração em INT-06)  
**Arquivo(s):** `{BE}/service/FuncionarioPermissaoService.java`; `{BE}/service/CargoPermissaoService.java`; `{BE}/dto/FuncionarioDetalhesDto.java`; `{BE}/dto/FuncionarioPermissoesAcessoDto.java`; `{BE}/dto/AtualizarCargoPermissoesDto.java`; `{BE}/model/Funcionario.java`; novo `{BE-RES}/db/migration/V24__funcionario_permissoes_personalizadas.sql`  
**Descrição:** *(BE 4.7)* Se existir qualquer linha em `funcionario_permissao`, as permissões do cargo são **ignoradas** (substituição total); se o ADMIN salva lista **vazia** (para revogar tudo) as linhas são apagadas e o usuário **volta** às permissões padrão do cargo — impossível "zerar" alguém. Cargo `null` vira `ENGENHEIRO` silenciosamente. **[NOVO]** O frontend sempre envia o conjunto **completo** (`Permissoes.handleSalvar` e `handleCargoChange`), mesmo quando só o cargo muda: cada salvamento grava uma cópia congelada do padrão do cargo, e mudar depois `PUT /permissoes/cargos/{cargo}` não alcança esses usuários.  
**Impacto:** revogação de acesso não funciona como o ADMIN espera; matriz de cargos deixa de valer silenciosamente para quem já foi editado.  
**Instrução para o Copilot (depende de DB-04):** (1) `V24`: `funcionario.permissoes_personalizadas BOOLEAN NOT NULL DEFAULT FALSE`, preenchido `TRUE` onde já existirem linhas; (2) `permissoesDoFuncionario`: se `permissoes_personalizadas = true` usar **somente** as linhas (mesmo se vazias ⇒ nenhuma permissão); senão usar o padrão do cargo; (3) `atualizar(id, lista)`: validar cada nome contra `Permissao` (inválido ⇒ 400); se a lista for **igual** ao padrão do cargo ⇒ `permissoes_personalizadas=false` e apagar as linhas (herdar); caso contrário `true` e gravar; (4) remover o fallback `ENGENHEIRO` (cargo é `NOT NULL`, DB-04); (5) incluir `personalizada: boolean` na resposta de `GET/PUT /funcionarios/{id}/permissoes` e `/me/permissoes` (campo **novo**, o frontend atual ignora).  
**Restrições:** `DELETAR_OBRA/GASTO/FUNCIONARIO` continuam exclusivas do ADMIN (decisão de produto fora do escopo); incrementar `token_version` do usuário afetado (SEG-09) ao alterar permissões; manter `permissoes[]` e `cargo` no formato atual.  
**Exemplo de correção:**
```java
boolean igualAoPadrao = new HashSet<>(novas).equals(cargoPermissaoService.permissoesPorCargo(funcionario.getCargoGlobal()));
funcionario.setPermissoesPersonalizadas(!igualAoPadrao);
repo.deleteByFuncionarioId(id); repo.flush();
if (!igualAoPadrao) repo.saveAll(novas.stream().map(p -> new FuncionarioPermissao(funcionario, p)).toList());
```

---

### [BE-16] [MÉDIO] `totalRecebido` sobrescrito, sem histórico de recebimentos

**Camada:** Backend  
**Arquivo(s):** `{BE}/service/ObraFinanceiroService.java`; `{BE}/controller/ObraFinanceiroController.java`; novos `{BE}/model/RecebimentoObra.java`, `{BE}/repository/RecebimentoObraRepository.java`; `{BE}/model/Obra.java`  
**Descrição:** *(BE 4.9)* O "recebido" é um único valor editado por `PUT /obras/{id}/financeiro` (só ADMIN); o saldo do dashboard depende dele. Sem registro de cada recebimento (data, valor, origem, autor), um erro de digitação apaga o anterior sem rastro e o requisito desejável "entradas/saídas anuais" é inviável.  
**Impacto:** sem trilha e sem relatório anual.  
**Instrução para o Copilot (depende de DB-08):** implementar a entidade `RecebimentoObra`; novos endpoints `GET/POST /obras/{id}/recebimentos` e `DELETE` (estorno lógico), todos ADMIN; manter `PUT /obras/{id}/financeiro` **funcionando como hoje**: calcula a diferença para o total atual e grava um lançamento de ajuste (`descricao = "Ajuste via PUT financeiro"`, valor positivo ou negativo); `obra.total_recebido` passa a ser mantido como soma dos lançamentos **na mesma transação**.  
**Restrições:** não mudar o contrato de `GET/PUT /obras/{id}/financeiro` (`{totalRecebido,totalGasto,saldo}`); o frontend não terá UI de lançamentos nesta entrega (DOC-01); `valor` negativo representa estorno/ajuste (DB-08).  
**Exemplo de correção:**
```java
BigDecimal diferenca = novoTotal.subtract(obra.getTotalRecebido());
if (diferenca.signum() != 0) {
    recebimentos.save(new RecebimentoObra(obra, diferenca, LocalDate.now(clock), "Ajuste via PUT financeiro", acesso.atual()));
    obra.setTotalRecebido(novoTotal);
}
```

---

### [BE-17] [MÉDIO] Validação de entrada insuficiente e regras de obra/gasto incompletas

**Camada:** Backend (+ Integração em INT-03)  
**Arquivo(s):** `{BE}/dto/FuncionarioPerfilUpdateDto.java`, `FuncionarioDto.java`, `GastoDto.java`, `ObraDto.java`, `AtualizarCargoPermissoesDto.java`, `PresencaDto.java`; `{BE}/controller/FuncionarioController.java` (`atualizarCargo`), `CargoPermissaoController.java`; `{BE}/service/ObraService.java`; `{BE}/service/OcrService.java`  
**Descrição:** *(BE 2.12, BE 4.10)* `FuncionarioPerfilUpdateDto`: `nome` sem `@Pattern`, `email` sem `@Size`; `FuncionarioDto.nome` (`^[a-zA-ZÀ-ÿ\s]+$`) rejeita "D'Ávila", "Ana-Maria", "Jr."; `GastoDto.categoria/metodoPagamento/etapa` sem `@Size` (>255 ⇒ `DataIntegrityViolation` ⇒ 500); `PATCH /funcionarios/{id}/cargo` usa `Map<String,String>` sem `@Valid` (cargo inválido ⇒ 500); `PUT /permissoes/cargos/{cargo}` sem `@Valid`. Obra: a mensagem diz "término **posterior** ao início" mas a regra aceita datas iguais (`isBefore`); `atualizar` não revalida unicidade de título nem transições de status (`CONCLUIDA → EM_ANDAMENTO`); o título só aceita letras/números/espaço/hífen (rejeita "N.S. Aparecida", "Cond. São José / Bloco B"); orçamento `0` é aceito.  
**Impacto:** 500 para dados previsíveis, nomes reais rejeitados, dados sujos.  
**Instrução para o Copilot (decisão D-3):** relaxar `nome` para `^[\p{L}][\p{L}\s'.\-]+$` (3–100) e `titulo` para `^[\p{L}\p{N}\s\-.,()/'&]+$` (5–150); `@Size(max=255)` em `categoria`, `metodoPagamento`, `etapa`; DTO com `@NotNull Cargo cargo` para `PATCH /{id}/cargo` e `@Valid` nos `@RequestBody`; corrigir a mensagem de datas para "não pode ser anterior ao início"; `ObraService.atualizar` revalida título único (case-insensitive, excluindo o próprio id) e bloqueia reabertura de obra `CONCLUIDA/CANCELADA` para quem não é ADMIN; orçamento deve ser **> 0**; `OcrService.process` captura `InvalidMediaTypeException`.  
**Restrições:** só **relaxar** nome/título (nunca endurecer); o frontend passa a espelhar as mesmas regras em INT-03; não mudar o shape de `fields`. Combinar com SEG-20 (senha).  
**Exemplo de correção:**
```java
@Pattern(regexp = "^[\\p{L}\\p{N}\\s\\-.,()/'&]+$", message = "O título contém caracteres inválidos")
@Size(min = 5, max = 150) private String titulo;
```

---

### [BE-18] [MÉDIO] Domínio financeiro por strings mágicas e `Cargo` com 11 valores após a V8 reduzir para 3

**Camada:** Backend (fase "expandir"; contração em INT-04/INT-05)  
**Arquivo(s):** `{BE}/enums/Cargo.java`; novos `{BE}/enums/FuncaoObra.java`, `MetodoPagamento.java`, `StatusObra.java`; `{BE}/model/Gasto.java`, `Obra.java`, `AlocacaoObra.java`; `{BE}/service/GastoService.java`, `ReembolsoService.java`, `DashboardService.java`; `{BE}/repository/GastoFiltro.java`; `{BE}/dto/AlocacaoObraDto.java`, `GastoDto.java`, `ObraDto.java`  
**Descrição:** *(BE 1.4, BE 1.5, [NOVO])* `"REEMBOLSO"` é comparado (case-sensitive) em 8+ lugares; `"ETAPA 1"`, `"TODAS"`, `"IMPREVISTO"`, `"Custos extras"`, status de obra e categoria são `String` livre. O OCR sugere `PIX/CARTAO_DEBITO/CARTAO_CREDITO/DINHEIRO` e a seed usava `Débito/Crédito` (DB-09). A V8 migrou os cargos do sistema para 3 papéis, mas o enum `Cargo` mantém 11; `FuncionarioDto.cargo` aceita `PEDREIRO` e `AlocacaoObraDto.cargoNaObra` aceita até `ADMIN`. **[NOVO]** O frontend usa os valores legados como **função na obra** (`cargoNaObra: "GESTOR_OBRA"` em `NovaObra.tsx`; lista `ARQUITETO`, `MESTRE_DE_OBRAS`, `PEDREIRO`, … em `AlocacaoFuncionario.tsx`), enquanto o `UserContext` só aceita os 3 cargos do sistema.  
**Impacto:** `"reembolso"`/`"Reembolso "` fica fora dos relatórios; funcionário criado como `PEDREIRO` não consegue usar o app; remover valores do enum sem plano quebraria `NovaObra`.  
**Instrução para o Copilot (depende de DB-09):** (1) `Cargo` fica só com `ADMIN`, `RESPONSAVEL_ADMINISTRATIVO`, `ENGENHEIRO` **depois de** conferir no banco (`select distinct cargo_global from funcionario` e `select distinct cargo from alocacao_obra`) — migrar `alocacao_obra.cargo` para o novo enum **`FuncaoObra`** (`GESTOR_OBRA, ENGENHEIRO, ARQUITETO, MESTRE_DE_OBRAS, OPERADOR_LANCAMENTO, PEDREIRO, PINTOR, MARCENEIRO, GESSEIRO`), sem mudar a coluna (string); `AlocacaoObraDto.cargoNaObra` passa a `FuncaoObra`; (2) `MetodoPagamento` (`PIX, CARTAO_CREDITO, CARTAO_DEBITO, DINHEIRO, NAO_INFORMADO`) e `StatusObra` validados no DTO; **aceitar o literal legado `"REEMBOLSO"` como alias** (`reembolsavel=true`, método `NAO_INFORMADO`) enquanto o frontend não migrar; (3) substituir todas as comparações `"REEMBOLSO".equals(...)` por `gasto.isReembolsavel()` (`GastoFiltro`, `ReembolsoService`, `GastoService`, `DashboardService`); (4) expor `reembolsavel` no JSON (campo novo) **mantendo** `metodoPagamento: "REEMBOLSO"` para gastos reembolsáveis até INT-04.  
**Restrições:** não remover valores de enum usados por dados existentes sem a conferência (1); o JSON de `AlocacaoObra` continua com o campo `cargo` (o rename é INT-05); não mexer em categorias/etapas aqui (INT-04).  
**Exemplo de correção:**
```java
public enum FuncaoObra { GESTOR_OBRA, ENGENHEIRO, ARQUITETO, MESTRE_DE_OBRAS, OPERADOR_LANCAMENTO, PEDREIRO, PINTOR, MARCENEIRO, GESSEIRO }
```
```java
if ("REEMBOLSO".equalsIgnoreCase(dto.getMetodoPagamento())) { gasto.setReembolsavel(true); gasto.setMetodoPagamento("NAO_INFORMADO"); }
```
*(Obs.: durante a transição o getter JSON devolve `"REEMBOLSO"` quando `reembolsavel` for verdadeiro.)*

---

### [BE-19] [MÉDIO] Autenticação duplicada, rotas e serviços mortos

**Camada:** Backend  
**Arquivo(s):** `{BE}/controller/auth/AuthController.java`; `{BE}/service/AuthService.java`; `{BE}/service/FuncionarioService.java` (`autenticar`, `listar`); `{BE}/dto/FuncionarioTokenDto.java`; `{BE}/exceptions/NoContent.java`; `{BE}/config/SecurityConfiguracao.java`  
**Descrição:** *(BE 1.3, BE 5.6 parte)* `AuthController` expõe `POST /auth/login` que devolve só o `id` (sem token), **não está em `URLS_PERMITIDAS`** (inalcançável sem login e inútil com login), ignora flags de ativo e não usa `@Valid`. `FuncionarioService` injeta `GerenciadorTokenJwt` e `AuthenticationManager` apenas para `autenticar()` (ninguém chama); `listar()` também não é usado; `FuncionarioTokenDto.token` é sempre `null`; o matcher `POST /ocr` em `SecurityConfiguracao` é redundante. **Verificado:** o frontend só chama `/funcionarios/login`.  
**Impacto:** duas "verdades" de login e superfície de ataque sem uso.  
**Instrução para o Copilot:** remover `AuthController`, `AuthService`, `FuncionarioService.autenticar/listar`, as dependências não usadas do construtor de `FuncionarioService`, o campo `token` de `FuncionarioTokenDto` e o matcher redundante; ajustar testes. O login passa a usar um único `AuthService.login(email, senha)` que devolve o DTO e o token, e o controller só escreve o cookie (SEG-06).  
**Restrições:** manter `POST /funcionarios/login` e `/funcionarios/logout` com a mesma resposta (`{id,email,nome,cargo}` / 204); depende de SEG-06/SEG-07/SEG-09 (todos tocam o login).  
**Exemplo de correção:**
```java
public LoginResultado login(String email, String senha) {
    Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, senha));
    return new LoginResultado(dtoDe(auth), jwt.generateToken((FuncionarioDetalhesDto) auth.getPrincipal()));
}
```

---

### [BE-20] [BAIXO] Injeção por campo, `allow-bean-definition-overriding`, comentários históricos e configuração decorativa

**Camada:** Backend  
**Arquivo(s):** `{BE}/config/SecurityConfiguracao.java`; `{BE}/service/AutenticacaoService.java`; `{BE-RES}/application.properties`; `.env.example`; `{BE}/controller/GastoController.java`; vários `{BE}/**`  
**Descrição:** *(BE 1.7, BE 1.9)* `@Autowired` em campos; `spring.main.allow-bean-definition-overriding=true` esconde beans duplicados; `AutenticacaoFilter` é `@Bean` sem ser `@Component`; comentários "CORREÇÃO DE SEGURANÇA A0x … Antes/Agora" no código; nomes totalmente qualificados inline (`com.jaf.application.enums.Cargo`, `java.util.Map`); parâmetros `Authentication` não usados em `GastoController.listar/listarPaginado`; `@CrossOrigin` redundante; `.env.example` lista `JWT_REFRESH_EXPIRATION` sem uso; mistura PT/EN e indentação inconsistente nos DTOs.  
**Impacto:** o Spring não avisa de beans duplicados; configuração "decorativa" dá falsa sensação de controle.  
**Instrução para o Copilot:** converter para injeção por construtor; remover `allow-bean-definition-overriding` (se algo quebrar, é um bug real escondido — corrigir); remover comentários históricos (o Git guarda o histórico); trocar nomes qualificados por imports; remover parâmetros não usados; remover `JWT_REFRESH_EXPIRATION` do `.env.example` (CORS já tratado em SEG-11).  
**Restrições:** apenas refatoração sem mudança de comportamento; fazer depois de SEG-03…SEG-11 para evitar conflitos de merge.  
**Exemplo de correção:**
```java
public SecurityConfiguracao(AutenticacaoService autenticacaoService, AutenticacaoEntryPoint entryPoint) {
    this.autenticacaoService = autenticacaoService;
    this.autenticacaoJwtEntryPoint = entryPoint;
}
```

---

### [BE-21] [MÉDIO] Fuso horário: `LocalDate.now()` em UTC na AWS gera erro de regra

**Camada:** Backend + Infra  
**Arquivo(s):** `{BE}/service/AlocacaoObraService.java` (`criar`), `PresencaService.java` (`validarManual`, `alocadoNaData`, linhas com `isAfter(LocalDate.now())`), `ReembolsoService.java`; `{BE}/model/AlocacaoObra.java` (`@PrePersist`), `Funcionario.java` (`@PrePersist`); `{BE}/dto/GastoDto.java` (`@PastOrPresent`), `PresencaDto.java`; `{TF}/scripts/backend.sh`; `jaf-backend/Dockerfile`; `{BE-RES}/application.properties`  
**Descrição:** *(BE 6.2, FE P2 parte)* O `docker-compose` define `TZ=America/Sao_Paulo`, mas o `docker run` do Terraform não: a JVM roda em UTC. Depois das 21:00 (BRT) a "data de hoje" no servidor já é amanhã. **Caso concreto [NOVO]:** a alocação criada às 22:00 recebe `dtInicio = amanhã` (UTC); em seguida a presença de **hoje** (data calculada pelo frontend em `America/Sao_Paulo`) falha com "não está alocado na obra na data informada". As migrations (`CURRENT_DATE`) usam o fuso do banco (RDS = UTC).  
**Impacto:** erros intermitentes no fim do dia, difíceis de reproduzir.  
**Instrução para o Copilot:** (1) definir o fuso do negócio: `ENV TZ=America/Sao_Paulo` no `Dockerfile` e `-e TZ=America/Sao_Paulo -e JAVA_TOOL_OPTIONS=-Duser.timezone=America/Sao_Paulo` em `backend.sh`; (2) `spring.jackson.time-zone=America/Sao_Paulo` e `spring.jpa.properties.hibernate.jdbc.time_zone=America/Sao_Paulo`; (3) criar o bean `Clock` (`Clock.system(ZoneId.of("America/Sao_Paulo"))`) — **se BE-12 já o criou, apenas reutilizar** — e usar `LocalDate.now(clock)` em todos os services restantes; configurar o `ClockProvider` do Bean Validation para `@PastOrPresent`; (4) testes com `Clock.fixed` cobrindo 22:00 BRT.  
**Restrições:** o frontend já calcula "hoje" em `America/Sao_Paulo` (`dataHoje()`), portanto não alterar essa convenção; não converter datas existentes no banco; `@PrePersist` (sem injeção) deve usar `LocalDate.now(ZoneId.of("America/Sao_Paulo"))`.  
**Exemplo de correção:**
```java
@Bean Clock clock() { return Clock.system(ZoneId.of("America/Sao_Paulo")); }
// AlocacaoObraService
alocacao.setDtInicio(dto.getDtInicio() != null ? dto.getDtInicio() : LocalDate.now(clock));
```

---

### [BE-22] [MÉDIO] OCR assíncrono: jobs presos, resultado perdido, retenção de dados e endpoint legado sem timeout

**Camada:** Backend (+ worker `{OCR}`)  
**Arquivo(s):** `{BE}/service/OcrService.java`; `{BE}/config/RabbitMQConfig.java`; `{BE}/controller/OcrController.java`; `{BE}/model/OcrProcessamento.java`; `{BE-RES}/application.properties`; novo `{BE-RES}/db/migration/V25__ocr_solicitante_id.sql`; `{OCR}/app/infrastructure/messaging/consumer.py`  
**Descrição:** *(BE 6.4, BE 5.5)* Se o worker nunca responder, o job fica em `PROCESSANDO` para sempre; o listener de resultados tem retry (3×) e, esgotado, a mensagem é rejeitada **sem DLQ** em `ocr.resultado`; o arquivo (até 10 MB) viaja em Base64 dentro da mensagem AMQP; `ocr_processamento.texto_bruto` (CNPJ, nomes) é retido sem prazo (LGPD); `solicitante` é o **e-mail** (se mudar, o usuário perde o job); a mensagem de erro do worker é copiada à resposta (`"Erro ao processar...: " + erro`). `POST /ocr` (legado, exige `CRIAR_OBRA`) repassa arquivo e `Content-Type` do cliente via `RestTemplate` **sem timeout**, sem validar tipo/tamanho, e devolve o `JsonNode` cru de terceiros; usa a parte `file` (o assíncrono usa `arquivo`).  
**Impacto:** UX presa em "Processando…" (o frontend desiste após 2 min), acúmulo de dados pessoais, thread presa se o OCR travar.  
**Instrução para o Copilot:** (1) `@Scheduled` que marca como `ERRO` jobs `PROCESSANDO` há mais de `app.ocr.timeout` (padrão 3 min) com mensagem amigável; (2) DLQ para resultados: como argumentos de fila **não podem** ser alterados em fila existente (PRECONDITION_FAILED), criar uma fila nova (`ocr.resultado.v2` + `ocr.resultado.dlq`) e migrar o consumidor/worker; (3) `V25`: `ocr_processamento.solicitante_id BIGINT` (preenchido por join com e-mail), consultas por id; (4) job de retenção que zera `texto_bruto` após 30 dias; (5) mensagem ao usuário genérica ("Não foi possível ler a nota. Tente outra foto.") e detalhe só no log; (6) remover `POST /ocr` legado (o frontend usa `/ocr/nota-fiscal`) ou protegê-lo com `CRIAR_GASTO`; (7) `RestClient` com *connect* 3 s e *read* 30 s.  
**Restrições:** manter 200 + `status=ERRO` para arquivo inválido em `/ocr/nota-fiscal` (o frontend depende disso); não mudar os campos de `OcrResponseDto`; coordenar a troca de fila com o worker Python (mesmo PR/deploy); o Base64 → referência em storage é melhoria opcional posterior (BE-23).  
**Exemplo de correção:**
```java
@Scheduled(fixedDelay = 60_000)
@Transactional
public void expirarJobsPresos() {
    LocalDateTime limite = LocalDateTime.now(clock).minus(props.timeout());
    repo.findByStatusAndCriadoEmBefore(StatusOcr.PROCESSANDO, limite)
        .forEach(j -> j.marcarErro("Tempo de processamento excedido."));
}
```

---

### [BE-23] [MÉDIO] Uploads em disco local com backend atrás de ALB/Auto Scaling

**Camada:** Backend + Infra  
**Arquivo(s):** `{BE}/controller/FuncionarioPerfilController.java`; `{BE}/config/StaticResourceConfig.java`; novo `{BE}/storage/S3ArquivoStorage.java`; `{TF}/scripts/backend.sh`; `{TF}/modules/ec2/main.tf`; `docker-compose.yml`  
**Descrição:** *(BE 6.1)* As fotos ficam no diretório relativo `uploads/` do container. O `docker-compose` monta volume, mas o `docker run` do Terraform **não monta nada** e o módulo EC2 usa Auto Scaling com 2 instâncias: cada instância tem seu disco efêmero.  
**Impacto:** a foto subida na instância A dá 404 quando o ALB roteia para B (metade das requisições) e some a cada substituição da instância; o mesmo problema afetaria as NFs (BE-08).  
**Instrução para o Copilot:** implementar `S3ArquivoStorage` (AWS SDK v2) atrás da interface `ArquivoStorage` (BE-08), guardando só a **chave** no banco, e servir as fotos por um controller autenticado em `GET /uploads/fotos/{arquivo}` que lê do storage (**mantendo a URL** `/uploads/fotos/<uuid>.<ext>` que o frontend e o nginx já usam). Terraform: bucket S3 privado (SSE habilitado), *instance profile*/role do backend com acesso ao bucket e `-e STORAGE_BUCKET`. Alternativa se S3 não estiver disponível no ambiente: EFS montado em `/app/uploads`.  
**Restrições:** não mudar a URL pública das fotos nem a resposta `{url}` do upload; manter `LocalArquivoStorage` para dev/compose; migrar as fotos existentes (copiar `uploads/fotos/*` para o bucket) no runbook.  
**Exemplo de correção:**
```java
public interface ArquivoStorage {
    void gravar(String chave, InputStream dados, long tamanho, String contentType);
    Resource ler(String chave);
    void remover(String chave);
}
```

---

### [BE-24] [MÉDIO] Sem trilha de auditoria e quase sem observabilidade

**Camada:** Backend  
**Arquivo(s):** `jaf-backend/pom.xml`; `{BE-RES}/application.properties`; novo `{BE}/config/RequestIdFilter.java`; `{BE}/service/GastoService.java`, `ReembolsoService.java`, `FuncionarioService.java`, `FuncionarioPermissaoService.java`  
**Descrição:** *(BE 6.7)* Num sistema financeiro nenhuma alteração de gasto, pagamento de reembolso, permissão ou cargo é auditada (quem, quando, valor anterior). Logs são texto livre sem correlação; sem métricas; o `health` só tem banco.  
**Impacto:** incidentes sem diagnóstico; impossível responder "quem mudou este valor?" para a contabilidade.  
**Instrução para o Copilot (depende de BE-13):** (1) logger dedicado `AUDIT` com um evento estruturado (usuário, ação, entidade, id, antes/depois) em criar/editar/excluir gasto, registrar/estornar pagamento, alterar cargo/permissões e ativar/desativar; (2) `RequestIdFilter` gera/propaga `X-Request-Id`, grava no MDC e devolve o header; incluir `requestId` no envelope de erro (BE-02) e no padrão de log; (3) expor `prometheus` (Micrometer) em `management.endpoints.web.exposure.include=health,prometheus` protegido por `ADMIN`; (4) opcional: Hibernate Envers em `Gasto`, `PagamentoReembolso` e `Funcionario`.  
**Restrições:** **não** mover o *management* para outra porta nem proteger `/actuator/health/**` (o ALB usa `GET :8080/actuator/health/readiness`); não registrar senha/token/hash nos eventos.  
**Exemplo de correção:**
```java
private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
AUDIT.info("acao=GASTO_ATUALIZADO usuario={} gasto={} valorAnterior={} valorNovo={}", user.getId(), id, antes, depois);
```

---

### [BE-25] [BAIXO] Testes não cobrem os pontos de maior risco

**Camada:** Backend  
**Arquivo(s):** `{BE-TEST}/**` (novos: `SegurancaMatrizApiTest.java`, `FusoHorarioTest.java`, `ReembolsoConcorrenciaTest.java`, `MigracoesPostgresTest.java`); `jaf-backend/pom.xml`  
**Descrição:** *(BE 6.8)* Há testes de service e alguns de API, mas nenhum cobre vazamento aninhado de `senha`, IDOR de alocações/relatórios, isolamento por obra em todos os endpoints, 401 × 403, fuso horário, concorrência (reembolso em paralelo) nem migrations em Postgres real (parte usa H2, que difere em tipos, `ILIKE`, `lower()`).  
**Impacto:** regressões de segurança passam despercebidas.  
**Instrução para o Copilot:** consolidar os testes introduzidos nos itens anteriores sob Testcontainers (DB-01): matriz parametrizada **papel × endpoint × obra alocada/não alocada**; teste de contrato do JSON sem `senha` (SEG-01); 401 sem cookie e 403 sem permissão (SEG-03); fuso `22:00 BRT` com `Clock.fixed` (BE-21); duas threads registrando reembolso simultâneo (lock pessimista); `validate` do schema após todas as migrations.  
**Restrições:** não depender de H2 em novos testes; não reduzir cobertura existente; manter o tempo de CI razoável (um único contêiner Postgres reutilizado).  
**Exemplo de correção:**
```java
@Testcontainers
@SpringBootTest
class MigracoesPostgresTest {
    @Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16");
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }
    @Test void contextoSobeComValidate() {}
}
```

---

### [BE-26] [BAIXO] `PUT /funcionarios/{id}` exige senha que ignora e OpenAPI incoerente

**Camada:** Backend  
**Arquivo(s):** `{BE}/dto/FuncionarioDto.java`; novo `{BE}/dto/FuncionarioUpdateRequest.java`; `{BE}/controller/FuncionarioController.java`; `{BE}/service/FuncionarioService.java` (`atualizar`); `{BE}/config/swagger/OpenApiConfig.java`; controllers sem `@SecurityRequirement`  
**Descrição:** *(BE 5.8, BE 5.7)* O DTO de criação é reutilizado na edição: `senha` é `@NotBlank` e validada, mas nunca aplicada. O esquema OpenAPI é `Bearer`, enquanto a aplicação autentica por **cookie**; `ReembolsoController` e `ObraFinanceiroController` não têm `@SecurityRequirement`; contato/licença são placeholders (`grupo6`, `MIT`). **Verificado:** o frontend **não** chama `PUT /funcionarios/{id}`.  
**Impacto:** confusão e risco de enviar senha em texto à toa; documentação enganosa.  
**Instrução para o Copilot:** criar `FuncionarioUpdateRequest {nome, email, cargo}` (sem `senha`) para `PUT /funcionarios/{id}`; declarar o esquema `cookieAuth` (`SecuritySchemeType.APIKEY`, `in = COOKIE`, nome `jwt`) e aplicá-lo a todos os controllers; documentar respostas de erro (`ApiError`) e preencher contato/licença reais.  
**Restrições:** não mudar a criação (`POST /funcionarios`) nem a regra "alterar cargo/ADMIN exige ADMIN"; sem impacto no frontend.  
**Exemplo de correção:**
```java
@SecurityScheme(name = "cookieAuth", type = SecuritySchemeType.APIKEY, in = SecuritySchemeIn.COOKIE, paramName = "jwt")
```

## FASE 4 — Integração (contratos, DTOs, endpoints)

> Cada item desta fase muda os **dois lados**. Siga sempre a ordem: (1) backend publica o novo campo/endpoint mantendo o antigo; (2) frontend migra; (3) só então remover o legado (quando indicado).

### [INT-01] [MÉDIO] Entidades JPA usadas como contrato de API (sem DTO de resposta)

**Camada:** Ambos (backend define; frontend sem mudança de formato)  
**Arquivo(s):** `{BE}/controller/GastoController.java`, `ObraController.java`, `AlocacaoObraController.java`, `RelatorioController.java`; novos `{BE}/dto/GastoResponse.java`, `ObraResponse.java`, `AlocacaoResponse.java`, `RelatorioResponse.java`, `FuncionarioResumo.java`, `PageResponse.java`; `{BE}/model/Obra.java` (`@JsonSerialize`); `{BE}/config/ObraSerializer.java`; `{BE-RES}/application.properties`  
**Descrição:** *(BE 5.1, BE 1.6, BE 2.1 parte definitiva, BE 3.4)* `Gasto`, `Obra`, `AlocacaoObra` e `Relatorio` são devolvidas diretamente (inclusive em `Page<Gasto>`): o JSON depende do mapeamento JPA (EAGER/LAZY), do `ObraSerializer` (que varia por papel e consulta o banco) e de campos internos. Isso causou o vazamento de SEG-01 e o N+1 de BE-04, e acopla o frontend ao schema. Já existe o padrão correto em `FuncionarioResponseDto`/`PresencaResponseDto`.  
**Impacto:** qualquer mudança na entidade quebra o frontend; risco de expor campo novo por acidente.  
**Instrução para o Copilot:** criar *records* de resposta **idênticos, campo a campo, ao contrato da tabela "Contrato mínimo consumido pelo frontend"** e mapeá-los nos services (dentro da transação, BE-03):
- `ObraResponse`: `id, titulo, status, dtInicio, dtTerminoPrevisto` + `orcamento` (**string**) e `totalRecebido` **somente se ADMIN** (`@JsonInclude(NON_NULL)`), + `versao`.
- `FuncionarioResumo`: `id, nome, email, cargoGlobal` (acrescentar `fotoUrl` apenas na alocação). **Nunca** `senha`, `createdAt` ou auditoria.
- `GastoResponse`: campos atuais + `funcionario: FuncionarioResumo`, `obra: {id, titulo}`, `reembolsoConcluido` (derivado), `reembolsavel` e `versao`.
- `AlocacaoResponse`: `id, cargo, dtInicio, dtFim, funcionario{…fotoUrl}, obra: ObraResponse`.
- `PageResponse<T>`: `content, number, size, totalElements, totalPages` (mesmos nomes do `Page` do Spring).
Remover `ObraSerializer` e `@JsonSerialize` de `Obra`; por fim `spring.jpa.open-in-view=false`. Criar **testes de contrato** que comparem as chaves JSON de cada resposta com a tabela do contrato.  
**Restrições:** manter `@JsonIgnore` em `Funcionario.senha` (SEG-01); não renomear nem remover nenhum campo consumido pelo frontend; `orcamento` continua string; `AlocacaoResponse.cargo` continua `cargo` (rename em INT-05); depende de BE-03 e BE-04. Não ligar OSIV off antes de **todos** os controllers retornarem DTO.  
**Exemplo de correção:**
```java
public record ObraResponse(Long id, String titulo, String status, LocalDate dtInicio, LocalDate dtTerminoPrevisto,
        @JsonInclude(NON_NULL) String orcamento, @JsonInclude(NON_NULL) BigDecimal totalRecebido, Long versao) {
    static ObraResponse de(Obra o, boolean admin) {
        return new ObraResponse(o.getId(), o.getTitulo(), o.getStatus(), o.getDtInicio(), o.getDtTerminoPrevisto(),
                admin ? o.getOrcamento().toPlainString() : null, admin ? o.getTotalRecebido() : null, o.getVersao());
    }
}
```

---

### [INT-02] [MÉDIO] Três formatos de erro e tratamento de erros inconsistente no frontend

**Camada:** Ambos  
**Arquivo(s):** `{BE}/config/GlobalExceptionHandler.java` (já concluído em BE-02); `{FE}/Utils/financeiro.ts` (`erroApi`); novo `{FE}/Utils/erro.ts`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/Obras/DetalhamentoObras.tsx` (`deletarObra`); `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`; `{FE}/Pages/Dashboard/Dashboard.tsx`; `{FE}/Pages/ControlePresenca/GestaoPresenca.tsx`; `{FE}/Pages/Auth/Login/Login.tsx` e `{FE}/Service/Auth/Login/authService.tsx`; `{FE}/Pages/Profile/Profile.tsx`; `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`  
**Descrição:** *(BE 5.3, FE I3, BE 5.2 parte)* O backend devolve `ErrorResponse`, `ValidationError (fields)` e o JSON padrão do Spring. O frontend só trata 401 globalmente; `erroApi` lê `message/mensagem/detail` mas **ignora `fields`** (só `Profile.tsx` o lê): num 400 de validação o usuário vê "Erro de validação nos campos" **sem saber qual campo**. `NovoFuncionario.tsx` troca 400 por "Dados inválidos…" e atribui qualquer 409 a "e-mail"; `AlocacaoFuncionario.tsx` mostra "Request failed with status code 403" (texto técnico em inglês); `deletarObra` mostra "Não foi possível excluir a obra" mesmo quando o servidor explica (409); `Dashboard.tsx` troca a página por "Erro ao carregar dados do dashboard."; o login não conhece 429.  
**Impacto:** o usuário não sabe se foi permissão (403), conflito (409: "gasto com pagamentos não pode ser excluído"), validação ou rede; suporte recebe "deu erro".  
**Instrução para o Copilot (depende de BE-02):** criar `tratarErroApi(error, fallback)` em `Utils/erro.ts` que devolve `{ status, code, mensagem, campos }` com o mapa: sem resposta/`ERR_NETWORK` → "Sem conexão com o servidor."; `ECONNABORTED` → "A operação demorou demais. Tente novamente."; 400 com `fields` → mensagem + `campos`; 403 → "Você não tem permissão para esta ação."; 404; **409 → mensagem do servidor**; 413 → "Arquivo muito grande (máx. 10 MB)."; 429 → "Muitas tentativas. Aguarde alguns minutos."; 5xx → "Erro no servidor. Tente novamente em instantes." Manter `erroApi` como *wrapper* (reexport) e substituir os `catch` listados; formulários passam a exibir `campos[nome]` ao lado do input (`NovoFuncionario`, `NovaObra`, `Profile`).  
**Restrições:** o 401 continua tratado no interceptor (SEG-03); nunca exibir `error.message` do axios; textos em PT-BR com acento; não alterar a estrutura JSON de erro do backend (apenas `code` foi adicionado).  
**Exemplo de correção:**
```ts
export function tratarErroApi(e: unknown, fallback = "Não foi possível concluir a operação.") {
  if (!isAxiosError(e)) return { status: 0, mensagem: fallback, campos: {} };
  const d = e.response?.data as { message?: string; mensagem?: string; detail?: string; fields?: Record<string, string> } | undefined;
  if (!e.response) return { status: 0, mensagem: e.code === "ECONNABORTED" ? "A operação demorou demais. Tente novamente." : "Sem conexão com o servidor.", campos: {} };
  const status = e.response.status;
  if (status === 403 && !d?.message) return { status, mensagem: "Você não tem permissão para esta ação.", campos: {} };
  if (status === 429) return { status, mensagem: "Muitas tentativas. Aguarde alguns minutos.", campos: {} };
  return { status, mensagem: d?.message ?? d?.mensagem ?? d?.detail ?? fallback, campos: d?.fields ?? {} };
}
```

---

### [INT-03] [MÉDIO] Validações do frontend divergem das do backend

**Camada:** Ambos  
**Arquivo(s):** novo `{FE}/Utils/validacao.ts` (+ `validacao.test.ts`); `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`; `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`; `{FE}/Pages/Obras/DetalhamentoObras.tsx` (edição de obra); `{FE}/Pages/Profile/Profile.tsx`; `{BE}/dto/FuncionarioDto.java`, `AlterarSenhaDto.java`, `ObraDto.java`, `FuncionarioPerfilUpdateDto.java`; `{BE-TEST}/ValidacaoContratoTest.java`  
**Descrição:** *(FE R2, BE 2.12/2.17 parte)* Senha de novo usuário: o front exige ≥8, maiúscula, minúscula e **qualquer** especial, **sem dígito**; o back exigia dígito e só `@$!%*?&`. Nova senha no perfil: outra regex (`≥6, A, a, 1`). Nome: só "não vazio" no front × 3–100 e regex no back. Título de obra (criar): "não vazio" × 5–150 e regex; (editar): `length ≥ 5`. Resultado: `Abcdefgh!` ou `Senha#2026` passam na tela e são recusadas pela API; "Edifício Aurora (Bloco A)" era bloqueado só no servidor; há **três políticas de senha** distintas.  
**Impacto:** o usuário só descobre no 400 e sem explicação; administrador confuso.  
**Instrução para o Copilot (depende de SEG-20 e BE-17):** centralizar em `Utils/validacao.ts` as mesmas regras do backend já corrigidas — **senha** `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$` com ≤ 72 bytes UTF-8; **nome** 3–100 e `^[\p{L}][\p{L}\s'.\-]+$` (flag `u`); **título** 5–150 e `^[\p{L}\p{N}\s\-.,()/'&]+$`; **e-mail** com ≤ 100; orçamento > 0 (já `valorDecimal`). Usar esse módulo em `NovoFuncionario`, `NovaObra`, edição de obra e `Profile` (substituir `SENHA_REGEX`). Criar um conjunto único de **casos de teste** (válidos/ inválidos) executado nas suítes do front (`vitest`) e do back (JUnit) para impedir nova divergência.  
**Restrições:** o backend continua sendo a fonte de verdade (o front só antecipa); não endurecer regras além do backend; mensagens iguais às do backend quando possível; não enviar `confirmacaoSenha` além do necessário (o backend ainda exige o campo).  
**Exemplo de correção:**
```ts
export const SENHA_FORTE = /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;
export const senhaValida = (s: string) => SENHA_FORTE.test(s) && new TextEncoder().encode(s).length <= 72;
export const nomeValido = (s: string) => /^[\p{L}][\p{L}\s'.\-]{2,99}$/u.test(s.trim());
export const tituloObraValido = (s: string) => /^[\p{L}\p{N}\s\-.,()/'&]{5,150}$/u.test(s.trim());
```

---

### [INT-04] [MÉDIO] Domínio do gasto por strings: `"REEMBOLSO"` como método, categorias e etapas fixas no cliente **[NOVO]**

**Camada:** Ambos (contrai o legado de DB-09/BE-18)  
**Arquivo(s):** novo `{BE}/controller/DominioController.java`; `{BE}/dto/GastoDto.java`; `{BE}/repository/GastoFiltro.java`; `{BE}/controller/GastoController.java`; novo `{BE-RES}/db/migration/V26__metodo_reembolso_contracao.sql`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Service/Gastos/gastoService.ts`; `{FE}/Pages/Dashboard/Dashboard.tsx`; novo `{FE}/Service/Dominio/dominioService.ts`; `{FE}/Constants/dominio.ts`  
**Descrição:** *(BE 1.4, FE P1, [NOVO])* O frontend envia `metodoPagamento: "REEMBOLSO"` quando o usuário marca "Reembolso ao funcionário" e, ao editar, mostra `PIX` no lugar (linha `metodoPagamento: gasto.metodoPagamento === "REEMBOLSO" ? "PIX" : …`): **grava `PIX` por cima do método real sem avisar**. O filtro "Reembolso" usa `metodoPagamento=REEMBOLSO`. Etapas (`ETAPA 1/2`) e categorias (`MATERIAL, MAO_DE_OBRA, TRANSPORTE, EQUIPAMENTO, IMPREVISTO`) são fixas em `DetalhamentoObras.tsx` e `Dashboard.tsx`; ao editar um gasto cuja etapa não está na lista (ex.: "Etapa 3" ou dados legados como `Pintura`, `Alvenaria`, `Custos extras`), o `<select>` mostra outro valor e **grava a mudança sem o usuário perceber**. Obras reais têm mais de duas etapas.  
**Impacto:** perda do método real de pagamento; corrupção silenciosa de etapa/categoria; filtros que excluem os dados legados.  
**Instrução para o Copilot (depende de DB-09 e BE-18), em 3 etapas implantáveis separadamente:**
1. **Backend (expandir):** `GastoDto` aceita `reembolsavel` (boolean); se vier, `metodoPagamento` é o método **real** (ou `NAO_INFORMADO`); o alias `"REEMBOLSO"` continua válido. `GET /gastos` e `/gastos/paginado` aceitam o filtro `reembolsavel=true|false`. Criar `GET /dominios` (autenticado) → `{ metodosPagamento:[{codigo,rotulo}], categorias:[{codigo,rotulo,imprevisto}], statusObra:[…] }` e `GET /obras/{id}/etapas` → etapas padrão ∪ etapas já usadas na obra.
2. **Frontend:** o checkbox passa a enviar `reembolsavel` **sem** alterar `metodoPagamento`; ao editar, carregar o método real (se `NAO_INFORMADO`, exibir "Não informado" e **exigir escolha** ao salvar); o filtro "Reembolso" usa `reembolsavel=true`; categorias/etapas vêm de `dominioService`; em edição, **preservar** o valor existente mesmo que não esteja na lista (inserir como opção extra "valor legado"). Remover `normalizarMetodoPagamento` (dados já normalizados em DB-09). O `Dashboard.tsx` usa as etapas de `/obras/{id}/etapas` + "TODAS".
3. **Contrair:** após o frontend em produção, `V26`: `UPDATE gasto SET metodo_pagamento = 'NAO_INFORMADO' WHERE metodo_pagamento = 'REEMBOLSO'`; remover o alias no backend, o campo `reembolsoConcluido` da entrada e o literal `"REEMBOLSO"` restante.

**Restrições:** não executar a etapa 3 antes de **todos** os clientes estarem na etapa 2 (o literal `"REEMBOLSO"` ainda é lido pelo frontend antigo); categorias legadas (`Cimento`, `Pintura`…) continuam exibidas como texto bruto; não renomear códigos que o dashboard usa (`IMPREVISTO`/`Custos extras`).  
**Exemplo de correção:**
```ts
// DetalhamentoObras.tsx — payload do gasto
const payload = {
  descricao, categoria, etapa, valor: valorDecimal(novoGasto.valor)!, dtGasto, funcionarioId, obraId,
  metodoPagamento: novoGasto.metodoPagamento,          // método real
  reembolsavel: novoGasto.reembolsoFuncionario,        // separado
};
```

---

### [INT-05] [MÉDIO] Cargo do sistema × função na obra misturados (`GESTOR_OBRA`, `cargoNaObra` × `cargo`) **[NOVO]**

**Camada:** Ambos  
**Arquivo(s):** `{BE}/dto/AlocacaoObraDto.java`; `{BE}/dto/AlocacaoResponse.java`; `{FE}/Service/Alocacoes/alocacaoService.ts`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`; `{FE}/Types/user.ts`; `{FE}/Constants/dominio.ts`; `{FE}/Context/UserContext.tsx` (referência)  
**Descrição:** *(BE 1.5 parte, [NOVO])* A requisição de alocação usa `cargoNaObra`, mas a resposta usa `cargo` (assimetria); os valores são os cargos **legados** (`GESTOR_OBRA`, `ARQUITETO`, `PEDREIRO`…) que a V8 retirou dos cargos do sistema, e o `UserContext` só aceita 3 cargos de sistema: um mesmo vocabulário serve a dois conceitos. `CARGO_LABELS` está duplicado em `AlocacaoFuncionario.tsx` e `Types/user.ts`.  
**Impacto:** qualquer limpeza do enum `Cargo` quebra `NovaObra` (`cargoNaObra: "GESTOR_OBRA"`); rótulos divergentes.  
**Instrução para o Copilot (depende de BE-18):** (1) backend: `AlocacaoResponse` passa a incluir **também** `funcaoNaObra` (mesmo valor de `cargo`) e `AlocacaoObraDto` aceita `funcaoNaObra` **ou** `cargoNaObra` (o segundo como alias); (2) frontend: `alocacaoService`, `AlocacaoFuncionario` e `NovaObra` enviam/leem `funcaoNaObra`; criar `FuncaoLabel` e a lista de funções em `Constants/dominio.ts` e manter `CargoLabel` **apenas** para os 3 cargos do sistema; (3) em release posterior, remover os campos `cargo`/`cargoNaObra` legados do contrato.  
**Restrições:** manter `cargo` e `cargoNaObra` até o frontend migrar; `UserContext` deve continuar validando somente os 3 cargos de sistema; não mudar valores de enum.  
**Exemplo de correção:**
```ts
// NovaObra.tsx
await alocacaoService.criar({ funcionarioId: Number(responsavelId), obraId: obraCriada.id, funcaoNaObra: "GESTOR_OBRA" });
```

---

### [INT-06] [MÉDIO] Permissões: matriz duplicada no cliente, fallback fabricado, salvamento em duas chamadas e promessas que a UI não cumpre

**Camada:** Ambos  
**Arquivo(s):** `{BE}/controller/FuncionarioController.java`; `{BE}/service/FuncionarioPermissaoService.java`; `{BE}/dto/ReembolsoResponse.java`; `{FE}/Service/Permissoes/permissaoService.ts`; `{FE}/Pages/Permissoes/Permissoes.tsx`; `{FE}/Service/Auth/Login/authService.tsx` (`podeRegistrarPagamento`); `{FE}/Components/Financeiro/ReembolsoModal.tsx`; `{FE}/Constants/dominio.ts`  
**Descrição:** *(FE R3, FE R4 parte, FE R8, FE D1, FE D3, FE D7, BE 4.7 parte)* (1) `permissaoService.ts` mantém `PERMISSOES_POR_CARGO` fixa (contrariando `MELHORIAS-BENEFICIARIO.md`) e `Permissoes.tsx` a usa como estado inicial e como **fallback**: se `GET /funcionarios/{id}/permissoes` falhar, mostra as permissões **padrão do cargo como se fossem as reais**; ao salvar, o ADMIN pode sobrescrever as personalizadas com o padrão. (2) `handleSalvar` faz `PATCH /cargo` e depois `PUT /permissoes` (não atômico; se a segunda falha o cargo já mudou e a tela fica incoerente); `handleCargoChange` substitui as permissões pelo padrão do novo cargo **sem avisar**; promover a ADMIN não pede confirmação. (3) O toast diz "O novo acesso entra no próximo login" — **falso no servidor** (permissões são lidas do banco a cada request, vale na hora); a UI do usuário afetado só atualiza ao recarregar. (4) A matriz exibe direitos sem tela (`VISUALIZAR_RELATORIO`, `EDITAR_FUNCIONARIO`, `DELETAR_FUNCIONARIO`) e `EDITAR_OBRA`/`GERAR_RELATORIO` do RA que a UI não exerce (resolvidos em INT-10/FE-01). (5) `authService.podeRegistrarPagamento()` só olha o cargo e o modal exibe o formulário a qualquer RA, mas o backend exige estar **alocado** na obra.  
**Impacto:** estado incoerente, perda de personalizações, escalada de privilégio por clique acidental e chamados de suporte por promessas falsas.  
**Instrução para o Copilot (depende de BE-15; decisão D-4):**
1. Backend: novo `PUT /funcionarios/{id}/acesso` com `{cargo, permissoes[], versao?}` **em uma transação** (muda cargo, aplica permissões — herdar/personalizar como em BE-15 —, incrementa `token_version`); resposta `{funcionarioId, cargo, permissoes[], personalizada}`. Manter os endpoints antigos. Adicionar `podeRegistrar` (boolean calculado para o usuário/obra) a `GET /gastos/{id}/reembolsos`.
2. Frontend: remover `PERMISSOES_POR_CARGO`, `getMapaPadraoPermissoes` e `getPermissoesPorCargo`; carregar a matriz só de `GET /permissoes/cargos` (estado "carregando"); em `selecionarUsuario`, **sem fallback**: em erro mostrar mensagem com "Tentar novamente" e **desabilitar** salvar; `handleSalvar` usa `PUT /acesso` e, em qualquer falha, recarrega o usuário; avisar quando `handleCargoChange` descartar personalização (`personalizada`) e **confirmar** promoção a ADMIN; trocar o toast para "Acesso atualizado. Vale imediatamente no servidor; a pessoa verá a mudança na tela ao recarregar a página."; marcar com o selo "sem tela" as permissões listadas em `PERMISSOES_SEM_TELA` (constante).
3. `ReembolsoModal.tsx`: exibir o formulário de pagamento só se `podeRegistrar` for verdadeiro e tratar 403 com mensagem clara.

**Restrições:** não remover `PUT /funcionarios/{id}/permissoes` nem `PATCH /cargo` do backend; o ADMIN não pode editar o próprio acesso (já bloqueado no front e no back); não conceder `DELETAR_*` a não-ADMIN.  
**Exemplo de correção:**
```java
@PutMapping("/{id}/acesso")
@PreAuthorize("@acessoObraService.isAdmin()")
public ResponseEntity<FuncionarioPermissoesAcessoDto> atualizarAcesso(@PathVariable Long id, @Valid @RequestBody AtualizarAcessoDto dto) {
    return ResponseEntity.ok(funcionarioPermissaoService.atualizarAcesso(id, dto)); // @Transactional
}
```
```ts
catch (e) {
  setErroCarga(true);          // sem fallback fabricado
  setPermissoesAtivas(new Set()); // e salvar fica desabilitado enquanto erroCarga
}
```

---

### [INT-07] [MÉDIO] Dashboard: período invertido, `etapa` por string e pizza lida por nome/índice **[NOVO]**

**Camada:** Ambos  
**Arquivo(s):** `{BE}/controller/DashboardController.java`; `{FE}/Pages/Dashboard/Dashboard.tsx`; `{FE}/Components/PeriodoFiltro/PeriodoFiltro.tsx`; `{FE}/Components/PeriodoFiltro/periodUtils.ts`; `{FE}/Service/Dashboard/dashboardService.ts`  
**Descrição:** *(FE I5, [NOVO])* No preset "Personalizado" o usuário pode escolher `Até` menor que `De`; o backend responde 400 e a tela mostra "Erro ao carregar dados do dashboard." **substituindo a página inteira**. O frontend envia `etapa="TODAS"` literal e lê a pizza por `item.name === "Pendentes"` e cores por **índice**. **Armadilha de integração:** o relatório de frontend sugere "omitir `etapa` quando for TODAS", mas o backend usa `@RequestParam(defaultValue = "ETAPA 1")`: omitir o parâmetro hoje faria o dashboard mostrar **só a Etapa 1** como se fosse "todas".  
**Impacto:** mensagem enganosa; qualquer renomeação no backend ("Pendentes", ordem) inverte o gráfico sem erro; "correção" ingênua mudaria os números.  
**Instrução para o Copilot (depende de BE-11):** (1) backend: mudar o default para `defaultValue = "TODAS"` (aceitar também o valor explícito `TODAS`); (2) frontend: `PeriodoFiltro` valida `de <= ate` (atributos `min/max` + checagem JS com erro inline e botão desabilitado) e **nunca** dispara a requisição com período invertido; (3) a pizza usa o campo `tipo` (`CONCLUIDO`/`PENDENTE`) publicado em BE-11 e cores por `tipo`; (4) continuar enviando `etapa` explicitamente (`TODAS` quando não filtrada) — só depois do passo 1 é seguro omiti-la.  
**Restrições:** não omitir `etapa` antes da mudança do default; não alterar os nomes dos campos do dashboard; manter o relatório/print como está (FE-01 o substitui).  
**Exemplo de correção:**
```java
@RequestParam(defaultValue = "TODAS") String etapa
```
```ts
const periodoValido = !de || !ate || de <= ate;
const corPizza = (t: "CONCLUIDO" | "PENDENTE") => (t === "CONCLUIDO" ? "#43B89C" : "#FF6584");
```

---

### [INT-08] [MÉDIO] Responsável do gasto: padrão = primeira alocação e qualquer alocado pode ser escolhido

**Camada:** Ambos  
**Arquivo(s):** `{FE}/Pages/Obras/DetalhamentoObras.tsx` (`novoGasto.funcionarioId`, select "Responsável"); `{BE-RES}/application.properties` (`app.gasto.restringir-autor`); `{BE}/service/GastoService.java`  
**Descrição:** *(FE R9, BE 4.5, [NOVO])* O formulário pré-seleciona `alocacoes[0].funcionario.id` (a primeira alocação da obra) e permite escolher qualquer alocado; o reembolso fica em nome dessa pessoa. O usuário logado não é o padrão. O backend ainda aceita autoria arbitrária (BE-13 deixou a restrição desligada para não quebrar o frontend).  
**Impacto:** gasto e **dívida de reembolso** lançados por padrão em nome de outra pessoa; fraude ou erro difícil de rastrear.  
**Instrução para o Copilot (depende de BE-13):** frontend: padrão `funcionarioId = user.id` (via `useUser`) **quando o usuário está entre as alocações**; senão vazio e **obrigatório escolher** (nunca `alocacoes[0]`); o select só aparece para ADMIN e RA; ENGENHEIRO vê o próprio nome somente leitura. Na edição preservar o responsável existente. Depois de implantado o frontend, ligar `app.gasto.restringir-autor=true` no backend.  
**Restrições:** ligar a flag **só depois** do frontend em produção (ENGENHEIRO receberia 403 com o frontend antigo); ADMIN/RA continuam podendo lançar em nome de outro alocado; não alterar a regra "funcionário precisa estar alocado".  
**Exemplo de correção:**
```ts
const euAlocado = alocacoes.some(a => a.funcionario.id === user?.id);
const funcionarioPadrao = euAlocado ? String(user!.id) : "";
```
```properties
app.gasto.restringir-autor=true
```

---

### [INT-09] [MÉDIO] Criar obra + alocar responsável são duas chamadas não atômicas **[NOVO]**

**Camada:** Ambos  
**Arquivo(s):** `{BE}/dto/ObraDto.java`; `{BE}/service/ObraService.java` (`criar`); `{BE}/controller/ObraController.java`; `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`; `{FE}/Service/Obras/obraService.ts`; `{FE}/Service/Alocacoes/alocacaoService.ts`  
**Descrição:** *(FE R7 parte, BE 4.3, [NOVO])* `NovaObra.tsx` cria a obra (`POST /obras`) e depois a alocação do responsável (`POST /alocacoes`). Se a segunda falha, a obra fica **sem gestor** e o fluxo depende de um toast e de uma navegação para completar manualmente (há código só para evitar repetir o POST). Para o RA, a obra criada nem aparece para ele se não estiver alocada (resolvido no backend em SEG-13).  
**Impacto:** obras órfãs e fluxo incompleto em rede móvel instável.  
**Instrução para o Copilot (depende de SEG-13 e BE-12):** backend: `ObraDto` ganha `responsavelId` **opcional**; `ObraService.criar` cria a obra e a alocação do responsável (`GESTOR_OBRA`, vigência a partir de hoje) **na mesma transação** (exige `CRIAR_ALOCACAO`; senão 403) e, se o criador não é ADMIN, aloca também o criador (sem duplicar). Frontend: `NovaObra.tsx` envia `responsavelId` em `obraService.criar` e **remove** a segunda chamada e o ramo de falha parcial; mantém o bloqueio de duplo envio (`useRef`).  
**Restrições:** manter `POST /alocacoes` (usado em `AlocacaoFuncionario`); sem `responsavelId` o comportamento é o atual; não mudar o formato da resposta de `POST /obras`.  
**Exemplo de correção:**
```ts
const obra = await obraService.criar({ titulo, orcamento: String(valorDecimal(orcamento)), status,
  dtInicio, dtTerminoPrevisto, responsavelId: podeAlocar ? Number(responsavelId) : undefined });
```

---

### [INT-10] [MÉDIO] Editar obra exige `orcamento`, que o RA nunca recebe **[NOVO]**

**Camada:** Ambos  
**Arquivo(s):** `{BE}/controller/ObraController.java`; `{BE}/service/ObraService.java` (`atualizar`); novo `{BE}/dto/ObraUpdateDto.java`; `{FE}/Pages/Obras/DetalhamentoObras.tsx` (`podeEditarObra`, modal de edição); `{FE}/Service/Obras/obraService.ts`  
**Descrição:** *(FE R4 parte, [NOVO])* O RA tem `EDITAR_OBRA`, mas `PUT /obras/{id}` valida `orcamento` (`@NotBlank`) e o `ObraSerializer` **oculta** o orçamento para não-ADMIN: o RA não tem o valor para reenviar. O frontend contorna com `podeEditarObra = admin`, escondendo o problema; a matriz de permissões promete algo que não funciona. Ao mesmo tempo, hoje um RA que enviasse um `orcamento` qualquer no `PUT` **sobrescreveria o orçamento** às cegas.  
**Impacto:** permissão sem efeito; risco de sobrescrever orçamento sem ser ADMIN.  
**Instrução para o Copilot:** backend: `PATCH /obras/{id}` com `ObraUpdateDto {titulo, status, dtInicio, dtTerminoPrevisto, versao?}` (sem orçamento), `EDITAR_OBRA` + `exigirObra`; `PUT /obras/{id}` passa a **recusar (403)** alteração de `orcamento` por não-ADMIN. Frontend: `podeEditarObra` = `can("EDITAR_OBRA")`; o modal só mostra/edita o orçamento para ADMIN; ADMIN continua usando `PUT`, os demais usam `PATCH`.  
**Restrições:** manter `PUT` com o mesmo contrato para ADMIN; revalidar título/datas com as regras de BE-17; `EDITAR_OBRA` deixa de ser "permissão sem efeito" na matriz.  
**Exemplo de correção:**
```java
@PatchMapping("/{id}")
@PreAuthorize("hasAuthority('EDITAR_OBRA')")
public ResponseEntity<ObraResponse> atualizarParcial(@PathVariable Long id, @Valid @RequestBody ObraUpdateDto dto) {
    return ResponseEntity.ok(obraService.atualizarParcial(id, dto));
}
```

---

### [INT-11] [MÉDIO] Filtro, ordenação e paginação 100% no cliente; N+1 na Home e total subestimado silenciosamente

**Camada:** Ambos  
**Arquivo(s):** `{BE}/controller/ObraController.java`, `GastoController.java`; `{BE}/dto/PageResponse.java`; novo `{BE}/dto/ObraResumoDto.java`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Pages/Home/Home.tsx`; `{FE}/Service/Gastos/gastoService.ts`; `{FE}/Service/Obras/obraService.ts`  
**Descrição:** *(FE Q8, FE I1, FE R5, FE D4, BE 5.4)* Cada obra baixa **todos** os gastos (com `funcionario` e `obra` aninhados) e ordena/filtra em JS a cada render (5 itens/página, fixo); com filtro de pessoa/status faz **duas** chamadas só para popular um `<select>`; o filtro de período é local, então "Mostrando X de Y" reflete só o que foi baixado. A documentação (`API-FINANCEIRO.md`, `RESUMO_PAGINACAO.md`) descreve os endpoints paginados como entregues, mas o frontend não os usa. `Home.tsx` faz 1 requisição de gastos (ADMIN) + 1 de alocações **por obra** (61 requisições com 30 obras) e calcula `totalGasto` somando `gasto.valor` em ponto flutuante; **se uma chamada falha (`allSettled → rejected`) ela é descartada e o total fica menor, sem aviso**; o "responsável" do card é a primeira alocação retornada. `Page<Entity>` é serializado direto (contrato instável).  
**Impacto:** lentidão em 3G/4G com milhares de gastos, total exibido pode divergir do servidor, e o dono vê um total subestimado achando que é completo.  
**Instrução para o Copilot (depende de BE-09 e INT-01):** backend: usar `PageResponse<T>` em `/gastos/paginado` e `/obras/paginado`; criar `GET /obras/resumo` (paginado) com `{id, titulo, status, dtInicio, dtTerminoPrevisto, responsavel{id,nome}, totalGasto}` (`totalGasto` **somente ADMIN**, agregado no banco, BE-07) e `GET /obras/{id}/pessoas` (`id, nome, funcaoNaObra`) para o filtro; limitar as listas legadas a 1000 itens (`X-Truncated: true`) e registrar `WARN`. Frontend: `gastoService.listarPaginado({obraId,page,size,sortBy,sortDir,from,to,…})`; `DetalhamentoObras` usa paginação/ordenação/filtro/período **no servidor** e o total real; o select de pessoas usa `/obras/{id}/pessoas`; `Home` usa `/obras/resumo` (1 requisição) e, se o total faltar, mostra **"Total indisponível"** (nunca um valor parcial).  
**Restrições:** manter os endpoints legados (array) até a migração; whitelist/teto de SEG-14/BE-09; `totalGasto` nunca para não-ADMIN; não calcular totais no cliente.  
**Exemplo de correção:**
```ts
const { data } = await api.get<PageResponse<Gasto>>("/gastos/paginado", {
  params: { obraId, page: pagina - 1, size: 10, sortBy: "dtGasto", sortDir: "desc", from, to, funcionarioId, statusReembolso },
});
```

---

### [INT-12] [MÉDIO] POSTs sem idempotência em rede móvel instável e edição concorrente silenciosa

**Camada:** Ambos  
**Arquivo(s):** novo `{BE}/config/IdempotencyFilter.java`; novo `{BE-RES}/db/migration/V27__idempotency_key.sql`; `{BE}/dto/GastoResponse.java`, `ObraResponse.java`; `{FE}/Service/Auth/Login/Api/Api.ts`; `{FE}/Service/Gastos/gastoService.ts`, `Obras/obraService.ts`, `Alocacoes/alocacaoService.ts`, `Financeiro/financeiroService.ts`, `Funcionarios/funcionarioService.ts`, `Presencas/presencaService.ts`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`  
**Descrição:** *(FE P3, BE 6.5, BE 3.8)* Só o pagamento de reembolso tem mitigação explícita (a documentação reconhece "sem chave de idempotência"). Os demais POSTs, em caso de *timeout* de 10 s com o servidor já tendo gravado, levam o usuário a tentar de novo e criar **gasto/obra duplicados**; `cadastrarGasto` protege com `useState` (não `useRef`, ao contrário de `NovaObra`), então o duplo toque rápido passa. Nenhuma edição envia a versão: *last write wins*.  
**Impacto:** gastos e obras duplicados, total gasto inflado; perda de edição concorrente.  
**Instrução para o Copilot (depende de BE-06):** backend: aceitar o header `Idempotency-Key` (UUID) em `POST /gastos`, `/obras`, `/alocacoes`, `/funcionarios`, `/presencas` e `/gastos/{id}/reembolsos`: tabela `idempotency_key(chave UUID, usuario_id, metodo, caminho, fingerprint CHAR(64), estado, status, corpo, criado_em)` com `UNIQUE (chave, usuario_id)`, onde `fingerprint` = SHA-256 de `metodo + caminho + corpo` e `estado ∈ {PENDENTE, CONCLUIDA}`; **inserir a chave com `PENDENTE` antes de executar** e marcar `CONCLUIDA` (gravando `status`/`corpo`) ao terminar; chave repetida com o **mesmo** fingerprint e `CONCLUIDA` ⇒ devolver a mesma resposta (mesmo status/corpo); com `PENDENTE` ⇒ `409` com `code=IDEMPOTENCY_IN_PROGRESS` (o frontend aguarda ~2 s e recarrega o recurso em vez de reenviar); fingerprint **diferente** ⇒ `422` com `code=IDEMPOTENCY_KEY_REUSED`; limpeza após 24 h. Incluir `versao` em `GastoResponse`/`ObraResponse`. Frontend: gerar `crypto.randomUUID()` ao abrir cada formulário, enviar em `Idempotency-Key` e **reutilizar a mesma chave no retry**, trocando-a apenas após sucesso; trava `useRef` em todos os submits; enviar `versao` em `PUT /gastos/{id}` e `PUT/PATCH /obras/{id}` e tratar `409 STALE_VERSION` com "Registro alterado por outra pessoa" + recarregar.  
**Restrições:** a chave é obrigatória **só** quando o frontend a envia (clientes antigos continuam funcionando); escopo por usuário; não repetir POST automaticamente no cliente.  
**Exemplo de correção:**
```ts
const chaveRef = useRef(crypto.randomUUID());
await api.post("/gastos", payload, { headers: { "Idempotency-Key": chaveRef.current } });
chaveRef.current = crypto.randomUUID(); // só após sucesso
```

---

### [INT-13] [MÉDIO] Presença usa "alternar" (toggle) em vez do estado desejado

**Camada:** Ambos  
**Arquivo(s):** `{FE}/Service/Presencas/presencaService.ts` (`alternarPresenca`); `{FE}/Pages/ControlePresenca/GestaoPresenca.tsx` (`salvarStatus`); `{FE}/Pages/ControlePresenca/ControlePresenca.tsx` (`registrarPresenca`); `{BE}/controller/PresencaController.java`  
**Descrição:** *(FE R6, FE D8)* Para um registro existente a UI envia `PATCH /presencas/{id}/alternar`, **ignorando** o valor `presente` calculado: o resultado depende do estado do servidor no momento (lista desatualizada, dois gestores, duplo clique com latência). `PUT /presencas/{id}` aceita o estado explícito e não é usado. A API aceita `horarioEntrada/horarioSaida` mas nenhuma tela os captura.  
**Impacto:** "marcar presente" pode virar ausente; afeta folha/dias trabalhados.  
**Instrução para o Copilot:** frontend: com registro existente usar `PUT /presencas/{id}` com `presente` explícito (idempotente); sem registro, `POST`; em 409 (já existe) recarregar a lista e repetir como `PUT`. Backend: manter `PATCH /alternar`, aceitando `?esperado=true|false` (divergência ⇒ 409) e marcá-lo como depreciado. (Opcional) campos de horário no `GestaoPresenca`.  
**Restrições:** não remover `PATCH /alternar` ainda; manter as regras de alocação vigente (BE-12/BE-21); usar a data em `America/Sao_Paulo`.  
**Exemplo de correção:**
```ts
if (colaborador.id) await presencaService.atualizar(colaborador.id, { funcionarioId, obraId, data, presente: novoStatus });
else await presencaService.registrar({ funcionarioId, obraId, data, presente: novoStatus });
```

---

### [INT-14] [MÉDIO] Fluxo de alocação incompleto, rotas redundantes e "responsável" arbitrário

**Camada:** Ambos  
**Arquivo(s):** `{FE}/Service/Alocacoes/alocacaoService.ts`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/Home/Home.tsx`; `{BE}/controller/AlocacaoObraController.java`  
**Descrição:** *(FE R7, BE 5.6)* O requisito "associar funcionários às obras" só tem **criar** e listar no frontend; o backend tem `PUT/DELETE /alocacoes/{id}` e as permissões `EDITAR_ALOCACAO`/`DELETAR_ALOCACAO`, sem botão. A presença exige alocação vigente na data e a UI nunca envia datas; o filtro de período usa `dtInicio` (opcional): alocações sem data somem quando um período é escolhido; o "funcionário responsável" do card da Home é a **primeira** alocação. Existem quatro formas de listar alocações por obra (`/alocacoes/obra/{id}`, `/alocacoes/filtro?obraId=`, `/obras/{id}/alocacoes`, `/alocacoes`).  
**Impacto:** não é possível desalocar nem corrigir função/vigência pela interface; o responsável exibido pode não ser o gestor; manutenção divergente entre rotas equivalentes.  
**Instrução para o Copilot (depende de BE-12, INT-05, INT-11):** frontend: usar a rota canônica `GET /obras/{id}/alocacoes`; adicionar **editar função/vigência**, **encerrar alocação** (`PATCH /alocacoes/{id}/encerrar`) e **remover** (`DELETE`, com confirmação) condicionados a `EDITAR_ALOCACAO`/`DELETAR_ALOCACAO`; campos opcionais de vigência; tratar `dtInicio` nulo como "sem restrição" no filtro de período; o responsável da Home vem de `responsavel` em `/obras/resumo` (alocação `GESTOR_OBRA`). Backend: marcar `/alocacoes/obra/{id}` e `/alocacoes/filtro` como depreciadas (mesmas regras de escopo).  
**Restrições:** não remover as rotas antigas nesta entrega; ações só para quem tem a permissão; exibir mensagem do servidor em erros de vigência.  
**Exemplo de correção:**
```ts
encerrar: async (id: number, dtFim?: string) => (await api.patch(`/alocacoes/${id}/encerrar`, { dtFim })).data,
listarPorObra: async (obraId: number) => (await api.get(`/obras/${obraId}/alocacoes`)).data,
```

---

### [INT-15] [MÉDIO] Timeout global de 10 s também para upload e OCR, sem validação de arquivo; polling sem controle

**Camada:** Ambos (frontend + nginx)  
**Arquivo(s):** `{FE}/Service/Auth/Login/Api/Api.ts`; `{FE}/Service/Gastos/ocrService.ts`; `{FE}/Service/Funcionarios/funcionarioService.ts` (`uploadFoto`); `{FE}/Service/Ocr/ocrService.ts` (morto); `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE-ROOT}/nginx.conf`  
**Descrição:** *(FE I2, FE I6)* A foto de nota fiscal (JPEG de celular, 3–8 MB) em rede móvel pode levar mais de 10 s e o axios aborta com "Não foi possível processar a nota fiscal". O OCR não valida tipo/tamanho no cliente; arquivo > 10 MB retorna 413 do nginx com mensagem genérica. Há **dois** serviços de OCR (um morto, apontando para o `/ocr` legado) e o polling a cada 2 s por 2 min não respeita backoff, não cancela e continua após o usuário sair da tela.  
**Impacto:** falhas intermitentes exatamente no fluxo *mobile* mais valioso; carga fixa se o OCR estiver lento.  
**Instrução para o Copilot (depende de BE-22):** frontend: `timeout: 120000` apenas em `processarNotaFiscal` e `uploadFoto`; validar tipo (`image/jpeg|png|webp`, `application/pdf`) e tamanho (≤ 10 MB; foto de perfil ≤ 5 MB) antes de enviar; comprimir imagens > 2 MB no cliente (canvas, lado maior 2000 px, JPEG 0,8); polling com *backoff* (2 s → 3 s → 5 s, máx. 10 s) e `AbortSignal` cancelado ao desmontar; tratar 413 (INT-02); remover `Service/Ocr/ocrService.ts`. nginx: `client_max_body_size 11m` (margem do multipart).  
**Restrições:** não alterar o contrato `{jobId,status,…}`; manter o limite de 2 min total; não mudar o timeout global de 10 s das demais chamadas.  
**Exemplo de correção:**
```ts
export const processarNotaFiscal = (arquivo: File, signal?: AbortSignal) => {
  const form = new FormData(); form.append("arquivo", arquivo);
  return api.post("/ocr/nota-fiscal", form, { timeout: 120_000, signal });
};
```

---

### [INT-16] [MÉDIO] Fuso horário e virada de dia: datas calculadas e exibidas de formas diferentes

**Camada:** Ambos  
**Arquivo(s):** novo `{FE}/Utils/data.ts`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Pages/ControlePresenca/GestaoPresenca.tsx`, `ControlePresenca.tsx`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Components/Financeiro/ReembolsoModal.tsx`; `{FE}/Components/PeriodoFiltro/periodUtils.ts`  
**Descrição:** *(FE P2, BE 6.2 parte)* `new Date(\`${data}T00:00:00\`).toLocaleDateString("pt-BR", {timeZone: "America/Sao_Paulo"})` interpreta no fuso do navegador e depois converte: num dispositivo em fuso à frente de SP a data exibida **volta um dia**; `AlocacaoFuncionario` não usa `timeZone`, então as telas divergem; `ReembolsoModal` exibe `Gasto em 2026-10-30` e `p.dataPagamento` em ISO. `useState(dataHoje())` congela "hoje" ao abrir a tela: uma tela aberta de um dia para o outro (comum no celular) valida gasto/presença com a data de ontem.  
**Impacto:** datas de gasto/presença exibidas ou validadas erradas em um sistema contábil.  
**Instrução para o Copilot (depende de BE-21):** criar `Utils/data.ts` com `formatarDataIso(iso)` (divide `AAAA-MM-DD` sem passar por `Date`), `hojeSaoPaulo()` (a lógica atual de `dataHoje()/hojeIso()`) e usá-los em todas as telas citadas; recalcular "hoje" **no momento do submit** e quando a aba volta ao foco (`visibilitychange`) se o usuário não alterou a data; formatar `ReembolsoModal` em `dd/MM/aaaa`.  
**Restrições:** manter o fuso `America/Sao_Paulo` (mesma convenção do backend após BE-21); não mudar o formato ISO enviado à API.  
**Exemplo de correção:**
```ts
export const formatarDataIso = (iso: string) => { const [a, m, d] = iso.split("-"); return `${d}/${m}/${a}`; };
```

---

### [INT-17] [MÉDIO] Situação do reembolso invisível na lista de gastos; contração de `reembolso_concluido`

**Camada:** Ambos  
**Arquivo(s):** `{BE}/dto/GastoResponse.java`; `{BE}/repository/GastoRepository.java`; novo `{BE-RES}/db/migration/V28__drop_reembolso_concluido.sql`; `{FE}/Pages/Obras/DetalhamentoObras.tsx` (`renderizarReembolso`); `{FE}/Service/Gastos/gastoService.ts`  
**Descrição:** *(FE U5, BE 4.6 parte, BE 3.5 parte)* O gasto não traz o status de reembolso: o selo só aparece **quando um filtro de status está ativo** (comentário: "só é confiável quando filtrado no servidor"); sem filtro o gestor vê só "Pagamentos / Registrar" e precisa filtrar três vezes ou abrir cada modal. A coluna legada `gasto.reembolso_concluido` ainda existe e o JSON a expõe.  
**Impacto:** a dor central dos "reembolsos" (quem já foi pago) não é visível; dupla fonte de verdade.  
**Instrução para o Copilot (depende de BE-14, INT-01, INT-04):** backend: `GastoResponse` ganha `statusReembolso` (`PENDENTE|PARCIAL|PAGO`, `null` se não reembolsável), `totalPago` e `saldoPendente`, calculados com **uma** consulta agregada por página (sem N+1). Frontend: mostrar sempre o selo e o saldo na linha (tabela e lista mobile); remover `reembolsoConcluido` do tipo `Gasto`. Contrair: `V28` remove `gasto.reembolso_concluido` e o getter derivado de BE-14.  
**Restrições:** executar `V28` somente depois de o frontend novo estar em produção; não alterar o fluxo do `ReembolsoModal`; não calcular o status no cliente.  
**Exemplo de correção:**
```ts
<span className={styles[`status${gasto.statusReembolso}`]}>{gasto.statusReembolso}</span>
{gasto.saldoPendente > 0 && <small>Falta {formatarMoeda(gasto.saldoPendente)}</small>}
```

---

### [INT-18] [MÉDIO] nginx: `X-Forwarded-Proto` errado, cabeçalhos perdidos por `location`, sem CSP/HSTS e cache imutável para arquivos sem hash **[NOVO]**

**Camada:** Integração (infra/frontend)  
**Arquivo(s):** `{FE-ROOT}/nginx.conf`; novo `{FE-ROOT}/security.conf`; `{FE-ROOT}/Dockerfile`  
**Descrição:** *(FE S5, FE P4, [NOVO])* O nginx escuta em HTTP (o TLS termina no ALB), então `proxy_set_header X-Forwarded-Proto $scheme` envia **sempre `http`** ao backend, mesmo quando o cliente usou HTTPS. Não há `Content-Security-Policy`, `Strict-Transport-Security` nem `Permissions-Policy`; `X-XSS-Protection` é obsoleto; `Referrer-Policy: no-referrer-when-downgrade` vaza a URL completa; os `add_header` dentro de `location /assets/` e `/uploads/` **substituem** (não herdam) os do `server`, deixando esses `location` sem `X-Frame-Options`/`nosniff`. `location /assets/` aplica `expires 1y; immutable` também a arquivos **sem hash** (`public/assets/Geral/*`, `Login_Img/*`, avatares): trocar a logo não chega aos navegadores por até 1 ano. `/uploads/` usa `public, immutable 30d` para conteúdo que passa a ser autenticado (SEG-15). O app carrega imagens de terceiros (Unsplash, Globo) em telas autenticadas.  
**Impacto:** XSS futuro com acesso total à sessão; fotos privadas cacheáveis por proxies; branding desatualizado; cabeçalhos ausentes em partes do site.  
**Instrução para o Copilot (depende de SEG-05; CSP final depende de FE-01 e FE-12):**
1. `map $http_x_forwarded_proto $proxy_xfp { default $http_x_forwarded_proto; "" $scheme; }` e `proxy_set_header X-Forwarded-Proto $proxy_xfp;` nos dois proxies; `map` para HSTS (`https` ⇒ `max-age=31536000; includeSubDomains`, vazio caso contrário).
2. Mover os cabeçalhos para `security.conf` (copiado para `/etc/nginx/snippets/` no `Dockerfile`) e incluí-lo em **cada** `location`; `Referrer-Policy: strict-origin-when-cross-origin`; `Permissions-Policy: geolocation=(), camera=(self), microphone=()`; remover `X-XSS-Protection`.
3. CSP inicialmente como **`Content-Security-Policy-Report-Only`**: `default-src 'self'; img-src 'self' data: blob:; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'`. Mudar para enforcement só depois de FE-01 (popup com script inline) e FE-12 (imagens externas) estarem implantados.
4. Cache: `location ~ ^/assets/[^/]+$ { expires 1y; immutable }` (bundles com hash estão na raiz de `/assets/`); `location /assets/` (subpastas sem hash) com `Cache-Control: public, max-age=86400, must-revalidate`; `/uploads/` com `private, max-age=86400`.

**Restrições:** **não mover** `public/assets/Geral/avatar-*.svg` nem renomear `/assets/...` (usuários têm `fotoUrl` gravada com esses caminhos); `/health` deve continuar respondendo 200 (health check do ALB); não ligar CSP em modo bloqueante antes de FE-01/FE-12.  
**Exemplo de correção:**
```nginx
map $http_x_forwarded_proto $proxy_xfp { default $http_x_forwarded_proto; "" $scheme; }
map $http_x_forwarded_proto $hsts { default ""; https "max-age=31536000; includeSubDomains"; }

location ~ ^/assets/[^/]+$ { include snippets/security.conf; expires 1y; add_header Cache-Control "public, immutable" always; }
location /assets/          { include snippets/security.conf; add_header Cache-Control "public, max-age=86400, must-revalidate" always; }
```

---

### [INT-19] [BAIXO] Dinheiro como `number` (ponto flutuante) e tipos monetários heterogêneos

**Camada:** Ambos  
**Arquivo(s):** `{FE}/Pages/Home/Home.tsx` (`reduce`); `{FE}/Service/Gastos/gastoService.ts` (`valor: number`); `{FE}/Service/Obras/obraService.ts` (`orcamento?: string`, `totalRecebido?: number`); `{FE}/Utils/financeiro.ts`; novo `{FE}/Utils/dinheiro.ts`; `{BE-RES}/application.properties`  
**Descrição:** *(FE R10, BE 3.5 parte)* `valorDecimal` converte centavos com cuidado ao **enviar**, mas somas e comparações usam floats; `orcamento` vem como string e `Number(orcamento)` é feito sem tratar `NaN` (possível `R$ NaN`); o backend serializa `BigDecimal` como número JSON.  
**Impacto:** erros de arredondamento em somas grandes e `NaN` na tela.  
**Instrução para o Copilot:** backend: `spring.jackson.generator.write-bigdecimal-as-plain=true` (evita notação científica). Frontend: `Utils/dinheiro.ts` com `paraCentavos(n)`, `somarCentavos(...)` e `formatarBRL(centavos)`; converter na borda dos serviços (`Math.round(valor * 100)`), somar em inteiros e formatar só na exibição; tratar `orcamento` inválido como "—".  
**Restrições:** não mudar o formato enviado à API (`valor` number, `orcamento` string); não introduzir bibliotecas só para isso (inteiros bastam).  
**Exemplo de correção:**
```ts
export const paraCentavos = (v: number | string | null | undefined) => {
  const n = Number(v); return Number.isFinite(n) ? Math.round(n * 100) : null;
};
export const formatarBRL = (c: number) => (c / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });
```

## FASE 5 — Frontend (componentes, fluxos, chamadas)

### [FE-01] [CRÍTICO] "Exportação de relatório de gastos" para a contabilidade não existe

**Camada:** Frontend (consome BE-09/BE-10)  
**Arquivo(s):** `{FE}/Pages/Obras/DetalhamentoObras.tsx`; novo `{FE}/Components/ExportarGastos/ExportarGastosModal.tsx`; `{FE}/Service/Gastos/gastoService.ts`; `{FE}/Pages/Dashboard/Dashboard.tsx` (`gerarRelatorio`); `{FE}/routes.tsx` (`/relatorios → /home`); `{FE}/Utils/erro.ts`  
**Descrição:** *(FE R1, FE D6, FE S9, BE 4.1)* O `contexto.txt` define como essencial a exportação do relatório com filtro de data e como restrição o envio à contabilidade por exportação. O que existe é um popup do Dashboard (só ADMIN) que imprime **agregados** (totais, pizza, barras) via `document.write` + `<script>` inline + `window.print()`; não exporta a **lista** de gastos, não leva NFs, não gera arquivo e ignora os filtros Pessoa/Status/Método. O RA tem `GERAR_RELATORIO` e nunca consegue gerar nada. O HTML do popup interpola `item.value` sem escape (hoje números) e usa `window.alert`; é incompatível com a CSP estrita de INT-18.  
**Impacto:** o objetivo central do projeto (reduzir o esforço de consolidar dados para a contabilidade) não é atendido.  
**Instrução para o Copilot (depende de BE-09 e BE-10; decisão D-2):**
1. Em `DetalhamentoObras`, botão **"Exportar"** visível com `can("GERAR_RELATORIO")` que abre `ExportarGastosModal` (formato CSV/XLSX, período `De/Até` pré-preenchido com o filtro atual) e chama `GET /gastos/export` com **todos** os filtros ativos (pessoa, status, método, categoria, etapa, período).
2. Download por *blob*: `api.get("/gastos/export", { params, responseType: "blob", timeout: 120_000 })`, nome do arquivo vindo de `Content-Disposition`, `URL.createObjectURL` + `<a download>` e `revokeObjectURL`. **Atenção:** com `responseType: "blob"` o corpo do erro também vem como `Blob` — converter com `await blob.text()` + `JSON.parse` antes de usar `tratarErroApi`.
3. `Dashboard.gerarRelatorio`: remover `window.open`/`document.write`/script inline/`window.alert`; imprimir a própria página com CSS `@media print` (ou rota `/obras/:id/relatorio` renderizada em React) respeitando o escopo do ADMIN.
4. Estados de carregando/erro e desabilitar o botão durante a geração (duplo clique).

**Restrições:** não remover o Dashboard nem seus gráficos; não gerar o arquivo no cliente (usar o backend); manter o botão oculto para quem não tem `GERAR_RELATORIO`; **não** gravar o arquivo em `localStorage`.  
**Exemplo de correção:**
```ts
// gastoService.ts
export async function exportarGastos(params: ExportParams) {
  const resp = await api.get<Blob>("/gastos/export", { params, responseType: "blob", timeout: 120_000 });
  const nome = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(resp.headers["content-disposition"] ?? "")?.[1] ?? "gastos.csv";
  const url = URL.createObjectURL(resp.data);
  const a = Object.assign(document.createElement("a"), { href: url, download: decodeURIComponent(nome) });
  a.click(); URL.revokeObjectURL(url);
}
```

---

### [FE-02] [CRÍTICO] Interface de Notas Fiscais (anexar, consultar, baixar) **[NOVO]**

**Camada:** Frontend (consome BE-08)  
**Arquivo(s):** `{FE}/Pages/Obras/DetalhamentoObras.tsx` (formulário/lista de gastos); novo `{FE}/Service/NotasFiscais/notaFiscalService.ts`; novo `{FE}/Components/NotasFiscais/NotasFiscaisGasto.tsx`; `{FE}/Service/Gastos/ocrService.ts`  
**Descrição:** *(BE 4.1, [NOVO])* "Inserção e gestão de Notas Fiscais" é essencial e o backend passa a oferecê-la em BE-08, mas nenhuma tela anexa ou consulta NFs: o fluxo do OCR lê a foto, preenche o formulário e **descarta o arquivo**.  
**Impacto:** sem comprovante armazenado, o requisito não é entregue ao usuário mesmo com a API pronta.  
**Instrução para o Copilot (depende de BE-08, DB-07 e INT-12):** (1) ao salvar um gasto que usou o OCR, enviar o **mesmo arquivo** da leitura para `POST /gastos/{id}/notas-fiscais` (parte `arquivo`); (2) no formulário, campo "Anexar nota fiscal" (aceita imagem/PDF ≤ 10 MB, validação de INT-15) com `numero`/`cnpjEmitente` opcionais; (3) em cada gasto, ícone/contagem de NFs e lista (nome, número) com **download autenticado** (`<a href="/api/notas-fiscais/{id}/arquivo">`, same-origin) e exclusão visível só para ADMIN; (4) se o anexo falhar depois de o gasto ter sido salvo, avisar e oferecer "Tentar anexar novamente" **sem recriar o gasto** (usar a chave de idempotência).  
**Restrições:** não bloquear o lançamento do gasto por falha no anexo; não alterar a leitura do OCR; respeitar `CRIAR_GASTO`/`EDITAR_GASTO`; não exibir `arquivo_chave` do storage.  
**Exemplo de correção:**
```ts
export const anexarNotaFiscal = (gastoId: number, arquivo: File, extra?: { numero?: string; cnpjEmitente?: string }) => {
  const form = new FormData(); form.append("arquivo", arquivo);
  if (extra?.numero) form.append("numero", extra.numero);
  if (extra?.cnpjEmitente) form.append("cnpjEmitente", extra.cnpjEmitente);
  return api.post(`/gastos/${gastoId}/notas-fiscais`, form, { timeout: 120_000 });
};
```

---

### [FE-03] [MÉDIO] Estado de autenticação do cliente por `localStorage`, troca de usuário em outra aba e expiração sem aviso

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Service/Auth/Login/authService.tsx`; `{FE}/Service/Auth/Login/authorizationSession.ts`; `{FE}/Context/UserContext.tsx`; `{FE}/Service/Auth/Login/Api/Api.ts`; `{FE}/Pages/Auth/Login/Login.tsx`; `{FE}/Pages/Profile/Profile.tsx`; `{FE}/Components/PrivateRoute/PrivateRoute.tsx`; testes correspondentes  
**Descrição:** *(FE S2, FE S3, FE U1, FE U7, BE 2.8 parte)* **S2:** `isAuthenticated()` é `localStorage.getItem("userEmail") !== null`; e-mail, nome, id e cargo ficam em texto no disco **depois** que o cookie expira e mesmo após fechar o navegador; `userCargo` é gravado e nunca lido; qualquer script/extensão pode setar `userEmail` para passar a primeira checagem. **S3:** o cookie é compartilhado entre abas, o estado React não: se a aba A loga como outro usuário, a aba B continua mostrando o usuário antigo mas **cada requisição já sai autenticada como o novo** (sem listener de `storage`/`visibilitychange`), quebrando a rastreabilidade (gasto lançado "como" um usuário diferente do exibido). **U1:** o cookie dura 1 h sem renovação; a primeira chamada com 401 faz `window.location.replace('/')` **sem mensagem** e o formulário em edição (gasto com OCR processado, observação de pagamento) é perdido; o `Login` descarta a sessão em silêncio para **qualquer** erro (inclusive queda de rede). **U7:** trocar o e-mail desloga sem aviso — é um *workaround* do `sub` mutável que SEG-09 elimina.  
**Impacto:** vazamento de PII em computador compartilhado (comum em obra/escritório), troca de identidade silenciosa, perda de trabalho e confusão.  
**Instrução para o Copilot (depende de SEG-03 e SEG-09):**
1. Manter a sessão **só em memória**: o estado inicial é obtido chamando `GET /funcionarios/me` na carga do app (`status: "carregando" | "autenticado" | "anonimo"`); remover o uso de `userEmail/userName/userId/userCargo` e **apagar** essas chaves legadas no primeiro boot. `authorizationSession` passa a comparar apenas o `userId` em memória.
2. `BroadcastChannel("jaf-auth")`: em login/logout enviar `{tipo, userId}`; ao receber de outra aba um `userId` diferente → `refreshUser()` e, se divergir, redirecionar a `/` com aviso; revalidar `/me` em `visibilitychange` (máx. 1×/30 s) e comparar o id.
3. Expiração: no interceptor, em vez de `window.location.replace`, emitir um evento `auth:expirou` tratado no *router* (`navigate("/", { state: { de: location, motivo: "sessao-expirada" } })`) com toast "Sua sessão expirou. Entre novamente."; após o login, voltar à rota de origem. Persistir rascunho do formulário de gasto em `sessionStorage` (`rascunho-gasto-{obraId}`) e limpá-lo ao salvar.
4. `Login`: diferenciar 401 (sessão inválida) de erro de rede ("Sem conexão com o servidor") ao validar sessão existente.
5. `Profile.tsx`: após SEG-09, **remover** `clearUser(); authService.logout()` na troca de e-mail e atualizar o contexto com a resposta; toast "E-mail alterado com sucesso".

**Restrições:** não armazenar token nem PII em `localStorage`/`sessionStorage` (o rascunho do gasto não contém dados do usuário e some ao fechar a aba); o cookie `HttpOnly` continua sendo a única credencial; se SEG-09 ainda não estiver implantado, **manter** o logout no e-mail e apenas mostrar a mensagem "E-mail alterado. Faça login novamente com o novo e-mail."; atualizar `UserContext.test.tsx`, `authService.test.ts`, `Api.test.ts`.  
**Exemplo de correção:**
```ts
const canal = new BroadcastChannel("jaf-auth");
canal.onmessage = (e) => { if (e.data.userId !== userIdAtual) void refreshUser(); };
// no interceptor (401): window.dispatchEvent(new Event("auth:expirou"));
```

---

### [FE-04] [MÉDIO] Autorização lida de um singleton fora do React, durante o render; `UserContext` não memoizado

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Service/Auth/Login/authService.tsx` (`hasAuthority`, `isAdmin`, `podeRegistrarPagamento`); `{FE}/Service/Auth/Login/authorizationSession.ts`; `{FE}/Context/UserContext.tsx`; `{FE}/Context/useUser.ts`; `{FE}/Components/CardObra/cardObra.tsx`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Components/Layout/Sidebar/Sidebar.tsx`; `{FE}/Pages/Home/Home.tsx`; `{FE}/Pages/Dashboard/Dashboard.tsx`; `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`; `{FE}/Components/PrivateRoute/PrivateRoute.tsx`  
**Descrição:** *(FE Q2, FE Q10)* `authService.hasAuthority()`/`isAdmin()` leem uma variável de módulo mutável (`session`); os componentes a consultam no render sem assinar estado e só atualizam quando algo não relacionado os re-renderiza. `authorizationSession.current()` tem **efeito colateral** (chama `clear()`) dentro de uma leitura feita no render e lê `localStorage` a cada chamada. O `value` do Provider é um objeto novo a cada render e o efeito inicial com cleanup `clearAuthorization()` invalida a primeira chamada, gerando requisições duplicadas a `/me` e `/me/permissoes` em `StrictMode`; no login, `refreshUser` roda de novo logo após `POST /login`.  
**Impacto:** botões de um estado antigo (ou escondidos indevidamente) após refresh/relogin; sessão zerada no meio do render; re-renders e tráfego desnecessários.  
**Instrução para o Copilot (depende de FE-03):** expor as permissões pelo `UserContext` (estado React) e consumi-las via hook `useAuthz()` → `{ can(permissao), isAdmin, podeRegistrarPagamento }`; substituir as chamadas `authService.hasAuthority/isAdmin` nos componentes listados; manter o singleton **apenas** para o interceptor do axios; `current()` deixa de ter efeito colateral; `useMemo` no `value` do Provider; substituir o cleanup que invalida a requisição por um *request id*/`AbortController` para não duplicar chamadas; evitar o `refreshUser` redundante após o login.  
**Restrições:** não mudar as regras de autorização (apenas a fonte); manter `PrivateRoute` com `requiredPermission`/`adminOnly`; atualizar `src/test/authorization.ts` e testes dos componentes.  
**Exemplo de correção:**
```tsx
const { can, isAdmin } = useAuthz();
{can("EDITAR_GASTO") && <button onClick={abrirEdicao}>Editar</button>}
```

---

### [FE-05] [MÉDIO] Senhas no console do navegador após falha de requisição

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Service/Auth/Login/authService.tsx` (linhas `console.error("Erro ao fazer login:", error)`/logout); `{FE}/Pages/Auth/Login/Login.tsx` (`console.error(error)`); `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx` (`console.error("Erro na API:", error)`); `{FE}/Context/UserContext.tsx`; demais `console.error(...)` de `Pages/**`; novo `{FE}/Utils/logger.ts`  
**Descrição:** *(FE S4)* O `error` é um `AxiosError`, e `error.config.data` contém o JSON `{"email":…,"senha":…}` enviado. **[NOVO]** O mesmo acontece em `NovoFuncionario.tsx`, que registra o erro do `POST /funcionarios` com a **senha do novo usuário** no corpo, e em `Login.tsx`.  
**Impacto:** qualquer ferramenta que capture o console (extensão, monitor futuro, suporte remoto) recebe senhas.  
**Instrução para o Copilot:** criar `logger.erro(contexto, erro)` que registra apenas `status`, `code` e `contexto` (nunca `config`, `data`, `headers`) e substituir **todos** os `console.error(error)` que recebem erro de axios; adicionar regra ESLint `no-console` (permitindo `logger`).  
**Restrições:** não remover a exibição de mensagens ao usuário; não logar corpo de requisição em nenhum nível; manter `console.error` somente em `logger.ts`.  
**Exemplo de correção:**
```ts
export const logger = {
  erro(contexto: string, e: unknown) {
    console.error(contexto, isAxiosError(e) ? { status: e.response?.status, code: e.code } : (e as Error)?.message);
  },
};
```

---

### [FE-06] [MÉDIO] Requisições sem cancelamento, respostas fora de ordem e respostas sem tipo/validação

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Pages/Obras/DetalhamentoObras.tsx` (`carregarDados`); `{FE}/Pages/Dashboard/Dashboard.tsx`; `{FE}/Pages/ControlePresenca/GestaoPresenca.tsx`; `{FE}/Pages/Home/Home.tsx`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/Permissoes/Permissoes.tsx` (`selecionarUsuario`); `{FE}/Pages/Profile/Profile.tsx` (`URL.createObjectURL`); `{FE}/Service/Obras/obraService.ts`, `Alocacoes/alocacaoService.ts`, `Presencas/presencaService.ts`, `Dashboard/dashboardService.ts`  
**Descrição:** *(FE Q3, FE I4, FE Q9)* Os efeitos disparam `await` sem invalidar respostas antigas: no `Dashboard` a resposta mais lenta sobrescreve a mais nova; em `Permissoes.selecionarUsuario`, clicar em dois usuários seguidos pode exibir (e depois **salvar**) as permissões do usuário errado; em `DetalhamentoObras.carregarDados`, se `GET /obras/{id}` falha o título da obra **anterior** continua na tela. Os serviços `obraService`, `alocacaoService`, `presencaService` e `dashboardService` usam `api.get(...)` sem genérico (`any` que se propaga) e `parseInt("12abc")` retorna 12 / `parseInt("abc")` vira `/obras/NaN`. `Profile.tsx` nunca revoga o *object URL* do preview.  
**Impacto:** dados de outra obra/usuário/período exibidos como atuais (inaceitável em finanças e permissões), `TypeError` em registros legados com campos nulos, vazamento de memória.  
**Instrução para o Copilot:** padrão único por efeito: `AbortController` e repassar `signal` aos serviços (`api.get(url, { signal })`), ignorando `CanceledError`; em `Permissoes`, guardar o id da requisição vigente e **desabilitar "Salvar" enquanto carrega**; em `carregarDados` chamar `setObra(null)` no `catch`; tipar todos os `api.get<T>`; validar parâmetros de rota com `const id = Number(param); if (!Number.isInteger(id) || id <= 0) return <Navigate to="/home" replace />`; revogar *object URLs* em `useEffect` de limpeza. (Opcional: validar DTOs críticos com `zod` na borda dos serviços.)  
**Restrições:** não alterar o contrato das chamadas; o polling do OCR já é tratado em INT-15; não adicionar bibliotecas de cache aqui (FE-19).  
**Exemplo de correção:**
```tsx
useEffect(() => {
  const ctrl = new AbortController();
  dashboardService.stats(obraId, etapa, from, to, ctrl.signal).then(setStats).catch((e) => { if (!isCancel(e)) setErro(tratarErroApi(e).mensagem); });
  return () => ctrl.abort();
}, [obraId, etapa, from, to]);
```

---

### [FE-07] [MÉDIO] Sem `ErrorBoundary`

**Camada:** Frontend  
**Arquivo(s):** novo `{FE}/Components/ErrorBoundary/ErrorBoundary.tsx`; `{FE}/main.tsx`; `{FE}/App.tsx`; `{FE}/routes.tsx`; `{FE}/Components/Layout/Layout.tsx`  
**Descrição:** *(FE Q4)* Não há `ErrorBoundary` nem `errorElement` nas rotas. Qualquer exceção de render (ex.: `gasto.funcionario` nulo, `item.categoria.toLowerCase()` com `null` vindo da API) derruba a aplicação inteira em tela branca.  
**Impacto:** beco sem saída para o perfil de usuário descrito no `contexto.txt` (baixa familiaridade tecnológica).  
**Instrução para o Copilot:** criar o componente (classe com `getDerivedStateFromError`/`componentDidCatch`) com mensagem amigável e botão **"Recarregar"**, envolver o `<Outlet />` do `Layout` e definir `errorElement` nas rotas do `createBrowserRouter`; registrar o erro com `logger.erro` (FE-05); teste com um componente que lança.  
**Restrições:** não engolir erros de requisição (esses seguem `tratarErroApi`); não exibir stack ao usuário.  
**Exemplo de correção:**
```tsx
<ErrorBoundary fallback={<TelaErro onReload={() => window.location.reload()} />}><Outlet /></ErrorBoundary>
```

---

### [FE-08] [MÉDIO] Componente-monolito `DetalhamentoObras` (≈1149 linhas)

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Pages/Obras/DetalhamentoObras.tsx` (+ `DetalhamentoObras.module.css`, `DetalhamentoObras.test.tsx`); novos `{FE}/Pages/Obras/componentes/*` e `{FE}/Hooks/useGastos.ts`, `useObra.ts`, `useOcrNotaFiscal.ts`  
**Descrição:** *(FE Q1)* Um único componente com ≈35 `useState`, 4 modais inline (edição de obra, gasto, exclusão de obra, reembolso), upload e OCR, filtros, ordenação, paginação, formatadores e regras de permissão; os dois templates (tabela desktop e lista mobile) duplicam todo o JSX da linha de gasto. É o arquivo com as regras mais críticas de dinheiro, e INT-04/08/09/10/11/12/17 e FE-01/02 o modificam.  
**Impacto:** qualquer tecla em qualquer modal re-renderiza a página inteira; mudanças arriscadas, testes frágeis, conflitos de merge.  
**Instrução para o Copilot (executar **depois** dos itens INT e FE-01/02 que alteram este arquivo):** (1) escrever primeiro testes de caracterização do comportamento atual; (2) extrair `GastoFormModal`, `ObraEditModal`, `ExcluirObraModal`, `GastosTabela`/`GastosLista` (ambos usando um único `GastoLinha`) e os hooks `useGastos(obraId, filtros)`, `useObra(id)`, `useOcrNotaFiscal()`; (3) manter props explícitas e estado local de cada modal (digitar não re-renderiza a página); (4) preservar integralmente as regras de dinheiro (`valorDecimal`), de reembolso e as chamadas à API.  
**Restrições:** refatoração pura — nenhuma mudança de comportamento, de textos ou de contrato; não misturar com FE-10 (modais acessíveis) no mesmo PR; cada extração em commit separado com testes verdes.  
**Exemplo de correção:**
```tsx
export default function DetalhamentoObras() {
  const { obra, erro, recarregar } = useObra(id);
  const gastos = useGastos(id, filtros);
  return (<><ObraCabecalho obra={obra} /><GastosTabela {...gastos} /><GastoFormModal aberto={...} /></>);
}
```

---

### [FE-09] [MÉDIO] Duplicação sistemática de helpers/constantes e formatação inconsistente

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Pages/Home/Home.tsx`, `{FE}/Components/CardObra/cardObra.tsx`, `{FE}/Pages/Obras/DetalhamentoObras.tsx`, `{FE}/Pages/Dashboard/Dashboard.tsx`, `{FE}/Pages/Dashboard/data.ts`, `{FE}/Utils/financeiro.ts`, `{FE}/Types/user.ts`, `{FE}/Pages/Permissoes/Permissoes.tsx`, `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`, `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`, `{FE}/Components/Financeiro/ReembolsoModal.tsx`, `{FE}/Pages/Profile/Profile.tsx`; novos `{FE}/Utils/format.ts`, `{FE}/Constants/dominio.ts`  
**Descrição:** *(FE Q6, FE U6)* Cópias independentes de `formatarMoeda` (Home, CardObra, DetalhamentoObras; `moeda` em `Utils/financeiro.ts`; `formatBRL` em Dashboard e `Dashboard/data.ts` — esta sem espaço depois do `R$`), `getIniciais` (3 versões com comportamento diferente para espaços duplicados), `formatarData` (4 versões, com e sem `timeZone`), `dataHoje()` = `hojeIso()`, `normalizarMetodoPagamento` ≈ `normalizarMetodoPagamentoOcr`, rótulos de cargo (`CargoLabel`, `CARGO_LABELS`+`CARGO_BADGES`, `opcoesCargo`) e URL de imagem de obra (glbimg). U6: `ReembolsoModal` mostra `Gasto em 2026-10-30` e `PENDENTE` em maiúsculas; `Profile.tsx` tem textos sem acento ("Configuracoes", "seguranca", "confirmacao"); `obra.status.replace("_"," ")` troca só o primeiro `_`; "0 Dias" tanto para obra atrasada quanto concluída; opções de status em caixa alta sem acento.  
**Impacto:** correções ficam pela metade e já há divergência real de fuso/formatação; aparência pouco profissional.  
**Instrução para o Copilot (depois de INT-16/INT-05):** consolidar em `Utils/format.ts` (`formatarMoeda`, `formatarData` usando INT-16, `getIniciais`, `rotuloStatus` com `/_/g`) e `Constants/dominio.ts` (cargos, funções na obra, métodos, status, rótulos/badges); trocar todas as cópias por imports; exibir "Atrasada há N dias" / "Concluída" em vez de "0 Dias"; corrigir acentuação e capitalização dos textos; cobrir os helpers com testes.  
**Restrições:** mudança sem alteração de regra; não mudar o formato enviado à API; `formatarMoeda` padrão com espaço (`R$ 1.234,56`).  
**Exemplo de correção:**
```ts
export const rotuloStatus = (s: string) => s.replace(/_/g, " ").toLowerCase().replace(/(^|\s)\S/g, (c) => c.toUpperCase());
```

---

### [FE-10] [MÉDIO] Modais artesanais inacessíveis que fecham ao clicar fora e perdem os dados; elementos clicáveis sem semântica

**Camada:** Frontend  
**Arquivo(s):** novo `{FE}/Components/Modal/Modal.tsx`; `{FE}/Pages/Obras/DetalhamentoObras.tsx` (3 modais); `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/ControlePresenca/ControlePresenca.tsx`; `{FE}/Components/Layout/Navbar/Navber.tsx`; `{FE}/Components/Financeiro/ReembolsoModal.tsx` (modelo); `{FE}/Pages/Permissoes/Permissoes.tsx`; `{FE}/Components/Layout/Sidebar/Sidebar.tsx`; `{FE}/Pages/Auth/Login/Login.tsx`  
**Descrição:** *(FE U2, FE U8)* Os overlays não têm `role="dialog"`, `aria-modal`, `aria-labelledby`, *focus trap* nem retorno de foco; o clique no overlay fecha o formulário de gasto e **descarta** tudo o que foi digitado (um toque acidental em tela pequena); exclusão de gasto/presença usa `window.confirm`, e a de obra um modal — três padrões. `Permissoes.tsx`: cartões de usuário e cabeçalhos de módulo são `<div onClick>`; vários `<h1>` por página (Sidebar + cada página); campos de gasto sem `htmlFor`.  
**Impacto:** perda de dados no celular; inacessível por teclado/leitor de tela; experiência inconsistente.  
**Instrução para o Copilot (depois de FE-08):** criar `<Modal>` sobre `<dialog>` (como `ReembolsoModal`), com `aria-labelledby`, `Esc`, foco inicial/retorno, e a opção `sujo` que **não fecha ao clicar fora** (pede confirmação); substituir todos os modais e os `window.confirm`; trocar `<div onClick>` por `<button type="button">`/`<details>`; manter um único `<h1>` por página; associar `label`/`htmlFor`.  
**Restrições:** não alterar regras de negócio nem textos dos formulários; manter `ReembolsoModal` como referência de comportamento; testes de teclado (Tab/Esc) para o novo `Modal`.  
**Exemplo de correção:**
```tsx
<Modal aberto={aberto} titulo="Novo gasto" sujo={formSujo} onFechar={fechar}>{/* formulário */}</Modal>
```

---

### [FE-11] [MÉDIO] Dashboard desmonta a tela inteira a cada filtro; barras de progresso sem limite

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Pages/Dashboard/Dashboard.tsx`; `{FE}/Components/StatCard/StatCard.tsx` (+ mover `{FE-ROOT}/StatCard.test.tsx` para `{FE}/Components/StatCard/`)  
**Descrição:** *(FE U3, FE U9)* `if (carregando) return <…Carregando…>` desmonta os próprios filtros: o foco do teclado é perdido, a rolagem volta ao topo e, em erro, os filtros também somem (não dá para corrigir o período). `StatCard` usa `style={{ width: \`${progress}%\` }}` sem limitar: saldo negativo ou `progressoSaldo` > 100 (BE-11 preserva valores reais) extravasa o card ou some.  
**Impacto:** navegação travada em erro de período; visual quebrado justamente quando o saldo é negativo.  
**Instrução para o Copilot (depois de INT-07):** manter cabeçalho/filtros montados e exibir *skeleton*/overlay só nas áreas de dados; erro **inline** com "Tentar novamente"; `StatCard` limita a barra com `Math.min(100, Math.max(0, progress))` e usa cor/ícone de **alerta** quando exceder 100% ou o saldo for negativo.  
**Restrições:** não mudar os cálculos nem os nomes dos campos; o teste do `StatCard` deve passar a ser verificado pelo `tsc -b` (movê-lo para `src`).  
**Exemplo de correção:**
```tsx
const largura = Math.min(100, Math.max(0, progress));
<div className={progress > 100 ? styles.barraAlerta : styles.barra} style={{ width: `${largura}%` }} />
```

---

### [FE-12] [MÉDIO] Home: falha vira "Nenhuma obra encontrada", imagens falsas/externas, "primeira obra" implícita

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Pages/Home/Home.tsx`; `{FE}/Components/CardObra/cardObra.tsx`; `{FE}/Pages/Obras/DetalhamentoObras.tsx`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{FE}/Pages/ControlePresenca/GestaoPresenca.tsx`; novo `{FE-ROOT}/public/assets/Geral/obra-placeholder.svg`  
**Descrição:** *(FE U4, FE D9, FE U11)* Se `obraService.listar()` falha, só há um toast e a tela mostra "Nenhuma obra encontrada." (indistinguível de lista vazia); a foto de cada obra é `imagensObras[index % 4]` (Unsplash) usando o índice da lista **filtrada/ordenada** (muda ao filtrar) e, em `DetalhamentoObras`/`AlocacaoFuncionario`, uma foto fixa de apartamento hospedada no Globo — imagens de imóveis de terceiros como se fossem as obras reais, com dependência externa e vazamento de IP/`Referer`; o `MapPin` mostra o **status** no lugar de "local" (a documentação diz que o cadastro deixou de pedir localização). `/obras/detalhamento` sem id mostra a primeira obra retornada e `GestaoPresenca` abre sempre na primeira obra: risco de registrar presença na obra errada.  
**Impacto:** o gestor acha que não tem obras; imagens enganosas; presença registrada na obra errada.  
**Instrução para o Copilot:** estado de **erro** com botão "Tentar novamente" distinto de lista vazia; substituir todas as imagens externas por um placeholder neutro local (`obra-placeholder.svg`); trocar o `MapPin` por um selo de status; `/obras/detalhamento` sem `:id` redireciona a `/home`; `GestaoPresenca` exige seleção explícita da obra (lembrando a última em `sessionStorage`).  
**Restrições:** não renomear nem mover `public/assets/Geral/avatar-*.svg`; este item é pré-requisito para a CSP bloqueante de INT-18.  
**Exemplo de correção:**
```tsx
{erro ? <Erro mensagem={erro} onTentarNovamente={carregar} /> : obras.length === 0 ? <Vazio /> : <Lista />}
<img src="/assets/Geral/obra-placeholder.svg" alt="" />
```

---

### [FE-13] [BAIXO] Sem code-splitting

**Camada:** Frontend  
**Arquivo(s):** `{FE}/routes.tsx`; `{FE}/routes.test.tsx`  
**Descrição:** *(FE Q5)* Todas as páginas são importadas estaticamente: Dashboard, gestão de presença e perfis de acesso entram no bundle da tela de login.  
**Impacto:** primeiro carregamento mais lento em rede móvel (o produto é *mobile first*).  
**Instrução para o Copilot:** `const Dashboard = lazy(() => import("./Pages/Dashboard/Dashboard"))` (e idem para `Permissoes`, `GestaoPresenca`, `AlocacaoFuncionario`, `Profile`, `DetalhamentoObras`) com `<Suspense fallback={<Carregando/>}>` no `Layout`; ajustar `routes.test.tsx` para aguardar o carregamento (`findBy*`).  
**Restrições:** manter `Login` e `Home` no bundle principal; não alterar paths nem guards.  
**Exemplo de correção:**
```tsx
const Dashboard = lazy(() => import("./Pages/Dashboard/Dashboard"));
```

---

### [FE-14] [BAIXO] Código morto, restos de template, estilos inline e nomes de arquivo inconsistentes

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Service/Ocr/ocrService.ts`; `{FE}/Pages/Dashboard/data.ts`; `{FE}/Types/user.ts` (`JwtPayload`); `{FE}/Service/Auth/Login/authService.tsx` (`getToken`, `decodeToken`); `{FE}/Types/auth.ts` (`LoginResponse.token`); `{FE}/Service/Permissoes/permissaoService.ts` (`getMapaPadraoPermissoes`, `getPermissoesPorCargo`); `{FE-ROOT}/package.json` (`name`, `recharts`); `{FE-ROOT}/public/assets/hero.png`, `react.svg`, `vite.svg`; `{FE}/Components/Layout/Navbar/Navber.tsx`; `{FE}/Components/CardObra/cardObra.tsx`; `{FE}/Pages/Funcionarios/NovoFuncionario/NovoFuncionario.tsx`  
**Descrição:** *(FE Q7, FE Q11, FE D11)* Código sem uso (endpoint `/ocr` legado, dados *mock*, tipos/funções de token que retornam `null`, `localStorage "userCargo"`, dependência `recharts` não importada — **confirmado**: nenhum `import ... from "recharts"` em `src`), comentários longos "CORREÇÃO DE SEGURANÇA A07", assets do template Vite e `README` do template. Arquivos com nome errado (`Navber.tsx`; `cardObra.tsx` × `CardObra.module.css`), `style={{color:"red"}}` repetido 5× em `NovoFuncionario`, `package.json:name = "meu-projeto"`.  
**Impacto:** confunde quem lê, amplia a superfície e quebra imports em sistemas de arquivos que diferenciam maiúsculas (Linux/CI).  
**Instrução para o Copilot:** remover o código/arquivos listados (verificar com `tsc -b` e `grep` que não há uso; `permissaoService` só depois de INT-06); `npm uninstall recharts`; renomear com `git mv` em dois passos no Windows (`Navber.tsx → Navbar.tsx`, `cardObra.tsx → CardObra.tsx`) e atualizar os imports de `Layout.tsx` e `Home.tsx`; mover os estilos inline para CSS Module; `package.json` `name` = `jaf-frontend`; substituir o `README` do template por um com como rodar/testar.  
**Restrições:** não remover `public/assets/Geral/*` nem `Login_Img/*` (em uso, e os avatares estão gravados em `fotoUrl` de usuários); o protótipo legado fora de `react/` é tratado em SEG-18.  
**Exemplo de correção:**
```powershell
git mv jaf-frontend/react/src/Components/Layout/Navbar/Navber.tsx jaf-frontend/react/src/Components/Layout/Navbar/Navbar.tmp.tsx
git mv jaf-frontend/react/src/Components/Layout/Navbar/Navbar.tmp.tsx jaf-frontend/react/src/Components/Layout/Navbar/Navbar.tsx
```

---

### [FE-15] [BAIXO] Rotas de obra sem permissão explícita e lista completa de funcionários em telas de operação

**Camada:** Frontend (+ endpoint do backend)  
**Arquivo(s):** `{FE}/routes.tsx` (`/obras/detalhamento`, `/obras/detalhamento/:id`, `/home`, `/perfil`); `{FE}/Pages/Obras/NovaObra/NovaObra.tsx`; `{FE}/Pages/Alocacoes/AlocacaoFuncionario.tsx`; `{BE}/controller/FuncionarioController.java`  
**Descrição:** *(FE S10, FE S11)* As rotas de obra não usam `requiredPermission="VISUALIZAR_OBRA"`: quem não tem a permissão vê o esqueleto da tela com erros em vez de "sem acesso" (a proteção de rota é só de UX; o enforcement é da API). `NovaObra` e `AlocacaoFuncionario` carregam `GET /funcionarios` completo (com e-mail e cargo) só para preencher um `<select>`.  
**Impacto:** UX confusa e minimização de dados fraca para quem tem `CRIAR_ALOCACAO`.  
**Instrução para o Copilot:** adicionar `requiredPermission="VISUALIZAR_OBRA"` às rotas de obra e uma tela "Sem acesso" para o caso negado; criar no backend `GET /funcionarios/alocaveis` (`id, nome, cargo`, sem e-mail, `CRIAR_ALOCACAO`) e usá-lo nos dois selects.  
**Restrições:** manter o enforcement no backend; não remover `GET /funcionarios` (usado em `Permissoes`); `/home` e `/perfil` continuam acessíveis a qualquer autenticado.  
**Exemplo de correção:**
```tsx
{ path: "obras/detalhamento/:id", element: <PrivateRoute requiredPermission="VISUALIZAR_OBRA"><DetalhamentoObras /></PrivateRoute> }
```

---

### [FE-16] [BAIXO] Linguagem técnica para o perfil de usuário-alvo

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Components/Financeiro/ReembolsoModal.tsx`; `{FE}/Components/Financeiro/FinanceiroObra.tsx`; `{FE}/Components/Financeiro/ResumoReembolsos.tsx`  
**Descrição:** *(FE U10)* Frases como "Parcelas não podem ser editadas ou excluídas", "Resultado do pagamento desconhecido. Consulte o histórico…", "Recebido acumulado… não cria histórico de recebimentos" e "Saldo dos recebimentos" são precisas, porém densas para quem tem "baixa familiaridade tecnológica" (`contexto.txt`).  
**Impacto:** risco de erro de operação em lançamentos financeiros.  
**Instrução para o Copilot:** reescrever em frases curtas e adicionar **confirmação final com resumo** antes de registrar pagamento ("Pagar R$ 500,00 a Gabriel em 30/10?"); ajuda contextual (`title`/texto de apoio); manter o estado "resultado desconhecido" com instrução simples ("Confira o histórico antes de tentar de novo.").  
**Restrições:** não alterar a lógica de bloqueio de duplo envio com `useRef` nem a regra de não repetir o POST; ajustar os testes de texto.  
**Exemplo de correção:**
```tsx
<p>Pagar {formatarMoeda(valor)} a {pessoa} em {formatarDataIso(data)}?</p>
```

---

### [FE-17] [BAIXO] Cobertura de testes desigual e documentação de testes não verificável

**Camada:** Frontend  
**Arquivo(s):** `{FE}/**/*.test.tsx` (novos para `Dashboard`, `Permissoes`, `GestaoPresenca`, `AlocacaoFuncionario`, `Layout/Sidebar/Navbar`, `PeriodoFiltro`); `{FE-ROOT}/StatCard.test.tsx` → `{FE}/Components/StatCard/`; `{FE-ROOT}/vitest.config.ts`  
**Descrição:** *(FE Q12, FE D10)* Há testes de login, rotas, financeiro, perfil etc., mas **nenhum** para `Dashboard` (relatório), `Permissoes` (escrita de permissões), `GestaoPresenca`, `AlocacaoFuncionario`, `Sidebar/Navbar` e `PeriodoFiltro`. `StatCard.test.tsx` está fora de `src` e não passa pelo `tsc -b`. As afirmações "124 testes / lint aprovado" da documentação não foram verificadas.  
**Impacto:** as telas de maior risco (permissões, presença, relatório) não têm rede de segurança.  
**Instrução para o Copilot:** cobrir ao menos `Permissoes.handleSalvar` (sucesso, falha parcial, sem fallback), `GestaoPresenca.salvarStatus` (PUT idempotente), exportação (FE-01) e `PeriodoFiltro` (período invertido); mover `StatCard.test.tsx`; executar `npm run lint`, `npx tsc -b` e `npx vitest run` e registrar os números **reais** na documentação.  
**Restrições:** não depender de rede real (mockar `api`); não relaxar regras de ESLint para passar.  
**Exemplo de correção:**
```tsx
it("não permite salvar quando as permissões do usuário não carregaram", async () => { /* mock 500 em /funcionarios/2/permissoes */ });
```

---

### [FE-18] [BAIXO] Build e entrega do frontend

**Camada:** Frontend (infra)  
**Arquivo(s):** `{FE-ROOT}/Dockerfile`; novo `{FE-ROOT}/.dockerignore`; `.github/workflows/deploy.yml`  
**Descrição:** *(FE P5)* O `Dockerfile` usa `npm install` (em vez de `npm ci`), não há `.dockerignore` (o `COPY . .` leva `node_modules`/`dist` locais para a imagem de build), `nginx:alpine` roda como root, `node:20-alpine` sem fixar versão/digest, e lint/testes não rodam no build.  
**Impacto:** builds não reproduzíveis e imagem maior do que precisa.  
**Instrução para o Copilot:** `npm ci`; `.dockerignore` (`node_modules`, `dist`, `.git`, `*.log`); trocar a imagem final por `nginxinc/nginx-unprivileged` (porta 8080) **ajustando** `nginx.conf` (`listen 8080`), `EXPOSE`, `docker-compose.yml` e o target group/security group do Terraform se a porta mudar — ou manter `nginx:alpine` e registrar o risco; fixar versões; o CI de SEG-21 roda `lint`, `tsc -b` e `vitest run` antes da imagem.  
**Restrições:** não mudar `VITE_API_URL=/api`; se a porta do container mudar, atualizar **todos** os pontos (compose, ALB target group `frontend`, health check); não incluir `.env` na imagem.  
**Exemplo de correção:**
```dockerfile
FROM node:20.18-alpine AS builder
COPY package*.json ./
RUN npm ci
```

---

### [FE-19] [BAIXO] Cada tela implementa seu próprio fetch/loading/erro/cache

**Camada:** Frontend  
**Arquivo(s):** `{FE}/Service/*`; `{FE}/Pages/*`; `{FE}/Hooks/*`; `{FE-ROOT}/package.json`  
**Descrição:** *(FE P6)* Não há camada de dados (React Query/SWR), formulários sem biblioteca e padrões de bloqueio de duplo envio diferentes (`useRef` × `useState`). Cada nova tela (anexos, e-mail automático, "monitoramento anual", listados como desejáveis) repete os problemas de FE-06, INT-02 e INT-12.  
**Impacto:** custo de manutenção crescente e bugs repetidos a cada funcionalidade.  
**Instrução para o Copilot (depois de FE-06, INT-02, INT-12):** adotar TanStack Query para leitura/mutação (cancelamento, cache, *retry* só de GET, estado de erro), `react-hook-form` + `zod` para formulários (reutilizando `Utils/validacao.ts`) e hooks por domínio (`useObras`, `useGastos`, `usePresencas`), migrando **uma tela por PR** (começar por `Home`).  
**Restrições:** não migrar tudo de uma vez; não repetir automaticamente `POST`/`PUT`/`DELETE`; manter a mesma UI e os mesmos textos.  
**Exemplo de correção:**
```ts
export const useObras = () => useQuery({ queryKey: ["obras", "resumo"], queryFn: ({ signal }) => obraService.resumo({ signal }) });
```

---

### [DOC-01] [BAIXO] Documentação desatualizada ou que contradiz o código

**Camada:** Ambos (documentação)  
**Arquivo(s):** `docs/RELATORIO_SEGURANCA_OWASP.md` (e `.html`, `docs/EVIDENCIAS_RELATORIO/*`); `docs/MELHORIAS-BENEFICIARIO.md`; `jaf-backend/API-FINANCEIRO.md`; `docs/RESUMO_PAGINACAO.md`; `{FE-ROOT}/README.md`; `README.md`; `.env.example`  
**Descrição:** *(FE D1–D11)* **D1** a doc diz "permissões efetivas do servidor, não uma tabela fixa", mas existia `PERMISSOES_POR_CARGO` fixa (corrigido em INT-06); **D2** a doc afirma cookies "HttpOnly e Secure" — era falso até SEG-06; **D3** "próximo login" × realidade (D-4); **D4** endpoints paginados descritos como entregues sem uso (INT-11); **D5** permissões `GERAR/VISUALIZAR_RELATORIO` sem tela (FE-01, INT-06); **D6** exportação essencial não atendida (FE-01); **D7** UI promete "alocado" sem verificar (INT-06); **D8** presença com horários sem UI (INT-13); **D9** `MapPin` enganoso (FE-12); **D10** "124 testes / lint aprovado" não verificável (FE-17); **D11** `react/README.md` do template, `recharts` não usado (FE-14). Também faltam: funcionalidades **sem UI** (desativar funcionário — BE-05; editar/excluir funcionário — `EDITAR/DELETAR_FUNCIONARIO`; lançamentos de recebimento — BE-16), que devem constar como pendências.  
**Impacto:** decisões tomadas com base em informação falsa (ex.: achar que o cookie é `Secure`).  
**Instrução para o Copilot (executar por último):** atualizar cada documento para refletir o estado real **depois** das correções (cookie/CSRF/HTTPS, permissões efetivas, paginação, exportação, presença); registrar em `README.md` como rodar backend/frontend/testes e as variáveis (`APP_COOKIE_SECURE`, `APP_TRUSTED_PROXY_HOPS`, `JWT_EXPIRATION` em segundos, `APP_BOOTSTRAP_ADMIN_*`); listar as pendências sem UI; remover afirmações não verificadas ou substituí-las pelos números reais.  
**Restrições:** apenas documentação; não afirmar o que não foi verificado; não incluir valores de segredos.  
**Exemplo de correção:**
```markdown
Cookie de sessão: `jwt` — HttpOnly; Secure (quando `APP_COOKIE_SECURE=true`, obrigatório em produção); SameSite=Strict; Max-Age = `JWT_EXPIRATION` (segundos).
```

---

## Resumo Final

| ID | Severidade | Camada | Nome do Problema | Ordem de Execução |
|----|------------|--------|-----------------|-------------------|
| DB-01 | CRÍTICO | Banco de Dados | Schema gerado pelo Hibernate (`ddl-auto=update`) e tabela `presenca` sem migration | 1 |
| DB-02 | CRÍTICO | Banco de Dados + Backend | Unicidade garantida só por código (*check-then-insert*), sem `UNIQUE` no banco | 2 |
| DB-03 | MÉDIO | Banco de Dados | Falta de índices em colunas críticas | 3 |
| DB-04 | MÉDIO | Banco de Dados + Backend | Dinheiro como `VARCHAR`, colunas sem `NOT NULL`/`CHECK` e migrations sem constraints | 4 |
| DB-05 | MÉDIO | Banco de Dados | Exclusão física sem política, sem desativação de funcionário e sem versão de token | 5 |
| DB-06 | MÉDIO | Banco de Dados | Sem controle de concorrência (`@Version`) nem colunas de auditoria | 6 |
| DB-07 | CRÍTICO | Banco de Dados | Tabela de Notas Fiscais inexistente (requisito essencial) | 7 |
| DB-08 | MÉDIO | Banco de Dados | `totalRecebido` é um número sobrescrito, sem histórico de entradas | 8 |
| DB-09 | MÉDIO | Banco de Dados | `"REEMBOLSO"` usado como método de pagamento e valores de domínio inconsistentes **[NOVO]** | 9 |
| SEG-01 | CRÍTICO | Backend | Hash BCrypt das senhas exposto por vários endpoints | 10 |
| SEG-02 | CRÍTICO | Backend + Infra | Credenciais padrão conhecidas, seed ligado por padrão e dados de demonstração em produção | 11 |
| SEG-03 | CRÍTICO | Backend + Integração | Sessão ausente/expirada responde 403 (e não 401): o frontend nunca se recupera **[NOVO]** | 12 |
| SEG-04 | CRÍTICO | Backend + Infra | Unidade de `JWT_EXPIRATION` diverge entre Terraform e código; segredo sem validação na subida **[NOVO]** | 13 |
| SEG-05 | CRÍTICO | Integração | Tráfego em HTTP: HTTPS opcional no ALB, sem HSTS | 14 |
| SEG-06 | CRÍTICO | Backend | Cookie JWT sem `Secure`/`SameSite` e com `Max-Age` fixo | 15 |
| SEG-07 | CRÍTICO | Backend | Sem proteção contra força bruta no login | 16 |
| SEG-08 | MÉDIO | Backend | Login devolve 500 para e-mail inexistente e 401 para senha errada (enumeração de usuários) | 17 |
| SEG-09 | MÉDIO | Backend | JWT sem revogação, `sub` mutável (e-mail) e cookie inválido ignora o header | 18 |
| SEG-10 | MÉDIO | Backend | CSRF desabilitado com autenticação por cookie | 19 |
| SEG-11 | MÉDIO | Backend | CORS com curinga `*.elb.amazonaws.com` e credenciais; `TRACE` e `*` em headers | 20 |
| SEG-12 | MÉDIO | Backend | Swagger, H2 console e `frameOptions` desabilitado em produção; sem cabeçalhos de segurança no backend | 21 |
| SEG-13 | MÉDIO | Backend | IDOR de leitura em alocações/relatórios e escalada por auto-alocação | 22 |
| SEG-14 | MÉDIO | Backend | `/obras/paginado` sem whitelist de ordenação nem teto de tamanho (vazamento do orçamento por ordenação) | 23 |
| SEG-15 | MÉDIO | Backend | Upload de foto de perfil inseguro e `/uploads` público; `fotoUrl` arbitrária | 24 |
| SEG-16 | MÉDIO | Backend | Detalhes de erro, SQL e PII em logs e respostas | 25 |
| SEG-17 | MÉDIO | Ambos | Exclusão da própria conta sem reautenticação e sem proteção do último ADMIN | 26 |
| SEG-18 | MÉDIO | Ambos | Artefatos sensíveis e protótipo legado versionados no Git | 27 |
| SEG-19 | MÉDIO | Integração | `frontend.sh` usa `set -x` e imprime o token do GHCR no log do cloud-init **[NOVO]** | 28 |
| SEG-20 | BAIXO | Backend | Política de senha inconsistente entre criação, troca e frontend | 29 |
| SEG-21 | BAIXO | Backend + Infra | Dependências, imagens, defaults de credenciais, portas expostas e pipeline | 30 |
| BE-01 | MÉDIO | Backend | Regras de autorização/escopo duplicadas e inconsistentes | 31 |
| BE-02 | MÉDIO | Backend | `GlobalExceptionHandler` transforma erros do framework em 500 e há 3 formatos de erro | 32 |
| BE-03 | MÉDIO | Backend | Transações ausentes e inconsistentes | 33 |
| BE-04 | MÉDIO | Backend | N+1, `@ManyToOne` EAGER, consultas repetidas por request, `ObraSerializer` consultando o banco e pool sem tuning | 34 |
| BE-05 | MÉDIO | Backend | Desativação de funcionário e exclusão com histórico devolvendo 500 | 35 |
| BE-06 | MÉDIO | Backend | Sem controle otimista de concorrência | 36 |
| BE-07 | MÉDIO | Backend | Agregações feitas em memória e listas sem paginação | 37 |
| BE-08 | CRÍTICO | Backend | Gestão de Notas Fiscais inexistente no backend (requisito essencial) | 38 |
| BE-09 | CRÍTICO | Backend | Listagem de gastos sem filtro de data (requisito essencial) | 39 |
| BE-10 | CRÍTICO | Backend | Exportação de relatório de gastos para a contabilidade inexistente | 40 |
| BE-11 | MÉDIO | Backend | Dashboard: contrato de acesso contraditório e números/rótulos imprecisos | 41 |
| BE-12 | MÉDIO | Backend | Alocação: `dtFim` ignorada nas permissões, sem como encerrar, `atualizar` troca funcionário/obra | 42 |
| BE-13 | MÉDIO | Backend | Gasto: autoria forjável, edição por qualquer alocado, sem auditoria nem validação de status da obra | 43 |
| BE-14 | MÉDIO | Backend | Reembolso: sem estorno, dupla fonte de verdade e flag legada aceita | 44 |
| BE-15 | MÉDIO | Backend | Permissões: lista personalizada substitui, lista vazia volta ao padrão, cargo nulo vira ENGENHEIRO | 45 |
| BE-16 | MÉDIO | Backend | `totalRecebido` sobrescrito, sem histórico de recebimentos | 46 |
| BE-17 | MÉDIO | Backend | Validação de entrada insuficiente e regras de obra/gasto incompletas | 47 |
| BE-18 | MÉDIO | Backend | Domínio financeiro por strings mágicas e `Cargo` com 11 valores após a V8 reduzir para 3 | 48 |
| BE-19 | MÉDIO | Backend | Autenticação duplicada, rotas e serviços mortos | 49 |
| BE-20 | BAIXO | Backend | Injeção por campo, `allow-bean-definition-overriding`, comentários históricos e configuração decorativa | 50 |
| BE-21 | MÉDIO | Backend + Infra | Fuso horário: `LocalDate.now()` em UTC na AWS gera erro de regra | 51 |
| BE-22 | MÉDIO | Backend | OCR assíncrono: jobs presos, resultado perdido, retenção de dados e endpoint legado sem timeout | 52 |
| BE-23 | MÉDIO | Backend + Infra | Uploads em disco local com backend atrás de ALB/Auto Scaling | 53 |
| BE-24 | MÉDIO | Backend | Sem trilha de auditoria e quase sem observabilidade | 54 |
| BE-25 | BAIXO | Backend | Testes não cobrem os pontos de maior risco | 55 |
| BE-26 | BAIXO | Backend | `PUT /funcionarios/{id}` exige senha que ignora e OpenAPI incoerente | 56 |
| INT-01 | MÉDIO | Ambos | Entidades JPA usadas como contrato de API (sem DTO de resposta) | 57 |
| INT-02 | MÉDIO | Ambos | Três formatos de erro e tratamento de erros inconsistente no frontend | 58 |
| INT-03 | MÉDIO | Ambos | Validações do frontend divergem das do backend | 59 |
| INT-04 | MÉDIO | Ambos | Domínio do gasto por strings: `"REEMBOLSO"` como método, categorias e etapas fixas no cliente **[NOVO]** | 60 |
| INT-05 | MÉDIO | Ambos | Cargo do sistema × função na obra misturados (`GESTOR_OBRA`, `cargoNaObra` × `cargo`) **[NOVO]** | 61 |
| INT-06 | MÉDIO | Ambos | Permissões: matriz duplicada no cliente, fallback fabricado, salvamento em duas chamadas e promessas que a UI não cumpre | 62 |
| INT-07 | MÉDIO | Ambos | Dashboard: período invertido, `etapa` por string e pizza lida por nome/índice **[NOVO]** | 63 |
| INT-08 | MÉDIO | Ambos | Responsável do gasto: padrão = primeira alocação e qualquer alocado pode ser escolhido | 64 |
| INT-09 | MÉDIO | Ambos | Criar obra + alocar responsável são duas chamadas não atômicas **[NOVO]** | 65 |
| INT-10 | MÉDIO | Ambos | Editar obra exige `orcamento`, que o RA nunca recebe **[NOVO]** | 66 |
| INT-11 | MÉDIO | Ambos | Filtro, ordenação e paginação 100% no cliente; N+1 na Home e total subestimado silenciosamente | 67 |
| INT-12 | MÉDIO | Ambos | POSTs sem idempotência em rede móvel instável e edição concorrente silenciosa | 68 |
| INT-13 | MÉDIO | Ambos | Presença usa "alternar" (toggle) em vez do estado desejado | 69 |
| INT-14 | MÉDIO | Ambos | Fluxo de alocação incompleto, rotas redundantes e "responsável" arbitrário | 70 |
| INT-15 | MÉDIO | Ambos | Timeout global de 10 s também para upload e OCR, sem validação de arquivo; polling sem controle | 71 |
| INT-16 | MÉDIO | Ambos | Fuso horário e virada de dia: datas calculadas e exibidas de formas diferentes | 72 |
| INT-17 | MÉDIO | Ambos | Situação do reembolso invisível na lista de gastos; contração de `reembolso_concluido` | 73 |
| INT-18 | MÉDIO | Integração | nginx: `X-Forwarded-Proto` errado, cabeçalhos perdidos por `location`, sem CSP/HSTS e cache imutável para arquivos sem hash **[NOVO]** | 74 |
| INT-19 | BAIXO | Ambos | Dinheiro como `number` (ponto flutuante) e tipos monetários heterogêneos | 75 |
| FE-01 | CRÍTICO | Frontend | "Exportação de relatório de gastos" para a contabilidade não existe | 76 |
| FE-02 | CRÍTICO | Frontend | Interface de Notas Fiscais (anexar, consultar, baixar) **[NOVO]** | 77 |
| FE-03 | MÉDIO | Frontend | Estado de autenticação do cliente por `localStorage`, troca de usuário em outra aba e expiração sem aviso | 78 |
| FE-04 | MÉDIO | Frontend | Autorização lida de um singleton fora do React, durante o render; `UserContext` não memoizado | 79 |
| FE-05 | MÉDIO | Frontend | Senhas no console do navegador após falha de requisição | 80 |
| FE-06 | MÉDIO | Frontend | Requisições sem cancelamento, respostas fora de ordem e respostas sem tipo/validação | 81 |
| FE-07 | MÉDIO | Frontend | Sem `ErrorBoundary` | 82 |
| FE-08 | MÉDIO | Frontend | Componente-monolito `DetalhamentoObras` (≈1149 linhas) | 83 |
| FE-09 | MÉDIO | Frontend | Duplicação sistemática de helpers/constantes e formatação inconsistente | 84 |
| FE-10 | MÉDIO | Frontend | Modais artesanais inacessíveis que fecham ao clicar fora e perdem os dados; elementos clicáveis sem semântica | 85 |
| FE-11 | MÉDIO | Frontend | Dashboard desmonta a tela inteira a cada filtro; barras de progresso sem limite | 86 |
| FE-12 | MÉDIO | Frontend | Home: falha vira "Nenhuma obra encontrada", imagens falsas/externas, "primeira obra" implícita | 87 |
| FE-13 | BAIXO | Frontend | Sem code-splitting | 88 |
| FE-14 | BAIXO | Frontend | Código morto, restos de template, estilos inline e nomes de arquivo inconsistentes | 89 |
| FE-15 | BAIXO | Frontend | Rotas de obra sem permissão explícita e lista completa de funcionários em telas de operação | 90 |
| FE-16 | BAIXO | Frontend | Linguagem técnica para o perfil de usuário-alvo | 91 |
| FE-17 | BAIXO | Frontend | Cobertura de testes desigual e documentação de testes não verificável | 92 |
| FE-18 | BAIXO | Frontend | Build e entrega do frontend | 93 |
| FE-19 | BAIXO | Frontend | Cada tela implementa seu próprio fetch/loading/erro/cache | 94 |
| DOC-01 | BAIXO | Ambos | Documentação desatualizada ou que contradiz o código | 95 |

- **Total de problemas críticos:** 15
- **Total de problemas médios:** 66
- **Total de problemas baixos:** 14
- **Total geral:** **95**

**Por fase:** Banco de Dados 9 · Segurança e Autenticação 21 · Backend 26 · Integração 19 · Frontend 19 · Documentação 1.

**Críticos (15):** DB-01, DB-02, DB-07, SEG-01, SEG-02, SEG-03, SEG-04, SEG-05, SEG-06, SEG-07, BE-08, BE-09, BE-10, FE-01, FE-02.

---

## Apêndice A — Mapa de migrations (numeração reservada)

| Versão | Arquivo | Item |
|---|---|---|
| V14 | `V14__create_presenca.sql` | DB-01 |
| V15 | `V15__constraints_unicidade.sql` | DB-02 |
| V16 | `V16__indices.sql` | DB-03 |
| V17 | `V17__tipos_e_constraints.sql` | DB-04 |
| V18 | `V18__funcionario_ativo_token.sql` | DB-05 |
| V19 | `V19__versao_e_auditoria.sql` | DB-06 |
| V20 | `V20__create_nota_fiscal.sql` | DB-07 |
| V21 | `V21__create_recebimento_obra.sql` | DB-08 |
| V22 | `V22__gasto_reembolsavel_normalizacao.sql` | DB-09 |
| V23 | `V23__estorno_pagamento_reembolso.sql` | BE-14 |
| V24 | `V24__funcionario_permissoes_personalizadas.sql` | BE-15 |
| V25 | `V25__ocr_solicitante_id.sql` | BE-22 |
| V26 | `V26__metodo_reembolso_contracao.sql` | INT-04 (só depois do frontend novo em produção) |
| V27 | `V27__idempotency_key.sql` | INT-12 |
| V28 | `V28__drop_reembolso_concluido.sql` | INT-17 (só depois do frontend novo em produção) |

## Apêndice B — Marcos de implantação sugeridos

*Os marcos agrupam itens para **implantar** juntos; a ordem de **implementação** continua sendo a numeração do "Resumo Final". Exceção permitida: **M1 pode ser feito antes da fase 1** (não depende de banco nem de frontend) se houver urgência de contenção.*

| Marco | Itens | Observação |
|---|---|---|
| **M1 — Contenção** | SEG-01, SEG-04, SEG-19 | Rotacionar `JWT_SECRET`, senhas e token do GHCR; trocar a senha de `admin@gmail.com` manualmente; não depende de mudança no frontend nem de migrations |
| **M2 — Banco confiável e credenciais** | DB-01…DB-09, SEG-02 | Backup antes; reconciliar schema e resolver duplicidades/sobreposições antes da V15; `baseline` manual só depois da reconciliação; SEG-02 só depois de DB-01 |
| **M3 — Sessão** | SEG-03, SEG-05, SEG-06, SEG-07, SEG-08, SEG-09, SEG-10, SEG-11 | HTTPS implantado **antes** de `Secure=true`; ao subir SEG-09 todas as sessões são encerradas (avisar) |
| **M4 — Backend funcional** | BE-01…BE-26 | Contrato JSON inalterado (apenas campos/endpoints novos); `app.gasto.restringir-autor=false` |
| **M5 — Contratos** | INT-01…INT-19 | Em cada item: backend primeiro, depois frontend, depois (se houver) contração; `restringir-autor=true` e V26/V28 só ao final |
| **M6 — Frontend e documentação** | FE-01…FE-19, DOC-01 | CSP bloqueante (INT-18) somente depois de FE-01 e FE-12 |

## Apêndice C — Rastreabilidade (nada foi omitido)

Cada achado original aponta para o(s) item(ns) que o corrigem.

**Backend (`ANALISE_CRITICA_BACKEND.md`, 66):**  
1.1 → BE-01, SEG-13 · 1.2 → BE-02 · 1.3 → BE-19 · 1.4 → BE-18, DB-09, INT-04 · 1.5 → BE-18, INT-05 · 1.6 → BE-04, INT-01 · 1.7 → BE-20 · 1.8 → BE-02 · 1.9 → BE-20, SEG-11 ·
2.1 → SEG-01, INT-01 · 2.2 → SEG-02 · 2.3 → SEG-05, SEG-06, SEG-10 · 2.4 → SEG-07 · 2.5 → SEG-08 · 2.6 → SEG-12 · 2.7 → SEG-11 · 2.8 → SEG-04, SEG-09 · 2.9 → SEG-15 · 2.10 → SEG-13 · 2.11 → SEG-13 · 2.12 → BE-17 · 2.13 → SEG-16 · 2.14 → SEG-14 · 2.15 → SEG-17 · 2.16 → SEG-18 · 2.17 → SEG-20 · 2.18 → SEG-21 · 2.19 → SEG-21 ·
3.1 → DB-01 · 3.2 → DB-02 · 3.3 → DB-03 · 3.4 → BE-04 · 3.5 → DB-04, DB-09, BE-14, INT-17, INT-19 · 3.6 → BE-03 · 3.7 → DB-05, BE-05 · 3.8 → DB-06, BE-06, INT-12 · 3.9 → DB-04, DB-06, SEG-02 · 3.10 → BE-07, INT-11 ·
4.1 → DB-07, BE-08, BE-09, BE-10, FE-01, FE-02 · 4.2 → BE-11 · 4.3 → SEG-13, INT-09 · 4.4 → BE-12 · 4.5 → BE-13, INT-08, DB-06 · 4.6 → BE-14, INT-17 · 4.7 → BE-15, INT-06 · 4.8 → DB-05, BE-05 · 4.9 → DB-08, BE-16 · 4.10 → BE-17 · 4.11 → BE-11 ·
5.1 → INT-01 · 5.2 → SEG-03, BE-02 · 5.3 → BE-02, INT-02 · 5.4 → INT-11, SEG-14 · 5.5 → BE-22 · 5.6 → BE-19, INT-14 · 5.7 → BE-26 · 5.8 → BE-26 ·
6.1 → BE-23 · 6.2 → BE-21, INT-16 · 6.3 → BE-04 · 6.4 → BE-22 · 6.5 → DB-02, INT-12 · 6.6 → BE-07 · 6.7 → BE-24, BE-13, DB-06 · 6.8 → BE-25 · 6.9 → SEG-21

**Frontend (`ANALISE_CRITICA_FRONTEND.md`, 56):**  
Q1 → FE-08 · Q2 → FE-04 · Q3 → FE-06 · Q4 → FE-07 · Q5 → FE-13 · Q6 → FE-09 · Q7 → FE-14, SEG-18 · Q8 → INT-11, BE-09 · Q9 → FE-06 · Q10 → FE-04 · Q11 → FE-14 · Q12 → FE-17 ·
S1 → SEG-05, SEG-06, SEG-10, SEG-11 · S2 → FE-03 · S3 → FE-03 · S4 → FE-05 · S5 → INT-18 · S6 → SEG-18 · S7 → SEG-17 · S8 → SEG-15 · S9 → FE-01 · S10 → FE-15 · S11 → FE-15 ·
R1 → FE-01, BE-10 · R2 → INT-03, SEG-20, BE-17 · R3 → INT-06 · R4 → INT-06, INT-10, FE-01 · R5 → INT-11 · R6 → INT-13 · R7 → INT-14, INT-09 · R8 → INT-06 · R9 → INT-08, BE-13 · R10 → INT-19 ·
I1 → INT-11 · I2 → INT-15 · I3 → INT-02 · I4 → FE-06 · I5 → INT-07 · I6 → INT-15 ·
U1 → FE-03, SEG-03 · U2 → FE-10 · U3 → FE-11 · U4 → FE-12 · U5 → INT-17 · U6 → FE-09 · U7 → FE-03, SEG-09 · U8 → FE-10 · U9 → FE-11 · U10 → FE-16 · U11 → FE-12 ·
P1 → INT-04 · P2 → INT-16 · P3 → INT-12 · P4 → INT-18 · P5 → FE-18, SEG-21 · P6 → FE-19

**Inconsistências documentação × código (D1–D11):** todas → DOC-01 (e item técnico correspondente): D1 → INT-06 · D2 → SEG-06 · D3 → INT-06 · D4 → INT-11 · D5 → FE-01, INT-06 · D6 → FE-01 · D7 → INT-06 · D8 → INT-13 · D9 → FE-12 · D10 → FE-17 · D11 → FE-14.

**Novos (integração):** SEG-03 (401×403), SEG-04 (unidade do JWT), SEG-19 (`set -x`), DB-09/INT-04 (`REEMBOLSO` como método), INT-05 (cargo × função), INT-07 (`etapa` padrão), INT-09 (obra + alocação), INT-10 (editar obra sem orçamento), INT-18 (nginx), FE-02 (UI de notas fiscais) e os agravantes marcados **[NOVO]** em SEG-07, SEG-08, SEG-13, BE-13, BE-15, BE-21, INT-02, INT-06, INT-08, FE-05, DB-02.

