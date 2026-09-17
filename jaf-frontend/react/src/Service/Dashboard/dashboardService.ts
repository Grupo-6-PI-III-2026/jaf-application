import api from "../Auth/Login/Api/Api";

export interface PieItem  { name: string; value: number; }
export interface LineItem { mes: string; valor: number; }
export interface BarItem  { categoria: string; valor: number; }

export interface DashboardStats {
  gastosEtapa: number;
  progressoEtapa: number;
  reembolsosPendentes: number;
  saldoRestante: number;
  progressoSaldo: number;
  reembolsosPizza: PieItem[];
  gastosImprevistos: LineItem[];
  gastosPorCategoria: BarItem[];
}

export const dashboardService = {
  buscarStats: async (obraId: number, etapa: string): Promise<DashboardStats> => {
    const response = await api.get("/dashboard/stats", { params: { obraId, etapa } });
    return response.data;
  },
};
