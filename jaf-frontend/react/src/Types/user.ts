export type Cargo = "ADMIN" | "GESTOR_OBRA" | "OPERADOR_LANCAMENTO";

export const CargoLabel: Record<Cargo, string> = {
  ADMIN: "Administrador",
  GESTOR_OBRA: "Gestor de Obra",
  OPERADOR_LANCAMENTO: "Operador de Lancamento",
};

export const DEFAULT_AVATAR_URL =
  "https://tse1.mm.bing.net/th/id/OIP.l3waMeOdc8D_y_odZx2IcwHaHa?rs=1&pid=ImgDetMain&o=7&rm=3";

export interface FuncionarioResponseDto {
  id: number;
  nome: string;
  email: string;
  cargo: Cargo;
  fotoUrl: string | null;
}

export interface JwtPayload {
  sub: string;
  exp: number;
  id?: number;
  cargo?: Cargo;
  authorities?: string;
}

export interface PerfilUpdateDto {
  nome: string;
  email: string;
  fotoUrl: string | null;
}

export interface AlterarSenhaDto {
  senhaAtual: string;
  novaSenha: string;
  confirmacaoSenha: string;
}
