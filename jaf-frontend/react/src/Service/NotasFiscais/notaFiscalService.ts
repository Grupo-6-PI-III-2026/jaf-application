import api from "../Auth/Login/Api/Api";

export interface NotaFiscal {
  id: number;
  numero: string | null;
  cnpjEmitente: string | null;
  valor: number | null;
  dataEmissao: string | null;
  nomeArquivo: string;
}

export const notaFiscalService = {
  listar: async (gastoId: number): Promise<NotaFiscal[]> => {
    const response = await api.get<NotaFiscal[]>(`/gastos/${gastoId}/notas-fiscais`);
    return response.data;
  },

  anexar: async (
    gastoId: number,
    arquivo: File,
    extra: { numero?: string; cnpjEmitente?: string } = {},
  ): Promise<NotaFiscal> => {
    const formData = new FormData();
    formData.append("arquivo", arquivo);
    if (extra.numero?.trim()) formData.append("numero", extra.numero.trim());
    if (extra.cnpjEmitente?.trim()) formData.append("cnpjEmitente", extra.cnpjEmitente.trim());
    const response = await api.post<NotaFiscal>(`/gastos/${gastoId}/notas-fiscais`, formData, {
      timeout: 120_000,
    });
    return response.data;
  },

  deletar: async (id: number): Promise<void> => {
    await api.delete(`/notas-fiscais/${id}`);
  },
};
