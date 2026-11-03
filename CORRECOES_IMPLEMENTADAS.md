# Correções Implementadas

> Escopo desta rodada: problemas **P1 a P10**. Os itens do `CORRECOES.md` **não foram aplicados** (adiados por decisão do solicitante).
> Validação feita apenas por leitura de código; **testes, build e lint não foram executados** (sem JDK e sem dependências npm instaladas no ambiente).

## Implementado

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
| P10 | Exclusão verifica alocações, presenças, gastos e relatórios; retorna 409 com a mensagem pedida. Novos endpoints `PATCH /funcionarios/{id}/inativar` e `/reativar`. Campo `ativo` (migration **V14**); usuário inativo não faz login (provider e filtro JWT). UI de inativar/reativar/excluir. | `FuncionarioService.java`, `FuncionarioController.java`, `Funcionario.java`, `V14__add_ativo_funcionario.sql`, `AutenticacaoProvider.java`, `AutenticacaoFilter.java`, `FuncionarioDetalhesDto.java`, DTOs, `Permissoes.tsx`, `funcionarioService.ts`, `authService.tsx` |

## Observações

- **P10 – local da UI:** não existia tela para excluir outros usuários; os botões foram colocados no painel de "Configurações de Acesso" (`Permissoes`), visíveis conforme `EDITAR_FUNCIONARIO` / `DELETAR_FUNCIONARIO`.
- **P10 – exclusão da própria conta** (`DELETE /funcionarios/me`) também passou a retornar 409 quando há registros vinculados (mensagem própria), em vez de erro de FK.
- **P2:** o mapa `fields` continua sendo retornado; o `message` agora contém as mensagens específicas.
- **P1:** o dropdown nativo só aceita bordas arredondadas na lista aberta em navegadores com `appearance: base-select`; nos demais, o campo fechado segue o padrão arredondado.
- Frontend de usuário inativo no login: tela mostra "Usuário inativo..." quando o backend responde 401 com essa mensagem.

## Não implementado / pendente

- **Todo o `CORRECOES.md`** (95 itens): adiado a pedido.
- **Execução de testes/build/lint:** não realizada (sem JDK e sem `node_modules`). Recomendado rodar `mvnw.cmd test` e `npm run lint; npx tsc -b; npx vitest run`.
- Testes de UI para os novos botões de inativar/excluir em `Permissoes` não foram criados.
