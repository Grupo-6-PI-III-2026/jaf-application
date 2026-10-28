# Contrato: financeiro de obras, reembolsos e presença

Implementação exclusivamente no backend. Beneficiário é o cliente solicitante;
não foi criada entidade de beneficiário, pedido ou comentário. Notinhas continuam
sendo os gastos/notas fiscais existentes.

## Autorização

- Todas as rotas exigem autenticação.
- ADMIN tem acesso global. Demais cargos precisam estar alocados à obra para
  acessar seus gastos, reembolsos ou presenças. A política de escopo segue o
  vínculo de alocação existente (não exige alocação ainda vigente hoje para
  consultar o histórico).
- Consultas de gastos/reembolsos exigem `VISUALIZAR_GASTOS`.
- Registrar pagamento exige cargo global ADMIN ou RESPONSAVEL_ADMINISTRATIVO;
  o responsável também precisa de alocação na obra. Uma permissão de edição de
  gastos isolada não concede autorização para registrar pagamentos.
- Recebido, total gasto e saldo da obra são restritos a ADMIN. O dashboard
  financeiro inteiro retorna 403 para os demais cargos; a permissão
  `VISUALIZAR_GASTOS` continua sendo exigida pelo endpoint do dashboard.
- Nas respostas de Obra, não-admin recebe `id`, `titulo`, `status`, `dtInicio` e
  `dtTerminoPrevisto`, sem `orcamento` nem `totalRecebido`. A mesma proteção vale
  para listas, páginas, criação/edição e obras aninhadas em gastos/alocações.
  ADMIN recebe também `orcamento` e `totalRecebido`.
- Gastos individuais e os totais de reembolso por pessoa continuam disponíveis
  aos usuários com permissão e escopo: não são o total financeiro da obra.

## Proteção contra escalada de privilégio

As decisões de ADMIN consultam o `cargoGlobal` atual do funcionário no banco,
identificado pelo e-mail da autenticação. Authorities como `ROLE_ADMIN` ou
`EDITAR_FUNCIONARIO`, inclusive de uma autenticação anterior ao rebaixamento,
não substituem essa verificação. Os serviços também aplicam os bloqueios,
independentemente das anotações dos controllers.

- POST `/funcionarios`: mantém `CRIAR_FUNCIONARIO`; criar com `cargo=ADMIN`
  também exige ADMIN persistido. O responsável continua cadastrando não-admin.
- PUT `/funcionarios/{id}`: mantém `EDITAR_FUNCIONARIO`. Editar qualquer dado de
  uma conta ADMIN ou alterar o cargo global exige ADMIN persistido, inclusive
  quando o cargo vem dentro do DTO. Atualizar dados de não-admin mantendo seu
  cargo continua disponível ao responsável com essa permissão.
- PATCH `/funcionarios/{id}/cargo`: apenas ADMIN persistido, para qualquer cargo.
- DELETE `/funcionarios/{id}`: mantém `DELETAR_FUNCIONARIO`; excluir conta ADMIN
  exige também ADMIN persistido.
- GET/PUT `/permissoes/cargos` (PUT com `/{cargo}`) e
  GET/PUT `/funcionarios/{id}/permissoes`: apenas ADMIN persistido, tanto para
  consultas gerenciais como para mudanças. `EDITAR_FUNCIONARIO` não basta.
  A política existente de cargos configuráveis e do resolver foi preservada.
- As rotas de perfil `/funcionarios/me` continuam operando somente sobre o
  próprio principal, sem permitir mudança de cargo ou permissões pelo DTO de
  perfil. Ninguém, inclusive ADMIN, altera o próprio cargo (PATCH/PUT) ou as
  próprias permissões (403); a autoexclusão usa somente DELETE `/funcionarios/me`.
  PUT `/permissoes/cargos/ADMIN` retorna 400: as permissões do ADMIN são fixas.
- Tentativas sem autorização retornam **403**, antes de modificar a entidade
  ou suas permissões. Nenhuma resposta nova contém senha ou hash.

### GET `/funcionarios/me/permissoes`

Disponível a qualquer funcionário autenticado existente, sem exigir
`EDITAR_FUNCIONARIO` nem outra authority gerencial. Não recebe ID/e-mail de
terceiros; o usuário é obtido exclusivamente da autenticação e do banco.
Resposta **200**:

```json
{
  "funcionarioId": 42,
  "cargo": "ENGENHEIRO",
  "permissoes": ["VISUALIZAR_OBRA", "REGISTRAR_PRESENCA"]
}
```

`funcionarioId` é inteiro; `cargo` é o cargo global persistido; `permissoes` é
um array de nomes do enum `Permissao`, sem duplicatas. O array acima é somente
exemplo, não a lista padrão completa do cargo. É usado exatamente o resolver
`FuncionarioPermissaoService.permissoesDoFuncionario`, também usado por
`AutenticacaoService` no fluxo JWT: overrides individuais não vazios prevalecem;
sem eles, usam-se as regras persistidas do cargo ou seu padrão quando não há
regras. Lista individual vazia mantém o comportamento legado de retornar ao
padrão do cargo, não significa revogação de todas as permissões.
O frontend deve consumir este endpoint para a sessão corrente, não a consulta
gerencial por ID. Autorizações financeiras ADMIN-only continuam dependendo do
cargo persistido, não apenas deste array. Anônimo ou principal removido retorna
403, conforme o envelope/política de autenticação existente.

## Financeiro da obra

| Método/rota | Corpo | Resposta 200 |
|---|---|---|
| GET `/obras/{id}/financeiro` | Nenhum | `{totalRecebido,totalGasto,saldo}` |
| PUT `/obras/{id}/financeiro` | `{totalRecebido}` | `{totalRecebido,totalGasto,saldo}` |

- Os três valores de resposta são números decimais. `totalRecebido` é obrigatório
  no PUT, não negativo, com até 17 dígitos inteiros e 2 casas decimais.
- `totalRecebido` inicia em zero para obras existentes e novas; não é copiado do
  orçamento. PUT substitui o acumulado recebido; não soma ao valor anterior.
- `totalGasto` é a soma de todos os gastos da obra, sem somar novamente as parcelas
  de reembolso. `saldo = totalRecebido - totalGasto`; saldo negativo é permitido.
- Não existe histórico de recebimentos neste contrato, apenas o total solicitado.
- Orçamento continua com seu tipo/contrato legado, separado do recebido.

## Reembolsos por gasto

### GET `/gastos/{id}/reembolsos`

Resposta 200 com exatamente:

- `gastoId`: inteiro;
- `valorTotal`, `totalPago`, `saldoPendente`: números decimais;
- `status`: `PENDENTE`, `PARCIAL` ou `PAGO`;
- `pagamentos`: array de `{id,valor,dataPagamento,observacao}`, ordenado por
  `dataPagamento` e depois `id`, ambos crescentes. `observacao` pode ser null.

`saldoPendente = valorTotal - totalPago`. Status é PENDENTE sem pagamentos,
PARCIAL quando há pagamento inferior ao valor do gasto, PAGO quando quitado.
Apenas gastos com `metodoPagamento` exatamente `REEMBOLSO` aceitam essa operação.

### POST `/gastos/{id}/reembolsos`

Corpo: `{valor,dataPagamento,observacao}`. Retorna **201**, com o mesmo objeto
completo do GET, já incluindo o pagamento registrado.

- `valor`: obrigatório, estritamente positivo, até 17 dígitos inteiros e 2 casas
  decimais; deve ser menor ou igual ao saldo pendente.
- `dataPagamento`: obrigatória, data ISO `yyyy-MM-dd`, entre a data do gasto e
  a data atual, inclusive.
- `observacao`: opcional, até 1000 caracteres.
- Lock pessimista da linha do gasto e transação protegem a soma paga contra
  requisições concorrentes, inclusive edição/exclusão do gasto.
- Não há endpoint para apagar ou editar parcelas. Sem chave de idempotência:
  um POST repetido é outro pagamento se ainda houver saldo; o cliente deve
  consultar o histórico antes de repetir uma chamada com resultado desconhecido.

### GET `/gastos/reembolsos/resumo?obraId={id}`

`obraId` é obrigatório. Retorna 200 com array de
`{funcionarioId,funcionarioNome,valorTotal,totalPago,saldoPendente}`, um item
por pessoa com gasto de reembolso na obra, ordenado por `funcionarioId`.
Considera apenas método REEMBOLSO e pagamentos reais. Sem gastos, retorna `[]`.

## Filtros de gastos

GET `/gastos` e GET `/gastos/paginado` aceitam, combinados com AND:

- `obraId` (inteiro, opcional);
- `funcionarioId` (inteiro, opcional, dono do gasto);
- `categoria` (texto exato, opcional);
- `metodoPagamento` (texto exato, opcional);
- `statusReembolso` (`PENDENTE`/`PARCIAL`/`PAGO`, opcional).

O filtro de status restringe automaticamente a REEMBOLSO e usa a soma das
parcelas, não a flag legada. Filtros nunca ampliam as obras permitidas.
Obra fora de escopo na listagem retorna lista/página vazia.

`/gastos` mantém array de gastos. `/gastos/paginado` mantém Page de gastos e aceita
`page` (padrão 0, mínimo 0), `size` (padrão 20, entre 1 e 200), `sortBy` (padrão
`dtGasto`; permitido: `id`, `dtGasto`, `valor`, `descricao`, `categoria`,
`metodoPagamento`, `etapa`) e `sortDir` (`asc`/`desc`, padrão `desc`).

## Flag legada e alterações de gastos

- `reembolsoConcluido` permanece no banco e nas respostas para compatibilidade.
  Novos reembolsos começam false; outros métodos ficam null. Pagamentos e edição
  do valor recalculam a flag a partir do histórico.
- Não se pode quitar um gasto enviando `reembolsoConcluido=true` sem pagamentos
  suficientes: retorna 400. O registro financeiro deve passar pelo POST de
  reembolsos. A flag false/null do request não desfaz pagamentos.
- Com pagamentos, excluir gasto retorna 409. Alterar funcionário, obra ou método
  retorna 409. Reduzir valor abaixo do total pago retorna 409.
- Alterar `dtGasto` para depois da menor `dataPagamento` registrada retorna
  **409 Conflict**, sem alterar o gasto ou as parcelas. Data anterior ou igual
  ao primeiro pagamento continua permitida. A validação usa o mesmo lock do
  gasto que protege o registro de parcelas concorrentes.
- Descrição, categoria, etapa, data e valores válidos continuam editáveis sem
  apagar parcelas.
- Gasto exige valor positivo com até 2 casas decimais. As permissões de criar,
  editar e excluir gastos são mantidas e as mutações também validam escopo da
  obra de origem e da obra de destino.

## Dashboard

GET `/dashboard/stats` mantém seu DTO e os filtros de obra/etapa/período, mas é
restrito a ADMIN. Período invertido retorna 400.

- `saldoRestante`: recebido **acumulado** menos todos os gastos **acumulados da
  obra inteira**, sem filtros de período ou etapa, igual ao `saldo` de
  `/obras/{id}/financeiro`. Não usa orçamento como recebido; pode ser negativo.
  O recebido é o total acumulado atual, não um recebimento do período nem um
  saldo histórico na data final do filtro.
- `progressoSaldo`: todos os gastos acumulados da obra / recebido acumulado,
  em percentual, sem filtros de período ou etapa; zero quando recebido é zero.
- `gastosEtapa` e `progressoEtapa` mantêm a seleção por etapa e período; o
  progresso da etapa continua comparado ao orçamento.
- `reembolsosPendentes`: soma dos saldos pendentes dos gastos selecionados por
  etapa e data do gasto. Parcelas desses gastos são consideradas até o momento,
  independentemente da data do pagamento.
- Pizza `Concluídos`/`Pendentes`: percentuais monetários arredondados do pago e
  pendente, não contagem de notas. Ambos zero quando não há reembolsos.
- Pagamentos nunca são somados de novo a gastos/gráficos de categoria.
- Gráficos de imprevistos e de categorias mantêm os filtros de etapa e período,
  com datas inicial e final inclusivas; a pizza acompanha os reembolsos filtrados.

## Presença

Mantidas as rotas existentes, com validação de escopo em leitura, criação,
edição, alternância e exclusão. As permissões continuam independentes:
`VISUALIZAR_PRESENCAS`, `REGISTRAR_PRESENCA`, `EDITAR_PRESENCA`, `DELETAR_PRESENCA`.

- GET `/presencas/obra/{obraId}/data/{data}` e GET
  `/presencas/obra/{obraId}/periodo?from=...&to=...` aceitam
  `funcionarioId` opcional. Datas são ISO; data inválida e período invertido
  retornam 400, sem virar erro 500.
- Criação/edição manual exige alocação da pessoa na obra **na data informada**,
  incluindo os limites da alocação; data não futura.
- Ausência não pode ter horários. Saída exige entrada e deve ser posterior.
- A lista diária deduplica pessoas com múltiplas alocações e considera a
  vigência na data consultada. A visão de período mantém registros históricos.
- Alternar para ausência limpa horários e não permite registrar presença futura.
- Edição de presença verifica escopo tanto da origem quanto do destino e
  mantém validação de duplicidade funcionário/obra/data.

## Erros

- 400: corpo/parâmetro/data/status/decimal inválido, sobrepagamento, gasto que
  não é reembolso ou tentativa de quitar pela flag.
- 403: falta de cargo, permissão ou escopo.
- 404: gasto/obra/presença não encontrado na busca correspondente.
- 409: alteração/exclusão que destruiria histórico, presença duplicada ou pessoa
  não alocada na data.

Mantidos os envelopes de erro existentes do backend.

## Banco e testes

[V13](src/main/resources/db/migration/V13__financeiro_e_pagamentos_reembolso.sql)
adiciona recebido com default zero, tabela/index/FK/checks de pagamentos e cria
uma parcela integral para cada gasto REEMBOLSO positivo já marcado como pago.
A flag original não é removida nem alterada pelo backfill. Como não havia data
de pagamento no legado, usa a data do gasto (ou hoje se ausente), com observação
explícita de migração. Não é uma data de quitação histórica recuperada.

[DatabaseSeeder](src/main/java/com/jaf/application/config/DatabaseSeeder.java)
também preenche parcelas dos gastos pagos da carga de demonstração, que roda
após as migrations, apenas quando ainda não existe pagamento para o gasto.
A carga mantém inserts condicionais à ausência dos registros; não normaliza
nem sobrescreve orçamentos, cargos, categorias, etapas ou outros campos
editáveis existentes em cada inicialização. A normalização legada permanece
responsabilidade das migrations existentes, sem reexecutá-la no runtime.

Testes novos:

- [FuncionarioAutorizacaoApiTest](src/test/java/com/jaf/application/FuncionarioAutorizacaoApiTest.java): bloqueios HTTP de escalada, authorities obsoletas, gestão ADMIN-only, fluxo normal do responsável e permissões próprias com defaults/regras/overrides.
- [FuncionarioAutorizacaoTest](src/test/java/com/jaf/application/service/FuncionarioAutorizacaoTest.java): guardas persistentes chamadas diretamente pelos serviços, sem depender de controllers, e ausência de mutações antes de autorizar.
- [ReembolsoServiceTest](src/test/java/com/jaf/application/service/ReembolsoServiceTest.java): parcelas, quitação, sobrepagamento, decimais/datas, autorização e resumo.
- [DatabaseSeederTest](src/test/java/com/jaf/application/config/DatabaseSeederTest.java): carga inicial e reinicializações preservando dados editados e parcelas, sem duplicação.
- [GastoServiceTest](src/test/java/com/jaf/application/service/GastoServiceTest.java): histórico, redução, dono/método/obra, cronologia de pagamentos (409 e limites inclusivos), flag e escopo.
- [AcessoFinanceiroTest](src/test/java/com/jaf/application/service/AcessoFinanceiroTest.java): cargos, alocação, serialização direta/aninhada e recebido separado de orçamento.
- [DashboardServiceTest](src/test/java/com/jaf/application/service/DashboardServiceTest.java): parcelas sem duplicidade, saldo acumulado independente dos filtros, gráficos filtrados e bloqueio não-admin.
- [PresencaServiceTest](src/test/java/com/jaf/application/service/PresencaServiceTest.java): escopo, datas, alocação e horários.
- [ReembolsoPersistenciaTest](src/test/java/com/jaf/application/repository/ReembolsoPersistenciaTest.java): filtros SQL/lista/página/resumo e dois pagamentos concorrentes.
- [FinanceiroMigrationTest](src/test/java/com/jaf/application/repository/FinanceiroMigrationTest.java): backfill, default e FK de histórico.
- [FinanceiroApiTest](src/test/java/com/jaf/application/FinanceiroApiTest.java): HTTP 400/403/201, permissões manuais, escopo e ausência de vazamento.

Os testes de contexto/persistência/HTTP usam H2 isolado e desativam seed e
listeners externos; não exigem segredos, PostgreSQL ou RabbitMQ. O teste de
migration executa V1, V5 e V13 em H2 no modo PostgreSQL; não substitui a
validação do upgrade completo em PostgreSQL antes do deploy.

## Arquivos de produção

- Controladores: [ObraFinanceiroController](src/main/java/com/jaf/application/controller/ObraFinanceiroController.java), [ReembolsoController](src/main/java/com/jaf/application/controller/ReembolsoController.java), [GastoController](src/main/java/com/jaf/application/controller/GastoController.java), [ObraController](src/main/java/com/jaf/application/controller/ObraController.java), [PresencaController](src/main/java/com/jaf/application/controller/PresencaController.java).
- Serviços: [AcessoObraService](src/main/java/com/jaf/application/service/AcessoObraService.java), [ObraFinanceiroService](src/main/java/com/jaf/application/service/ObraFinanceiroService.java), [ReembolsoService](src/main/java/com/jaf/application/service/ReembolsoService.java), [GastoService](src/main/java/com/jaf/application/service/GastoService.java), [DashboardService](src/main/java/com/jaf/application/service/DashboardService.java), [PresencaService](src/main/java/com/jaf/application/service/PresencaService.java).
- Modelos: [Obra](src/main/java/com/jaf/application/model/Obra.java), [PagamentoReembolso](src/main/java/com/jaf/application/model/PagamentoReembolso.java), [StatusReembolso](src/main/java/com/jaf/application/enums/StatusReembolso.java).
- Persistência: [GastoRepository](src/main/java/com/jaf/application/repository/GastoRepository.java), [GastoFiltro](src/main/java/com/jaf/application/repository/GastoFiltro.java), [PagamentoReembolsoRepository](src/main/java/com/jaf/application/repository/PagamentoReembolsoRepository.java).
- DTOs: [ObraFinanceiroDto](src/main/java/com/jaf/application/dto/ObraFinanceiroDto.java), [ObraFinanceiroRequest](src/main/java/com/jaf/application/dto/ObraFinanceiroRequest.java), [PagamentoReembolsoRequest](src/main/java/com/jaf/application/dto/PagamentoReembolsoRequest.java), [ReembolsoResponse](src/main/java/com/jaf/application/dto/ReembolsoResponse.java), [ReembolsoResumo](src/main/java/com/jaf/application/dto/ReembolsoResumo.java), [GastoDto](src/main/java/com/jaf/application/dto/GastoDto.java), [PresencaDto](src/main/java/com/jaf/application/dto/PresencaDto.java).
- Configuração: [ObraSerializer](src/main/java/com/jaf/application/config/ObraSerializer.java), [GlobalExceptionHandler](src/main/java/com/jaf/application/config/GlobalExceptionHandler.java), seed e V13 acima.
- Testes/configuração de testes: arquivos acima, [ApplicationTests](src/test/java/com/jaf/application/ApplicationTests.java) e [pom.xml](pom.xml) (dependência Spring Security apenas em escopo test).