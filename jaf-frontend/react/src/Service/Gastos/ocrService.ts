import api from "../Auth/Login/Api/Api";
import { comprimirImagem } from "../../Utils/arquivo";

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

// Intervalos entre consultas: começa rápido e vai espaçando para não sobrecarregar um OCR lento.
const INTERVALOS_CONSULTA_MS = [2000, 3000, 5000, 10000];
const TEMPO_MAXIMO_ESPERA_MS = 2 * 60 * 1000;
// Foto de celular em rede móvel pode levar bem mais que os 10 s das demais chamadas.
const TIMEOUT_ENVIO_MS = 120_000;

function aguardar(ms: number, signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) {
      reject(new DOMException("Cancelado", "AbortError"));
      return;
    }
    const aoCancelar = () => {
      clearTimeout(espera);
      reject(new DOMException("Cancelado", "AbortError"));
    };
    const espera = setTimeout(() => {
      signal?.removeEventListener("abort", aoCancelar);
      resolve();
    }, ms);
    signal?.addEventListener("abort", aoCancelar, { once: true });
  });
}

export const ocrService = {
  processarNotaFiscal: async (arquivo: File, signal?: AbortSignal): Promise<OcrNotaFiscalResponse> => {
    const formData = new FormData();
    formData.append("arquivo", await comprimirImagem(arquivo));

    const { data: solicitacao } = await api.post<OcrNotaFiscalResponse>("/ocr/nota-fiscal", formData, {
      headers: { "Content-Type": "multipart/form-data" },
      timeout: TIMEOUT_ENVIO_MS,
      signal,
    });

    if (solicitacao.status !== "PROCESSANDO" || !solicitacao.jobId) {
      return solicitacao;
    }

    const limite = Date.now() + TEMPO_MAXIMO_ESPERA_MS;
    for (let tentativa = 0; Date.now() < limite; tentativa += 1) {
      const intervalo = INTERVALOS_CONSULTA_MS[Math.min(tentativa, INTERVALOS_CONSULTA_MS.length - 1)];
      await aguardar(Math.min(intervalo, Math.max(0, limite - Date.now())), signal);
      const { data } = await api.get<OcrNotaFiscalResponse>(
        `/ocr/nota-fiscal/${encodeURIComponent(solicitacao.jobId)}`,
        { signal },
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