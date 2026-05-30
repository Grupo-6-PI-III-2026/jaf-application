import api from "../Auth/Login/Api/Api";

export interface AlocacaoObraDto {
  funcionarioId: number;
  obraId: number;
  cargoNaObra: string;
}

export interface AlocacaoObra {
  id: number;
  funcionarioId: number;
  obraId: number;
  cargo: string;
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

export const alocacaoService = {
  listar: async (): Promise<AlocacaoObra[]> => {
    const response = await api.get("/alocacoes");
    return response.data;
  },

  listarPorObra: async (obraId: number): Promise<AlocacaoObra[]> => {
    const response = await api.get(`/alocacoes/obra/${obraId}`);
    return response.data;
  },

  listarPorFuncionario: async (funcionarioId: number): Promise<AlocacaoObra[]> => {
    const response = await api.get(`/alocacoes/funcionario/${funcionarioId}`);
    return response.data;
  },

  criar: async (payload: AlocacaoObraDto): Promise<AlocacaoObra> => {
    const response = await api.post("/alocacoes", payload);
    return response.data;
  },

  atualizar: async (id: number, payload: AlocacaoObraDto): Promise<AlocacaoObra> => {
    const response = await api.put(`/alocacoes/${id}`, payload);
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/alocacoes/${id}`);
  },
};
