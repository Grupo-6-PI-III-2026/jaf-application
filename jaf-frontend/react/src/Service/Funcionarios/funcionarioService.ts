import api from "../Auth/Login/Api/Api";
import { opcoesIdempotencia } from "../../Utils/idempotencia";
import { comprimirImagem } from "../../Utils/arquivo";

import { type NovoFuncionarioPayload } from "../../Types/Register";
import type {
  AlterarSenhaDto,
  FuncionarioResponseDto,
  PerfilUpdateDto,
} from "../../Types/user";
import type { FuncionarioPermissoes } from "../../Types/permissoes";
import type { FuncionarioPermissoesAcesso } from "../Permissoes/permissaoService";

export type FuncionarioAlocavel = Pick<FuncionarioPermissoes, "id" | "nome" | "cargo">;

export const funcionarioService = {
  cadastrar: async (payload: NovoFuncionarioPayload, chave?: string) => {
    const response = await api.post("/funcionarios", payload, opcoesIdempotencia(chave));
    return response.data;
  },

  listar: async (): Promise<FuncionarioPermissoes[]> => {
    const response = await api.get<FuncionarioPermissoes[]>("/funcionarios");
    return response.data;
  },

  listarAlocaveis: async (): Promise<FuncionarioAlocavel[]> => {
    const response = await api.get<FuncionarioAlocavel[]>("/funcionarios/alocaveis");
    return response.data;
  },

  getMeuPerfil: async (): Promise<FuncionarioResponseDto> => {
    const response = await api.get<FuncionarioResponseDto>("/funcionarios/me");
    return response.data;
  },

  getMinhasPermissoes: async (): Promise<FuncionarioPermissoesAcesso> => {
    const response = await api.get<FuncionarioPermissoesAcesso>("/funcionarios/me/permissoes");
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

  excluirConta: async (senhaAtual: string): Promise<void> => {
    await api.delete("/funcionarios/me", { data: { senhaAtual } });
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/funcionarios/${id}`);
  },

  inativar: async (id: number): Promise<FuncionarioPermissoes> => {
    const response = await api.patch<FuncionarioPermissoes>(`/funcionarios/${id}/inativar`);
    return response.data;
  },

  reativar: async (id: number): Promise<FuncionarioPermissoes> => {
    const response = await api.patch<FuncionarioPermissoes>(`/funcionarios/${id}/reativar`);
    return response.data;
  },

  uploadFoto: async (file: File): Promise<{ url: string }> => {
    const formData = new FormData();
    formData.append("file", await comprimirImagem(file));
    const response = await api.post<{ url: string }>("/funcionarios/me/foto", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
      // Foto de celular em rede móvel pode passar dos 10 s padrão.
      timeout: 120_000,
    });
    return response.data;
  },
};
