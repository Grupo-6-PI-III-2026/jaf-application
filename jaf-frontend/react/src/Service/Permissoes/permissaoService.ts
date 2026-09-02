import api from "../Auth/Login/Api/Api";
import type { FuncionarioPermissoes } from "../../Types/permissoes";

// Mapeamento das permissões por cargo (espelha o enum Cargo do backend)
const PERMISSOES_POR_CARGO: Record<string, string[]> = {
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
};

export const permissaoService = {
  listarFuncionarios: async (): Promise<FuncionarioPermissoes[]> => {
    const response = await api.get("/funcionarios");
    return response.data;
  },

  atualizarCargo: async (funcionarioId: number, novoCargo: string): Promise<void> => {
    await api.patch(`/funcionarios/${funcionarioId}/cargo`, { cargo: novoCargo });
  },

  getPermissoesPorCargo: (cargo: string): string[] => {
    return PERMISSOES_POR_CARGO[cargo] || [];
  },
};
