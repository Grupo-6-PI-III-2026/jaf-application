export interface LoginCredentials {
  email: string;
  senha: string; // Backend espera "senha", não "password"
}

export interface LoginResponse {
  token: string; // Backend retorna token JWT
}

export interface LoginResponse {
  // provavelmente já existe o token aqui:
  token: string;

  // ADICIONE ESTAS LINHAS:
  email: string;
  nome: string;
  id: number; // (ou string, dependendo de como o backend envia)
  cargo: string;
}