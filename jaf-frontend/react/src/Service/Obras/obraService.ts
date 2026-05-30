import api from "../Auth/Login/Api/Api";

export interface ObraDto {
  titulo: string;
  orcamento: string;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
}

export interface Obra {
  id: number;
  titulo: string;
  orcamento: string;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
}

export const obraService = {
  listar: async (): Promise<Obra[]> => {
    const response = await api.get("/obras");
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Obra> => {
    const response = await api.get(`/obras/${id}`);
    return response.data;
  },

  criar: async (payload: ObraDto): Promise<Obra> => {
    const response = await api.post("/obras", payload);
    return response.data;
  },

  atualizar: async (id: number, payload: ObraDto): Promise<Obra> => {
    const response = await api.put(`/obras/${id}`, payload);
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/obras/${id}`);
  },

  listarGastosDaObra: async (id: number): Promise<any[]> => {
    const response = await api.get(`/obras/${id}/gastos`);
    return response.data;
  },

  listarAlocacoesDaObra: async (id: number): Promise<any[]> => {
    const response = await api.get(`/obras/${id}/alocacoes`);
    return response.data;
  },
};
