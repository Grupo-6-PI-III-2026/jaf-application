# Melhorias da visita ao beneficiário

Branch: `feat/melhorias-beneficiario`.

## Funcionalidades entregues

| Solicitação | Implementação |
| --- | --- |
| Recebido, gasto e saldo exclusivos do dono | Painel no detalhe da obra, disponível somente a ADMIN. Recebido é independente do orçamento; restrição também na API e na serialização de obras aninhadas. |
| Reembolso de um gasto em vários pagamentos | Botão **Pagamentos / Registrar** em cada gasto REEMBOLSO, com histórico de valor, data e observação. O saldo e a situação são calculados pelos pagamentos. |
| Notinhas por usuário | Filtro **Pessoa** na lista de gastos, incluindo autores históricos que já não aparecem nas alocações atuais. |
| Total de reembolso por pessoa | Quadro com total, pago e pendente por funcionário, sem confundir os filtros da listagem com o resumo completo da obra. |
| Filtrar reembolsos pagos | Filtro **Status do reembolso**: Pendente, Parcial e Pago. |
| Presença por pessoa e data | Tela Presenças com obra, data ou período, busca de pessoa/função e situação. |

### Regras adotadas

- ADMIN representa o dono; responsável administrativo não recebe o painel financeiro global.
- ADMIN e RESPONSAVEL_ADMINISTRATIVO podem registrar pagamentos; responsável precisa estar alocado na obra. Somente ter permissão de editar gastos não permite pagar reembolsos.
- Presença é manual, feita por usuário autenticado com `REGISTRAR_PRESENCA`. Alterar/excluir requer permissões específicas. O funcionário marcado não precisa fazer login, mas precisa de alocação válida na data.
- Os cargos padrão existentes permitem a engenheiros registrar/editar presenças. O dono pode alterar isso em Perfis de Acesso; a interface agora usa as permissões efetivas do servidor, não uma tabela fixa.
- Pagamento não é somado novamente ao gasto. Exemplo: gasto de R$ 3.000,00 com pagamentos de R$ 500,00, R$ 1.000,00 e R$ 1.500,00 continua sendo um gasto de R$ 3.000,00.
- Pagamentos não são editáveis/excluíveis nesta entrega. Gastos com histórico não podem ser apagados ou transferidos; não podem ficar menores que o pago nem ter data posterior ao primeiro pagamento.
- Recebido é um acumulado editável, não um extrato de entradas. Obras existentes começam com recebido zero; preencher com o valor real, sem copiar automaticamente o orçamento.

## Correções adicionais

- Acesso a todas as obras pela Home, com busca e filtro de situação.
- Bloqueio de escalada de privilégio: não-admin não pode promover alguém a ADMIN, editar/remover conta ADMIN ou administrar permissões.
- Permissões carregadas pelo endpoint próprio do usuário; falhas bloqueiam o acesso até validação/retry, sem reutilizar permissões de outra sessão.
- Datas, valores monetários, períodos e horários validados; proteção transacional contra sobrepagamento concorrente.
- A carga de demonstração deixou de normalizar/sobrescrever orçamentos e cargos existentes em cada reinício.
- Cadastro de obra deixou de solicitar localização/documentos que não eram persistidos. OCR permanece no cadastro de gastos/notas fiscais, não no título da obra.
- Se a obra for criada e o vínculo do responsável falhar, a interface informa o resultado parcial e encaminha para alocações, evitando um segundo cadastro da obra.
- Compose aguarda a saúde do backend; testes usam perfil separado. Nginx resolve novamente o backend recriado e não mantém HTML antigo em cache. Serviços de banco/backend usam o fuso de São Paulo.

## Validação executada

- **Backend:** 94 testes aprovados, zero falhas/erros. Inclui HTTP/autorização, migrations em H2, filtros SQL, parcelas, concorrência, histórico e regressão do seed.
- **Frontend:** 124 testes aprovados em 19 arquivos; build TypeScript/Vite e ESLint aprovados.
- **PostgreSQL do Compose:** Flyway de V1 a V13 com sucesso; histórico quitado legado disponível no modal de reembolsos.
- **Infraestrutura:** backend, banco e RabbitMQ saudáveis; frontend, serviço OCR e worker em execução. Configuração Nginx validada.
- **Navegador:** abertas as telas de Home, detalhe de obra, dashboard, presenças, novo usuário, nova obra, perfis, alocações e perfil. Verificação desktop sem erros de console no percurso; em 390px, sem overflow horizontal da página ou alertas de carregamento.
- **Fluxos de reembolso:** combinação Pessoa + Pago retornou somente os dois gastos quitados de Gabriel na obra de demonstração; histórico legado e formulário de pendente abertos; tentativa acima do saldo foi bloqueada sem POST.
- **Perfil engenheiro:** financeiro ausente da tela, orçamento/recebido omitidos da resposta de obra e HTTP 403 ao consultar financeiro/dashboard diretamente. Endpoint de permissões próprias retornou 200.
- Nenhum pagamento real foi lançado na base do usuário para a validação visual. Parcelas novas e concorrência foram exercitadas em banco isolado de testes.

## Execução e revisão

Em **Terminal → Executar Tarefa**, estão disponíveis:

- **JAF: iniciar aplicação** — build e inicialização via Compose, aguardando a saúde do backend.
- **JAF: validar melhorias** — testes backend, build/testes/lint frontend (PowerShell/Windows).
- **JAF: validar frontend** — validação somente do React.
- **JAF: conferir serviços e migração** — verificação do diff, serviços, Nginx, Flyway e relatórios Java.

Aplicação: http://localhost. Se uma aba foi aberta antes da correção de cache, fazer atualização forçada uma vez (Ctrl+F5).

Contrato detalhado: [API financeiro](../jaf-backend/API-FINANCEIRO.md).

## Deploy na AWS (correção dos 502 e do OCR)

- Health check do ALB passou para `/actuator/health/readiness` (banco obrigatório, RabbitMQ não). Antes, sem broker na AWS, o health ficava DOWN, o ASG substituía o backend em ciclo e toda a API respondia 502.
- Nginx virou template (`BACKEND_HOST` + DNS lido do container) e resolve o ALB interno a cada 10s; antes o IP era fixado na inicialização.
- Terraform cria um broker RabbitMQ único na subnet do OCR e sobe o worker OCR no ASG; o backend recebe `RABBITMQ_*`. O CI publica a imagem `jaf-ocr`.
- Antes do `terraform apply`, preencher no `terraform.tfvars`: `rabbitmq_password` (mín. 12 caracteres) e `ocr_space_api_key`. Depois do push na branch de deploy, recriar as instâncias (instance refresh dos ASGs) para aplicar o novo `user_data`.
- Validado localmente com `terraform validate`/`fmt` (sem apply). Os scripts não usam mais `set -x`, que gravava segredos no log do cloud-init.

## Limites e cuidados

- Não representa auditoria exaustiva de segurança nem teste de carga. OCR externo não foi submetido novamente com documento do usuário nesta revisão.
- V13 cria uma parcela para quitações legadas; como não havia data histórica, utiliza a data do gasto e registra essa limitação na observação.
- Não há chave de idempotência no POST de pagamento: após falha de rede, a tela exige consultar o histórico antes de repetir. Não repetir automaticamente.
- O dono ainda pode conceder/remover ADMIN. Não há conceito separado de proprietário único ou bloqueio do último ADMIN.
- A carga de demonstração permanece habilitada conforme configuração existente; desabilitá-la em produção e substituir credenciais públicas de demonstração e segredos de desenvolvimento.
- Alterações mantidas na branch local, sem commit, push ou merge automático.