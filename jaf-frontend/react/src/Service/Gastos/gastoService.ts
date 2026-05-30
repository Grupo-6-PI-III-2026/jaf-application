import api from "../Auth/Login/Api/Api";

export interface GastoDto {
  descricao: string;
  categoria: string;
  metodoPagamento: string;
  etapa: string;
  valor: number;
  dtGasto: string;
  funcionarioId: number;
  obraId: number;
}

export interface Gasto {
  id: number;
  descricao: string;
  categoria: string;
  metodoPagamento: string;
  etapa: string;
  valor: number;
  dtGasto: string;
  funcionarioId: number;
  obraId: number;
  funcionario?: {
    id: number;
    nome: string;
    email: string;
  };
  obra?: {
    id: number;
    titulo: string;
  };
}

export const gastoService = {
  listar: async (obraId?: number): Promise<Gasto[]> => {
    const params = obraId ? `?obraId=${obraId}` : "";
    const response = await api.get(`/gastos${params}`);
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Gasto> => {
    const response = await api.get(`/gastos/${id}`);
    return response.data;
  },

  criar: async (payload: GastoDto): Promise<Gasto> => {
    const response = await api.post("/gastos", payload);
    return response.data;
  },

  atualizar: async (id: number, payload: GastoDto): Promise<Gasto> => {
    const response = await api.put(`/gastos/${id}`, payload);
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/gastos/${id}`);
  },
};
