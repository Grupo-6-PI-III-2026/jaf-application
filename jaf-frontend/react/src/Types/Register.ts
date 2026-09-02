export type CargoApi = "ADMIN" | "GESTOR_OBRA" | "OPERADOR_LANCAMENTO";

export interface NovoFuncionarioPayload {
  nome: string;
  email: string;
  senha: string;
  cargo: CargoApi;
}
