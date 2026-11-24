import api from "../Auth/Login/Api/Api";

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
  reembolsoConcluido: boolean | null;
  possuiNotaFiscal?: boolean;
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
  ocrJobId?: string | null;
}

export type StatusReembolso = "PENDENTE" | "PARCIAL" | "PAGO";
export interface GastoFiltro {
  obraId?: number;
  funcionarioId?: number;
  categoria?: string;
  metodoPagamento?: string;
  statusReembolso?: StatusReembolso;
}

export const gastoService = {
  listar: async (params: GastoFiltro = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params });
    return response.data;
  },

  listarPorObra: async (obraId: number, filtros: Omit<GastoFiltro, "obraId"> = {}): Promise<Gasto[]> => {
    const response = await api.get<Gasto[]>("/gastos", { params: { ...filtros, obraId } });
    return response.data;
  },

  criar: async (dto: GastoCriarDto): Promise<Gasto> => {
    const response = await api.post("/gastos", dto);
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

  anexarNotaFiscal: async (id: number, arquivo: File): Promise<Gasto> => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);
    const response = await api.post<Gasto>(`/gastos/${id}/nota-fiscal`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },

  obterUrlNotaFiscal: async (id: number): Promise<string> => {
    const response = await api.get<{ url: string }>(`/gastos/${id}/nota-fiscal`);
    return response.data.url;
  },

  removerNotaFiscal: async (id: number): Promise<Gasto> => {
    const response = await api.delete<Gasto>(`/gastos/${id}/nota-fiscal`);
    return response.data;
  },
};
