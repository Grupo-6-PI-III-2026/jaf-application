import type { AxiosResponse } from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api from "../Auth/Login/Api/Api";
import { ocrService, type OcrNotaFiscalResponse } from "./ocrService";

vi.mock("../Auth/Login/Api/Api", () => ({ default: { post: vi.fn(), get: vi.fn() } }));

const resposta = (data: OcrNotaFiscalResponse) => ({ data }) as AxiosResponse<OcrNotaFiscalResponse>;
const arquivo = new File(["conteudo"], "nota.png", { type: "image/png" });

describe("ocrService.processarNotaFiscal", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("consulta o job até sair de PROCESSANDO e devolve o resultado final", async () => {
    vi.mocked(api.post).mockResolvedValue(resposta({ jobId: "job-1", status: "PROCESSANDO", sucesso: true, mensagem: "" }));
    vi.mocked(api.get)
      .mockResolvedValueOnce(resposta({ jobId: "job-1", status: "PROCESSANDO", sucesso: true, mensagem: "" }))
      .mockResolvedValueOnce(resposta({ jobId: "job-1", status: "CONCLUIDO", sucesso: true, mensagem: "ok", valor: 10 }));

    const promessa = ocrService.processarNotaFiscal(arquivo);
    await vi.advanceTimersByTimeAsync(4000);

    await expect(promessa).resolves.toMatchObject({ status: "CONCLUIDO", valor: 10 });
    expect(api.get).toHaveBeenCalledTimes(2);
    expect(api.get).toHaveBeenCalledWith("/ocr/nota-fiscal/job-1");
  });

  it("não consulta quando o envio é recusado pelo backend", async () => {
    vi.mocked(api.post).mockResolvedValue(resposta({ status: "ERRO", sucesso: false, mensagem: "Formato inválido" }));

    await expect(ocrService.processarNotaFiscal(arquivo)).resolves.toMatchObject({ sucesso: false });
    expect(api.get).not.toHaveBeenCalled();
  });

  it("desiste após o tempo máximo de espera", async () => {
    vi.mocked(api.post).mockResolvedValue(resposta({ jobId: "job-2", status: "PROCESSANDO", sucesso: true, mensagem: "" }));
    vi.mocked(api.get).mockResolvedValue(resposta({ jobId: "job-2", status: "PROCESSANDO", sucesso: true, mensagem: "" }));

    const promessa = ocrService.processarNotaFiscal(arquivo);
    await vi.advanceTimersByTimeAsync(2 * 60 * 1000 + 2000);

    await expect(promessa).resolves.toMatchObject({ sucesso: false, status: "PROCESSANDO" });
  });
});
