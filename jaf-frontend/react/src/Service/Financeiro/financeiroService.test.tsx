import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "../Auth/Login/Api/Api";
import { financeiroService } from "./financeiroService";
import { gastoService } from "../Gastos/gastoService";

vi.mock("../Auth/Login/Api/Api", () => ({ default: { get: vi.fn(), put: vi.fn(), post: vi.fn(), delete: vi.fn() } }));
beforeEach(() => {
  vi.resetAllMocks();
  vi.mocked(api.get).mockResolvedValue({ data: [] });
  vi.mocked(api.put).mockResolvedValue({ data: { totalRecebido: 0 } });
  vi.mocked(api.post).mockResolvedValue({ data: { gastoId: 7 } });
});
describe("clientes do contrato financeiro", () => {
  it("envia recebido separado do orçamento e aceita zero", async () => {
    await financeiroService.buscar(3);
    expect(api.get).toHaveBeenCalledWith("/obras/3/financeiro");
    await expect(financeiroService.atualizar(3, 0)).resolves.toEqual({ totalRecebido: 0 });
    expect(api.put).toHaveBeenCalledWith("/obras/3/financeiro", { totalRecebido: 0 });
  });
  it("consulta e registra parcelas com corpo exato", async () => {
    await financeiroService.historico(7);
    expect(api.get).toHaveBeenCalledWith("/gastos/7/reembolsos");
    const parcela = { valor: 25.50, dataPagamento: "2026-10-27", observacao: "Parcela" };
    await financeiroService.registrar(7, parcela);
    expect(api.post).toHaveBeenCalledWith("/gastos/7/reembolsos", parcela);
  });
  it("usa endpoint resumo e combina filtros por AND no endpoint gastos", async () => {
    await financeiroService.resumo(3);
    expect(api.get).toHaveBeenCalledWith("/gastos/reembolsos/resumo", { params: { obraId: 3 } });
    await gastoService.listarPorObra(3, { funcionarioId: 4, statusReembolso: "PARCIAL" });
    expect(api.get).toHaveBeenCalledWith("/gastos", { params: { obraId: 3, funcionarioId: 4, statusReembolso: "PARCIAL" } });
  });
  it("não repete POST depois de falha de rede", async () => {
    vi.mocked(api.post).mockRejectedValue(new Error("Rede"));
    await expect(financeiroService.registrar(7, { valor: 1, dataPagamento: "2026-10-27" })).rejects.toThrow("Rede");
    expect(api.post).toHaveBeenCalledTimes(1);
  });
});