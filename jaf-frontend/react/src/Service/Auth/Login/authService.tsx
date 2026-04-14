import { type LoginCredentials, type LoginResponse } from "../../../Types/auth";
import api from "./Api/Api";
export const authService = {
  login: async (credentials: LoginCredentials): Promise<LoginResponse> => {
    try {
      const response = await api.post<LoginResponse>(
        "/auth/login",
        credentials,
      );

      return response.data;
    } catch (error) {
      console.error("Erro ", error);

      throw error;
    }
  },
};
