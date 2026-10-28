import api from "../Auth/Login/Api/Api";

export interface Funcionario {
  id: number;
  nome: string;
  email: string;
  cargoGlobal: string;
}

export interface Gasto {
  id: number;
  descricao: string;
  categoria: string;
  metodoPagamento: string;
  etapa: string;
  valor: number;
  dtGasto: string;
  funcionario: Funcionario;
  reembolsoConcluido: boolean | null;
  obra: {
    id: number;
    titulo: string;
  };
}

export interface GastoCriarDto {
  descricao: string;
  categoria: string;
  metodoPagamento: string;
  etapa?: string;
  valor: number;
  dtGasto: string;
  funcionarioId: number;
  obraId: number;
  reembolsoConcluido?: boolean | null;
}

export type StatusReembolso = "PENDENTE" | "PARCIAL" | "PAGO";
export interface GastoFiltro {
  obraId?: number;
  funcionarioId?: number;
  categoria?: string;
  metodoPagamento?: string;
  statusReembolso?: StatusReembolso;
}

export const gastoService = {
  listar: async (params: GastoFiltro = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params });
    return response.data;
  },

  listarPorObra: async (obraId: number, filtros: Omit<GastoFiltro, "obraId"> = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params: { ...filtros, obraId } });
    return response.data;
  },

  criar: async (dto: GastoCriarDto): Promise<Gasto> => {
    const response = await api.post("/gastos", dto);
    return response.data;
  },

  atualizar: async (id: number, dto: GastoCriarDto): Promise<Gasto> => {
    const response = await api.put(`/gastos/${id}`, dto);
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Gasto> => {
    const response = await api.get(`/gastos/${id}`);
    return response.data;
  },
  deletar: async (id: number): Promise<void> => {
    await api.delete(`/gastos/${id}`);
  },
};
