import api from "../Auth/Login/Api/Api";
import type { FuncionarioPermissoes } from "../../Types/permissoes";
import type { Cargo } from "../../Types/user";

export interface CargoPermissoes {
  cargo: Cargo;
  permissoes: string[];
}

export interface FuncionarioPermissoesAcesso {
  funcionarioId: number;
  cargo: Cargo | null;
  permissoes: string[];
  personalizada?: boolean;
}

export interface AtualizarAcessoPayload {
  cargo: Cargo;
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

  // A matriz padrão vem só do servidor: não há cópia local para usar como se fosse a real.
  listarPermissoesPorCargo: async (): Promise<Partial<Record<Cargo, string[]>>> => {
    const response = await api.get<CargoPermissoes[]>("/permissoes/cargos");
    return response.data.reduce<Partial<Record<Cargo, string[]>>>((acc, item) => {
      acc[item.cargo] = item.permissoes;
      return acc;
    }, {});
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

  // Cargo e permissões na mesma transação: se falhar, nada muda.
  atualizarAcesso: async (funcionarioId: number, payload: AtualizarAcessoPayload): Promise<FuncionarioPermissoesAcesso> => {
    const response = await api.put<FuncionarioPermissoesAcesso>(`/funcionarios/${funcionarioId}/acesso`, payload);
    return response.data;
  },

};
