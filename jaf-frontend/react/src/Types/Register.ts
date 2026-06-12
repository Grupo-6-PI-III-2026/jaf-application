export type CargoApi =
  | "ADMIN"
  | "GESTOR_OBRA"
  | "OPERADOR_LANCAMENTO"
  | "MESTRE_DE_OBRAS"
  | "ENGENHEIRO"
  | "ARQUITETO"
  | "PEDREIRO";

export interface NovoFuncionarioPayload {
  nome: string;
  email: string;
  senha: string;
  cargo: CargoApi;
}
