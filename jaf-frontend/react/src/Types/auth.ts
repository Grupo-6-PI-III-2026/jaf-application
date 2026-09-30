import type { Cargo } from "./user";

export interface LoginCredentials {
  email: string;
  senha: string;  // Backend espera "senha", não "password"
}

export interface LoginResponse {
  id: number;
  nome: string;
  email: string;
  cargo: Cargo;
  token: string | null;
}
