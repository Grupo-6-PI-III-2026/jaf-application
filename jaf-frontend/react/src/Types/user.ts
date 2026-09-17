export type Cargo =
  | "ADMIN"
  | "RESPONSAVEL_ADMINISTRATIVO"
  | "ENGENHEIRO";

export const CargoLabel: Record<Cargo, string> = {
  ADMIN: "Administrador",
  RESPONSAVEL_ADMINISTRATIVO: "Responsável Administrativo",
  ENGENHEIRO: "Engenheiro",
};

export const GENERIC_AVATARS = [
  { label: "Administrador", value: "/assets/Geral/avatar-admin.svg" },
  { label: "Gestor", value: "/assets/Geral/avatar-gestor.svg" },
  { label: "Operador", value: "/assets/Geral/avatar-operador.svg" },
  { label: "Equipe", value: "/assets/Geral/avatar-equipe.svg" },
  { label: "JAF", value: "/assets/Geral/avatar-jaf.svg" },
] as const;

export const DEFAULT_AVATAR_URL = GENERIC_AVATARS[0].value;

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
