import api from "../Auth/Login/Api/Api";

export interface AlocacaoObra {
  id: number;
  funcionario: {
    id: number;
    nome: string;
    email: string;
    cargoGlobal: string;
  };
  obra: {
    id: number;
    titulo: string;
  };
  cargo: string;
}

export const alocacaoService = {
  listar: async (): Promise<AlocacaoObra[]> => {
    const response = await api.get("/alocacoes");
    return response.data;
  },

  buscarPorId: async (id: number): Promise<AlocacaoObra> => {
    const response = await api.get(`/alocacoes/${id}`);
    return response.data;
  },
};
