import type { Cargo } from "./user";

export interface FuncionarioPermissoes {
  id: number;
  nome: string;
  email: string;
  cargo: Cargo | null;
  createdAt?: string | null;
  ativo?: boolean;
}

export interface Permissao {
  chave: string;
  label: string;
  descricao: string;
  nivel: "restrito" | "critico" | null;
}

export interface ModuloPermissao {
  id: string;
  nome: string;
  icon: string;
  permissoes: Permissao[];
}
