import { isAxiosError } from "axios";

export const logger = {
  erro(contexto: string, erro: unknown): void {
    const dados = isAxiosError(erro)
      ? { status: erro.response?.status, code: erro.code }
      : { mensagem: erro instanceof Error ? erro.message : "Erro desconhecido" };
    console.error(contexto, dados);
  },
};
