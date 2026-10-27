import api from "../Auth/Login/Api/Api";

export interface OcrCatalogItem {
  Nome_Produto?: string;
  Preco?: string;
}

export interface OcrResponse {
  status: string;
  ocr?: {
    raw_text?: string;
  };
  dados_extraidos?: {
    itens?: OcrCatalogItem[];
  };
}

export const ocrService = {
  processar: async (file: File): Promise<OcrResponse> => {
    const formData = new FormData();
    formData.append("file", file);

    const response = await api.post<OcrResponse>("/ocr", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    });

    return response.data;
  },
};