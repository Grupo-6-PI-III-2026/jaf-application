import api from "../Auth/Login/Api/Api";

export interface Obra {
  id: number;
  titulo: string;
  orcamento?: string;
  totalRecebido?: number;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
  fotoUrl?: string | null;
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

  atualizar: async (id: number, dto: ObraCriarDto): Promise<Obra> => {
    const response = await api.put(`/obras/${id}`, dto);
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/obras/${id}`);
  },

  enviarFoto: async (id: number, arquivo: File): Promise<Obra> => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);
    const response = await api.post<Obra>(`/obras/${id}/foto`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  removerFoto: async (id: number): Promise<Obra> => {
    const response = await api.delete<Obra>(`/obras/${id}/foto`);
    return response.data;
  },
};
