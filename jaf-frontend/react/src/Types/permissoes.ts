export interface FuncionarioPermissoes {
  id: number;
  nome: string;
  email: string;
  cargo: string;
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
