export interface LoginCredentials {
  email: string;
  senha: string;  // Backend espera "senha", não "password"
}

export interface LoginResponse {
  email: string;
  nome: string;
  id: number;
  cargo: string;
  // CORREÇÃO DE SEGURANÇA A07: Token não é mais retornado no corpo da resposta
  // Token é enviado via cookie HttpOnly pelo backend (proteção contra XSS)
}
