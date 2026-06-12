import api from "../Auth/Login/Api/Api";

import { type NovoFuncionarioPayload } from "../../Types/Register";
import type {
  AlterarSenhaDto,
  FuncionarioResponseDto,
  PerfilUpdateDto,
} from "../../Types/user";
import type { FuncionarioPermissoes } from "../../Types/permissoes";

export const funcionarioService = {
  cadastrar: async (payload: NovoFuncionarioPayload) => {
    const response = await api.post("/funcionarios", payload);
    return response.data;
  },

  listar: async (): Promise<FuncionarioPermissoes[]> => {
    const response = await api.get<FuncionarioPermissoes[]>("/funcionarios");
    return response.data;
  },

  getMeuPerfil: async (): Promise<FuncionarioResponseDto> => {
    const response = await api.get<FuncionarioResponseDto>("/funcionarios/me");
    return response.data;
  },

  atualizarPerfil: async (
    payload: PerfilUpdateDto,
  ): Promise<FuncionarioResponseDto> => {
    const response = await api.put<FuncionarioResponseDto>(
      "/funcionarios/me",
      payload,
    );
    return response.data;
  },

  alterarSenha: async (payload: AlterarSenhaDto): Promise<void> => {
    await api.patch("/funcionarios/me/senha", payload);
  },

  excluirConta: async (): Promise<void> => {
    await api.delete("/funcionarios/me");
  },
};
