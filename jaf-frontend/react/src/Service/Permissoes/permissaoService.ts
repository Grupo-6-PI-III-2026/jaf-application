import api from "../Auth/Login/Api/Api";
import type { FuncionarioPermissoes } from "../../Types/permissoes";
import type { Cargo } from "../../Types/user";

// Mapeamento das permissões por cargo (espelha o enum Cargo do backend)
export const PERMISSOES_POR_CARGO: Record<Cargo, string[]> = {
  ADMIN: [
    "CRIAR_OBRA", "EDITAR_OBRA", "DELETAR_OBRA", "VISUALIZAR_OBRA",
    "CRIAR_FUNCIONARIO", "EDITAR_FUNCIONARIO", "DELETAR_FUNCIONARIO", "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "DELETAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "DELETAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "GERAR_RELATORIO", "VISUALIZAR_RELATORIO",
    "REGISTRAR_PRESENCA", "EDITAR_PRESENCA", "DELETAR_PRESENCA", "VISUALIZAR_PRESENCAS",
  ],
  GESTOR_OBRA: [
    "CRIAR_OBRA", "EDITAR_OBRA", "VISUALIZAR_OBRA",
    "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "GERAR_RELATORIO", "VISUALIZAR_RELATORIO",
    "REGISTRAR_PRESENCA", "EDITAR_PRESENCA", "DELETAR_PRESENCA", "VISUALIZAR_PRESENCAS",
  ],
  OPERADOR_LANCAMENTO: [
    "VISUALIZAR_OBRA",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_RELATORIO",
    "VISUALIZAR_PRESENCAS",
  ],
  MESTRE_DE_OBRAS: [
    "VISUALIZAR_OBRA",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_PRESENCAS", "REGISTRAR_PRESENCA", "EDITAR_PRESENCA",
  ],
  ENGENHEIRO: [
    "VISUALIZAR_OBRA", "CRIAR_OBRA", "EDITAR_OBRA",
    "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_RELATORIO", "GERAR_RELATORIO",
    "VISUALIZAR_PRESENCAS", "REGISTRAR_PRESENCA", "EDITAR_PRESENCA",
  ],
  ARQUITETO: [
    "VISUALIZAR_OBRA", "CRIAR_OBRA", "EDITAR_OBRA",
    "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_RELATORIO", "GERAR_RELATORIO",
    "VISUALIZAR_PRESENCAS", "REGISTRAR_PRESENCA", "EDITAR_PRESENCA",
  ],
  PEDREIRO: [
    "VISUALIZAR_OBRA",
    "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_PRESENCAS", "REGISTRAR_PRESENCA",
  ],
};

export const permissaoService = {
  listarFuncionarios: async (): Promise<FuncionarioPermissoes[]> => {
    const response = await api.get("/funcionarios");
    return response.data;
  },

  atualizarCargo: async (funcionarioId: number, novoCargo: Cargo): Promise<void> => {
    await api.patch(`/funcionarios/${funcionarioId}/cargo`, { cargo: novoCargo });
  },

  getPermissoesPorCargo: (cargo: Cargo | string): string[] => {
    return PERMISSOES_POR_CARGO[cargo as Cargo] || [];
  },

  temPermissao: (cargo: Cargo | null | undefined, permissao: string): boolean => {
    if (!cargo) return false;
    return PERMISSOES_POR_CARGO[cargo]?.includes(permissao) ?? false;
  },
};
