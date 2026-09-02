import { type LoginCredentials, type LoginResponse } from "../../../Types/auth";
import type { Cargo, JwtPayload } from "../../../Types/user";
import api from "./Api/Api";
import axios from "axios";

const TOKEN_KEYS = ["token", "auth_token"];

export const authService = {
  // endpoint de login
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    try {
      const response = await api.post<LoginResponse>(
        "/funcionarios/login",
        credentials,
      );

      const { token } = response.data;

      localStorage.setItem("token", token);
      localStorage.setItem("userEmail", credentials.email);

      return response.data;
    } catch (error: unknown) {
      console.error("Erro ao fazer login:", error);

      if (axios.isAxiosError(error) && error.response?.status === 401) {
        throw new Error("Email ou senha inválidos");
      }
      throw new Error("Erro ao fazer login");
    }
  },

  logout: () => {
    TOKEN_KEYS.forEach((key) => localStorage.removeItem(key));
    localStorage.removeItem("userEmail");
    window.location.href = "/";
  },

  isAuthenticated: (): boolean => {
    const payload = authService.decodeToken();
    if (!payload?.exp) return false;
    return payload.exp * 1000 > Date.now();
  },

  getUserEmail: (): string | null => {
    return authService.getEmail() ?? localStorage.getItem("userEmail");
  },

  getToken: (): string | null => {
    for (const key of TOKEN_KEYS) {
      const token = localStorage.getItem(key);
      if (token) return token;
    }
    return null;
  },

  decodeToken: (): JwtPayload | null => {
    const token = authService.getToken();
    if (!token) return null;
    try {
      const payload = token.split(".")[1];
      return JSON.parse(atob(payload)) as JwtPayload;
    } catch {
      return null;
    }
  },

  getEmail: (): string | null => {
    return authService.decodeToken()?.sub ?? null;
  },

  getId: (): number | null => {
    return authService.decodeToken()?.id ?? null;
  },

  getCargo: (): Cargo | null => {
    return authService.decodeToken()?.cargo ?? null;
  },
};
