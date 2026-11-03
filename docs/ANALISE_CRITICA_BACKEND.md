# Análise Crítica do Backend — JAF (Spring Boot)

> **Escopo:** apenas `jaf-backend/` (Spring Boot 3.4.5, Java 21, PostgreSQL, Flyway, RabbitMQ). Infra (Terraform/Docker) e `POC-API` só foram consultados quando afetam o comportamento do backend.
> **Método:** leitura estática de todo o código-fonte (115 classes Java, 13 migrations, `application.properties`, testes, Dockerfile). **Nada foi executado**; os achados são inferências do código. Onde a conclusão depende de configuração externa, isso está indicado.
> **Documentação usada:** `contexto.txt` (o placeholder `[COLE AQUI SUA DOCUMENTAÇÃO]` não foi preenchido, então assumi que esse arquivo, que estava anexado, é a documentação de referência). A documentação é de **escopo/requisitos**, não técnica. Por isso, "contradições" abaixo são entre requisitos e código. Onde a documentação é omissa, o texto indica **(inferido)**.
> **SQL Injection:** não encontrei. Todo acesso usa derived queries, JPQL com parâmetros nomeados, Criteria API (`GastoFiltro`) ou `JdbcTemplate` parametrizado (`DatabaseSeeder`).

---

## Sumário

1. [Qualidade do Código](#1-qualidade-do-código)
2. [Segurança](#2-segurança)
3. [Banco de Dados](#3-banco-de-dados-postgresql)
4. [Regra de Negócio](#4-regra-de-negócio)
5. [API e Integração](#5-api-e-integração)
6. [Problemas Potenciais](#6-problemas-potenciais)
7. [Resumo Executivo](#resumo-executivo)

---

## 1. Qualidade do Código

### [MÉDIO] 1.1 Regras de autorização/escopo duplicadas e inconsistentes
**Arquivo:** `service/ObraService.java`, `service/GastoService.java`, `service/AlocacaoObraService.java`, `service/RelatorioService.java`, `service/AcessoObraService.java`, `controller/*`
**Descrição:** "O usuário pode ver esta obra?" é reimplementado em pelo menos 5 lugares:
- `ObraService.buscarPorIdComEscopo`
- `GastoService.buscarPorIdComEscopo` e `filtro()`
- `AcessoObraService.exigirObra`
- `ObraService.listarPorUsuario`
- `AlocacaoObraService.listarPorUsuario`

Cada versão faz uma checagem ligeiramente diferente. Alguns endpoints não checam nada (`/alocacoes/obra/{id}`, `/relatorios`). A autorização está dividida entre `@PreAuthorize` (controller) e `exigirX()` (service). `ReembolsoController` e `ObraFinanceiroController` não têm `@PreAuthorize`, então a segurança depende de o dev lembrar de chamar `acesso.exigirX()` no service.
**Impacto:** é a causa raiz dos furos de IDOR do item 2.10 e da auto-alocação do 2.11. Cada endpoint novo é um risco de esquecer a checagem.
**Sugestão:** centralizar em um único componente e aplicar de forma declarativa:
```java
@PreAuthorize("hasAuthority('VISUALIZAR_ALOCACOES') and @acesso.podeVerObra(#obraId)")
public List<AlocacaoObraDto> listarPorObra(@PathVariable Long obraId) { ... }
```
Cachear o usuário atual por request (ver 3.4) e criar um teste que percorra todos os endpoints e valide 401/403.

### [MÉDIO] 1.2 `GlobalExceptionHandler` transforma erros do framework em HTTP 500
**Arquivo:** `config/GlobalExceptionHandler.java` (`@ExceptionHandler(Exception.class)`)
**Descrição:** o handler genérico captura **tudo** e responde 500. Isso inclui casos que não são erro de servidor:
- `HttpRequestMethodNotSupportedException` (deveria ser 405)
- `HttpMediaTypeNotSupportedException` (415)
- `NoResourceFoundException` (404)
- `MaxUploadSizeExceededException` (413)
- `MissingServletRequestPartException` (400)
- `IllegalArgumentException`, lançada em `FuncionarioService.atualizarCargo` com cargo inválido e em `OcrService.process` com arquivo inválido
- `IllegalStateException` (OCR fora do ar, deveria ser 502/503)
- `UsernameNotFoundException` (ver 2.5)
- `DataIntegrityViolationException` (excluir funcionário ou obra com histórico, ver 3.7)
- `PropertyReferenceException` (`sortBy` inválido em `/obras/paginado`)

**Impacto:** alertas de 5xx falsos, o frontend não consegue tratar erros previsíveis, e o log de erro enche de stack trace de ruído.
**Sugestão:** estender `ResponseEntityExceptionHandler` (que já mapeia os erros do Spring MVC) e adicionar handlers específicos:
```java
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail conflito(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Registro em uso ou duplicado.");
    }
    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail auth(AuthenticationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    }
}
```

### [MÉDIO] 1.3 Controller com regra de autenticação, serviço com dependências e código mortos, rota `/auth/login` morta
**Arquivo:** `controller/FuncionarioController.java` (login/logout), `service/FuncionarioService.java`, `controller/auth/AuthController.java`, `service/AuthService.java`
**Descrição:**
- O controller monta o JWT e o cookie, e reconsulta o funcionário depois do `authenticate`.
- `FuncionarioService` injeta `GerenciadorTokenJwt` e `AuthenticationManager` apenas para `autenticar()`, que **ninguém chama**. `listar()` também não é usado, só `listarTodos()`.
- `AuthController` expõe `POST /auth/login`, que devolve só o `id` (sem token) e **não está em `URLS_PERMITIDAS`**. Com `anyRequest().authenticated()` ele é inalcançável para quem não está logado e inútil para quem está. Além disso, ignora qualquer flag de ativo, não gera token e não tem `@Valid`.
- `FuncionarioTokenDto.token` é sempre `null`.

**Impacto:** duas "verdades" para login, superfície de ataque sem uso, confusão para novos devs e violação de SRP.
**Sugestão:** remover `AuthController`, `AuthService` e `FuncionarioService.autenticar/listar`. Criar um `AuthService.login(email, senha)` que devolva o DTO e o token; o controller só escreve o cookie.

### [MÉDIO] 1.4 Strings mágicas e tipos fracos no domínio financeiro
**Arquivo:** `GastoService`, `ReembolsoService`, `DashboardService`, `GastoFiltro`, `model/Gasto.java`, `model/Obra.java`
**Descrição:** `"REEMBOLSO"` (comparação case-sensitive e repetida em 8+ lugares), `"ETAPA 1"`, `"TODAS"`, `"IMPREVISTO"`, `"Custos extras"`, `status` de obra e `categoria` como `String` livre. `GastoDto.metodoPagamento` aceita qualquer texto, mas todo o fluxo de reembolso depende do literal exato. O OCR sugere `PIX`, `CARTAO_DEBITO`, `CARTAO_CREDITO` e `DINHEIRO`, enquanto a seed usa `Débito`/`Crédito`. Esses valores **não casam entre si**.
**Impacto:** um gasto com `metodoPagamento="reembolso"` ou `"Reembolso "` fica fora de todos os relatórios de reembolso. A categoria "Custos extras" digitada de outra forma some do gráfico de imprevistos. Agrupamentos do dashboard fragmentam.
**Sugestão:** `enum MetodoPagamento`, `enum StatusObra`, e `Categoria`/`Etapa` como tabelas (ou ao menos enums) validadas no DTO. Converter com migration.

### [MÉDIO] 1.5 Enum `Cargo` ainda tem 8 valores após a V8 simplificar para 3, e as permissões têm duas fontes
**Arquivo:** `enums/Cargo.java`, `V8__simplify_system_roles.sql`, `V9__create_cargo_permissao.sql`, `dto/FuncionarioDto.java`, `dto/AlocacaoObraDto.java`, `service/CargoPermissaoService.java`
**Descrição:** a V8 migrou `GESTOR_OBRA`, `MESTRE_DE_OBRAS`, `PEDREIRO` etc. para 3 papéis, mas o enum mantém todos. `FuncionarioDto.cargo` e `AlocacaoObraDto.cargoNaObra` aceitam qualquer um. Via API ainda é possível criar um funcionário `PEDREIRO` (permissões do enum, não da tabela). `AlocacaoObraDto` aceita até `ADMIN` como "cargo na obra". As permissões ficam no enum **e** em `cargo_permissao` (V9). Se a tabela estiver vazia, volta silenciosamente para o enum.
**Impacto:** comportamento diferente do documentado ("controle de acesso por nível de permissão") e do banco. Permissões "fantasma" quando a tabela é limpa.
**Sugestão:** separar `CargoSistema` (ADMIN/RESPONSAVEL_ADMINISTRATIVO/ENGENHEIRO) de `FuncaoNaObra`. Escolher uma única fonte de verdade (a tabela) e fazer o seed via migration.

### [MÉDIO] 1.6 `ObraSerializer` consulta o banco durante a serialização JSON
**Arquivo:** `config/ObraSerializer.java`, `model/Obra.java` (`@JsonSerialize`)
**Descrição:** para cada `Obra` serializada, `acesso.isAdmin()` executa `findByEmailIgnoreCase`. Como `Gasto`, `AlocacaoObra` e `Presenca` embutem `Obra`, uma lista de 500 gastos faz até 500 queries **fora** de qualquer transação de serviço (só funciona por causa do Open-In-View). Além disso, o mesmo endpoint devolve JSON com formatos diferentes conforme o papel.
**Impacto:** N+1 oculto, difícil de achar em profiling, e latência cresce linearmente com o tamanho da resposta.
**Sugestão:** usar DTOs de resposta (`ObraResumoDto` e `ObraAdminDto`) escolhidos no service. Se precisar manter o serializer, ler o papel de `SecurityContextHolder` (`authorities`), sem acessar o banco.

### [BAIXO] 1.7 Injeção por campo e `allow-bean-definition-overriding=true`
**Arquivo:** `config/SecurityConfiguracao.java`, `service/AutenticacaoService.java`, `application.properties`
**Descrição:** `@Autowired` em campos (dificulta testes). `spring.main.allow-bean-definition-overriding=true` esconde beans duplicados, e `AutenticacaoFilter` é criado por `@Bean` enquanto a classe não é `@Component`.
**Impacto:** o Spring não avisa se alguém registrar um segundo `PasswordEncoder`, `AuthenticationManager` ou filtro; o mais recente vence sem alerta.
**Sugestão:** injeção por construtor e remover a flag. Se algo quebrar, é um bug real que estava escondido.

### [BAIXO] 1.8 Handlers repetitivos, `NoContent` como exceção, código morto
**Arquivo:** `GlobalExceptionHandler.java`, `exceptions/NoContent.java`, `RelatorioService.listar`
**Descrição:** 10 handlers constroem o mesmo `ErrorResponse` com `request.getDescription(false).replace("uri=", "")`. `NoContent` responde 204 **com corpo** (inválido em HTTP) e é lançada em `FuncionarioService.listar` (não usado). `RelatorioService.listar` testa `relatorioRepository == null` (nunca acontece). Erros de negócio de validação são logados como `ERROR` (`handleBadRequest`).
**Impacto:** ruído e manutenção. Um 204 com corpo pode ser descartado por proxies.
**Sugestão:** um método utilitário (ou `ProblemDetail`), lista vazia = `200 []`, remover o código morto e logar 4xx em `WARN`/`INFO`.

### [BAIXO] 1.9 Higiene geral
**Arquivo:** vários
**Descrição:**
- Comentários "CORREÇÃO DE SEGURANÇA A0x ... Antes/Agora" dentro do código (histórico pertence ao git).
- Nomes totalmente qualificados inline (`com.jaf.application.enums.Cargo`, `java.util.Map`).
- Indentação inconsistente nos DTOs.
- Parâmetros `Authentication` não usados em `GastoController.listar` e `listarPaginado`.
- `@CrossOrigin` redundante em `AuthController`.
- `app.cors.allowed-origins` no `.properties` não é lido por nada (a lista real está hardcoded em `SecurityConfiguracao`). `.env.example` lista `CORS_ALLOWED_ORIGINS` e `JWT_REFRESH_EXPIRATION`, ambos sem uso.
- Mistura de PT/EN.

**Impacto:** configuração "decorativa" dá falsa sensação de controle (alguém muda a env e nada acontece).
**Sugestão:** limpar e ligar o CORS a `@ConfigurationProperties`.

---

## 2. Segurança

### [CRÍTICO] 2.1 Hash BCrypt das senhas é exposto por vários endpoints (entidades JPA serializadas direto)
**Arquivo:** `model/Funcionario.java` (sem `@JsonIgnore` em `senha`); `controller/GastoController.java`, `AlocacaoObraController.java`, `RelatorioController.java`, `ObraController.java` (`/obras/{id}/gastos`, `/obras/{id}/alocacoes`)
**Descrição:** `Gasto.funcionario`, `AlocacaoObra.funcionario` e `Relatorio.funcionarioResponsavel` são `@ManyToOne` para a entidade `Funcionario`, que tem `getSenha()` sem nenhuma anotação de Jackson. Os controllers retornam as entidades diretamente. Logo `GET /gastos`, `/gastos/{id}`, `/gastos/paginado`, `/obras/{id}/gastos`, `/alocacoes*`, `/obras/{id}/alocacoes` e `/relatorios*` devolvem `"funcionario": {"id":…, "email":…, "senha":"$2a$10$…", "cargoGlobal":…}`. Os testes (`FuncionarioAutorizacaoApiTest`) só verificam `$.senha` no nível raiz dos endpoints de funcionário, então **não pegam** o vazamento aninhado.
**Impacto:** qualquer usuário com `VISUALIZAR_GASTOS` ou `VISUALIZAR_ALOCACOES` (inclusive o papel `ENGENHEIRO`) baixa os hashes de todos os colegas. Combinado com 2.2 (todos os hashes seed são **o mesmo hash** de uma senha conhecida), o vazamento é imediatamente explorável. Como o hash seed é comum, o atacante já sabe qual é a senha dos usuários que não a trocaram.
**Sugestão:** (1) nunca devolver entidade; criar DTOs de resposta (ver 5.1). (2) Defesa em profundidade na entidade:
```java
@JsonIgnore
@Column(name = "senha", nullable = false)
private String senha;
```
(3) Adicionar teste que varre o JSON de **todos** os endpoints e falha se encontrar `"senha"` em qualquer nível. (4) Tratar como incidente: forçar troca de todas as senhas depois da correção.

### [CRÍTICO] 2.2 Credenciais padrão conhecidas e dados de demonstração criados em produção
**Arquivo:** `config/DatabaseSeeder.java`, `V2__insert_user.sql`, `V3__insert_dados_iniciais.sql`, `V6__align_gastos_dashboard.sql`, `application.properties` (`app.seed.enabled=${APP_SEED_ENABLED:true}`), `dto/FuncionarioLoginDto.java`, `terraform/scripts/backend.sh`
**Descrição:**
- O seed é **ligado por padrão** (`matchIfMissing = true` e default `true`). O `terraform/scripts/backend.sh` não define `APP_SEED_ENABLED`, então em produção o backend cria `admin@gmail.com` (ADMIN) e 9 outros usuários, todos com a senha `Admin@123` (declarada nos comentários do seeder e no `@Schema(example=...)` do login, **visível no Swagger público**).
- Além do seeder, as migrations V2 e V3 inserem os mesmos usuários, obras e gastos fictícios em qualquer banco novo.
- O `NOT EXISTS` do seeder faz com que um usuário excluído **ressuscite no próximo restart**.

**Impacto:** takeover imediato de uma conta ADMIN em produção por qualquer pessoa que leia o repositório. Dados falsos misturados aos dados reais da JAF, o que corrompe relatórios enviados à contabilidade.
**Sugestão:**
- Seed apenas no profile `dev` (`@Profile("dev")`; remover `matchIfMissing`).
- Remover V2/V3/V6 das migrations de produção; criar o primeiro ADMIN por variável de ambiente (`APP_BOOTSTRAP_ADMIN_EMAIL` / senha gerada e `must_change_password=true`).
- Remover exemplos de senha do `@Schema`.
- Alterar a senha do `admin@gmail.com` e dos demais imediatamente se algum ambiente já subiu.

### [CRÍTICO] 2.3 Cookie JWT sem `Secure`/`SameSite`, com CSRF desabilitado
**Arquivo:** `controller/FuncionarioController.java` (login/logout), `config/SecurityConfiguracao.java` (`csrf.disable()`), `terraform/modules/alb`
**Descrição:** o cookie é criado com `setSecure(false)` fixo ("ajustado para desenvolvimento"), sem `SameSite` (a classe `jakarta.servlet.http.Cookie` não suporta; o comportamento depende do default do navegador) e `Max-Age=3600` hardcoded, desacoplado de `jwt.validity`. O CSRF está desabilitado, o que só é seguro com token em header. Aqui a autenticação é por **cookie**, e o filtro dá prioridade ao cookie sobre o header. O ALB aceita HTTP (porta 80 → frontend, porta 8080 → backend); o HTTPS é opcional na infra.
**Impacto:** em HTTP, o token trafega em texto claro e pode ser capturado em rede compartilhada (obra, celular em Wi-Fi público: o caso de uso documentado, "mobile first"). Sem `SameSite=Lax/Strict` explícito e sem CSRF, browsers que não aplicam Lax por padrão ficam vulneráveis a requisições forjadas (excluir gasto, mudar cargo, criar funcionário).
**Sugestão:**
```java
ResponseCookie cookie = ResponseCookie.from("jwt", token)
    .httpOnly(true).secure(true).sameSite("Strict").path("/")
    .maxAge(Duration.ofSeconds(jwtValiditySeconds)).build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```
`secure` deve vir de propriedade (`true` em prod). Forçar HTTPS no ALB e enviar HSTS. Se mantiver cookie, habilitar CSRF (`CookieCsrfTokenRepository`) ou exigir header customizado em requisições mutáveis.

### [CRÍTICO] 2.4 Sem proteção contra força bruta no login
**Arquivo:** `FuncionarioController.login`, `config/AutenticacaoProvider.java`, `SecurityConfiguracao.java`
**Descrição:** não há rate limit, contador de falhas, lockout, CAPTCHA, nem atraso progressivo. `isAccountNonLocked()` sempre retorna `true`. Somados a uma conta ADMIN com senha conhecida (2.2) e à política fraca de troca de senha (2.17), o login é um alvo trivial.
**Impacto:** ataque de dicionário contra o ADMIN, sem nenhuma barreira.
**Sugestão:** `Bucket4j` ou filtro simples por IP + e-mail (ex.: 5 falhas/15 min → 429), registrar tentativas e usar `AuthenticationFailureBadCredentialsEvent`. No ALB, usar AWS WAF com rate-based rule.

### [MÉDIO] 2.5 Login devolve 500 para e-mail inexistente e 401 para senha errada (enumeração de usuários)
**Arquivo:** `config/AutenticacaoProvider.java`, `service/AutenticacaoService.java`, `GlobalExceptionHandler.java`
**Descrição:** `AutenticacaoService.loadUserByUsername` lança `UsernameNotFoundException`. O `AutenticacaoProvider` customizado não converte em `BadCredentialsException` (o `DaoAuthenticationProvider` faria). O handler genérico responde **500**, enquanto senha errada responde **401**. Também há diferença de tempo: com usuário inexistente não há `BCrypt.matches` (resposta muito mais rápida).
**Impacto:** o atacante descobre quais e-mails existem; erro 500 de login polui o monitoramento.
**Sugestão:** capturar `UsernameNotFoundException` no provider, executar um `passwordEncoder.matches` fictício (para equalizar o tempo) e lançar sempre `BadCredentialsException("Credenciais inválidas")`. Ou usar `DaoAuthenticationProvider` padrão.

### [MÉDIO] 2.6 Swagger, H2 console e `frameOptions` desabilitado em produção
**Arquivo:** `SecurityConfiguracao.URLS_PERMITIDAS`, `HomeController`
**Descrição:** `/swagger-ui/**`, `/v3/api-docs/**`, `/h2-console/**` e `/` (redireciona para o Swagger) são `permitAll`. `headers.frameOptions().disable()` vale para **toda** a aplicação (só seria necessário para o console H2). Não há CSP, HSTS, `X-Content-Type-Options`, `Referrer-Policy`. H2 está como dependência de runtime e o console não é habilitado por padrão, mas a rota está liberada e a dependência vai no jar.
**Impacto:** mapa completo da API para qualquer visitante; clickjacking; MIME sniffing em `/uploads`.
**Sugestão:** `@Profile("!prod")` para Swagger (ou `springdoc.api-docs.enabled=false` em prod), remover `/h2-console/**` e a dependência H2 (trocar por Testcontainers/Postgres nos testes), `frameOptions.deny()` e headers de segurança padrão.

### [MÉDIO] 2.7 CORS com curinga em ELBs da AWS e `allowCredentials(true)`
**Arquivo:** `SecurityConfiguracao.corsConfigurationSource`
**Descrição:** `allowedOriginPatterns` inclui `http://*.elb.amazonaws.com` e `https://*.elb.amazonaws.com`. Esse domínio é compartilhado por **todos os clientes da AWS**: qualquer pessoa pode subir um ALB e ter uma origem aceita **com credenciais**. Também permite `TRACE` e `allowedHeaders=*`. A origem `http://` aceita páginas sem TLS.
**Impacto:** hoje é mitigado pelo comportamento padrão dos browsers modernos com cookies cross-site, mas é um furo de design que se agrava com `SameSite=None` ou com browsers sem esse default.
**Sugestão:** lista exata do(s) DNS do seu ALB/CloudFront (via propriedade `app.cors.allowed-origins` que **já existe** e não é usada), remover `TRACE`, restringir headers (`Content-Type`, `Authorization`). Como o frontend usa proxy `/api` na mesma origem (`VITE_API_URL=/api`), o CORS pode até ser desnecessário em prod.

### [MÉDIO] 2.8 JWT: sem revogação, subject mutável, secret sem validação, lib antiga
**Arquivo:** `config/GerenciadorTokenJwt.java`, `AutenticacaoFilter.java`, `FuncionarioController.logout`, `pom.xml` (jjwt 0.11.5)
**Descrição:**
- Logout só apaga o cookie no navegador; o token continua válido até expirar (o header `Authorization` também é aceito). Trocar senha, rebaixar ou excluir o usuário não invalida o token existente (exclusão pelo menos falha em `loadUserByUsername`).
- `sub` = e-mail, que o usuário pode alterar (`PUT /funcionarios/me`); o token antigo para de funcionar e o JOB OCR perde o dono (`solicitante` é e-mail).
- `JWT_SECRET` e `JWT_EXPIRATION` sem validação: se o segredo (Base64) tiver < 256 bits, `Keys.hmacShaKeyFor` lança `WeakKeyException` **no primeiro login**, não na subida; o app sobe "saudável" e ninguém loga. `JWT_EXPIRATION` não tem default.
- Claims `authorities`/`cargo` são geradas e nunca lidas (as permissões vêm do banco a cada request, o que é bom, mas é código inútil).
- Sem `iss`/`aud`/`jti`; sem refresh token (apesar de `.env.example` listar `JWT_REFRESH_EXPIRATION`).
- Se o cookie `jwt` estiver inválido/expirado, o header Bearer válido **não** é tentado.

**Impacto:** token roubado vale por toda a duração; app com configuração errada sobe e falha tarde.
**Sugestão:** usar o ID do usuário (`id`) como `sub`; validar secret e expiração com `@ConfigurationProperties` + `@Validated` (falha na subida); adicionar `tokenVersion` no usuário (incrementado em troca de senha/cargo/logout) e conferi-lo no filtro; atualizar para jjwt 0.12.x ou usar `spring-security-oauth2-resource-server` (Nimbus).

### [MÉDIO] 2.9 Upload de foto de perfil inseguro e público
**Arquivo:** `controller/FuncionarioPerfilController.java` (`uploadFoto`), `config/StaticResourceConfig.java`, `SecurityConfiguracao` (`/uploads/**` permitAll)
**Descrição:** o tipo é validado só por `file.getContentType()` (header do cliente, forjável); a extensão vem do **nome original** do arquivo (`foto.html`, `foto.svg`, `foto.jsp`) e é preservada. Não há verificação de *magic bytes*. O arquivo é servido por `ResourceHandler` com o Content-Type deduzido da extensão, sem autenticação. O upload não atualiza o perfil (o cliente depois chama `PUT /me` com `fotoUrl` livre). Sem limite por usuário, sem limpeza de órfãos.
**Impacto:** XSS armazenado (HTML/SVG servido como `text/html`/`image/svg+xml`) na origem da API; hospedagem de conteúdo arbitrário; disco cheio. Torna-se crítico se `/uploads` for servido na mesma origem do frontend.
**Sugestão:** validar *magic bytes* (ex.: `ImageIO.read` / Apache Tika), **ignorar** a extensão do cliente e derivá-la do tipo detectado, rejeitar SVG, servir com `X-Content-Type-Options: nosniff` e `Content-Disposition`, exigir autenticação em `/uploads/**`, gravar o arquivo e atualizar `fotoUrl` na mesma operação (e remover o antigo). Em produção, usar S3 (ver 6.1).

### [MÉDIO] 2.10 IDOR de leitura em alocações e relatórios (falta checar escopo por obra)
**Arquivo:** `controller/AlocacaoObraController.java` (`/obra/{obraId}`, `/funcionario/{funcionarioId}`, `/filtro`, `/{id}`), `service/AlocacaoObraService.java`, `controller/RelatorioController.java`
**Descrição:** o documentado (e implementado em `/obras`, `/gastos`, `/presencas`) é que usuário não-ADMIN só vê as obras em que está alocado. Em `/alocacoes/obra/{id}`, `/alocacoes/funcionario/{id}`, `/alocacoes/filtro?obraId=` e `/alocacoes/{id}`, basta ter `VISUALIZAR_ALOCACOES` para ler a alocação de **qualquer** obra/funcionário. `/relatorios` e `/relatorios/{id}` também não filtram. Essas respostas ainda carregam o hash de senha (2.1).
**Impacto:** quebra do isolamento por obra; ex-funcionários e obras confidenciais visíveis a todos.
**Sugestão:** aplicar `acesso.exigirObra(obraId)` e filtrar listas pelo escopo do usuário (ver 1.1). Para `/alocacoes/funcionario/{id}`, permitir só ADMIN/o próprio usuário.

### [MÉDIO] 2.11 Escalada: quem pode criar alocação pode se alocar em qualquer obra
**Arquivo:** `service/AlocacaoObraService.criar/atualizar`, `controller/AlocacaoObraController`, `enums/Cargo.RESPONSAVEL_ADMINISTRATIVO`
**Descrição:** `RESPONSAVEL_ADMINISTRATIVO` tem `CRIAR_ALOCACAO`/`EDITAR_ALOCACAO`/`DELETAR_ALOCACAO` e não há nenhuma verificação de escopo, nem de "não alterar a si mesmo" (existe para cargo/permissões, mas não aqui). Ele cria uma alocação para si mesmo em qualquer obra e passa a enxergar gastos, presenças e reembolsos dela. `atualizar` ainda permite mover uma alocação para outro funcionário/obra.
**Impacto:** o controle "por obra" é contornável por qualquer gestor; só o ADMIN deveria ver tudo (o `ObraSerializer` esconde orçamento de não-ADMIN, mas a obra inteira fica acessível).
**Sugestão:** definir a regra de negócio (quem pode alocar quem) e aplicar: ou só ADMIN aloca, ou o responsável só aloca em obras às quais já pertence e nunca a si mesmo.

### [MÉDIO] 2.12 Validação de entrada insuficiente ou incorreta
**Arquivo:** `dto/*`, controllers
**Descrição:**
- `FuncionarioPerfilUpdateDto.fotoUrl` aceita qualquer string até 512 (`javascript:`, URLs externas de tracking). `nome` sem `@Pattern`; `email` sem `@Size`.
- `FuncionarioDto.nome` com regex `^[a-zA-ZÀ-ÿ\s]+$` rejeita "D'Ávila", "Ana-Maria", "Jr.".
- `GastoDto.categoria`, `metodoPagamento` e `etapa` sem `@Size`: > 255 caracteres gera `DataIntegrityViolation` → 500.
- `PATCH /funcionarios/{id}/cargo` e `PUT /permissoes/cargos/{cargo}` usam `Map<String,String>`/DTO sem `@Valid`; cargo inválido → 500.
- `AuthController.LoginRequest` sem `@Valid`.
- `PUT /funcionarios/{id}` exige o campo `senha` (`@NotBlank`) e o ignora.
- `PresencaDto`, `OcrService.process` (content-type do cliente em `MediaType.parseMediaType` pode lançar exceção), `size` sem limite em `/obras/paginado` e `sortBy` sem whitelist (ver 2.14).

**Impacto:** 500 para dados previsíveis, nomes legítimos rejeitados, dados sujos no banco.
**Sugestão:** validar tudo em DTOs com `@Valid`, usar enums nos campos de domínio e `@Size` coerente com as colunas. Usar `@Validated` nos controllers.

### [MÉDIO] 2.13 Detalhes de erro, SQL e PII nos logs/respostas
**Arquivo:** `application.properties`, `FuncionarioService`, `AutenticacaoFilter`, `GlobalExceptionHandler`
**Descrição:** `server.error.include-message=always` e `include-binding-errors=always` expõem mensagens internas nas respostas do `/error` padrão (usado pelo `AutenticacaoEntryPoint`, que chama `sendError`). `spring.jpa.show-sql=true` imprime todo SQL, inclusive com PII, no stdout em produção. Logs com e-mails em `INFO` e IDs; `WARN` do filtro registra mensagens do JJWT (com nomes/claims).
**Impacto:** fuga de detalhes internos, logs gigantes e potencial violação LGPD.
**Sugestão:** `include-message=never`, `include-binding-errors=never`, `show-sql=false` (usar `logging.level.org.hibernate.SQL=debug` só em dev), mascarar e-mails nos logs.

### [MÉDIO] 2.14 `/obras/paginado` sem whitelist de ordenação nem limite de tamanho: *side-channel* do orçamento
**Arquivo:** `ObraController.listarPaginado`, `ObraService.listarPaginadoPorUsuario`
**Descrição:** `sortBy` vai direto para `Sort.by(sortBy)`. Um não-ADMIN pode usar `sortBy=orcamento` (campo que o `ObraSerializer` esconde dele) e deduzir a ordem de grandeza dos orçamentos. `sortBy=xyz` vira `PropertyReferenceException` → 500. `size` não tem teto (`size=1000000`). Em `/gastos/paginado` o teto (200) e a whitelist existem, o que prova a inconsistência.
**Impacto:** vazamento de informação financeira e DoS por consulta enorme.
**Sugestão:** whitelist de colunas ordenáveis (`id`, `titulo`, `status`, `dtInicio`) e `size` ≤ 100, centralizados em um helper reaproveitado por todos os endpoints paginados.

### [MÉDIO] 2.15 Exclusão da própria conta sem reautenticação e sem proteção do último ADMIN
**Arquivo:** `FuncionarioPerfilController.excluirConta`, `FuncionarioService.excluirConta`, `AcessoObraService`
**Descrição:** `DELETE /funcionarios/me` apaga a conta com apenas o token (sem pedir senha). Não há verificação de "último ADMIN", então o único administrador pode se excluir (ou ser excluído por outro ADMIN) e o sistema fica sem administração. Alterar senha também não derruba as outras sessões.
**Impacto:** token roubado = conta apagada; sistema sem ADMIN exige acesso ao banco para recuperar.
**Sugestão:** exigir senha atual, bloquear exclusão/rebaixamento do último ADMIN e preferir desativação (ver 4.8).

### [MÉDIO] 2.16 Artefatos sensíveis versionados no Git
**Arquivo:** `data/jafdb.mv.db`, `jaf-backend/data/jafdb.mv.db`, `jaf-backend/data/jafdb.trace.db`, `hs_err_pid36652.log` (confirmados por `git ls-files`)
**Descrição:** bancos H2 (que podem ter usuários, hashes e gastos) e um log de crash da JVM estão no repositório. `.gitignore` não cobre `*.db`, `data/` ou `hs_err_pid*`.
**Impacto:** dados de teste/reais e informações do ambiente ficam no histórico do Git para sempre.
**Sugestão:** `git rm --cached`, adicionar ao `.gitignore`, avaliar reescrita do histórico (`git filter-repo`) se contiverem dados reais, e rotacionar qualquer segredo que apareça neles.

### [BAIXO] 2.17 Política de senha inconsistente e fraca na troca
**Arquivo:** `dto/AlterarSenhaDto.java` vs `dto/FuncionarioDto.java`
**Descrição:** criação exige 8+ caracteres com maiúscula/minúscula/número/especial; **troca** aceita 6+ sem caractere especial (e a regex do `@Pattern` não limita o tamanho). Sem verificação de senha repetida/comum. Senha acima de 72 bytes é truncada pelo BCrypt. Senha atual errada retorna **403** (o frontend pode interpretar como sessão expirada).
**Impacto:** a troca enfraquece a política; o usuário pode trocar para uma senha pior que a inicial.
**Sugestão:** uma única anotação `@SenhaForte` reutilizada, `@Size(max=72)`, e 400/422 para senha atual errada.

### [BAIXO] 2.18 Dependências e imagem
**Arquivo:** `pom.xml`, `Dockerfile`
**Descrição:** Spring Boot 3.4.5 já saiu do suporte open source (migrar para 3.5.x ou mais recente); jjwt 0.11.5; Dockerfile roda como **root**, sem `HEALTHCHECK`, sem `USER` e sem flags de memória do container. O build usa `-DskipTests`. O deploy Terraform usa a tag `:latest`. Não há varredura de dependências (OWASP Dependency-Check/Dependabot).
**Impacto:** CVEs sem correção, escalada em caso de RCE, deploys não reproduzíveis.
**Sugestão:** atualizar BOM, `USER 10001`, `-XX:MaxRAMPercentage=75`, tags por commit SHA, Dependabot e etapa de testes na pipeline.

### [BAIXO] 2.19 Defaults de credencial e portas expostas
**Arquivo:** `application.properties`, `docker-compose.yml`
**Descrição:** defaults `postgres` (usuário de banco) e `guest` (RabbitMQ) no `application.properties`: se a env faltar, o app tenta conectar com credencial padrão em vez de falhar. O `docker-compose` publica 5432 (Postgres), 5672/15672 (RabbitMQ management) e 8000 (OCR) no host.
**Impacto:** serviços internos acessíveis em qualquer host que use o compose como base de deploy.
**Sugestão:** remover defaults sensíveis (falha explícita), e publicar portas internas apenas em `127.0.0.1` ou não publicar.

---

## 3. Banco de Dados (PostgreSQL)

### [CRÍTICO] 3.1 `ddl-auto=update` convivendo com Flyway; tabela `presenca` não tem migration
**Arquivo:** `application.properties` (`spring.jpa.hibernate.ddl-auto=update`, `flyway.baseline-on-migrate=true`), `model/Presenca.java`, `db/migration/*`
**Descrição:** o schema real é a soma de Flyway + o que o Hibernate decidir criar/alterar na subida. A tabela `presenca` (entidade `Presenca`) **não existe em nenhuma migration**: em banco novo ela só nasce porque o Hibernate a cria. As colunas e constraints dela não são revisáveis nem versionadas. Qualquer alteração de entidade vira `ALTER TABLE` automático em produção (Hibernate nunca remove nem altera tipos com segurança). `baseline-on-migrate=true` aceita bancos que já têm tabelas e só marca baseline.
**Impacto:** schema divergente entre ambientes, mudanças silenciosas em produção, impossível auditar/reverter. O RDS de produção pode ter uma `presenca` sem índices/constraints.
**Sugestão:** `spring.jpa.hibernate.ddl-auto=validate` (e `none` em prod), criar `V14__create_presenca.sql` (com `UNIQUE (funcionario_id, obra_id, data)`) e `baseline-on-migrate=false`. Rodar os testes com Testcontainers + Postgres para o `validate` falhar cedo.

### [CRÍTICO] 3.2 Unicidade garantida só por código (com *check-then-insert*) e sem constraint no banco
**Arquivo:** `V1__init.sql`, `FuncionarioService.criar/atualizar`, `FuncionarioRepository`, `AlocacaoObraService.criar`, `PresencaService.criar`, `ObraService.criar`
**Descrição:** não há `UNIQUE` em: `funcionario.email`, `obra.titulo`, `alocacao_obra (funcionario_id, obra_id)`, `presenca (funcionario_id, obra_id, data)`. As verificações são `existsBy…` seguidas de `save`, vulneráveis a corrida (duplo clique no celular, retry do ALB). Pior:
- `FuncionarioService.atualizar` **não verifica e-mail duplicado** nem normaliza; `criar` não normaliza (`Joao@x.com` ≠ `joao@x.com` no banco, mas `findByEmailIgnoreCase` os trata como iguais).
- Com dois registros de e-mail "iguais", `findByEmailIgnoreCase` (retorno `Optional`) lança `IncorrectResultSizeDataAccessException` e **nenhum dos dois consegue logar** (500).
- `existsByNome` impede dois funcionários com o mesmo nome (homônimos são comuns); é regra de negócio errada e fácil de contornar com espaços/maiúsculas.
- `RESPONSAVEL_ADMINISTRATIVO` pode editar e-mail de outros usuários não-admin (`EDITAR_FUNCIONARIO`), o que facilita causar essa colisão.

**Impacto:** usuários bloqueados, presenças/alocações duplicadas distorcendo relatórios.
**Sugestão:**
```sql
CREATE UNIQUE INDEX uk_funcionario_email_lower ON funcionario (lower(email));
ALTER TABLE alocacao_obra ADD CONSTRAINT uk_alocacao UNIQUE (funcionario_id, obra_id);
ALTER TABLE presenca ADD CONSTRAINT uk_presenca UNIQUE (funcionario_id, obra_id, data);
ALTER TABLE obra ADD CONSTRAINT uk_obra_titulo UNIQUE (titulo);
```
Antes, deduplicar os dados existentes. Tratar `DataIntegrityViolationException` como 409. Remover `existsByNome`. Normalizar e-mail (`trim().toLowerCase()`) em todos os pontos de entrada.

### [MÉDIO] 3.3 Falta de índices em colunas críticas
**Arquivo:** `V1__init.sql`, `V9`–`V13`
**Descrição:** o PostgreSQL **não** indexa FKs automaticamente. Faltam, entre outros:
- `funcionario (lower(email))`: a query `findByEmailIgnoreCase` gera `upper(email)=upper(?)`, o que **nunca** usa índice comum e roda ≥ 3 vezes por request autenticado.
- `gasto (obra_id, dt_gasto)`, `gasto (funcionario_id)`.
- `alocacao_obra (funcionario_id, obra_id)`, `alocacao_obra (obra_id)`.
- `presenca (obra_id, data)`.
- `funcionario_permissao (funcionario_id)`: coberto apenas pelo UNIQUE composto (ok), e `cargo_permissao (cargo)` (ok pelo UNIQUE). `pagamento_reembolso (gasto_id)` já tem índice.

**Impacto:** *seq scans* crescentes com o volume; o custo de autenticação (full scan de `funcionario` por request) cresce com o número de usuários.
**Sugestão:** criar os índices acima numa migration nova; para o e-mail, usar a coluna normalizada (`lower`) com o índice único (3.2) e consultar `findByEmail(lowerCase)`.

### [MÉDIO] 3.4 N+1, `@ManyToOne` EAGER, Open-In-View e queries repetidas por request
**Arquivo:** `model/Gasto.java`, `AlocacaoObra.java`, `Presenca.java`, `Relatorio.java`, `AutenticacaoFilter`, `AcessoObraService`, `ObraSerializer`
**Descrição:** `@ManyToOne` sem `fetch = LAZY` é EAGER: cada `findAll`/`findByObraId` dispara 1 query por funcionário/obra distintos. `spring.jpa.open-in-view` não está definido (default `true`): a conexão fica presa até o fim da serialização (e é isso que permite o `ObraSerializer` consultar o banco). Cada request autenticado executa: `findByEmailIgnoreCase` + `findByFuncionarioId` (permissões) + eventualmente `findByCargo` no filtro, e de novo no `@PreAuthorize("@acessoObraService.isAdmin()")` e em `acesso.atual()`.
**Impacto:** 5–10 queries só para autenticar/autorizar; listas grandes explodem em N+1; esgotamento do pool (default Hikari = 10 conexões).
**Sugestão:** `fetch = LAZY` nos `@ManyToOne`, `@EntityGraph`/`JOIN FETCH` ou projeções DTO nas listas, `spring.jpa.open-in-view=false`, e carregar o usuário (com permissões) uma vez por request (guardar no `UserDetails`/`Authentication`, que já contém `authorities`). Cache curto (Caffeine, 30–60 s) para `CargoPermissao`.

### [MÉDIO] 3.5 Dinheiro e domínio modelados como `String`; dupla fonte de verdade em reembolsos
**Arquivo:** `model/Obra.java` (`orcamento` VARCHAR), `V1__init.sql`, `model/Gasto.java` (`reembolsoConcluido` + `PagamentoReembolso`), `DashboardService`
**Descrição:** `obra.orcamento` é `VARCHAR(255)` (regex no DTO, `parseOrcamento` com `catch` que silencia erro e devolve 0). Um dado inválido vira orçamento 0 sem aviso. O estado do reembolso está em dois lugares: `gasto.reembolso_concluido` (legado, recalculado à mão em `GastoService.atualizar` e `ReembolsoService.registrar`) e a soma de `pagamento_reembolso`. O dashboard converte `BigDecimal` para `double` (`doubleValue()`) nos gráficos.
**Impacto:** cálculos financeiros sensíveis a dado sujo; possibilidade de as duas fontes divergirem (qualquer escrita direta/migration que mexer em uma só).
**Sugestão:** migrar `orcamento` para `NUMERIC(19,2) NOT NULL CHECK (orcamento >= 0)`; eliminar `reembolso_concluido` (derivar de `pagamento_reembolso`, view ou coluna gerada); enviar valores monetários como `BigDecimal`/string no JSON.

### [MÉDIO] 3.6 Transações ausentes e inconsistentes
**Arquivo:** `ObraService`, `AlocacaoObraService`, `FuncionarioService`, `PresencaService`, `RelatorioService` (sem `@Transactional`); `FuncionarioPermissaoService`/`CargoPermissaoService` usam `jakarta.transaction.Transactional` enquanto o resto usa o do Spring
**Descrição:** operações que envolvem leitura + escrita (`FuncionarioService.atualizar`, `PresencaService.atualizar`, `AlocacaoObraService.atualizar`) rodam sem transação explícita; cada `save` é uma transação isolada. `jakarta.transaction.Transactional` funciona, mas ignora `readOnly`, `rollbackFor` e outros atributos do Spring.
**Impacto:** leituras/escritas não atômicas (ex.: checagem de duplicidade e `save` em transações diferentes), comportamento de rollback diferente do esperado.
**Sugestão:** `@Transactional` na camada de serviço (classe, com `readOnly=true` nos métodos de leitura) e usar sempre `org.springframework.transaction.annotation.Transactional`.

### [MÉDIO] 3.7 Exclusão física com FKs sem política: erro 500 e perda de histórico financeiro
**Arquivo:** `FuncionarioService.deletar/excluirConta`, `ObraService.deletar`, `V1__init.sql`, `V10__create_funcionario_permissao.sql`
**Descrição:** `funcionario` é referenciado por `gasto`, `alocacao_obra`, `relatorio`, `presenca` e `funcionario_permissao`, sem `ON DELETE`. Excluir um funcionário com qualquer histórico (o caso normal) viola FK → `DataIntegrityViolationException` → **500**. Excluir uma obra com gastos idem. Para o caso raro em que a exclusão passa (funcionário sem histórico), ela é irreversível e **não limpa `funcionario_permissao`**.
**Impacto:** impossibilidade prática de remover quem saiu da empresa (4.8), mensagem de erro genérica, e, se alguém "resolver" com `ON DELETE CASCADE`, perda de histórico contábil.
**Sugestão:** exclusão lógica (`ativo`/`deleted_at`) para `funcionario` e `obra`, `ON DELETE RESTRICT` explícito nas FKs, e 409 com mensagem clara.

### [MÉDIO] 3.8 Sem controle otimista de concorrência (`@Version`)
**Arquivo:** `model/*`, `ObraFinanceiroService.atualizar`, `FuncionarioPermissaoService.atualizar`, `GastoService.atualizar`
**Descrição:** nenhuma entidade tem `@Version`. Dois gestores editando o mesmo gasto/obra ou ajustando `totalRecebido` fazem *last write wins* silencioso. `FuncionarioPermissaoService.atualizar` faz `deleteByFuncionarioId` + `saveAll`: duas chamadas concorrentes violam `uk_funcionario_permissao` (500). O pessimista em `Gasto` (`buscarParaAtualizar`) é bom, mas só cobre gasto/reembolso.
**Impacto:** perda de atualização em dados financeiros.
**Sugestão:** `@Version Long versao` nas entidades editáveis (`Gasto`, `Obra`, `Funcionario`) e devolver/exigir a versão no DTO (409 em conflito).

### [MÉDIO] 3.9 Migrations: dados de demo versionados, IDs hardcoded e schema sem constraints
**Arquivo:** `V1`–`V13`
**Descrição:**
- V2/V3/V6 inserem dados fictícios e dependem de IDs fixos (`alocacao_obra` usa `funcionario_id` 2–5, `obra_id` 1–2), o que quebra se a sequência divergir.
- A V1 não tem `NOT NULL`, `CHECK (valor > 0)` em `gasto`, `UNIQUE`, nem índices; só a V13 adiciona `CHECK` (apenas para pagamentos e total recebido).
- V7/V8 são migrations de dados irreversíveis e rodam `UPDATE` sem `WHERE` de segurança (mudam cargos de **todos**).
- O seeder (Java) repete os mesmos dados de V2/V3; duas fontes.
- Ausência de `created_at/updated_at/created_by` em `gasto`, `obra`, `alocacao_obra`.

**Impacto:** ambiente novo nasce sujo; migrations não são idempotentes entre ambientes.
**Sugestão:** mover dados de exemplo para `src/main/resources/db/dev/` (`flyway.locations` por profile), novas migrations adicionando `NOT NULL`/`CHECK`/`UNIQUE`/índices e colunas de auditoria.

### [MÉDIO] 3.10 Agregações feitas em memória e listagens sem paginação
**Arquivo:** `DashboardService.buscarStats`, `ObraFinanceiroService.resposta`, `GastoService.listarFiltrado`, `ObraService.listarPorUsuario`, `GastoRepository.findByObraId`, `PresencaService.listarPorObraEData`
**Descrição:** o dashboard carrega **todos** os gastos da obra (`findByObraId`) e faz filtros/somas com streams. `ObraFinanceiroService` carrega todos os gastos só para somar. `GET /gastos`, `/obras`, `/alocacoes`, `/funcionarios`, `/relatorios` retornam listas completas. `listarPorObraEData` faz *stream* aninhado O(alocações × presenças).
**Impacto:** latência e memória crescem linearmente com o histórico de cada obra; JSON gigante para o celular.
**Sugestão:** `SELECT sum(valor) … GROUP BY categoria/etapa/mês` com `@Query`/projeções, paginação obrigatória nas listas, e mapas por ID em `PresencaService`.

---

## 4. Regra de Negócio

### [CRÍTICO] 4.1 Requisitos "essenciais" da documentação não existem no backend
**Arquivo:** `model/Relatorio.java`, `service/RelatorioService.java`, `repository/GastoFiltro.java`, `service/OcrService.java`, `pom.xml`
**Descrição (documentação × código):**
| Documentação (`contexto.txt`) | Código |
|---|---|
| "Inserção e gestão de **Notas Fiscais**" (essencial) | **Não existe** entidade/tabela/endpoint de Nota Fiscal. O OCR (`/ocr/nota-fiscal`) lê o arquivo, devolve campos sugeridos e **descarta o arquivo**; o `Gasto` não guarda número da NF, CNPJ, nem o anexo. |
| "**Exportação de relatório de gastos**" (essencial) | `Relatorio` é só `titulo + dtEmissao + funcionario`. Não há geração de CSV/PDF/XLSX nem dependência para isso no `pom.xml`. |
| "**Filtro de data** relacionado aos gastos" (essencial) | `GET /gastos` e `/gastos/paginado` **não aceitam** `from/to`. Só o dashboard filtra por período. |
| "Envio de e-mail automatizado com o relatório" (desejável) | Sem `spring-boot-starter-mail`. |
| "Monitoramento de entradas/saídas anuais" (desejável) | Parcial (`totalRecebido` manual, sem série anual). |
| "Envio à contabilidade por meio da exportação de relatórios" (restrição) | Depende do item de exportação, que não existe. |

**(Inferido)** o frontend pode estar gerando relatório/filtro no cliente; isso não foi verificado, mas, mesmo assim, o backend não tem como garantir consistência, permissão por obra nem auditoria do que foi exportado.
**Impacto:** as dores centrais do cliente (organizar NFs e enviar números à contabilidade) não são atendidas pelo servidor. É o maior risco de entrega do projeto.
**Sugestão:** criar `NotaFiscal` (número, CNPJ, valor, data, arquivo no S3/disco versionado, `gasto_id`, hash do arquivo), `GET /gastos?from&to`, e `GET /relatorios/gastos?obraId&from&to&formato=csv|xlsx|pdf` respeitando o escopo por obra (e registrando em `Relatorio`).

### [MÉDIO] 4.2 Dashboard: o service exige ADMIN, o controller permite `VISUALIZAR_GASTOS`
**Arquivo:** `DashboardService.buscarStats` (`acesso.exigirAdmin()`), `DashboardController`
**Descrição:** o `@PreAuthorize` libera quem tem `VISUALIZAR_GASTOS` (inclusive `ENGENHEIRO` e `RESPONSAVEL_ADMINISTRATIVO`), mas o service lança 403 para quem não é ADMIN. O contrato é contraditório (e o 403 só aparece depois de passar pelo `@PreAuthorize`). A documentação prevê o Dashboard como requisito essencial e fala em "gestor" com visão clara das movimentações (permissão por nível); não diz que seja exclusivo do ADMIN (**inferido**).
**Impacto:** o frontend mostra o menu para quem tem a permissão e recebe 403; usuários de campo ficam sem visão da obra.
**Sugestão:** decidir a regra e expressar em um só lugar (ex.: `@PreAuthorize("hasAuthority('VISUALIZAR_DASHBOARD') and @acesso.podeVerObra(#obraId)")`) e criar a permissão própria.

### [MÉDIO] 4.3 Quem cria uma obra não consegue ver nem editar a obra criada
**Arquivo:** `ObraService.criar`, `ObraController`
**Descrição:** `RESPONSAVEL_ADMINISTRATIVO` tem `CRIAR_OBRA`. Depois de criar, a obra só aparece para ele se houver alocação (`listarPorUsuario`/`buscarPorIdComEscopo` filtram por alocação). `criar` não cria alocação. O mesmo ocorre ao `PUT` logo em seguida (`buscarPorIdComEscopo` → 403).
**Impacto:** fluxo quebrado (cria e "some"), só resolvido por um ADMIN ou por auto-alocação (2.11).
**Sugestão:** alocar automaticamente o criador (se não-ADMIN) na criação, dentro da mesma transação.

### [MÉDIO] 4.4 Alocação: `dtFim` é ignorada nas permissões; datas e cargo na obra sem regra
**Arquivo:** `AcessoObraService.exigirObra`, `AlocacaoObraRepository.existsByFuncionarioIdAndObraId`, `GastoService`, `ObraService`, `AlocacaoObraService`
**Descrição:** `V11` introduziu `dt_fim`, e a presença respeita (`alocadoNaData`), mas **acesso à obra e lançamento de gastos não**: quem foi desalocado (com `dtFim` preenchida) continua enxergando e lançando gastos. Não há endpoint que defina `dtFim` (o DTO só tem funcionário/obra/cargo). `atualizar` pode trocar funcionário e obra de uma alocação existente, sem validar duplicidade nem presenças/gastos já vinculados. `cargoNaObra` aceita `ADMIN`.
**Impacto:** ex-funcionário com acesso a dados financeiros da obra; histórico de presença incoerente.
**Sugestão:** `existsAtiva(funcionarioId, obraId, hoje)` com `dtFim is null or dtFim >= hoje`, endpoint "encerrar alocação" e restringir `atualizar` a cargo/datas.

### [MÉDIO] 4.5 Gasto: autoria forjável, edição por qualquer alocado e sem trilha de auditoria
**Arquivo:** `GastoService.criar/atualizar/deletar`, `GastoDto.funcionarioId`
**Descrição:** o funcionário "responsável" vem do corpo da requisição: quem tem `CRIAR_GASTO` registra gasto em nome de **qualquer** colega alocado na obra. `EDITAR_GASTO` permite alterar valor/descrição de gastos de outras pessoas. Não há `criado_por`, `atualizado_por`, histórico de alterações. Gasto com `dtGasto` anterior à `dtInicio` da obra ou em obra `CONCLUIDA`/`CANCELADA` é aceito.
**Impacto:** impossível atribuir responsabilidade, ponto fraco para auditoria contábil (o objetivo do sistema).
**Sugestão:** `funcionario` = usuário autenticado (exceto ADMIN/RESPONSAVEL, que podem lançar em nome de outro explicitamente), colunas de auditoria (`@CreatedBy`, `@LastModifiedBy`, `@EnableJpaAuditing`) e tabela de histórico (Envers). Validar status/período da obra.

### [MÉDIO] 4.6 Reembolsos: pagamentos não podem ser estornados; flag legada ainda aceita
**Arquivo:** `ReembolsoService.registrar`, `ReembolsoController`, `GastoDto.reembolsoConcluido`
**Descrição:** só existe `POST /gastos/{id}/reembolsos`; não há correção/estorno. Pagamento lançado com valor/data errados é permanente (e o `GastoService.deletar` bloqueia gastos com pagamentos). O DTO de gasto ainda aceita `reembolsoConcluido` (rejeitado com 400 só quando `true`), um contrato legado confuso.
**Impacto:** erro operacional vira dado contábil errado sem correção possível.
**Sugestão:** endpoint de estorno (lançamento negativo ou `cancelado_em/por/motivo`, ADMIN) com histórico, e remover `reembolsoConcluido` do DTO.

### [MÉDIO] 4.7 Permissões: lista personalizada substitui (não soma), lista vazia "volta" ao padrão, cargo nulo vira ENGENHEIRO
**Arquivo:** `FuncionarioPermissaoService.permissoesDoFuncionario/atualizar`, `FuncionarioDetalhesDto`, `CargoPermissaoService.permissoesPorCargo`
**Descrição:** se existir qualquer linha em `funcionario_permissao`, as permissões do cargo são **ignoradas** (override total); se o ADMIN salvar uma lista **vazia** (para revogar tudo), as linhas são apagadas e o usuário **volta às permissões padrão do cargo**. Impossível "zerar" um usuário. Cargo `null` vira `ENGENHEIRO` silenciosamente (privilégio padrão). `DELETAR_GASTO`, `DELETAR_OBRA` e `DELETAR_FUNCIONARIO` só existem para ADMIN (o enum), e não há como conceder pelos defaults da V9.
**Impacto:** revogação de acesso não funciona como o ADMIN espera; comportamento diferente do esperado ("controle de acesso por nível de permissão").
**Sugestão:** modelar overrides como `GRANT`/`REVOKE` ou usar sempre a tabela com modo explícito (`herdar` vs `personalizado`), e tornar `cargo_global NOT NULL`.

### [MÉDIO] 4.8 Não existe desativação de funcionário
**Arquivo:** `model/Funcionario.java`, `FuncionarioDetalhesDto.isEnabled()` (sempre `true`), `FuncionarioService.deletar`
**Descrição:** não há campo `ativo`. Para bloquear alguém que saiu da empresa só resta excluir (que falha por FK, ver 3.7) ou trocar a senha manualmente. O `isEnabled()` retorna `true` fixo. A documentação diz que o administrador "gerencia" os funcionários, sem tratar desligamento (**inferido**).
**Impacto:** ex-funcionários continuam logando e vendo dados financeiros.
**Sugestão:** coluna `ativo BOOLEAN NOT NULL DEFAULT TRUE`, refletida em `isEnabled()` e verificada no filtro JWT; endpoint `PATCH /funcionarios/{id}/ativo`.

### [MÉDIO] 4.9 `totalRecebido` é um número sobrescrito, sem histórico de entradas
**Arquivo:** `ObraFinanceiroService.atualizar`, `model/Obra.totalRecebido`
**Descrição:** o "recebido" da obra é um único valor editado por `PUT` (só ADMIN). Não há registro de cada recebimento (data, valor, origem, quem lançou). O saldo do dashboard depende dele. O requisito desejável ("monitoramento de entradas/saídas anuais") não pode ser atendido sem lançamentos.
**Impacto:** sem trilha, sem relatório anual, e um erro de digitação apaga o valor anterior sem rastro.
**Sugestão:** tabela `recebimento_obra (id, obra_id, valor, data, descricao, criado_por)`, `totalRecebido` calculado.

### [BAIXO] 4.10 Validações de obra e gasto incompletas
**Arquivo:** `ObraService.criar/atualizar`, `ObraDto`, `GastoService`
**Descrição:** a mensagem diz "término **posterior** ao início", mas a regra aceita datas iguais (`isBefore`). `atualizar` não revalida unicidade de título nem transições de status (`CONCLUIDA → EM_ANDAMENTO`). Título só aceita letras/números/espaço/hífen (rejeita "N.S. Aparecida", "Cond. São José / Bloco B"). Orçamento `0` aceito.
**Impacto:** dados inconsistentes e rejeição de nomes reais.
**Sugestão:** regras explícitas no domínio e `@Pattern` mais flexível.

### [BAIXO] 4.11 Dashboard: arredondamentos, doubles e mês sem ano
**Arquivo:** `DashboardService`, `DashboardResponseDto`
**Descrição:** percentuais inteiros podem passar de 100; "Pendentes" = `100 − Concluídos` (soma sempre 100 mesmo com arredondamento); a série mensal agrupa por `YearMonth` mas exibe só o nome do mês (janeiro/2025 e janeiro/2026 aparecem iguais); `new Locale("pt","BR")` é deprecated; categorias de imprevistos fixas no código.
**Impacto:** gráficos confusos em obras de mais de um ano.
**Sugestão:** rótulo `MM/yyyy`, `Locale.forLanguageTag("pt-BR")` e categorias configuráveis.

---

## 5. API e Integração

### [MÉDIO] 5.1 Entidades JPA como contrato da API (sem DTO de resposta)
**Arquivo:** `GastoController`, `ObraController`, `AlocacaoObraController`, `RelatorioController`
**Descrição:** `Gasto`, `Obra`, `AlocacaoObra` e `Relatorio` são devolvidas diretamente (além de serem usadas no `Page<Gasto>`). O formato do JSON depende do mapeamento JPA (EAGER/LAZY), da serialização customizada da `Obra` e de campos internos (`reembolsoConcluido`). Isso causa o vazamento do 2.1, o N+1 de 3.4 e acopla frontend ↔ schema. Já existe o padrão certo em `FuncionarioResponseDto`/`PresencaResponseDto`.
**Impacto:** qualquer mudança na entidade quebra o frontend; risco de expor campos novos por acidente.
**Sugestão:** DTOs de resposta (`GastoResponse`, `ObraResponse`, `AlocacaoResponse`) por *records*, com mappers (MapStruct).

### [MÉDIO] 5.2 Códigos HTTP incorretos ou ambíguos
**Arquivo:** `AutenticacaoEntryPoint`, `GlobalExceptionHandler`, `FuncionarioService.alterarSenha`, `OcrController`
**Descrição:**
- Requisição **sem token** recebe **403** (o `AutenticacaoEntryPoint` só devolve 401 para `BadCredentialsException`; o caso normal é `InsufficientAuthenticationException`). O frontend não consegue diferenciar "sessão expirada" de "sem permissão".
- E-mail inexistente no login → 500 (2.5).
- Senha atual incorreta → 403 (deveria ser 400/422).
- Regras de negócio (alocação ausente, data inválida) usam `409 Conflict` ou `403`, sem padrão.
- `NoContent` → 204 com corpo.
- OCR com arquivo inválido → **200** com `status=ERRO`.
- `ReembolsoController.registrar` devolve 201 sem `Location`.

**Impacto:** frontend com tratamento condicional e frágil; observabilidade distorcida.
**Sugestão:** entry point sempre 401 com corpo JSON; 403 só para `AccessDeniedException`; 400/422 para validação; 404/409 conforme o caso; padronizar em `ProblemDetail` (RFC 7807).

### [MÉDIO] 5.3 Formatos de erro múltiplos
**Arquivo:** `GlobalExceptionHandler`, `AutenticacaoEntryPoint`, `server.error.*`
**Descrição:** existem pelo menos 3 formatos: `ErrorResponse`, `ValidationError` (com `fields`) e o JSON padrão do Spring (`BasicErrorController`, usado pelo entry point e por erros de segurança). Sem `code` estável por tipo de erro; mensagens ora com acento, ora sem ("Usuario nao encontrado").
**Impacto:** o frontend precisa conhecer 3 formatos e depende de texto de mensagem.
**Sugestão:** um envelope único (`ProblemDetail` + `code` + `fields`), mensagens consistentes (ou i18n) e entry point/`AccessDeniedHandler` escrevendo o mesmo formato.

### [MÉDIO] 5.4 Paginação inconsistente e `Page` serializado direto
**Arquivo:** `GastoController.listarPaginado`, `ObraController.listarPaginado`, demais listagens
**Descrição:** só `/gastos` e `/obras` têm versão paginada; as demais devolvem listas completas. Em `/obras/paginado` o teto de `size` e a whitelist de `sortBy` não existem (2.14). `Page<Entity>` é serializado diretamente (`PageImpl`): o Spring Data 3.3+ avisa que essa estrutura JSON não é estável (usar `PagedModel`/DTO). Há `sugestoes_paginacao.html` e `docs/RESUMO_PAGINACAO.md` na raiz, sinal de que o tema não foi fechado.
**Impacto:** endpoints sem paginação não escalam e o contrato de página pode mudar em atualização de versão.
**Sugestão:** padronizar `PageResponse<T>` (conteúdo, página, tamanho, total) e paginar todas as listagens que crescem.

### [MÉDIO] 5.5 OCR: contrato e permissões confusos
**Arquivo:** `OcrController`, `OcrService.process`
**Descrição:** `POST /ocr` (síncrono/legado) exige `CRIAR_OBRA` (permissão errada para ler nota fiscal), repassa o arquivo e o `Content-Type` fornecidos pelo cliente para outro serviço via `RestTemplate` **sem timeout**, não valida tipo/tamanho (além do limite multipart de 10 MB) e devolve o `JsonNode` cru do serviço externo. `POST /ocr/nota-fiscal` usa o parâmetro `arquivo`, o legado usa `file`. `SecurityConfiguracao` ainda tem o matcher redundante `.requestMatchers(POST, "/ocr").authenticated()`.
**Impacto:** thread presa se o serviço OCR travar; contrato do endpoint legado depende do formato de terceiros.
**Sugestão:** remover `/ocr` legado (ou protegê-lo como o assíncrono), configurar `RestClient` com *connect/read timeout* e usar a mesma permissão (`CRIAR_GASTO`).

### [BAIXO] 5.6 Rotas redundantes
**Arquivo:** `AlocacaoObraController`, `ObraController`
**Descrição:** quatro formas de listar alocações por obra (`/alocacoes/obra/{id}`, `/alocacoes/filtro?obraId=`, `/obras/{id}/alocacoes`, e `/alocacoes` para o usuário); `/auth/login` x `/funcionarios/login`.
**Impacto:** manutenção e segurança divergentes entre rotas equivalentes (o IDOR de 2.10 existe só em algumas).
**Sugestão:** uma rota canônica por recurso.

### [BAIXO] 5.7 Documentação OpenAPI incompleta e incoerente
**Arquivo:** `config/swagger/OpenApiConfig.java`, controllers
**Descrição:** o esquema de segurança é `Bearer`, mas a aplicação autentica por **cookie** (o "Authorize" do Swagger não funciona do jeito que o cookie funciona). Sem `@Operation`/`@ApiResponse`; `ReembolsoController` e `ObraFinanceiroController` sem `@SecurityRequirement`; contato/URL de licença de placeholder (`grupo6`, `MIT`). `FuncionarioTokenDto` expõe `token` sempre `null`.
**Impacto:** documentação enganosa para quem integra.
**Sugestão:** declarar o `cookieAuth` (`SecuritySchemeType.APIKEY`, `in = COOKIE`), documentar respostas de erro e remover o campo `token`.

### [BAIXO] 5.8 `PUT /funcionarios/{id}` exige senha que ignora
**Arquivo:** `FuncionarioDto`, `FuncionarioService.atualizar`
**Descrição:** o DTO de criação é reutilizado na edição: `senha` é obrigatória e validada, mas nunca aplicada. O frontend precisa enviar uma senha qualquer para editar nome/cargo.
**Impacto:** confusão e risco de o frontend enviar a senha em texto puro à toa.
**Sugestão:** DTOs distintos (`FuncionarioCreateRequest`, `FuncionarioUpdateRequest`).

---

## 6. Problemas Potenciais

### [MÉDIO] 6.1 Uploads em disco local com backend atrás de ALB/Auto Scaling
**Arquivo:** `FuncionarioPerfilController` (`Paths.get("uploads","fotos")`), `StaticResourceConfig`, `terraform/scripts/backend.sh`, `terraform/modules/ec2`
**Descrição:** as fotos ficam no diretório relativo `uploads/` do container. No `docker-compose` existe volume (`./data/uploads:/app/uploads`), mas o `docker run` do Terraform **não monta volume nenhum**, e o módulo EC2 usa Auto Scaling Group. Cada instância tem seu próprio diretório efêmero.
**Impacto:** fotos somem a cada redeploy/replace da instância e ficam inconsistentes entre instâncias (a foto subida na instância A dá 404 quando o ALB roteia para B).
**Sugestão:** S3 (URL pré-assinada ou CloudFront), ou EFS. Armazenar só a chave do objeto no banco.

### [MÉDIO] 6.2 Fuso horário: `LocalDate.now()` em UTC no AWS gera erro de regra
**Arquivo:** `AlocacaoObraService.criar` (`setDtInicio(LocalDate.now())`), `AlocacaoObra.@PrePersist`, `PresencaService.validarManual/alocadoNaData`, `ReembolsoService`, `GastoDto @PastOrPresent`; `terraform/scripts/backend.sh` (sem `TZ`)
**Descrição:** o `docker-compose` define `TZ=America/Sao_Paulo`, o `docker run` do Terraform não. A JVM roda em UTC. Depois das 21:00 (BRT) a "data de hoje" no servidor já é amanhã. Caso concreto: alocação criada às 22:00 recebe `dtInicio = amanhã`; tentar registrar presença de **hoje** para esse funcionário falha com "não está alocado na obra na data informada". `Migrations` (`CURRENT_DATE`) usam o fuso do banco (RDS = UTC).
**Impacto:** erros intermitentes no fim do dia, difíceis de reproduzir.
**Sugestão:** `spring.jackson.time-zone`, `-Duser.timezone=America/Sao_Paulo` ou `TZ` no container, e injetar `Clock` nos services (facilita teste e fuso explícito: `LocalDate.now(clock)`).

### [MÉDIO] 6.3 Pool de conexões, timeouts e Open-In-View
**Arquivo:** `application.properties`, `OcrService` (`new RestTemplate()`)
**Descrição:** nada configurado para Hikari (default 10 conexões, sem `leak-detection-threshold`), `server.tomcat.threads`, ou timeouts de `RestTemplate`. Com OSIV ligado e ≥ 5 queries por request (3.4), 10 conexões se esgotam com poucos usuários concorrentes em picos (início de expediente, quando todos lançam presença).
**Impacto:** latência alta e `Connection is not available` em picos.
**Sugestão:** OSIV off, `spring.datasource.hikari.maximum-pool-size`/`connection-timeout`, `RestClient` com timeouts, e testes de carga (k6/Gatling) no fluxo de login + lançamento de gasto.

### [MÉDIO] 6.4 OCR assíncrono: jobs presos, mensagens perdidas e retenção de dados
**Arquivo:** `OcrService`, `RabbitMQConfig`, `application.properties` (`listener.simple.retry`), `model/OcrProcessamento`
**Descrição:**
- Se o worker nunca responder, o job fica em `PROCESSANDO` **para sempre** (sem timeout nem varredura).
- O listener de resultados tem retry (3 tentativas) e, esgotado, a mensagem é rejeitada **sem DLQ** na fila `ocr.resultado`. O resultado é perdido e o job fica preso.
- A imagem/PDF (até 10 MB) é codificada em Base64 e enviada **dentro** da mensagem AMQP (≈ 13 MB).
- `ocr_processamento.texto_bruto` guarda o texto integral de notas fiscais (CNPJ, nomes) sem prazo de retenção (LGPD) e a tabela cresce sem limpeza.
- O `solicitante` é o e-mail; se ele mudar, o usuário perde o job.
- A mensagem de erro do worker é copiada para a resposta (`"Erro ao processar...: " + resultado.erro()`).

**Impacto:** UX travada em "Processando…" e acúmulo de dados pessoais.
**Sugestão:** job `@Scheduled` que marca como `ERRO` jobs > N minutos, DLQ para resultados, guardar o arquivo em storage e mandar só a referência na mensagem, retenção (ex.: 30 dias) e usar o `id` do usuário como dono.

### [MÉDIO] 6.5 Condições de corrida em verificações "checa e depois grava"
**Arquivo:** `FuncionarioService.criar`, `AlocacaoObraService.criar`, `PresencaService.criar`, `ObraService.criar`, `FuncionarioPermissaoService.atualizar`
**Descrição:** cada uma faz `exists…`/`find…` e só depois `save`, sem lock e sem constraint (3.2). Em mobile com rede instável, o duplo envio é comum. As presenças duplicadas passam a quebrar `findByFuncionarioIdAndObraIdAndData` (retorna `Optional`; dois registros → `IncorrectResultSize`).
**Impacto:** dados duplicados e 500 em endpoints que antes funcionavam.
**Sugestão:** constraints no banco (3.2) + tratamento de `DataIntegrityViolationException` como 409 + idempotência (header `Idempotency-Key` nos POSTs financeiros).

### [MÉDIO] 6.6 Escalabilidade de memória e CPU
**Arquivo:** `DashboardService`, `ObraFinanceiroService`, `PresencaService`, `GastoService.listarFiltrado`
**Descrição:** agregações e filtros em memória e sem paginação (3.10); `RelatorioService.listar` retorna tudo; `Collectors.toMap` com chave de funcionário em `PresencaService` sem validação.
**Impacto:** com poucos anos de dados e ~10 obras, as respostas passam de MBs; `-Xmx512m` (compose) fica no limite.
**Sugestão:** agregação no banco, paginação obrigatória e limite de `size`.

### [MÉDIO] 6.7 Sem trilha de auditoria e quase sem observabilidade
**Arquivo:** `pom.xml` (apenas `actuator`), `application.properties` (`health` apenas)
**Descrição:** num sistema financeiro, nenhuma alteração de gasto, pagamento de reembolso, permissão ou cargo é auditada (quem, quando, valor anterior). Logs são texto livre, sem `traceId`/correlação; sem métricas (Micrometer/Prometheus), sem alertas; o `health` só tem DB (ok para o ALB), mas nada de latência/erros.
**Impacto:** incidentes sem diagnóstico e impossibilidade de responder "quem mudou esse valor?" para a contabilidade.
**Sugestão:** Spring Data Auditing/Envers, `logging.pattern` com `traceId` (Micrometer Tracing), e expor `/actuator/prometheus` somente na rede interna.

### [BAIXO] 6.8 Testes não cobrem os pontos de maior risco
**Arquivo:** `src/test/java/**`
**Descrição:** existem testes de service e alguns de API (financeiro, autorização de funcionário, health), mas nenhum cobre: vazamento aninhado de `senha`, IDOR de alocações/relatórios, isolamento por obra em todos os endpoints, 401 vs 403, fuso horário, concorrência (reembolso em paralelo), migration em Postgres real (parte usa H2: `DatabaseSeederTest` cria tabela à mão). O H2 ≠ PostgreSQL (tipos, `ILIKE`, `lower()`), então testes verdes não garantem produção.
**Impacto:** regressões de segurança passam despercebidas.
**Sugestão:** Testcontainers (PostgreSQL), testes parametrizados que varrem todos os endpoints × papéis, e teste de contrato do JSON sem `senha`.

### [BAIXO] 6.9 Pipeline/imagem de entrega
**Arquivo:** `Dockerfile`, `terraform/scripts/backend.sh`
**Descrição:** `-DskipTests` no build da imagem, tag `:latest`, ausência de `HEALTHCHECK` no Dockerfile, sem limites de memória/CPU por container, e `restart: on-failure:5` no compose (para de reiniciar depois de 5).
**Impacto:** imagem sem testes e deploys não reproduzíveis; rollback difícil.
**Sugestão:** CI executando `mvn verify`, tags imutáveis (SHA) e health/limits no deploy.

---

# Resumo Executivo

### Total de problemas por severidade

| Severidade | Quantidade | IDs |
|---|---:|---|
| **CRÍTICO** | **7** | 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 4.1 |
| **MÉDIO** | **46** | 1.1–1.6 · 2.5–2.16 · 3.3–3.10 · 4.2–4.9 · 5.1–5.5 · 6.1–6.7 |
| **BAIXO** | **13** | 1.7–1.9 · 2.17–2.19 · 4.10–4.11 · 5.6–5.8 · 6.8–6.9 |
| **Total** | **66** | |

### Ordem de correção sugerida (antes de qualquer go-live)
1. **2.1:** parar de devolver entidades e esconder `senha`; depois forçar troca de todas as senhas.
2. **2.2:** desligar seed/migrations de demo em produção e trocar `admin@gmail.com`/`Admin@123`.
3. **2.3 + 2.4:** cookie `Secure`/`SameSite`, HTTPS obrigatório, CSRF e rate limit no login.
4. **3.1 + 3.2:** `ddl-auto=validate`, migration da `presenca` e constraints `UNIQUE` (e-mail, alocação, presença).
5. **4.1:** implementar o que a documentação chama de essencial: Nota Fiscal, exportação de relatório e filtro por data nos gastos.
6. Em seguida: IDOR (2.10/2.11), N+1/índices (3.3/3.4), timezone (6.2), uploads em S3 (6.1) e auditoria (6.7).

### Pontos positivos (para equilíbrio)
- Boa modelagem de reembolso (pagamentos parciais com `NUMERIC`, `CHECK`, lock pessimista e testes).
- Paginação de `/gastos` com whitelist de ordenação e teto de tamanho.
- Permissões lidas do banco a cada request (revogação de permissão vale imediatamente).
- Uso consistente de BCrypt, queries parametrizadas e injeção por construtor na maior parte do código.
- Cookie `HttpOnly`, topologia RabbitMQ com DLQ para solicitações e `/actuator/health/readiness` desacoplado do OCR/RabbitMQ.

### Avaliação geral (uma frase)
**O backend tem uma base de código razoável e organizada, mas, no estado atual, não está pronto para produção: ele vaza hashes de senha em vários endpoints, cria um ADMIN com senha conhecida por padrão, emite cookies sem `Secure`/CSRF, depende do Hibernate para criar tabelas e ainda não implementa os requisitos essenciais de Notas Fiscais, exportação de relatórios e filtro de datas.**
