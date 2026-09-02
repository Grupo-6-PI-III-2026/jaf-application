import api from "../Auth/Login/Api/Api";

export interface OcrNotaFiscalResponse {
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

export const ocrService = {
  processarNotaFiscal: async (arquivo: File): Promise<OcrNotaFiscalResponse> => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);

    const response = await api.post<OcrNotaFiscalResponse>("/ocr/nota-fiscal", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });
    return response.data;
  },
};