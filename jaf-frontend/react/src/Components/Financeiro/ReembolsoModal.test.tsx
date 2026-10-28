import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ReembolsoModal from "./ReembolsoModal";
import { financeiroService, type Reembolso } from "../../Service/Financeiro/financeiroService";
import type { Gasto } from "../../Service/Gastos/gastoService";
import { hojeIso } from "../PeriodoFiltro/periodUtils";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Financeiro/financeiroService", () => ({ financeiroService: { historico: vi.fn(), registrar: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
const gasto: Gasto = { id: 8, descricao: "Nota fiscal", categoria: "MATERIAL", metodoPagamento: "REEMBOLSO", etapa: "ETAPA 1", valor: 100, dtGasto: "2020-01-01", funcionario: { id: 2, nome: "Pessoa A", email: "", cargoGlobal: "ENGENHEIRO" }, obra: { id: 1, titulo: "Obra" }, reembolsoConcluido: false };
const parcial: Reembolso = { gastoId: 8, valorTotal: 100, totalPago: 30, saldoPendente: 70, status: "PARCIAL", pagamentos: [{ id: 1, valor: 30, dataPagamento: "2020-01-02", observacao: "Primeira parcela" }] };
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); validarSessao("ADMIN");
  // happy-dom não implementa o comportamento nativo de abertura/foco.
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute("open", ""); };
  HTMLDialogElement.prototype.close = function () { this.removeAttribute("open"); };
  vi.mocked(financeiroService.historico).mockResolvedValue(parcial);
  vi.mocked(financeiroService.registrar).mockResolvedValue({ ...parcial, totalPago: 100, saldoPendente: 0, status: "PAGO" });
});
const abrir = (onAtualizado = vi.fn()) => render(<ReembolsoModal gasto={gasto} onFechar={vi.fn()} onAtualizado={onAtualizado} />);
describe("parcelas de reembolso", () => {
  it("consulta histórico sem oferecer pagamento ao engenheiro", async () => {
    validarSessao("ENGENHEIRO"); abrir();
    expect(await screen.findByText("Primeira parcela")).toBeInTheDocument();
    expect(screen.queryByLabelText("Valor da parcela")).not.toBeInTheDocument();
    expect(financeiroService.registrar).not.toHaveBeenCalled();
  });
  it.each(["ADMIN", "RESPONSAVEL_ADMINISTRATIVO"] as const)("registra parcela como %s e atualiza resumo", async (cargo) => {
    validarSessao(cargo);
    const atualizado = vi.fn(); abrir(atualizado);
    fireEvent.change(await screen.findByLabelText("Valor da parcela"), { target: { value: "70,00" } });
    fireEvent.change(screen.getByLabelText("Observação (opcional)"), { target: { value: "Última parcela" } });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar pagamento" }));
    await waitFor(() => expect(financeiroService.registrar).toHaveBeenCalledWith(8, { valor: 70, dataPagamento: hojeIso(), observacao: "Última parcela" }));
    expect(await screen.findByText("PAGO")).toBeInTheDocument();
    expect(atualizado).toHaveBeenCalledTimes(1);
    expect(screen.queryByRole("button", { name: "Confirmar pagamento" })).not.toBeInTheDocument();
  });
  it.each(["0", "-1", "70.01", "10.001"])("bloqueia parcela inválida %s", async (valor) => {
    abrir(); fireEvent.change(await screen.findByLabelText("Valor da parcela"), { target: { value: valor } });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar pagamento" }));
    expect(financeiroService.registrar).not.toHaveBeenCalled();
  });
  it("bloqueia data anterior ao gasto e futura mesmo no submit programático", async () => {
    abrir(); const input = await screen.findByLabelText("Valor da parcela");
    fireEvent.change(input, { target: { value: "10" } });
    for (const data of ["2019-12-31", "2999-01-01"]) {
      fireEvent.change(screen.getByLabelText("Data do pagamento"), { target: { value: data } });
      fireEvent.submit(input.closest("form")!);
    }
    expect(financeiroService.registrar).not.toHaveBeenCalled();
  });
  it("bloqueia duplo envio enquanto a parcela está sendo registrada", async () => {
    vi.mocked(financeiroService.registrar).mockReturnValue(new Promise(() => {}));
    abrir(); const input = await screen.findByLabelText("Valor da parcela");
    fireEvent.change(input, { target: { value: "10" } });
    fireEvent.submit(input.closest("form")!); fireEvent.submit(input.closest("form")!);
    expect(financeiroService.registrar).toHaveBeenCalledTimes(1);
  });
  it("exige consultar histórico após resultado desconhecido e não reenvia", async () => {
    vi.mocked(financeiroService.registrar).mockRejectedValue(new Error("Timeout"));
    abrir(); fireEvent.change(await screen.findByLabelText("Valor da parcela"), { target: { value: "10" } });
    fireEvent.click(screen.getByRole("button", { name: "Confirmar pagamento" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Confirmar pagamento" })).toBeDisabled());
    expect(screen.getByRole("alert")).toHaveTextContent("Confira".toLowerCase());
    fireEvent.click(screen.getByRole("button", { name: "Consultar histórico atualizado" }));
    await waitFor(() => expect(screen.getByRole("button", { name: "Confirmar pagamento" })).toBeEnabled());
    expect(financeiroService.registrar).toHaveBeenCalledTimes(1);
    expect(financeiroService.historico).toHaveBeenCalledTimes(2);
  });
});