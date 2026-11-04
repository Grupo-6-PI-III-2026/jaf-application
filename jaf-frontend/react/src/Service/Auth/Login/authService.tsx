import { type LoginCredentials, type LoginResponse } from "../../../Types/auth";
import type { Cargo } from "../../../Types/user";
import api from "./Api/Api";
import { logger } from "../../../Utils/logger";
import axios from "axios";
import { authorizationSession } from "./authorizationSession.ts";
import { canalAuth } from "./canalAuth";

// Chaves de versões antigas que guardavam dados pessoais no navegador.
const CHAVES_LEGADAS = ["userEmail", "userName", "userId", "userCargo"];

export function limparDadosLegados() {
  for (const key of CHAVES_LEGADAS) localStorage.removeItem(key);
}

export const authService = {
  // endpoint de login
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    authorizationSession.clear();
    limparDadosLegados();
    try {
      const response = await api.post<LoginResponse>(
        "/funcionarios/login",
        credentials,
      );
      return response.data;
    } catch (error: unknown) {
      logger.erro("Erro ao fazer login", error);

      if (axios.isAxiosError(error) && error.code === "ERR_NETWORK") {
        throw new Error("Servidor indisponivel. Tente novamente mais tarde.");
      }
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        throw new Error("Informe e-mail e senha para acessar.");
      }
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        if (/inativo/i.test(String(error.response.data?.message ?? ""))) {
          throw new Error("Usuário inativo. Entre em contato com o administrador.");
        }
        throw new Error("Email ou senha inválidos");
      }
      throw new Error("Erro ao fazer login");
    }
  },

  logout: async () => {
    authService.clearSession();
    canalAuth.publicar({ tipo: "logout", userId: null });
    try {
      await api.post("/funcionarios/logout");
    } catch (error) {
      logger.erro("Erro ao fazer logout", error);
    }

    window.location.replace("/");
  },

  clearSession: () => {
    authorizationSession.clear();
    limparDadosLegados();
  },

  // Verdadeiro apenas depois que o servidor validou o perfil nesta carga da página.
  isAuthenticated: (): boolean => authorizationSession.current() !== null,

  getUserEmail: (): string | null => authorizationSession.current()?.user.email ?? null,

  getEmail: (): string | null => authorizationSession.current()?.user.email ?? null,

  getId: (): number | null => authorizationSession.current()?.user.id ?? null,

  getCargo: (): Cargo | null => {
    return authorizationSession.current()?.user.cargo ?? null;
  },

  clearAuthorization: authorizationSession.clear,
  getAuthorizationVersion: authorizationSession.version,
  setValidatedSession: authorizationSession.set,

  hasAuthority: (authority: string): boolean => {
    // Orientação da UI; autorização e escopo definitivos pertencem à API.
    return authorizationSession.current()?.permissions.has(authority) ?? false;
  },
  isAdmin: (): boolean => authService.getCargo() === "ADMIN",
  podeRegistrarPagamento: (): boolean =>
    ["ADMIN", "RESPONSAVEL_ADMINISTRATIVO"].includes(authService.getCargo() ?? ""),
};
