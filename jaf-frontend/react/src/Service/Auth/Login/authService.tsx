import {type LoginCredentials, type LoginResponse } from "../../../Types/Auth";
import api from "./Api/Api";
export const authService = {
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    try {
      const response = await api.post<LoginResponse>("/login", credentials);

      return response.data;
    } catch (error) {
      console.error("Erro ", error);

      throw error;
    }
  },
};
