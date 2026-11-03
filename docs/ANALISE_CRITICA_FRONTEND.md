# Análise crítica do frontend (React) — JAF

> Escopo: `jaf-frontend/react` (React 19 + Vite + TypeScript + axios + react-router 7).
> Backend, Terraform e OCR só foram lidos onde o frontend depende deles para o comportamento (cookie, CORS, validações DTO, endpoints).

---

## 0. Método, premissas e ressalvas

- **Lido:** todos os `.tsx/.ts` de `src` (exceto o miolo de JSX puramente visual de alguns arquivos grandes: trechos finais de `Profile.tsx`, `NovoFuncionario.tsx`, `GestaoPresenca.tsx`), `nginx.conf`, `Dockerfile`, `package.json`, `vite.config.ts`, `eslint.config.js`, `tsconfig.app.json`, os arquivos legados em `jaf-frontend/` e a documentação (`contexto.txt`, `README.md`, `docs/MELHORIAS-BENEFICIARIO.md`, `jaf-backend/API-FINANCEIRO.md`, `docs/RESUMO_PAGINACAO.md`, `docs/RELATORIO_SEGURANCA_OWASP.md`).
- **Não executado:** `lint`, `tsc`, `vitest` e `build`. Não há `node_modules` instalado no workspace e esta tarefa é de análise. As afirmações de "124 testes aprovados / lint aprovado" da documentação **não foram verificadas por mim**.
- **Convenção:** itens marcados *(inferência)* vêm do código, porque a documentação é omissa. Itens marcados *(doc × código)* são conflitos em que **os dois lados são suspeitos**.
- Escala: **CRÍTICO** (bloqueia produção ou perde/expõe dados), **MÉDIO** (defeito real ou risco relevante com contorno), **BAIXO** (dívida técnica, polimento, risco remoto).

---

## 1. Inconsistências entre documentação e código

| # | Documentação diz | Código mostra | Avaliação |
|---|---|---|---|
| D1 | `MELHORIAS-BENEFICIARIO.md`: "a interface agora usa as permissões efetivas do servidor, **não uma tabela fixa**". | `Service/Permissoes/permissaoService.ts` mantém `PERMISSOES_POR_CARGO` fixa. `Pages/Permissoes/Permissoes.tsx` a usa como estado inicial e como *fallback* quando a API falha. | **Doc incorreta ou código incompleto.** A tabela fixa ainda existe e pode ser exibida como se fosse real (ver R3). |
| D2 | `RELATORIO_SEGURANCA_OWASP.md`: "Tokens movidos para cookies **HttpOnly e Secure**". | O backend (`FuncionarioController.login`) faz `setSecure(false)`, não define `SameSite`, e `SecurityConfiguracao` desabilita CSRF. | **A doc é falsa quanto a "Secure".** O próprio comentário do código diz "ajustado para desenvolvimento". É o mesmo código que roda no deploy AWS em HTTP (ver S1). |
| D3 | `MELHORIAS-BENEFICIARIO.md`: permissões carregadas pelo endpoint próprio, e falhas bloqueiam o acesso. | `Permissoes.tsx` informa "O novo acesso entra no **próximo login**", mas o `UserProvider` busca `/funcionarios/me/permissoes` só ao iniciar o app e não observa mudanças. | **Ambíguo.** Não dá para saber se a permissão é efetiva na hora ou só no relogin. A UI não reflete nenhuma das duas versões com clareza. |
| D4 | `API-FINANCEIRO.md` e `RESUMO_PAGINACAO.md` descrevem `GET /gastos/paginado` e `GET /obras/paginado`. | O frontend nunca os usa. Baixa tudo e pagina/ordena/filtra no cliente (5 itens por página, fixo). | **Funcionalidade documentada como entregue, mas não integrada** (ver Q8, I1). |
| D5 | `API-FINANCEIRO.md`: "Dashboard restrito a ADMIN". Permissões `GERAR_RELATORIO` e `VISUALIZAR_RELATORIO` são atribuídas a RESPONSAVEL_ADMINISTRATIVO e ENGENHEIRO. | A única tela de "relatório" é um botão no Dashboard (rota `adminOnly`). `/relatorios` redireciona para `/home`. | **Permissões sem tela correspondente** (ver R1 e R4). |
| D6 | `contexto.txt` lista como essencial "Exportação de relatório de gastos" e como restrição "o envio à contabilidade será feito por meio da exportação de relatórios". | Só existe "Gerar relatório" no Dashboard, que abre um popup com agregados e chama `window.print()`. Não exporta a lista de gastos nem notas, e não gera PDF/CSV/XLSX. | **Requisito essencial não atendido na prática** (ver R1). |
| D7 | `MELHORIAS-BENEFICIARIO.md`: ADMIN e RESPONSAVEL_ADMINISTRATIVO registram pagamento, e este último "precisa estar alocado na obra". | `authService.podeRegistrarPagamento()` só olha o cargo. O modal exibe o formulário a qualquer RA. O texto do modal fala em "alocado". | **A UI promete uma regra que não verifica.** O backend deve negar *(inferência: a doc afirma que a API impõe)*. |
| D8 | `API-FINANCEIRO.md`: presença valida horários (entrada/saída). | `presencaService.PresencaRequest` tem `horarioEntrada/horarioSaida`, mas nenhuma tela os captura. | Funcionalidade de API sem UI. |
| D9 | `MELHORIAS-BENEFICIARIO.md`: "Cadastro de obra deixou de solicitar localização". | `CardObra.tsx` ainda renderiza um ícone `MapPin` com o **status** da obra no lugar de "local". | Resquício visual enganoso. |
| D10 | `MELHORIAS-BENEFICIARIO.md`: "124 testes… lint aprovado… sem erros de console". | Não verificável aqui. Não há testes para `Dashboard`, `Permissoes`, `GestaoPresenca`, `AlocacaoFuncionario`, `Layout/Sidebar/Navbar` e `PeriodoFiltro`. `StatCard.test.tsx` fica fora de `src` (não entra no `tsc -b`). | A cobertura declarada não cobre as telas mais arriscadas. |
| D11 | `README` do frontend e `package.json`. | `react/README.md` é o template do Vite. O pacote se chama `meu-projeto`. `recharts` está em `dependencies` e não é importado. | Documentação e metadados de template. |

---

## 2. Qualidade do código

### [MÉDIO] Q1 · Componente-monolito `DetalhamentoObras` (1149 linhas)
**Arquivo:** `jaf-frontend/react/src/Pages/Obras/DetalhamentoObras.tsx`
**Descrição:** Um único componente tem cerca de 35 `useState`, 4 modais inline (edição de obra, gasto, exclusão de obra e reembolso), upload e OCR de nota fiscal, filtros, ordenação, paginação, formatadores, normalizadores de método de pagamento e regras de permissão. Os dois templates (tabela desktop e lista mobile) duplicam todo o JSX da linha de gasto.
**Impacto:** Qualquer tecla digitada em qualquer modal re-renderiza a página inteira. Mudanças são arriscadas, os testes ficam frágeis e há muita chance de conflito de merge. É o arquivo onde estão as regras mais críticas de dinheiro.
**Sugestão:** Extrair `GastoFormModal`, `ObraEditModal`, `GastosTable/GastosList` (um componente de linha usado nos dois layouts), `useGastos(obraId, filtros)` e `useObra(id)`. Exemplo de formato:
```tsx
function useGastos(obraId?: number, filtros?: GastoFiltro) {
  const [state, setState] = useState<{data: Gasto[]; loading: boolean; error?: string}>(...);
  useEffect(() => { const ctrl = new AbortController(); /* ... */ return () => ctrl.abort(); }, [obraId, filtros]);
  return state;
}
```

### [MÉDIO] Q2 · Autorização lida de um singleton fora do React, durante o render
**Arquivo:** `Service/Auth/Login/authService.tsx`, `authorizationSession.ts`; usado em `CardObra`, `DetalhamentoObras`, `Sidebar`, `Home`, `Dashboard`, `NovaObra` etc.
**Descrição:** `authService.hasAuthority()` e `isAdmin()` leem uma variável de módulo (`session`) mutável. Os componentes a consultam no render sem assinar nenhum estado. Só atualizam quando algo **não relacionado** os re-renderiza. Além disso, `authorizationSession.current()` tem efeito colateral (chama `clear()`) dentro de uma leitura feita no render, e lê `localStorage` a cada chamada.
**Impacto:** Se as permissões mudarem (refresh, relogin em outra aba, `refreshUser`), a UI pode ficar mostrando botões do estado antigo ou escondendo botões válidos. Além disso, `current()` pode zerar a sessão no meio de um render.
**Sugestão:** Expor as permissões pelo `UserContext` (estado React) e consumir via hook:
```tsx
const { can, isAdmin } = useAuthz(); // can("EDITAR_GASTO")
```
Manter o singleton apenas para o interceptor do axios, se for preciso.

### [MÉDIO] Q3 · Requisições sem cancelamento e com respostas fora de ordem
**Arquivo:** `DetalhamentoObras.tsx` (`carregarDados`), `Dashboard.tsx`, `GestaoPresenca.tsx`, `Home.tsx`, `AlocacaoFuncionario.tsx`, `Permissoes.tsx` (`selecionarUsuario`); nenhum uso de `AbortController`/flag `ativo` fora de `ResumoReembolsos`, `FinanceiroObra`, `NovaObra` e do efeito de gastos.
**Descrição:** Os efeitos disparam `await` sem invalidar respostas antigas. Exemplos concretos:
- `Dashboard`: trocar rapidamente etapa/período pode deixar a resposta mais lenta sobrescrever a mais nova.
- `Permissoes.selecionarUsuario`: clicar em dois usuários seguidos pode exibir (e depois **salvar**) as permissões do usuário errado.
- `DetalhamentoObras.carregarDados`: quando o `GET /obras/{id}` falha, `setObra` não é chamado e o título/dados da obra **anterior** continuam na tela, com um toast de erro.

**Impacto:** Dados de outra obra/usuário/período exibidos como se fossem atuais. Em finanças e permissões isso é inaceitável.
**Sugestão:** Padrão único por efeito: `let ativo = true` ou `AbortController` (`api.get(url, { signal })`). Adotar TanStack Query ou SWR resolve cancelamento, cache, retry e estado de erro de forma consistente. Em `carregarDados`, limpar `setObra(null)` no `catch`.

### [MÉDIO] Q4 · Sem `ErrorBoundary`
**Arquivo:** `src/main.tsx`, `src/App.tsx`, `src/routes.tsx`
**Descrição:** Não há `ErrorBoundary` nem `errorElement` nas rotas.
**Impacto:** Qualquer exceção de render (ex.: `gasto.funcionario` nulo, `item.categoria.toLowerCase()` com `null` vindo da API) derruba a aplicação inteira em tela branca. Para o perfil de usuário descrito no `contexto.txt`, isso é um beco sem saída.
**Sugestão:** Envolver `<Outlet />` no `Layout` com um boundary (ou `errorElement` no `createBrowserRouter`) com botão "Recarregar" e envio do erro a um serviço de monitoramento.

### [MÉDIO] Q6 · Duplicação sistemática de helpers e constantes
**Arquivo:** vários.
**Descrição:** Cópias independentes de:
- `formatarMoeda` em `Home`, `CardObra` e `DetalhamentoObras`, além de `moeda` em `Utils/financeiro.ts`, e `formatBRL` em `Dashboard` e `Dashboard/data.ts` (esta sem espaço depois do `R$`, diferente das demais).
- `getIniciais` (3 versões, com comportamentos diferentes para espaços duplicados).
- `formatarData` (4 versões; umas com `timeZone`, outras sem).
- `dataHoje()` em `DetalhamentoObras` = `hojeIso()` em `periodUtils`.
- `normalizarMetodoPagamento` ≈ `normalizarMetodoPagamentoOcr`.
- Rótulos de cargo: `CargoLabel` (`Types/user.ts`), `CARGO_LABELS` + `CARGO_BADGES` (`Permissoes.tsx`), `CARGO_LABELS` (`AlocacaoFuncionario.tsx`), `opcoesCargo` (`NovoFuncionario.tsx`).
- URL de imagem de obra (glbimg) em `DetalhamentoObras` e `AlocacaoFuncionario`.

**Impacto:** Correções ficam pela metade. Já há divergência real de fuso/formatação (ver P2 e U6).
**Sugestão:** Consolidar em `Utils/format.ts` e `Constants/dominio.ts` (cargos, métodos de pagamento, categorias, etapas) e importar em todos os lugares.

### [MÉDIO] Q8 · Filtro, ordenação e paginação 100% no cliente, apesar de endpoints paginados
**Arquivo:** `DetalhamentoObras.tsx` (`gastosFiltrados`, `ITENS = 5`), `AlocacaoFuncionario.tsx`, `Permissoes.tsx`, `Home.tsx`
**Descrição:** Cada obra baixa **todos** os gastos (com `funcionario` e `obra` aninhados) e ordena/filtra em JS a cada render, sem `useMemo`. Quando há filtro de pessoa ou status, faz **duas** chamadas (`historico` e `consulta`) só para popular um `<select>`. O backend já oferece `/gastos/paginado` com `sortBy/sortDir`. O filtro de **período** também é local, então "Mostrando X de Y" reflete só o que foi baixado.
**Impacto:** Com milhares de gastos por obra (cenário normal para uma construtora em 1-2 anos) a tela fica lenta em celular, o payload cresce sem limite e o total exibido pode divergir do servidor.
**Sugestão:** Usar `/gastos/paginado` com `page/size/sortBy/sortDir`, enviar período ao servidor (parâmetro hoje inexistente em `GastoFiltro`; adicioná-lo no backend) e uma consulta dedicada de "pessoas da obra" para o select.

### [BAIXO] Q5 · Sem code-splitting
**Arquivo:** `src/routes.tsx`
**Descrição:** Todas as páginas são importadas estaticamente. O Dashboard, a gestão de presença e os perfis de acesso entram no bundle da tela de login.
**Impacto:** Primeiro carregamento mais lento em rede móvel (o produto é *mobile first*).
**Sugestão:** `const Dashboard = lazy(() => import("./Pages/Dashboard/Dashboard"))` com `<Suspense>`.

### [BAIXO] Q7 · Código morto, restos de template e protótipo legado
**Arquivo:** `Service/Ocr/ocrService.ts` (endpoint `/ocr` não usado), `Pages/Dashboard/data.ts` (dados mock), `Types/user.ts:JwtPayload`, `authService.getToken/decodeToken/getEmail`, `LoginResponse.token`, `localStorage "userCargo"` (gravado e nunca lido), dependência `recharts`, `public/assets/{hero.png,react.svg,vite.svg}`, `jaf-frontend/{endpoint.js,index.html,index.css,banco.json}`, `permissaoService.getMapaPadraoPermissoes/getPermissoesPorCargo`.
**Descrição:** Código sem uso, com comentários "CORREÇÃO DE SEGURANÇA A07" longos, e um protótipo anterior (json-server + `sessionStorage` + `innerHTML`) versionado ao lado da aplicação.
**Impacto:** Confunde quem lê, ocupa superfície de ataque e mantém credenciais de exemplo no repositório (ver S6).
**Sugestão:** Remover. Se o protótipo precisar ser preservado, mover para outro repositório ou tag.

### [BAIXO] Q9 · Vazamentos pequenos de recursos
**Arquivo:** `Pages/Profile/Profile.tsx` (`URL.createObjectURL` nunca revogado), `Service/Gastos/ocrService.ts` (polling por até 2 min sem cancelamento).
**Descrição:** Cada foto escolhida cria um object URL que fica na memória até fechar a aba. O polling do OCR continua após o usuário sair da página.
**Impacto:** Baixo isoladamente; relevante em celulares modestos e uso longo.
**Sugestão:** `useEffect(() => () => URL.revokeObjectURL(preview), [preview])` e `signal` repassado ao `api.get` do polling.

### [BAIXO] Q10 · `UserContext` não memoizado e validação duplicada
**Arquivo:** `Context/UserContext.tsx`, `Pages/Auth/Login/Login.tsx`
**Descrição:** O `value` do Provider é um objeto novo a cada render, e todos os consumidores re-renderizam. O efeito inicial chama `refreshUser()` e o cleanup `clearAuthorization()` invalida a primeira chamada, então em `StrictMode` há requisições duplicadas a `/me` e `/me/permissoes`. No login, `refreshUser` roda de novo logo após `POST /login`.
**Impacto:** Re-renders e tráfego desnecessários. Não quebra nada hoje.
**Sugestão:** `useMemo` no `value`; evitar o cleanup que invalida a requisição em voo.

### [BAIXO] Q11 · Inconsistências de estilo e organização
**Arquivo:** `NovoFuncionario.tsx` (`style={{color:"red"…}}` repetido 5×), `Components/Layout/Navbar/Navber.tsx` (nome com erro de digitação), `cardObra.tsx` (minúscula) × `CardObra.module.css`, `package.json:name = "meu-projeto"`, `Sidebar.tsx` com `<h1>` próprio além do `<h1>` de cada página.
**Descrição:** Mistura de CSS Modules com `style` inline, nomes de arquivo inconsistentes e vários `<h1>` por página.
**Impacto:** Em sistemas de arquivos que diferenciam maiúsculas (Linux/CI) imports com caixa errada quebram. Acessibilidade ruim.
**Sugestão:** Padronizar nomes (`Navbar.tsx`, `CardObra.tsx`), mover o estilo inline para módulos e usar um único `<h1>` por página.

### [BAIXO] Q12 · Cobertura de testes desigual
**Arquivo:** `src/**/*.test.tsx`, `react/StatCard.test.tsx`
**Descrição:** Há testes para login, rotas, financeiro, perfil etc., mas **nenhum** para `Dashboard` (relatório HTML), `Permissoes` (escrita de permissões), `GestaoPresenca`, `AlocacaoFuncionario`, `Sidebar/Navbar` e `PeriodoFiltro`. `StatCard.test.tsx` está fora de `src` e não é verificado por `tsc -b`.
**Impacto:** As telas com maior risco de regra de negócio (permissões, presença) não têm rede de segurança.
**Sugestão:** Cobrir ao menos `Permissoes.handleSalvar` (falha parcial), `GestaoPresenca.salvarStatus` e `Dashboard.gerarRelatorio`. Mover o teste de `StatCard` para `src/Components/StatCard/`.

---

## 3. Segurança

> Não encontrei `dangerouslySetInnerHTML`, `eval` nem `innerHTML` no código React atual (`src`). O JSX escapa strings por padrão, então **não há XSS refletido/armazenado evidente na aplicação React**. Os riscos abaixo estão em cookie/sessão, CORS e infraestrutura que o frontend assume.

### [CRÍTICO] S1 · JWT em cookie sem `Secure`/`SameSite`, CSRF desabilitado, tráfego HTTP e CORS com credenciais para qualquer `*.elb.amazonaws.com`
**Arquivo:** `Service/Auth/Login/Api/Api.ts` (`withCredentials = true` em tudo); backend: `FuncionarioController.login/logout`, `SecurityConfiguracao` (`csrf.disable()`, `setAllowedOriginPatterns`); `nginx.conf` (só `listen 80`).
**Descrição:** O frontend depende 100% de um cookie `jwt` com estas características:
- `HttpOnly` (bom), **`Secure=false`**, **sem atributo `SameSite`**, validade de 1 h.
- O backend desabilita CSRF e confia apenas no cookie.
- CORS com `allowCredentials(true)` e padrões `http://*.elb.amazonaws.com` e `https://*.elb.amazonaws.com`. Esse padrão casa com **o ALB de qualquer conta AWS**, não só o seu.
- O serviço publicado só atende HTTP (porta 80; não vi TLS no `nginx.conf` nem HSTS).

**Impacto:** O cookie de sessão trafega em texto puro. Um site hospedado em qualquer ALB (ou qualquer origem que consiga enviar o cookie, por exemplo em navegadores sem SameSite=Lax por padrão) pode disparar requisições autenticadas (`POST /gastos`, `PUT /obras`, `DELETE`, troca de cargo) em nome do usuário logado. O relatório OWASP interno afirma o contrário (D2).
**Sugestão:**
- Terminar TLS no ALB/CloudFront, habilitar HSTS e configurar o cookie com `Secure; HttpOnly; SameSite=Lax` (ou `Strict`) e `Path=/`:
  ```java
  ResponseCookie.from("jwt", token).httpOnly(true).secure(true)
      .sameSite("Strict").path("/").maxAge(Duration.ofHours(1)).build();
  ```
- Reativar CSRF (token duplo `X-XSRF-TOKEN` com `CookieCsrfTokenRepository.withHttpOnlyFalse()`) ou, no mínimo, validar `Origin`/`Referer` e exigir um header custom em métodos mutantes.
- Trocar o CORS por uma lista **exata** de origens do próprio domínio.
- Como o app é servido pelo mesmo nginx em `/api`, considerar **remover o CORS** (mesma origem) e deixar `VITE_API_URL=/api`.

### [MÉDIO] S2 · Estado de autenticação do cliente baseado em `localStorage` (PII persistente e flag manipulável)
**Arquivo:** `authService.tsx` (`isAuthenticated`, `login`), `UserContext.tsx` (grava `userEmail`, `userName`, `userId`, `userCargo`), `authorizationSession.ts`
**Descrição:** `isAuthenticated()` é `localStorage.getItem("userEmail") !== null`. Isso persiste e-mail, nome, id e cargo em texto no disco, **depois** que o cookie expira (1 h) e mesmo após fechar o navegador. O `userCargo` gravado nunca é usado. Um usuário (ou script em uma extensão) pode setar `userEmail` manualmente para passar da primeira checagem.
**Impacto:** Em computador compartilhado (comum em obra/escritório), o próximo usuário vê nome/e-mail do anterior e o app "parece logado" até a primeira chamada falhar. A UI não confia nisso para autorizar (a revalidação por `/me` mitiga), por isso é MÉDIO e não CRÍTICO.
**Sugestão:** Guardar só em memória (o `UserContext` já faz isso) e descobrir o estado de login chamando `/funcionarios/me` ao iniciar. Remover `localStorage` do fluxo e a chave `userCargo`.

### [MÉDIO] S3 · Troca de usuário em outra aba deixa a UI de um usuário operando com o cookie do outro
**Arquivo:** `authorizationSession.current()`, `UserContext.tsx`, `Api.ts`
**Descrição:** O cookie é compartilhado entre abas, e o estado React não. Se a aba A fizer login com o usuário B, a aba B continua exibindo o nome/cargo do usuário antigo, só que **cada requisição já sai autenticada como o novo usuário**. Não há listener de `storage` nem de `visibilitychange`. Na aba antiga, `authorizationSession.current()` percebe a divergência de `localStorage` e zera as permissões, e os botões somem sem redirecionar.
**Impacto:** Um ADMIN que entrega o celular/PC e outra pessoa loga em outra aba pode acabar registrando gastos/pagamentos "como" um usuário diferente do que a tela mostra. Isso quebra a rastreabilidade financeira.
**Sugestão:** Ouvir `window.addEventListener("storage", …)` e `visibilitychange`; ao detectar mudança de usuário, chamar `refreshUser()` e, se divergir, redirecionar para `/`. Melhor ainda: `BroadcastChannel("auth")`.

### [MÉDIO] S4 · Senha em texto no console do navegador após falha de login
**Arquivo:** `Service/Auth/Login/authService.tsx` (`console.error("Erro ao fazer login:", error)`)
**Descrição:** O `error` é um `AxiosError`, e `error.config.data` contém o JSON `{"email":…,"senha":…}` enviado.
**Impacto:** Qualquer ferramenta que capture o console (extensão, Sentry futuro, suporte remoto) recebe a senha digitada.
**Sugestão:** Registrar só dados seguros: `console.error("login falhou", axios.isAxiosError(e) ? e.response?.status : e)`. Aplicar a mesma regra a `console.error(error)` espalhados nos serviços.

### [MÉDIO] S5 · Sem CSP, cabeçalhos de segurança perdidos em `location` filhos e recursos de terceiros
**Arquivo:** `react/nginx.conf`, `Pages/Home/Home.tsx`, `DetalhamentoObras.tsx`, `AlocacaoFuncionario.tsx`
**Descrição:**
- Não há `Content-Security-Policy`, `Strict-Transport-Security` nem `Permissions-Policy`. `X-XSS-Protection` é obsoleto. `Referrer-Policy: no-referrer-when-downgrade` vaza a URL completa.
- No nginx, `add_header` dentro de `location /assets/`, `/uploads/` e `/health` **substitui** (não herda) os `add_header` do `server`. Esses `location` ficam sem `X-Frame-Options`/`nosniff`.
- O app carrega imagens de `images.unsplash.com` e `s2.glbimg.com` em telas autenticadas, o que expõe IP e `Referer` a terceiros e cria dependência externa.

**Impacto:** Sem CSP, qualquer XSS futuro (um `dangerouslySetInnerHTML` ou dependência comprometida) tem acesso total à sessão. As imagens externas podem sumir ou ser trocadas.
**Sugestão:** Adicionar CSP restritiva (`default-src 'self'; img-src 'self' data: blob:; script-src 'self'; frame-ancestors 'none'`), `Referrer-Policy: strict-origin-when-cross-origin`, HSTS, repetir os cabeçalhos em cada `location` (ou usar `include snippets/security.conf`) e hospedar as imagens (ou removê-las, ver U4). Observação: o popup do relatório usa script inline (S9) e precisará ser ajustado.

### [MÉDIO] S6 · Credenciais em texto puro e protótipo inseguro versionados
**Arquivo:** `jaf-frontend/banco.json`, `jaf-frontend/endpoint.js`
**Descrição:** `banco.json` está no Git e contém e-mails e senhas em texto puro (`123456` e uma senha de aparência real, `V20r18p10`, para `felipe@gmail.com`). `endpoint.js` guarda token em `sessionStorage`, concatena e-mail em URLs e usa `innerHTML`.
**Impacto:** Se alguma dessas contas ou senhas existir em produção ou for reutilizada, o risco é imediato. O histórico do Git preserva os valores mesmo após remoção.
**Sugestão:** Remover os arquivos do repositório, **rotacionar** qualquer senha que tenha sido real, e fazer `git filter-repo`/BFG se for aplicável. Adicionar scanner de segredos (gitleaks) no CI.

### [MÉDIO] S7 · Exclusão da própria conta sem reautenticação e sem proteção do último ADMIN
**Arquivo:** `Pages/Profile/Profile.tsx` (`handleExcluirConta`), `Service/Funcionarios/funcionarioService.ts` (`DELETE /funcionarios/me`)
**Descrição:** A confirmação é digitar "EXCLUIR". Não pede a senha atual. A documentação admite que "não há bloqueio do último ADMIN". Qualquer usuário (inclusive o único ADMIN) pode apagar a conta, que é autor de gastos e pagamentos.
**Impacto:** Uma sessão deixada aberta permite apagar a conta sem a senha. A exclusão do único ADMIN deixa o sistema sem dono e sem gestão de permissões. Não sei como o backend trata o histórico de gastos desse usuário *(inferência: não li o service)*.
**Sugestão:** Exigir `senhaAtual` no `DELETE`, bloquear a exclusão do último ADMIN no backend e preferir desativação (soft delete) para preservar a autoria dos lançamentos.

### [BAIXO] S8 · `fotoUrl` arbitrária e `/uploads` público
**Arquivo:** `Profile.tsx`, `funcionarioService.ts`, `nginx.conf` (`/uploads/` com `immutable` por 30 dias), `SecurityConfiguracao` (`/uploads/** permitAll`)
**Descrição:** `PUT /funcionarios/me` aceita qualquer string de até 512 caracteres em `fotoUrl` e a tela renderiza em `<img src>`. As fotos enviadas ficam acessíveis **sem autenticação** e com cache de 30 dias.
**Impacto:** Pixel de rastreamento (URL externa) e fotos de funcionários públicas por URL (privacidade/LGPD). Impacto limitado, porque só o próprio usuário define a sua foto.
**Sugestão:** Aceitar apenas URLs do próprio `/uploads/` ou do catálogo de avatares; servir fotos autenticadas.

### [BAIXO] S9 · Relatório gerado com `document.write` e script inline
**Arquivo:** `Pages/Dashboard/Dashboard.tsx` (`gerarRelatorio`)
**Descrição:** O HTML é montado por template string e escrito num popup de mesma origem, com `<script>` inline para `window.print()`. As strings de texto passam por `escaparHtml`, mas `item.value` e valores numéricos entram sem escape (hoje são números). `window.alert` é usado para o bloqueio de popup.
**Impacto:** Hoje é seguro. Fica frágil se o DTO passar a devolver string e é incompatível com uma CSP estrita.
**Sugestão:** Gerar PDF no servidor (ver R1) ou uma rota `/relatorio/print` renderizada pelo React com CSS `@media print`.

### [BAIXO] S10 · Rotas de obra sem permissão explícita e proteção só no cliente
**Arquivo:** `src/routes.tsx` (`/obras/detalhamento`, `/obras/detalhamento/:id`, `/home`, `/perfil`)
**Descrição:** Essas rotas não usam `requiredPermission="VISUALIZAR_OBRA"`. A proteção de rota é só de UX (como deve ser), mas isso depende de a API aplicar o escopo em todas as chamadas.
**Impacto:** Um usuário sem `VISUALIZAR_OBRA` vê o esqueleto das telas com erros em vez de um "sem acesso" claro.
**Sugestão:** Adicionar `requiredPermission` onde fizer sentido e manter o enforcement na API (que a doc diz aplicar).

### [BAIXO] S11 · Lista completa de funcionários (com e-mail) carregada em telas de operação
**Arquivo:** `NovaObra.tsx`, `AlocacaoFuncionario.tsx` (`GET /funcionarios`)
**Descrição:** Para preencher um `<select>` carrega todos os usuários com e-mail e cargo.
**Impacto:** Minimização de dados fraca; só afeta quem tem `CRIAR_ALOCACAO`.
**Sugestão:** Endpoint de "pessoas alocáveis" com `id/nome/cargo` apenas.

---

## 4. Regra de negócio

### [CRÍTICO] R1 · "Exportação de relatório de gastos" para a contabilidade não existe
**Arquivo:** `Pages/Dashboard/Dashboard.tsx` (`gerarRelatorio`), `src/routes.tsx` (`/relatorios → /home`); backend tem `RelatorioController` (CRUD) que o frontend não consome.
**Descrição:** O `contexto.txt` define como requisito **essencial** a exportação do relatório de gastos com filtro de data, e como restrição que "o envio à contabilidade será feito por meio da exportação de relatórios". O que existe é um popup que imprime **agregados do dashboard** (totais, pizza, barras). Não exporta a lista de gastos, não leva notas fiscais, não gera arquivo (PDF/XLSX/CSV), só está disponível para ADMIN (rota `adminOnly`) e ignora o filtro "Pessoa/Status/Método" da lista. Tudo isso é *(inferência)*, porque a documentação do frontend é omissa.
**Impacto:** O objetivo central do projeto (reduzir o esforço de consolidar dados para a contabilidade) não é atendido. O RESPONSAVEL_ADMINISTRATIVO, que tem `GERAR_RELATORIO`, nunca consegue gerar nada.
**Sugestão:** Criar `GET /obras/{id}/gastos/export?format=csv|xlsx|pdf&from&to&…` no backend e um botão "Exportar" em `DetalhamentoObras` (visível por `GERAR_RELATORIO`), com download por `blob`:
```ts
const { data } = await api.get(`/gastos/export`, { params, responseType: "blob" });
saveAs(data, `gastos-obra-${obraId}.xlsx`);
```
Se o prazo não permitir, ao menos exportar CSV client-side da lista **completa** filtrada (com BOM UTF-8 para o Excel).

### [MÉDIO] R2 · Validações do frontend divergem das do backend (usuário só descobre no 400)
**Arquivo:** `NovoFuncionario.tsx`, `NovaObra.tsx`, `Profile.tsx` × DTOs do backend (`FuncionarioDto`, `AlterarSenhaDto`, `ObraDto`)
**Descrição:**
| Campo | Frontend | Backend |
|---|---|---|
| Senha de novo usuário | ≥8, maiúscula, minúscula e **qualquer** caractere especial; **não exige dígito** | `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$` (exige dígito; só aceita `@$!%*?&`) |
| Nova senha (perfil) | `^(?=.*[A-Z])(?=.*[a-z])(?=.*\d).{6,}$` (outra política) | `@Size(min=6,max=100)` |
| Nome do funcionário | só "não vazio" | 3–100 caracteres e `^[a-zA-ZÀ-ÿ\s]+$` (rejeita `'`, `-`, números) |
| Título da obra (criar) | só "não vazio" | 5–150 caracteres e `^[a-zA-Z0-9À-ÿ\s\-]+$` (rejeita `( ) , . /`) |
| Título da obra (editar) | `length ≥ 5` | igual ao anterior |

Em `NovoFuncionario` o 400 vira "Dados inválidos. Verifique as informações" (a mensagem do backend é descartada).
**Impacto:** Uma senha como `Abcdefgh!` ou `Senha#2026` passa na tela e é recusada pela API sem explicar o motivo. Um título como "Edifício Aurora (Bloco A)" é bloqueado só no servidor. Três políticas de senha diferentes confundem o administrador.
**Sugestão:** Centralizar as regras em `Utils/validacao.ts`, espelhando o backend (ou gerando o schema a partir de uma fonte única, por exemplo `zod` + OpenAPI), exibir `error.response.data.fields` por campo e **revisar com o backend** se o regex do título e do nome é realmente desejado (nomes com apóstrofo e obras com parênteses são comuns).

### [MÉDIO] R3 · Regras de autorização duplicadas no cliente e fallback "fabricado" em Permissoes
**Arquivo:** `Service/Permissoes/permissaoService.ts` (`PERMISSOES_POR_CARGO`), `Pages/Permissoes/Permissoes.tsx` (`selecionarUsuario`, `handleCargoChange`), `authService.podeRegistrarPagamento`, `authService.isAdmin`
**Descrição:** O frontend conhece a matriz padrão cargo→permissões e a usa:
1. como estado inicial de `permissoesPorCargo` (antes de o `GET /permissoes/cargos` responder);
2. em `selecionarUsuario`: se `GET /funcionarios/{id}/permissoes` falhar, a tela mostra as permissões **padrão do cargo** (como se fossem as reais), só com um toast;
3. em `handleSalvar`, que envia o conjunto atual inteiro para `PUT /funcionarios/{id}/permissoes`.

Quanto a `isAdmin()`/`podeRegistrarPagamento()`, a regra "quem é o dono" e "quem paga" está em strings de cargo no cliente.
**Impacto:** Numa falha de rede, o ADMIN pode alterar uma permissão em cima de dados que não são os reais e **sobrescrever** as permissões personalizadas do usuário com o padrão. Mudar a matriz no backend exige mudar o frontend (e a doc diz que isso já foi eliminado, ver D1).
**Sugestão:** Em `selecionarUsuario`, **não** preencher fallback: mostrar erro e desabilitar o formulário até carregar. Remover `PERMISSOES_POR_CARGO` do cliente e consumir apenas `/permissoes/cargos`. Expor `podePagar`/`isDono` como flags vindas da API (`/me`).

### [MÉDIO] R4 · Matriz de permissões inclui direitos que a UI não consegue exercer
**Arquivo:** `permissaoService.ts` (matriz), `DetalhamentoObras.tsx` (`podeEditarObra = admin && …`), `Dashboard.tsx` (`GERAR_RELATORIO`), `routes.tsx` (`/relatorios`)
**Descrição:** O RESPONSAVEL_ADMINISTRATIVO tem `EDITAR_OBRA` e `GERAR_RELATORIO` na matriz exibida em "Perfis de acesso", mas:
- `podeEditarObra` exige `admin`, porque o `PUT` legado exige o orçamento, que é oculto para não-ADMIN (comentário no código);
- o Dashboard e o relatório são `adminOnly`.

`VISUALIZAR_RELATORIO` não tem nenhuma tela.
**Impacto:** O ADMIN concede uma permissão achando que ela vale; o funcionário não vê efeito. Cria chamados de suporte e uma falsa sensação de controle.
**Sugestão:** Decidir a regra (por exemplo, "editar obra sem orçamento" com um `PATCH` parcial) e alinhar matriz, API e UI. Esconder da tela de permissões o que não tem efeito.

### [MÉDIO] R5 · "Total gasto" da Home calculado no cliente, com falhas silenciosas
**Arquivo:** `Pages/Home/Home.tsx` (`Promise.allSettled`, `totalGasto`, `totalGastoPorObra`)
**Descrição:** O KPI soma `gasto.valor` (float JS) de todas as obras buscando todos os gastos. Se a chamada de uma obra falhar (`rejected`), ela é descartada sem aviso, e o total mostrado fica **menor**, sem qualquer indicação. O backend já tem `/obras/{id}/financeiro` com `totalGasto` (somente ADMIN).
**Impacto:** O dono vê um total subestimado sem saber. Dado financeiro exibido como verdadeiro quando é parcial.
**Sugestão:** Usar um endpoint agregado (`GET /obras/resumo` ou o `financeiro` de cada obra) e, se algo falhar, mostrar "Total indisponível" em vez de um valor parcial.

### [MÉDIO] R6 · Presença usa "alternar" (toggle) em vez de definir o estado desejado
**Arquivo:** `Service/Presencas/presencaService.ts` (`alternarPresenca`), `GestaoPresenca.tsx` (`salvarStatus`), `ControlePresenca.tsx` (`registrarPresenca`)
**Descrição:** Para um registro existente a UI envia `PATCH /presencas/{id}/alternar`, ignorando o valor `presente` calculado. O resultado depende do **estado do servidor** no momento da chamada.
**Impacto:** Com a lista desatualizada (dois gestores, duas abas, duplo clique com latência de rede móvel), a ação "marcar presente" pode inverter para ausente. Isso afeta folha de pagamento/dias trabalhados. `PUT /presencas/{id}` aceita o estado explícito e não é usado para isso.
**Sugestão:** Usar `PUT` com `presente` explícito (idempotente) e mostrar a versão atual antes de salvar. Se o `alternar` for mantido, enviar o estado esperado (`?esperado=true`) e responder 409 quando divergir.

### [MÉDIO] R7 · Fluxo de alocação incompleto e "responsável" arbitrário
**Arquivo:** `Service/Alocacoes/alocacaoService.ts`, `AlocacaoFuncionario.tsx`, `NovaObra.tsx` (`cargoNaObra: "GESTOR_OBRA"`), `Home.tsx` (`resultado.value[0]`)
**Descrição:**
- O requisito "Associar funcionários às obras" tem só **criar** e listar. O backend tem `PUT /alocacoes/{id}` e `DELETE /alocacoes/{id}`, e as permissões `EDITAR_ALOCACAO`/`DELETAR_ALOCACAO` existem, mas não há botão.
- A API de presença exige alocação **vigente na data** (`dtInicio/dtFim`), e a UI nunca envia datas.
- O "funcionário responsável" do card da Home é simplesmente a **primeira** alocação retornada.
- O filtro de período da tela de alocações usa `dtInicio`, que é opcional: alocações sem data somem quando um período é escolhido.

**Impacto:** Não dá para desalocar nem corrigir a função de alguém pela interface, e a regra de presença pode falhar sem causa visível. O "responsável" exibido pode não ser o gestor.
**Sugestão:** Adicionar editar/remover (com confirmação), campos de vigência, marcar explicitamente o gestor da obra (campo na API) e tratar `dtInicio` nulo como "sem restrição".

### [MÉDIO] R8 · Alteração de cargo e de permissões em duas chamadas, sem confirmação e com reset silencioso
**Arquivo:** `Permissoes.tsx` (`handleSalvar`, `handleCargoChange`)
**Descrição:** `PATCH /cargo` e depois `PUT /permissoes` são chamadas separadas. Se a segunda falhar, o cargo já mudou, a tela não atualiza `selectedUser` (continua com o cargo antigo) e o toast só diz "Erro ao salvar cargo". `handleCargoChange` substitui as permissões por **todas as padrão** do novo cargo, descartando personalizações sem avisar. Promover alguém a ADMIN não pede confirmação.
**Impacto:** Estado incoerente entre tela e servidor, perda de permissões personalizadas e escalada de privilégio por clique acidental (a doc reconhece que o "dono" pode conceder ADMIN).
**Sugestão:** Um único endpoint transacional (`PUT /funcionarios/{id}/acesso {cargo, permissoes}`), diálogo de confirmação específico para ADMIN e recarregar o usuário após qualquer falha.

### [MÉDIO] R9 · Qualquer pessoa alocada pode ser escolhida como "Responsável" do gasto (inclusive reembolso)
**Arquivo:** `DetalhamentoObras.tsx` (select "Responsável", `funcionarioId`)
**Descrição:** O formulário de gasto permite escolher qualquer alocado como autor, e o reembolso fica em nome dele. Não há restrição por perfil nem default para o usuário logado. O backend valida escopo da obra, mas a documentação é omissa sobre se um ENGENHEIRO pode lançar uma nota em nome de outra pessoa *(inferência)*.
**Impacto:** Fraude ou erro: lançar um "reembolso" em nome de terceiro cria dívida para o dono. O resumo "Reembolsos por pessoa" passa a refletir isso.
**Sugestão:** Default = usuário logado. Só ADMIN/RA podem escolher outra pessoa, e a API deve impor isso.

### [BAIXO] R10 · Dinheiro como `number` (ponto flutuante) e tipos monetários heterogêneos
**Arquivo:** `Home.tsx` (`reduce`), `gastoService.Gasto.valor: number`, `Obra.orcamento?: string`, `totalRecebido?: number`
**Descrição:** `valorDecimal` converte centavos com cuidado ao **enviar**, mas somas e comparações (`Math.round(numero*100)`) são feitas com floats. `orcamento` vem como string e `Number(orcamento)` é feito sem tratar `NaN`.
**Impacto:** Erros de arredondamento em somas grandes e um `R$ NaN` possível.
**Sugestão:** Manter valores em centavos (inteiros) ou usar `decimal.js`; formatar apenas na borda.

---

## 5. Integração com o backend

### [MÉDIO] I1 · N+1 e carga excessiva na Home
**Arquivo:** `Pages/Home/Home.tsx`
**Descrição:** Para cada obra: 1 requisição de gastos (ADMIN) + 1 de alocações. Com 30 obras são 61 requisições na abertura, cada uma devolvendo entidades completas. Não há paginação (`/obras/paginado` existe) e a lista não tem virtualização.
**Impacto:** Abertura lenta em 3G/4G e carga desnecessária no backend atrás do ALB.
**Sugestão:** Endpoint único `GET /obras/resumo` com `{obra, responsavel, totalGasto}` e paginação.

### [MÉDIO] I2 · Timeout global de 10 s também para upload e OCR; sem validação de arquivo
**Arquivo:** `Service/Auth/Login/Api/Api.ts` (`timeout: 10000`), `ocrService.processarNotaFiscal`, `funcionarioService.uploadFoto`; `nginx.conf` (`client_max_body_size 10m`)
**Descrição:** A foto de nota fiscal (JPEG de celular de 3–8 MB) em rede móvel pode levar mais de 10 s para subir, e o axios aborta com "Não foi possível processar a nota fiscal". O OCR não valida tamanho/tipo no cliente; um arquivo de mais de 10 MB retorna 413 do nginx com mensagem genérica.
**Impacto:** Falhas intermitentes exatamente no fluxo *mobile* mais valioso do sistema.
**Sugestão:** `timeout` maior (60–120 s) só nos uploads, validar tipo e tamanho antes (como já é feito para a foto de perfil), comprimir a imagem no cliente (canvas) e tratar o 413.

### [MÉDIO] I3 · Tratamento de erros inconsistente
**Arquivo:** `Api.ts` (só trata 401), `AlocacaoFuncionario.tsx` ("Erro ao carregar alocações: Request failed with status code 403"), `deletarObra` ("Não foi possível excluir a obra"), `criarAlocacao`, `NovoFuncionario`, `Dashboard` ("Erro ao carregar dados do dashboard."), `GestaoPresenca.excluirRegistro`
**Descrição:** Uns usam `erroApi()` (mensagem do servidor), outros mostram mensagem fixa ou o `error.message` técnico do axios em inglês. Não há tratamento central para 403, 409, 429 e 5xx/timeout, nem *retry* para GET idempotente. `erroApi` só lê `message/mensagem/detail`, mas `Profile.tsx` lê `fields` (padrão de validação do backend) que `erroApi` ignora.
**Impacto:** O usuário não sabe se foi permissão (403), conflito (409: "gasto com pagamentos não pode ser excluído") ou rede. Suporte recebe "deu erro".
**Sugestão:** Um interceptor/`handleApiError(error)` único que mapeia status → mensagem em português e extrai `fields`. Usar em todos os `catch`. Para o 403, redirecionar a uma tela "sem permissão".

### [MÉDIO] I4 · Respostas sem tipo/validação em runtime e parâmetros de rota sem validação
**Arquivo:** `obraService`, `alocacaoService`, `presencaService`, `dashboardService` (usam `api.get(...)` sem genérico, logo `any`); `parseInt(id)` em `DetalhamentoObras`, `Dashboard`, `AlocacaoFuncionario`
**Descrição:** O `any` se propaga como `Obra`/`AlocacaoObra`, e nenhum DTO é validado. `parseInt("12abc")` retorna `12`; `parseInt("abc")` retorna `NaN`, e a requisição sai como `/obras/NaN`. Dados nulos inesperados (ex.: `gasto.funcionario` ou `categoria` nulos) causam `TypeError` no render (ver Q4).
**Impacto:** Um registro legado/corrompido derruba a tela. URLs malformadas geram chamadas inválidas.
**Sugestão:** Validar respostas com `zod` na borda dos serviços, usar `Number(id)` + `Number.isInteger` e `<Navigate>` para 404 quando inválido.

### [BAIXO] I5 · Dashboard: período invertido e acoplamento a strings
**Arquivo:** `Dashboard.tsx`, `PeriodoFiltro.tsx`, `dashboardService.ts`
**Descrição:** No preset "Personalizado" o usuário pode escolher `Até` menor que `De` (o campo só tem `min`, e o `min` não impede digitar). O backend responde 400, e a tela mostra "Erro ao carregar dados do dashboard." substituindo a página inteira. A etapa é enviada como a string literal `"TODAS"`, e a pizza é lida por `item.name === "Pendentes"` com cores por **índice** de array.
**Impacto:** Mensagem enganosa. Qualquer renomeação no backend (`"Pendentes"`, ordem) inverte o gráfico sem erro.
**Sugestão:** Validar `from <= to` no filtro, omitir `etapa` quando for "todas" e usar um campo `tipo` (enum) no DTO da pizza.

### [BAIXO] I6 · Dois clientes de OCR e polling sem controle
**Arquivo:** `Service/Gastos/ocrService.ts` (usado), `Service/Ocr/ocrService.ts` (não usado)
**Descrição:** Dois serviços para o mesmo domínio, um morto. O polling a cada 2 s por 2 min não respeita `Retry-After`, não cancela e não faz *backoff*.
**Impacto:** Confusão, e carga fixa por usuário se o OCR estiver lento.
**Sugestão:** Remover o morto. Polling com backoff e `AbortSignal`.

---

## 6. Experiência e consistência

### [MÉDIO] U1 · Expiração de sessão derruba o usuário sem aviso e sem preservar o trabalho
**Arquivo:** `Api.ts` (interceptor 401 → `window.location.replace('/')`)
**Descrição:** O cookie dura 1 h e não há renovação. Quando expira, a primeira chamada com 401 faz um *full reload* para `/` sem mensagem, e o formulário em edição (gasto com OCR já processado, observação de pagamento) é perdido. O `Login` também descarta a sessão em silêncio quando a validação falha por **qualquer** erro (inclusive queda de rede), e o usuário vê o login sem saber por quê.
**Impacto:** Frustração em um público de "baixa familiaridade tecnológica" (`contexto.txt`) que preenche notas em campo com sinal ruim.
**Sugestão:** Mostrar toast "Sua sessão expirou", navegar com `navigate` preservando `from` para voltar depois, e implementar refresh do token ou sessão deslizante. Diferenciar 401 de falha de rede no `Login`.

### [MÉDIO] U2 · Modais artesanais inacessíveis, que fecham ao clicar fora e perdem os dados
**Arquivo:** `DetalhamentoObras.tsx` (3 modais), `AlocacaoFuncionario.tsx`, `ControlePresenca.tsx`, `Navber.tsx` (este ao menos tem `role="alertdialog"`/Esc) × `ReembolsoModal.tsx` (usa `<dialog>`)
**Descrição:** Os overlays não têm `role="dialog"`, `aria-modal`, `aria-labelledby`, *focus trap*, nem retorno de foco. `onClick` no overlay fecha o formulário de gasto e **descarta** tudo o que foi digitado (um toque acidental fora do modal em tela pequena). Exclusão de gasto/presença usa `window.confirm`, e exclusão de obra usa um modal. Há três padrões diferentes.
**Impacto:** Perda de dados no celular, inacessível por teclado/leitor de tela e experiência inconsistente.
**Sugestão:** Um componente `<Modal>` baseado em `<dialog>` (como o do reembolso) usado em todos os lugares, sem fechar ao clicar fora quando o formulário estiver "sujo" (ou com confirmação).

### [MÉDIO] U3 · Dashboard desmonta a tela inteira a cada mudança de filtro
**Arquivo:** `Dashboard.tsx` (`if (carregando) return <…Carregando…>`)
**Descrição:** Ao trocar etapa/período, o componente retorna apenas "Carregando...", desmontando os próprios filtros. O foco do teclado é perdido, a rolagem volta ao topo e em caso de erro os filtros também somem (não dá para corrigir o período).
**Impacto:** Navegação travada em erro de período. Experiência ruim em celular.
**Sugestão:** Manter o cabeçalho e os filtros montados e mostrar *skeleton*/overlay só nas áreas de dados. Mostrar erro inline com botão "Tentar de novo".

### [MÉDIO] U4 · Home: falha de carga vira "Nenhuma obra encontrada" e imagens falsas mudam com o filtro
**Arquivo:** `Pages/Home/Home.tsx`, `CardObra.tsx`, `DetalhamentoObras.tsx`, `AlocacaoFuncionario.tsx`
**Descrição:**
- Se `obraService.listar()` falhar, só há um toast e a tela mostra "Nenhuma obra encontrada." (indistinguível de lista vazia).
- A foto de cada obra é `imagensObras[index % 4]` (Unsplash), usando o índice **da lista filtrada/ordenada**, e muda ao filtrar. Em `DetalhamentoObras` e `AlocacaoFuncionario` é uma foto fixa de apartamento hospedada no Globo.
- `MapPin` com o status faz o papel de "local" (D9).

**Impacto:** O gestor acha que não tem obras. Imagens de imóveis de terceiros como se fossem as obras reais da JAF.
**Sugestão:** Estado de erro com "Tentar novamente"; usar imagem real da obra (campo no cadastro) ou um placeholder neutro local; remover o `MapPin`.

### [MÉDIO] U5 · Situação do reembolso invisível na lista de gastos
**Arquivo:** `DetalhamentoObras.tsx` (`renderizarReembolso`)
**Descrição:** O gasto não traz o status de reembolso; o selo só aparece **quando um filtro de status está ativo** (comentário: "só é confiável quando filtrado no servidor"). Sem filtro, cada reembolso só mostra o botão "Pagamentos / Registrar".
**Impacto:** Para saber quem já foi pago o gestor precisa filtrar três vezes ou abrir cada modal. É a dor central dos "reembolsos".
**Sugestão:** Incluir `statusReembolso`, `totalPago` e `saldoPendente` no DTO de listagem e sempre exibir o selo e o saldo.

### [BAIXO] U6 · Formatação e textos inconsistentes
**Arquivo:** `ReembolsoModal.tsx` (`Gasto em 2026-10-30`, `p.dataPagamento` em ISO, status `PENDENTE` em maiúsculas), `Profile.tsx` (textos sem acento: "Configuracoes", "seguranca", "confirmacao"), `DetalhamentoObras.tsx` (`obra.status.replace("_"," ")` troca só o primeiro `_`; "0 Dias" tanto para obra atrasada quanto concluída), `Home.tsx` (opções de status em caixa alta sem acento)
**Descrição:** Mistura de formatos de data (ISO × pt-BR), enums crus e acentuação inconsistente.
**Impacto:** Aparência pouco profissional e leitura difícil para o perfil de usuário.
**Sugestão:** Funções únicas `formatarData`, `rotuloStatus`, `rotuloCargo` (ver Q6) e revisão de texto. Mostrar "Atrasada há N dias" / "Concluída".

### [BAIXO] U7 · Troca de e-mail desloga sem aviso
**Arquivo:** `Profile.tsx` (`emailChanged → clearUser(); authService.logout()`)
**Descrição:** Depois de salvar, o usuário é jogado ao login sem explicação.
**Impacto:** Parece um bug ou uma falha ao salvar.
**Sugestão:** Toast/tela "E-mail alterado. Faça login novamente com o novo e-mail".

### [BAIXO] U8 · Acessibilidade por teclado e semântica
**Arquivo:** `Permissoes.tsx` (cards de usuário e cabeçalhos de módulo são `<div onClick>`), `Sidebar.tsx`, `Login.tsx`
**Descrição:** Elementos clicáveis sem `button`/`tabIndex`/`role`. Vários `<h1>` por página. Campos de gasto sem `htmlFor` (apenas `label` envolvendo).
**Impacto:** Inacessível por teclado/leitor de tela.
**Sugestão:** Usar `<button>`/`<details>` ou `role="button"` com `onKeyDown`, e um único `<h1>` por página.

### [BAIXO] U9 · `StatCard` não limita a barra de progresso
**Arquivo:** `Components/StatCard/StatCard.tsx` × `Dashboard.tsx` (usa `limitarPercentual` nas barras mas não nos `StatCard`)
**Descrição:** `style={{ width: `${progress}%` }}` aceita valores negativos ou acima de 100 (o saldo pode ser negativo; `progressoSaldo` = gastos/recebido pode passar de 100).
**Impacto:** A barra extravasa o card ou some. Visual quebrado justamente quando o saldo é negativo.
**Sugestão:** `Math.min(100, Math.max(0, progress))` e cor de alerta quando exceder.

### [BAIXO] U10 · Linguagem técnica para o perfil de usuário-alvo
**Arquivo:** `ReembolsoModal.tsx`, `FinanceiroObra.tsx` *(inferência com base no `contexto.txt`)*
**Descrição:** Termos como "Parcelas não podem ser editadas ou excluídas", "Resultado do pagamento desconhecido. Consulte o histórico…", "Recebido acumulado… não cria histórico de recebimentos", "Saldo dos recebimentos" são precisos, mas densos.
**Impacto:** Risco de erro de operação por quem tem "baixa familiaridade tecnológica".
**Sugestão:** Textos curtos, confirmação final com resumo ("Pagar R$ 500,00 a Gabriel em 30/10?") e ajuda contextual.

### [BAIXO] U11 · "Primeira obra" implícita
**Arquivo:** `DetalhamentoObras.tsx` (`obras[0]` quando não há `:id`), `GestaoPresenca.tsx` (seleciona a primeira obra)
**Descrição:** `/obras/detalhamento` sem id mostra a primeira obra retornada; a tela de presença abre sempre na primeira obra.
**Impacto:** Registrar presença na obra errada porque ninguém escolheu.
**Sugestão:** Exigir seleção explícita ou memorizar a última obra usada.

---

## 7. Problemas potenciais (ainda não quebraram, mas vão)

### [MÉDIO] P1 · Etapas, categorias, métodos e funções fixos em vários arquivos
**Arquivo:** `DetalhamentoObras.tsx` (`"ETAPA 1"`, `"ETAPA 2"`, categorias), `Dashboard.tsx` (`etapas = ["TODAS","ETAPA 1","ETAPA 2"]`), `AlocacaoFuncionario.tsx` (`cargosObra`), `NovaObra.tsx` (`STATUS_OPCOES`, `"GESTOR_OBRA"`)
**Descrição:** Uma obra real tem mais de duas etapas. Ao editar um gasto cuja etapa não é "ETAPA 1/2", o `<select>` não tem a opção, exibe outro valor e **grava a mudança sem o usuário perceber**.
**Impacto:** Corrupção silenciosa de dados quando surgir a "Etapa 3".
**Sugestão:** Servir etapas/categorias da API (por obra) e preservar o valor existente se ele não estiver na lista.

### [MÉDIO] P2 · Fuso horário e virada de dia
**Arquivo:** `DetalhamentoObras.tsx`, `GestaoPresenca.tsx`, `ControlePresenca.tsx` (`new Date(\`${data}T00:00:00\`).toLocaleDateString("pt-BR",{timeZone:"America/Sao_Paulo"})`), `AlocacaoFuncionario.tsx` (sem `timeZone`), `useState(dataHoje())`
**Descrição:** `T00:00:00` é interpretado no fuso do navegador e depois convertido para São Paulo. Num dispositivo em fuso à frente de SP (viagem, VPN, configuração errada), a data exibida **volta um dia**. Em outras telas não há `timeZone`, então as telas divergem. `useState(hojeIso())` congela "hoje" no momento em que a tela abriu. Uma tela aberta de um dia para o outro (comum em celular) valida presença/gastos com a data de ontem.
**Impacto:** Datas de gasto/presença exibidas ou validadas erradas, em um sistema contábil.
**Sugestão:** Formatar a data ISO sem passar por `Date` (`const [a,m,d] = iso.split("-"); \`${d}/${m}/${a}\``) ou usar `T12:00:00`. Recalcular "hoje" no `submit`.

### [MÉDIO] P3 · Sem idempotência nos POST em rede móvel instável
**Arquivo:** `gastoService.criar`, `obraService.criar`, `alocacaoService.criar`, `financeiroService.registrar`, `funcionarioService.cadastrar`
**Descrição:** Só o pagamento de reembolso tem a mitigação explícita (a doc reconhece "sem chave de idempotência"). Os demais POSTs, em caso de *timeout* de 10 s com o servidor já tendo gravado, levam o usuário a tentar de novo e criar **gasto duplicado**. `cadastrarGasto` protege com `useState` (não com `useRef`, diferente de `NovaObra`), e o duplo toque rápido pode passar.
**Impacto:** Gastos e obras duplicados, total gasto inflado.
**Sugestão:** `Idempotency-Key` (UUID gerado ao abrir o formulário) aceito pelo backend e uma trava `useRef` em todos os submits.

### [MÉDIO] P4 · Cache imutável de 1 ano para arquivos sem hash
**Arquivo:** `react/nginx.conf` (`location /assets/ { expires 1y; add_header Cache-Control "public, immutable"; }`), `public/assets/Geral/*`, `public/assets/Login_Img/*`
**Descrição:** O Vite gera arquivos com hash em `/assets/`, mas `public/assets/…` (logo, avatares, fundo) também é servido nesse prefixo sem hash.
**Impacto:** Se a JAF trocar a logo/avatares, os navegadores continuam com a imagem antiga por até 1 ano (`immutable` impede até o revalidar).
**Sugestão:** Mover os estáticos sem hash para outro prefixo (ex.: `/static/`) com cache curto, ou importá-los pelo bundler para ganharem hash.

### [BAIXO] P5 · Build e entrega
**Arquivo:** `react/Dockerfile`, ausência de `.dockerignore`
**Descrição:** `npm install` em vez de `npm ci`, sem `.dockerignore` (o `COPY . .` copia `node_modules` e `dist` locais para a imagem de build), `nginx:alpine` rodando como root e `node:20-alpine as builder` (sem pin de versão/digest). O lint/test não estão no `Dockerfile` nem foram verificados aqui.
**Impacto:** Builds não reproduzíveis e imagem maior do que precisa.
**Sugestão:** `npm ci`, `.dockerignore` (`node_modules`, `dist`, `.git`), `nginxinc/nginx-unprivileged` e CI executando `lint`, `tsc -b` e `vitest run` antes da imagem.

### [BAIXO] P6 · Escalabilidade da base de código
**Arquivo:** `src/Service/*`, `src/Pages/*`
**Descrição:** Cada tela implementa seu próprio *fetch/loading/erro/cache*. Não há camada de dados (React Query/SWR), formulários sem biblioteca (`react-hook-form`) e padrões de bloqueio de duplo envio diferentes (`useRef` × `useState`). Cada nova tela repete os problemas Q3, I3 e P3.
**Impacto:** Custo de manutenção crescente e bugs repetidos a cada feature (anexos, e-mail automático, "monitoramento anual" listados como desejáveis).
**Sugestão:** Adotar TanStack Query + `react-hook-form` + `zod`, e criar hooks por domínio (`useObras`, `useGastos`, `usePresencas`).

---

## 8. Pontos que verifiquei e estão corretos

- O JWT não é acessível por JavaScript (cookie `HttpOnly`), e o frontend não guarda token em `localStorage` (a limitação está em S1/S2).
- Não há `dangerouslySetInnerHTML`/`eval` na aplicação React; a lógica de popup escapa as strings.
- Permissões do usuário são lidas do servidor (`/me/permissoes`) e validadas contra o perfil (`UserContext.refreshUser`); a sessão é invalidada por geração (`generation`) para evitar reaproveitar permissões de outra sessão.
- `ReembolsoModal`/`FinanceiroObra` tratam sobrepagamento, bloqueiam duplo envio com `useRef` e nunca repetem o POST automaticamente.
- `valorDecimal` e `dataValida` rejeitam notação exponencial, mais de 2 casas e datas impossíveis.
- `useBreakpoint` usa `matchMedia` com `useSyncExternalStore`, sem *listener* de `resize` por pixel.

---

## 9. Resumo Executivo

### Total de problemas por severidade

| Severidade | Quantidade |
|---|---|
| 🔴 CRÍTICO | **2** |
| 🟠 MÉDIO | **33** |
| 🟡 BAIXO | **21** |
| **Total** | **56** |

Por categoria: Qualidade 12 · Segurança 11 · Regra de negócio 10 · Integração 6 · Experiência 11 · Problemas potenciais 6.

**Críticos:** S1 (cookie/CSRF/CORS/HTTP) e R1 (exportação de relatório essencial inexistente).

### Avaliação geral

O frontend é funcional e tem bons cuidados pontuais em reembolsos e valores monetários. Porém **não está pronto para produção**: a sessão por cookie sem `Secure`/`SameSite` com CSRF desligado e CORS aberto a `*.elb.amazonaws.com`, o requisito essencial de exportação para a contabilidade não entregue, o tratamento frágil de erros/estado desatualizado e a divergência entre o que a documentação afirma e o que o código faz exigem correção antes de ir ao ar.

### Perguntas em aberto (para confirmar com o time)

1. O deploy final terá TLS (ALB/CloudFront com certificado)? Isso define a correção de S1.
2. Qual é o formato esperado de exportação para a contabilidade (PDF, XLSX, CSV)? E quem deve poder gerá-lo (só ADMIN ou também RA)?
3. A restrição de nomes (só letras) e de títulos de obra (sem pontuação) no backend é intencional (R2)?
4. A mudança de permissões vale na hora ou só no próximo login (D3)?
5. `banco.json`/`endpoint.js` podem ser removidos do repositório (S6)? Alguma dessas senhas é real?
