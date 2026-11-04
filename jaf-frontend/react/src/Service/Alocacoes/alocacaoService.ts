import api from "../Auth/Login/Api/Api";
import { opcoesIdempotencia } from "../../Utils/idempotencia";

export interface AlocacaoObra {
  id: number;
  funcionario: {
    id: number;
    nome: string;
    email: string;
    fotoUrl: string | null;
    cargoGlobal: string;
  };
  obra: {
    id: number;
    titulo: string;
    status: string;
    dtInicio: string;
    dtTerminoPrevisto: string;
    orcamento?: string;
    totalRecebido?: number;
  };
  cargo: string;
  dtInicio?: string | null;
  dtFim?: string | null;
}

export interface AlocacaoCriarDto {
  funcionarioId: number;
  obraId: number;
  cargoNaObra: string;
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

  criar: async (dto: AlocacaoCriarDto, chave?: string): Promise<AlocacaoObra> => {
    const response = await api.post("/alocacoes", dto, opcoesIdempotencia(chave));
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/alocacoes/${id}`);
  },

  buscarPorId: async (id: number): Promise<AlocacaoObra> => {
    const response = await api.get(`/alocacoes/${id}`);
    return response.data;
  },
};
