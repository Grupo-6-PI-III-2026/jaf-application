import { type LoginCredentials, type LoginResponse } from "../../../Types/auth";
import api from "./Api/Api";

export const authService = {
  // endpoint de login
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    try {
      const response = await api.post<LoginResponse>(
        "/funcionarios/login",
        credentials,
      );

      const { token, email } = response.data;
      
      localStorage.setItem('token', token);
      localStorage.setItem('userEmail', email);

      return response.data;
    } catch (error: any) {
      console.error("Erro ao fazer login:", error);
      
      if (error.response?.status === 401) {
        throw new Error('Email ou senha inválidos');
      }
      throw new Error('Erro ao fazer login');
    }
  },

  logout: () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    window.location.href = '/';
  },

  isAuthenticated: (): boolean => {
    const token = localStorage.getItem('token');
    
    if (!token) return false;
    
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const expiry = payload.exp * 1000;
      
      return Date.now() < expiry;
    } catch {
      return false;
    }
  },

  getUserEmail: (): string | null => {
    return localStorage.getItem('userEmail');
  },

  getToken: (): string | null => {
    return localStorage.getItem('token');
  }
};
