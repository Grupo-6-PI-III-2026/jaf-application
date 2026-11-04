import api from "../Auth/Login/Api/Api";
import { opcoesIdempotencia } from "../../Utils/idempotencia";
import type { Pagina } from "../../Types/pagina";

export type StatusReembolso = "PENDENTE" | "PARCIAL" | "PAGO";

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
  versao?: number;
  statusReembolso?: StatusReembolso | null;
  totalPago?: number | null;
  saldoPendente?: number | null;
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
  versao?: number;
}

export interface GastoFiltro {
  obraId?: number;
  funcionarioId?: number;
  categoria?: string;
  metodoPagamento?: string;
  statusReembolso?: StatusReembolso;
}

export type OrdemGasto = "dtGasto" | "valor" | "descricao";

export interface GastoPaginadoParams extends GastoFiltro {
  page: number;
  size: number;
  sortBy: OrdemGasto;
  sortDir: "asc" | "desc";
  from?: string;
  to?: string;
  busca?: string;
}

export const gastoService = {
  // Filtro, ordenação, período e paginação são feitos no servidor; o total é o real, não o que foi baixado.
  listarPaginado: async (params: GastoPaginadoParams, signal?: AbortSignal): Promise<Pagina<Gasto>> => {
    const response = await api.get<Pagina<Gasto>>("/gastos/paginado", { params, signal });
    return response.data;
  },

  listar: async (params: GastoFiltro = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params });
    return response.data;
  },

  listarPorObra: async (obraId: number, filtros: Omit<GastoFiltro, "obraId"> = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params: { ...filtros, obraId } });
    return response.data;
  },

  criar: async (dto: GastoCriarDto, chave?: string): Promise<Gasto> => {
    const response = await api.post("/gastos", dto, opcoesIdempotencia(chave));
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
