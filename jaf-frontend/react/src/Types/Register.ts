export type CargoApi =
  | "ADMIN"
  | "RESPONSAVEL_ADMINISTRATIVO"
  | "ENGENHEIRO";

export interface NovoFuncionarioPayload {
  nome: string;
  email: string;
  senha: string;
  cargo: CargoApi;
}
