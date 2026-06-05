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
  obra: {
    id: number;
    titulo: string;
  };
}

export const gastoService = {
  listar: async (): Promise<Gasto[]> => {
    const response = await api.get("/gastos");
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Gasto> => {
    const response = await api.get(`/gastos/${id}`);
    return response.data;
  },
};
