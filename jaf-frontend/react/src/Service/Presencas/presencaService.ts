import api from "../Auth/Login/Api/Api";

export interface Colaborador {
  id?: number;
  funcionarioId: number;
  funcionarioNome: string;
  funcionarioCargo: string;
  data: string;
  presente: boolean;
  desabilitado?: boolean;
}

export interface PresencaRequest {
  funcionarioId: number;
  obraId: number;
  data: string;
  presente: boolean;
  horarioEntrada?: string;
  horarioSaida?: string;
}

export const presencaService = {
  listarPorObraEData: async (obraId: number, data: string): Promise<Colaborador[]> => {
    const response = await api.get(`/presencas/obra/${obraId}/data/${data}`);
    return response.data;
  },

  criarPresenca: async (payload: PresencaRequest) => {
    const response = await api.post("/presencas", payload);
    return response.data;
  },

  alternarPresenca: async (id: number) => {
    await api.patch(`/presencas/${id}/alternar`);
  },

  atualizarPresenca: async (id: number, payload: PresencaRequest) => {
    const response = await api.put(`/presencas/${id}`, payload);
    return response.data;
  },

  deletarPresenca: async (id: number) => {
    await api.delete(`/presencas/${id}`);
  },
};
