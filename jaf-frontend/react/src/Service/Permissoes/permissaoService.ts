import api from "../Auth/Login/Api/Api";
import type { FuncionarioPermissoes } from "../../Types/permissoes";
import type { Cargo } from "../../Types/user";

export const PERMISSOES_POR_CARGO: Record<Cargo, string[]> = {
  ADMIN: [
    "CRIAR_OBRA", "EDITAR_OBRA", "DELETAR_OBRA", "VISUALIZAR_OBRA",
    "CRIAR_FUNCIONARIO", "EDITAR_FUNCIONARIO", "DELETAR_FUNCIONARIO", "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "DELETAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "DELETAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "GERAR_RELATORIO", "VISUALIZAR_RELATORIO",
    "REGISTRAR_PRESENCA", "EDITAR_PRESENCA", "DELETAR_PRESENCA", "VISUALIZAR_PRESENCAS",
  ],
  RESPONSAVEL_ADMINISTRATIVO: [
    "CRIAR_OBRA", "EDITAR_OBRA", "VISUALIZAR_OBRA",
    "CRIAR_FUNCIONARIO", "EDITAR_FUNCIONARIO", "VISUALIZAR_FUNCIONARIOS",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "CRIAR_ALOCACAO", "EDITAR_ALOCACAO", "DELETAR_ALOCACAO", "VISUALIZAR_ALOCACOES",
    "GERAR_RELATORIO", "VISUALIZAR_RELATORIO",
    "REGISTRAR_PRESENCA", "EDITAR_PRESENCA", "DELETAR_PRESENCA", "VISUALIZAR_PRESENCAS",
  ],
  ENGENHEIRO: [
    "VISUALIZAR_OBRA",
    "CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS",
    "VISUALIZAR_ALOCACOES",
    "VISUALIZAR_RELATORIO",
    "VISUALIZAR_PRESENCAS", "REGISTRAR_PRESENCA", "EDITAR_PRESENCA",
  ],
};

export interface CargoPermissoes {
  cargo: Cargo;
  permissoes: string[];
}

export interface FuncionarioPermissoesAcesso {
  funcionarioId: number;
  cargo: Cargo | null;
  permissoes: string[];
}

export const permissaoService = {
  listarFuncionarios: async (): Promise<FuncionarioPermissoes[]> => {
    const response = await api.get("/funcionarios");
    return response.data;
  },

  atualizarCargo: async (funcionarioId: number, novoCargo: Cargo): Promise<void> => {
    await api.patch(`/funcionarios/${funcionarioId}/cargo`, { cargo: novoCargo });
  },

  getMapaPadraoPermissoes: (): Record<Cargo, string[]> => ({ ...PERMISSOES_POR_CARGO }),

  listarPermissoesPorCargo: async (): Promise<Record<Cargo, string[]>> => {
    const response = await api.get<CargoPermissoes[]>("/permissoes/cargos");
    return response.data.reduce((acc, item) => {
      acc[item.cargo] = item.permissoes;
      return acc;
    }, { ...PERMISSOES_POR_CARGO });
  },

  atualizarPermissoesCargo: async (cargo: Cargo, permissoes: string[]): Promise<CargoPermissoes> => {
    const response = await api.put<CargoPermissoes>(`/permissoes/cargos/${cargo}`, { permissoes });
    return response.data;
  },

  buscarPermissoesFuncionario: async (funcionarioId: number): Promise<FuncionarioPermissoesAcesso> => {
    const response = await api.get<FuncionarioPermissoesAcesso>(`/funcionarios/${funcionarioId}/permissoes`);
    return response.data;
  },

  atualizarPermissoesFuncionario: async (funcionarioId: number, permissoes: string[]): Promise<FuncionarioPermissoesAcesso> => {
    const response = await api.put<FuncionarioPermissoesAcesso>(`/funcionarios/${funcionarioId}/permissoes`, { permissoes });
    return response.data;
  },

  getPermissoesPorCargo: (cargo: Cargo | string): string[] => {
    return PERMISSOES_POR_CARGO[cargo as Cargo] || [];
  },

  temPermissao: (cargo: Cargo | null | undefined, permissao: string): boolean => {
    if (!cargo) return false;
    return PERMISSOES_POR_CARGO[cargo]?.includes(permissao) ?? false;
  },
};
