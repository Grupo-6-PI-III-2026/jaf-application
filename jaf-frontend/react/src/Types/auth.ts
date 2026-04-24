export interface LoginCredentials {
  email: string;
  senha: string;  // Backend espera "senha", não "password"
}

export interface LoginResponse {
  token: string;  // Backend retorna token JWT
}
