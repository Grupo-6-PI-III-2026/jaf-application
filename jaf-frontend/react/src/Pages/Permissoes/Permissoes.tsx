import { useState, useEffect, useCallback } from "react";
import {
  Shield,
  Search,
  Mail,
  Building2,
  IdCard,
  Info,
  Save,
  ChevronDown,
  Pickaxe,
  Users,
  Wallet,
  Link2,
  FileBarChart,
  ClipboardCheck,
  UserCog,
} from "lucide-react";
import styles from "./Permissoes.module.css";
import { toast } from "sonner";
import { permissaoService } from "../../Service/Permissoes/permissaoService";
import type { FuncionarioPermissoes, ModuloPermissao } from "../../Types/permissoes";

const MODULOS: ModuloPermissao[] = [
  {
    id: "obras",
    nome: "Obras",
    icon: "Pickaxe",
    permissoes: [
      {
        chave: "VISUALIZAR_OBRA",
        label: "Visualizar Lista de Obras",
        descricao: "Permite ver o painel geral e o status resumido de todas as obras ativas.",
        nivel: null,
      },
      {
        chave: "CRIAR_OBRA",
        label: "Criar Nova Obra",
        descricao: "Permite cadastrar novos empreendimentos no sistema.",
        nivel: null,
      },
      {
        chave: "EDITAR_OBRA",
        label: "Editar Detalhes da Obra",
        descricao: "Modificar cronograma, responsáveis e escopo do projeto.",
        nivel: null,
      },
      {
        chave: "DELETAR_OBRA",
        label: "Excluir Obra",
        descricao: "Remover permanentemente registros de obras do banco de dados.",
        nivel: "critico",
      },
    ],
  },
  {
    id: "funcionarios",
    nome: "Funcionários",
    icon: "Users",
    permissoes: [
      {
        chave: "VISUALIZAR_FUNCIONARIOS",
        label: "Visualizar Funcionários",
        descricao: "Ver a lista de todos os funcionários cadastrados no sistema.",
        nivel: null,
      },
      {
        chave: "CRIAR_FUNCIONARIO",
        label: "Cadastrar Funcionário",
        descricao: "Adicionar novos funcionários ao sistema com definição de cargo.",
        nivel: "restrito",
      },
      {
        chave: "EDITAR_FUNCIONARIO",
        label: "Editar Funcionário",
        descricao: "Alterar dados cadastrais, cargo e informações dos funcionários.",
        nivel: "restrito",
      },
      {
        chave: "DELETAR_FUNCIONARIO",
        label: "Excluir Funcionário",
        descricao: "Remover permanentemente um funcionário do sistema.",
        nivel: "critico",
      },
    ],
  },
  {
    id: "gastos",
    nome: "Financeiro / Gastos",
    icon: "Wallet",
    permissoes: [
      {
        chave: "VISUALIZAR_GASTOS",
        label: "Visualizar Gastos",
        descricao: "Acessar orçamentos, custos alocados e notas fiscais das obras.",
        nivel: null,
      },
      {
        chave: "CRIAR_GASTO",
        label: "Registrar Gasto",
        descricao: "Lançar novas despesas e notas fiscais vinculadas às obras.",
        nivel: null,
      },
      {
        chave: "EDITAR_GASTO",
        label: "Editar Gasto",
        descricao: "Alterar informações de gastos já registrados no sistema.",
        nivel: "restrito",
      },
      {
        chave: "DELETAR_GASTO",
        label: "Excluir Gasto",
        descricao: "Remover permanentemente registros de gastos e notas fiscais.",
        nivel: "critico",
      },
    ],
  },
  {
    id: "alocacoes",
    nome: "Alocações",
    icon: "Link2",
    permissoes: [
      {
        chave: "VISUALIZAR_ALOCACOES",
        label: "Visualizar Alocações",
        descricao: "Ver quais funcionários estão vinculados a cada obra.",
        nivel: null,
      },
      {
        chave: "CRIAR_ALOCACAO",
        label: "Alocar Funcionário",
        descricao: "Vincular um funcionário a uma obra com cargo específico.",
        nivel: null,
      },
      {
        chave: "EDITAR_ALOCACAO",
        label: "Editar Alocação",
        descricao: "Alterar o cargo ou detalhes da alocação de um funcionário.",
        nivel: "restrito",
      },
      {
        chave: "DELETAR_ALOCACAO",
        label: "Remover Alocação",
        descricao: "Desvincular um funcionário de uma obra.",
        nivel: "restrito",
      },
    ],
  },
  {
    id: "relatorios",
    nome: "Relatórios",
    icon: "FileBarChart",
    permissoes: [
      {
        chave: "VISUALIZAR_RELATORIO",
        label: "Visualizar Relatórios",
        descricao: "Acessar relatórios financeiros gerados pelo sistema.",
        nivel: null,
      },
      {
        chave: "GERAR_RELATORIO",
        label: "Gerar Relatório",
        descricao: "Criar e exportar relatórios financeiros das obras.",
        nivel: "restrito",
      },
    ],
  },
  {
    id: "presencas",
    nome: "Controle de Presença",
    icon: "ClipboardCheck",
    permissoes: [
      {
        chave: "VISUALIZAR_PRESENCAS",
        label: "Visualizar Presenças",
        descricao: "Consultar o registro de presença dos funcionários nas obras.",
        nivel: null,
      },
      {
        chave: "REGISTRAR_PRESENCA",
        label: "Registrar Presença",
        descricao: "Marcar presença ou ausência de funcionários em uma obra.",
        nivel: null,
      },
      {
        chave: "EDITAR_PRESENCA",
        label: "Editar Presença",
        descricao: "Alterar registros de presença já lançados.",
        nivel: "restrito",
      },
      {
        chave: "DELETAR_PRESENCA",
        label: "Excluir Presença",
        descricao: "Remover registros de presença do sistema.",
        nivel: "critico",
      },
    ],
  },
];

const ICON_MAP: Record<string, React.ComponentType<{ className?: string }>> = {
  Pickaxe,
  Users,
  Wallet,
  Link2,
  FileBarChart,
  ClipboardCheck,
};

const CARGO_LABELS: Record<string, string> = {
  ADMIN: "Administrador",
  GESTOR_OBRA: "Gestor de Obras",
  OPERADOR_LANCAMENTO: "Operador de Lançamento",
};

export default function Permissoes() {
  const [funcionarios, setFuncionarios] = useState<FuncionarioPermissoes[]>([]);
  const [selectedUser, setSelectedUser] = useState<FuncionarioPermissoes | null>(null);
  const [busca, setBusca] = useState("");
  const [permissoesAtivas, setPermissoesAtivas] = useState<Set<string>>(new Set());
  const [cargoSelecionado, setCargoSelecionado] = useState<string>("");
  const [modulosAbertos, setModulosAbertos] = useState<Set<string>>(new Set(["obras"]));
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [permissoesOriginais, setPermissoesOriginais] = useState<Set<string>>(new Set());

  useEffect(() => {
    carregarFuncionarios();
  }, []);

  const carregarFuncionarios = async () => {
    try {
      setIsLoading(true);
      const data = await permissaoService.listarFuncionarios();
      setFuncionarios(data);
    } catch {
      toast.error("Erro ao carregar funcionários");
    } finally {
      setIsLoading(false);
    }
  };

  const selecionarUsuario = useCallback((user: FuncionarioPermissoes) => {
    setSelectedUser(user);
    setCargoSelecionado(user.cargo);
    const permissoes = permissaoService.getPermissoesPorCargo(user.cargo);
    setPermissoesAtivas(new Set(permissoes));
    setPermissoesOriginais(new Set(permissoes));
  }, []);

  const handleCargoChange = (cargo: string) => {
    setCargoSelecionado(cargo);
    const permissoes = permissaoService.getPermissoesPorCargo(cargo);
    setPermissoesAtivas(new Set(permissoes));
  };

  const togglePermissao = (chave: string) => {
    setPermissoesAtivas((prev) => {
      const next = new Set(prev);
      if (next.has(chave)) {
        next.delete(chave);
      } else {
        next.add(chave);
      }
      return next;
    });
  };

  const toggleModulo = (moduloId: string) => {
    setModulosAbertos((prev) => {
      const next = new Set(prev);
      if (next.has(moduloId)) {
        next.delete(moduloId);
      } else {
        next.add(moduloId);
      }
      return next;
    });
  };

  const toggleTodosModulo = (modulo: ModuloPermissao, ativar: boolean) => {
    setPermissoesAtivas((prev) => {
      const next = new Set(prev);
      modulo.permissoes.forEach((p) => {
        if (ativar) {
          next.add(p.chave);
        } else {
          next.delete(p.chave);
        }
      });
      return next;
    });
  };

  const hasChanges = (): boolean => {
    if (!selectedUser) return false;
    if (cargoSelecionado !== selectedUser.cargo) return true;
    if (permissoesAtivas.size !== permissoesOriginais.size) return true;
    for (const p of permissoesAtivas) {
      if (!permissoesOriginais.has(p)) return true;
    }
    return false;
  };

  const handleSalvar = async () => {
    if (!selectedUser) return;

    try {
      setIsSaving(true);
      await permissaoService.atualizarCargo(selectedUser.id, cargoSelecionado);
      toast.success(`Permissões de ${selectedUser.nome} atualizadas com sucesso!`);

      setFuncionarios((prev) =>
        prev.map((f) =>
          f.id === selectedUser.id ? { ...f, cargo: cargoSelecionado } : f
        )
      );
      setSelectedUser({ ...selectedUser, cargo: cargoSelecionado });
      setPermissoesOriginais(new Set(permissoesAtivas));
    } catch {
      toast.error("Erro ao salvar permissões");
    } finally {
      setIsSaving(false);
    }
  };

  const handleCancelar = () => {
    if (selectedUser) {
      selecionarUsuario(selectedUser);
    }
  };

  const funcionariosFiltrados = funcionarios.filter(
    (f) =>
      f.nome.toLowerCase().includes(busca.toLowerCase()) ||
      f.email.toLowerCase().includes(busca.toLowerCase())
  );

  const getModuloIcon = (iconName: string) => {
    const Icon = ICON_MAP[iconName];
    return Icon ? <Icon /> : null;
  };

  const getPermissoesAtivasModulo = (modulo: ModuloPermissao): number => {
    return modulo.permissoes.filter((p) => permissoesAtivas.has(p.chave)).length;
  };

  return (
    <div className={styles.container}>
      {/* Lista de Usuários */}
      <div className={styles.listaUsuarios}>
        <h2>Gerenciar Permissões</h2>
        <p>Selecione um usuário para configurar</p>

        <div className={styles.buscaUsuarios}>
          <Search />
          <input
            type="text"
            placeholder="Buscar funcionário..."
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
          />
        </div>

        {isLoading ? (
          <div className={styles.loading}>Carregando...</div>
        ) : (
          funcionariosFiltrados.map((user) => (
            <div
              key={user.id}
              className={`${styles.usuarioCard} ${
                selectedUser?.id === user.id ? styles.usuarioCardAtivo : ""
              }`}
              onClick={() => selecionarUsuario(user)}
            >
              <div className={styles.avatarUsuario}>
                {user.nome.charAt(0).toUpperCase()}
              </div>
              <div className={styles.infoUsuario}>
                <h4>{user.nome}</h4>
                <span>{CARGO_LABELS[user.cargo] || user.cargo}</span>
              </div>
              <span
                className={`${styles.badgeCargo} ${
                  user.cargo === "ADMIN"
                    ? styles.badgeAdmin
                    : user.cargo === "GESTOR_OBRA"
                    ? styles.badgeGestor
                    : styles.badgeOperador
                }`}
              >
                {user.cargo === "ADMIN"
                  ? "ADM"
                  : user.cargo === "GESTOR_OBRA"
                  ? "GES"
                  : "OPR"}
              </span>
            </div>
          ))
        )}
      </div>

      {/* Perfil do Usuário Selecionado */}
      {selectedUser ? (
        <>
          <div className={styles.perfilUsuario}>
            <div className={styles.avatarGrande}>
              {selectedUser.nome.charAt(0).toUpperCase()}
              <div className={styles.statusOnline}></div>
            </div>
            <h3 className={styles.perfilNome}>{selectedUser.nome}</h3>
            <p className={styles.perfilCargo}>
              {CARGO_LABELS[cargoSelecionado] || cargoSelecionado}
            </p>

            <div className={styles.divider}></div>

            <div className={styles.perfilDetalhes}>
              <div className={styles.detalheItem}>
                <Mail />
                <span>{selectedUser.email}</span>
              </div>
              <div className={styles.detalheItem}>
                <Building2 />
                <span>JAF Construtora</span>
              </div>
              <div className={styles.detalheItem}>
                <IdCard />
                <span>ID: JAF-{String(selectedUser.id).padStart(4, "0")}</span>
              </div>
            </div>

            <div className={styles.alertaImpacto}>
              <Info />
              <div>
                <h4>Impacto das Permissões</h4>
                <p>
                  Alterações salvas entrarão em vigor no próximo login do
                  usuário. Recomendamos notificar o colaborador sobre novos
                  acessos concedidos.
                </p>
              </div>
            </div>
          </div>

          {/* Painel de Permissões */}
          <div className={styles.painelPermissoes}>
            <div className={styles.headerPermissoes}>
              <div>
                <h2>
                  <Shield />
                  Configurações de Acesso
                </h2>
                <p>
                  Defina níveis de leitura, escrita e exclusão por módulo.
                </p>
              </div>
            </div>

            {/* Seletor de Cargo */}
            <div className={styles.seletorCargo}>
              {Object.entries(CARGO_LABELS).map(([key, label]) => (
                <button
                  key={key}
                  className={cargoSelecionado === key ? styles.seletorCargoAtivo : ""}
                  onClick={() => handleCargoChange(key)}
                >
                  <UserCog size={14} style={{ marginRight: 6, verticalAlign: "middle" }} />
                  {label}
                </button>
              ))}
            </div>

            {/* Módulos de Permissão */}
            {MODULOS.map((modulo) => {
              const isAberto = modulosAbertos.has(modulo.id);
              const ativasModulo = getPermissoesAtivasModulo(modulo);
              const todasAtivas = ativasModulo === modulo.permissoes.length;

              return (
                <div key={modulo.id} className={styles.moduloContainer}>
                  <div
                    className={styles.moduloHeader}
                    onClick={() => toggleModulo(modulo.id)}
                  >
                    <div className={styles.moduloHeaderEsquerda}>
                      {getModuloIcon(modulo.icon)}
                      <h3>Módulo: {modulo.nome}</h3>
                    </div>
                    <div className={styles.moduloHeaderDireita}>
                      <span className={styles.contadorPermissoes}>
                        {ativasModulo}/{modulo.permissoes.length}
                      </span>
                      <label
                        className={styles.toggleAll}
                        onClick={(e) => e.stopPropagation()}
                      >
                        <span>Todos</span>
                        <span className={styles.toggle}>
                          <input
                            type="checkbox"
                            checked={todasAtivas}
                            onChange={(e) =>
                              toggleTodosModulo(modulo, e.target.checked)
                            }
                          />
                          <span className={styles.toggleSlider}></span>
                        </span>
                      </label>
                      <ChevronDown
                        className={`${styles.chevron} ${
                          isAberto ? styles.chevronAberto : ""
                        }`}
                      />
                    </div>
                  </div>

                  {isAberto && (
                    <div className={styles.moduloBody}>
                      {modulo.permissoes.map((permissao) => (
                        <div
                          key={permissao.chave}
                          className={styles.permissaoItem}
                        >
                          <div className={styles.permissaoInfo}>
                            <h4>
                              {permissao.label}
                              {permissao.nivel === "restrito" && (
                                <span className={styles.badgeRestrito}>
                                  RESTRITO
                                </span>
                              )}
                              {permissao.nivel === "critico" && (
                                <span className={styles.badgeCritico}>
                                  CRÍTICO
                                </span>
                              )}
                            </h4>
                            <p>{permissao.descricao}</p>
                          </div>
                          <label className={styles.toggle}>
                            <input
                              type="checkbox"
                              checked={permissoesAtivas.has(permissao.chave)}
                              onChange={() => togglePermissao(permissao.chave)}
                            />
                            <span className={styles.toggleSlider}></span>
                          </label>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              );
            })}

            {/* Ações */}
            <div className={styles.acoes}>
              <button
                className={styles.btnCancelar}
                onClick={handleCancelar}
                disabled={!hasChanges()}
              >
                Cancelar
              </button>
              <button
                className={styles.btnSalvar}
                onClick={handleSalvar}
                disabled={!hasChanges() || isSaving}
              >
                <Save size={16} />
                {isSaving ? "Salvando..." : "Salvar Alterações"}
              </button>
            </div>
          </div>
        </>
      ) : (
        <div className={styles.estadoVazio}>
          <UserCog />
          <h3>Selecione um usuário</h3>
          <p>
            Escolha um funcionário na lista ao lado para visualizar e
            configurar suas permissões de acesso.
          </p>
        </div>
      )}
    </div>
  );
}
