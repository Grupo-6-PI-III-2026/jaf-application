import api from "../Auth/Login/Api/Api";

export type OcrStatus = "PROCESSANDO" | "CONCLUIDO" | "ERRO";

export interface OcrNotaFiscalResponse {
  jobId?: string | null;
  status?: OcrStatus | null;
  sucesso: boolean;
  mensagem: string;
  textoBruto?: string | null;
  valor?: number | null;
  dtGasto?: string | null;
  metodoPagamento?: string | null;
  materialInsumo?: string | null;
  descricaoAdicional?: string | null;
  etapa?: string | null;
}

const INTERVALO_CONSULTA_MS = 2000;
const TEMPO_MAXIMO_ESPERA_MS = 2 * 60 * 1000;

const aguardar = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export const ocrService = {
  processarNotaFiscal: async (arquivo: File): Promise<OcrNotaFiscalResponse> => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);

    const { data: solicitacao } = await api.post<OcrNotaFiscalResponse>("/ocr/nota-fiscal", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    if (solicitacao.status !== "PROCESSANDO" || !solicitacao.jobId) {
      return solicitacao;
    }

    const limite = Date.now() + TEMPO_MAXIMO_ESPERA_MS;
    while (Date.now() < limite) {
      await aguardar(INTERVALO_CONSULTA_MS);
      const { data } = await api.get<OcrNotaFiscalResponse>(
        `/ocr/nota-fiscal/${encodeURIComponent(solicitacao.jobId)}`,
      );
      if (data.status !== "PROCESSANDO") {
        return data;
      }
    }

    return {
      jobId: solicitacao.jobId,
      status: "PROCESSANDO",
      sucesso: false,
      mensagem: "A leitura da nota fiscal está demorando mais que o esperado. Tente novamente em instantes.",
    };
  },
};
