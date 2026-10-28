import { type LoginCredentials, type LoginResponse } from "../../../Types/auth";
import type { Cargo, JwtPayload } from "../../../Types/user";
import api from "./Api/Api";
import axios from "axios";
import { authorizationSession } from "./authorizationSession.ts";

function clearStoredUser() {
  for (const key of ["userEmail", "userName", "userId", "userCargo"]) {
    localStorage.removeItem(key);
  }
}

export const authService = {
  // endpoint de login
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    authorizationSession.clear();
    clearStoredUser();
    try {
      const response = await api.post<LoginResponse>(
        "/funcionarios/login",
        credentials,
      );

      // CORREÇÃO DE SEGURANÇA A07: Token não vem mais no corpo da resposta
      // Antes: const { token } = response.data;
      // Agora: Token é enviado via cookie HttpOnly pelo backend (proteção contra XSS)
      // Armazena informações do usuário que vieram na resposta
      const { email, nome, id, cargo } = response.data;
      localStorage.setItem("userEmail", email);
      localStorage.setItem("userName", nome);
      localStorage.setItem("userId", String(id));
      localStorage.setItem("userCargo", cargo);

      return response.data;
    } catch (error: unknown) {
      console.error("Erro ao fazer login:", error);

      if (axios.isAxiosError(error) && error.code === "ERR_NETWORK") {
        throw new Error("Servidor indisponivel. Tente novamente mais tarde.");
      }
      if (axios.isAxiosError(error) && error.response?.status === 400) {
        throw new Error("Informe e-mail e senha para acessar.");
      }
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        throw new Error("Email ou senha inválidos");
      }
      throw new Error("Erro ao fazer login");
    }
  },

  logout: async () => {
    authService.clearSession();
    // CORREÇÃO DE SEGURANÇA A07: Chama endpoint de logout do backend para limpar o cookie
    try {
      await api.post("/funcionarios/logout");
    } catch (error) {
      console.error("Erro ao fazer logout:", error);
    }

    window.location.replace("/");
  },

  clearSession: () => {
    authorizationSession.clear();
    clearStoredUser();
  },

  isAuthenticated: (): boolean => {
    // CORREÇÃO DE SEGURANÇA A07: Validação baseada em localStorage de userEmail
    // Antes: verificava expiração do token
    // Agora: assume autenticado se userEmail existe (validação real feita no backend)
    return localStorage.getItem("userEmail") !== null;
  },

  getUserEmail: (): string | null => {
    return localStorage.getItem("userEmail");
  },

  getToken: (): string | null => {
    // CORREÇÃO DE SEGURANÇA A07: Token não está mais disponível no localStorage
    // Antes: lia token do localStorage
    // Agora: Token é gerenciado via cookie HttpOnly pelo backend
    // Esta função retorna null pois o frontend não tem mais acesso ao token
    return null;
  },

  decodeToken: (): JwtPayload | null => {
    // CORREÇÃO DE SEGURANÇA A07: Token não está mais acessível no frontend
    // Antes: decodificava token do localStorage
    // Agora: Como o token está em cookie HttpOnly, o frontend não consegue decodificá-lo
    // Informações do usuário devem ser obtidas via endpoint dedicado ou passadas no login
    return null;
  },

  getEmail: (): string | null => {
    // CORREÇÃO DE SEGURANÇA A07: Retorna email do localStorage
    return localStorage.getItem("userEmail");
  },

  getId: (): number | null => {
    // CORREÇÃO DE SEGURANÇA A07: Retorna ID do localStorage
    const id = localStorage.getItem("userId");
    return id ? parseInt(id) : null;
  },

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
