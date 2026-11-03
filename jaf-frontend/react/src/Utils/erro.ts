import { isAxiosError } from "axios";

type DadosErro = {
  code?: string;
  message?: string;
  mensagem?: string;
  detail?: string;
  fields?: Record<string, string>;
};

export type ErroApi = {
  status: number;
  code?: string;
  mensagem: string;
  campos: Record<string, string>;
};

export function tratarErroApi(
  erro: unknown,
  fallback = "Não foi possível concluir a operação.",
): ErroApi {
  if (!isAxiosError<DadosErro>(erro)) {
    return { status: 0, mensagem: fallback, campos: {} };
  }

  if (!erro.response) {
    return {
      status: 0,
      mensagem:
        erro.code === "ECONNABORTED"
          ? "A operação demorou demais. Tente novamente."
          : "Sem conexão com o servidor.",
      campos: {},
    };
  }

  const status = erro.response.status;
  const dados = erro.response.data;
  const mensagemServidor = dados?.message || dados?.mensagem || dados?.detail;
  const mensagens: Record<number, string> = {
    400: mensagemServidor || "Dados inválidos.",
    403: "Você não tem permissão para esta ação.",
    404: mensagemServidor || "Recurso não encontrado.",
    413: "Arquivo muito grande (máx. 10 MB).",
    429: "Muitas tentativas. Aguarde alguns minutos.",
  };

  return {
    status,
    code: dados?.code,
    mensagem:
      mensagens[status] ||
      (status >= 500 ? "Erro no servidor. Tente novamente em instantes." : mensagemServidor || fallback),
    campos: dados?.fields || {},
  };
}

export async function tratarErroApiBlob(
  erro: unknown,
  fallback = "Não foi possível concluir a operação.",
): Promise<ErroApi> {
  if (!isAxiosError<Blob>(erro) || !(erro.response?.data instanceof Blob)) {
    return tratarErroApi(erro, fallback);
  }

  try {
    const texto = await erro.response.data.text();
    const dados = JSON.parse(texto) as DadosErro;
    return tratarErroApi(
      { ...erro, response: { ...erro.response, data: dados } },
      fallback,
    );
  } catch {
    return tratarErroApi(erro, fallback);
  }
}
