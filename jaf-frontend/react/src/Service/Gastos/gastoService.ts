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

export interface GastoCriarDto {
  descricao: string;
  categoria: string;
  metodoPagamento: string;
  etapa?: string;
  valor: number;
  dtGasto: string;
  funcionarioId: number;
  obraId: number;
}

export const gastoService = {
  listar: async (): Promise<Gasto[]> => {
    const response = await api.get("/gastos");
    return response.data;
  },

  listarPorObra: async (obraId: number): Promise<Gasto[]> => {
    const response = await api.get(`/obras/${obraId}/gastos`);
    return response.data;
  },

  criar: async (dto: GastoCriarDto): Promise<Gasto> => {
    const response = await api.post("/gastos", dto);
    return response.data;
  },

  buscarPorId: async (id: number): Promise<Gasto> => {
    const response = await api.get(`/gastos/${id}`);
    return response.data;
  },
};
