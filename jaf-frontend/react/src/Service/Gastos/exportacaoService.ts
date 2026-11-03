import api from "../Auth/Login/Api/Api";

export type FormatoExportacao = "csv" | "xlsx";

export interface ExportacaoGastosParams {
  obraId: number;
  from?: string;
  to?: string;
  funcionarioId?: number;
  metodoPagamento?: string;
  statusReembolso?: string;
  formato: FormatoExportacao;
}

function nomeArquivo(headers: Record<string, string | undefined>, formato: FormatoExportacao): string {
  const disposicao = headers["content-disposition"] ?? "";
  const encontrado = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(disposicao)?.[1];
  if (!encontrado) return `gastos.${formato}`;
  try {
    return decodeURIComponent(encontrado);
  } catch {
    return encontrado;
  }
}

export async function exportarGastos(params: ExportacaoGastosParams): Promise<void> {
  const resposta = await api.get<Blob>("/gastos/export", {
    params,
    responseType: "blob",
    timeout: 120_000,
  });
  const url = URL.createObjectURL(resposta.data);
  const link = document.createElement("a");
  link.href = url;
  link.download = nomeArquivo(resposta.headers as Record<string, string | undefined>, params.formato);
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
