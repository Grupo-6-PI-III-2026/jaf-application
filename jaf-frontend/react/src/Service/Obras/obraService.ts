import api from "../Auth/Login/Api/Api";

export interface Obra {
  id: number;
  titulo: string;
  orcamento: string;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
}

export interface ObraCriarDto {
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

  criar: async (dto: ObraCriarDto): Promise<Obra> => {
    const response = await api.post("/obras", dto);
    return response.data;
  },
};
