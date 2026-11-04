import api from "../Auth/Login/Api/Api";
import { opcoesIdempotencia } from "../../Utils/idempotencia";
import type { Pagina } from "../../Types/pagina";

export interface Obra {
  id: number;
  titulo: string;
  orcamento?: string;
  totalRecebido?: number;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
  versao?: number;
}

export interface ObraAtualizarParcialDto {
  titulo: string;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
  versao?: number;
}

export interface ObraCriarDto {
  titulo: string;
  orcamento: string;
  status: string;
  dtInicio: string; 
  dtTerminoPrevisto: string;
  responsavelId?: number;
  versao?: number;
}

export interface ObraResumo {
  id: number;
  titulo: string;
  status: string;
  dtInicio: string;
  dtTerminoPrevisto: string;
  responsavel?: { id: number; nome: string } | null;
  // Só vem para ADMIN; ausente significa que o total não está disponível.
  totalGasto?: number | null;
}

export interface PessoaDaObra {
  id: number;
  nome: string;
  funcaoNaObra: string | null;
}

export const obraService = {
  resumo: async (params: { page?: number; size?: number; sortBy?: string; sortDir?: "asc" | "desc"; status?: string } = {}): Promise<Pagina<ObraResumo>> => {
    const response = await api.get<Pagina<ObraResumo>>("/obras/resumo", { params });
    return response.data;
  },

  listarPessoas: async (obraId: number): Promise<PessoaDaObra[]> => {
    const response = await api.get<PessoaDaObra[]>(`/obras/${obraId}/pessoas`);
    return response.data;
  },

  listar: async (): Promise<Obra[]> => {
    const response = await api.get("/obras");
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Obra> => {
    const response = await api.get(`/obras/${id}`);
    return response.data;
  },

  criar: async (dto: ObraCriarDto, chave?: string): Promise<Obra> => {
    const response = await api.post("/obras", dto, opcoesIdempotencia(chave));
    return response.data;
  },

  atualizar: async (id: number, dto: ObraCriarDto): Promise<Obra> => {
    const response = await api.put(`/obras/${id}`, dto);
    return response.data;
  },

  atualizarParcial: async (id: number, dto: ObraAtualizarParcialDto): Promise<Obra> => {
    const response = await api.patch(`/obras/${id}`, dto);
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/obras/${id}`);
  },
};
