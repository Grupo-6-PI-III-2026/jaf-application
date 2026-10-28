import api from "../Auth/Login/Api/Api";
import type { StatusReembolso } from "../Gastos/gastoService";

export interface FinanceiroObra {
  totalRecebido: number;
  totalGasto: number;
  saldo: number;
}
export interface PagamentoReembolso {
  id: number;
  valor: number;
  dataPagamento: string;
  observacao: string | null;
}
export interface Reembolso {
  gastoId: number;
  valorTotal: number;
  totalPago: number;
  saldoPendente: number;
  status: StatusReembolso;
  pagamentos: PagamentoReembolso[];
}
export interface PagamentoRequest {
  valor: number;
  dataPagamento: string;
  observacao?: string;
}
export interface ResumoPessoa {
  funcionarioId: number;
  funcionarioNome: string;
  valorTotal: number;
  totalPago: number;
  saldoPendente: number;
}

export const financeiroService = {
  buscar: async (obraId: number): Promise<FinanceiroObra> =>
    (await api.get<FinanceiroObra>(`/obras/${obraId}/financeiro`)).data,
  atualizar: async (obraId: number, totalRecebido: number): Promise<FinanceiroObra> =>
    (await api.put<FinanceiroObra>(`/obras/${obraId}/financeiro`, { totalRecebido })).data,
  historico: async (gastoId: number): Promise<Reembolso> =>
    (await api.get<Reembolso>(`/gastos/${gastoId}/reembolsos`)).data,
  registrar: async (gastoId: number, pagamento: PagamentoRequest): Promise<Reembolso> =>
    (await api.post<Reembolso>(`/gastos/${gastoId}/reembolsos`, pagamento)).data,
  resumo: async (obraId: number): Promise<ResumoPessoa[]> =>
    (await api.get<ResumoPessoa[]>("/gastos/reembolsos/resumo", { params: { obraId } })).data,
};