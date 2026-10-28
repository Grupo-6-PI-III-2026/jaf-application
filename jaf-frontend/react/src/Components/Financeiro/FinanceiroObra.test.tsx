import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import FinanceiroObra from "./FinanceiroObra";
import ResumoReembolsos from "./ResumoReembolsos";
import { financeiroService } from "../../Service/Financeiro/financeiroService";
import { toast } from "sonner";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Financeiro/financeiroService", () => ({ financeiroService: { buscar: vi.fn(), atualizar: vi.fn(), resumo: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); validarSessao("ADMIN");
  vi.mocked(financeiroService.buscar).mockResolvedValue({ totalRecebido: 100, totalGasto: 150, saldo: -50 });
  vi.mocked(financeiroService.atualizar).mockResolvedValue({ totalRecebido: 0, totalGasto: 150, saldo: -150 });
});
describe("painel financeiro", () => {
  it("limpa dados anteriores durante carga e erro ao trocar obra", async () => {
    const { rerender } = render(<FinanceiroObra obraId={1} />);
    await screen.findByLabelText("Total recebido acumulado");
    let rejeitar!: (erro: Error) => void;
    vi.mocked(financeiroService.buscar).mockReturnValue(new Promise((_, reject) => { rejeitar = reject; }));
    rerender(<FinanceiroObra obraId={2} />);
    expect(screen.getByRole("status")).toHaveTextContent("Carregando financeiro");
    expect(screen.queryByText("Recebido acumulado")).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Total recebido acumulado")).not.toBeInTheDocument();
    await act(async () => rejeitar(new Error("Falha na segunda obra")));
    expect(screen.getByRole("alert")).toBeInTheDocument();
    expect(screen.queryByText("Saldo dos recebimentos")).not.toBeInTheDocument();
  });
  it("bloqueia envio duplicado e sincroniza input com a resposta", async () => {
    let concluir!: (valor: { totalRecebido: number; totalGasto: number; saldo: number }) => void;
    vi.mocked(financeiroService.atualizar).mockReturnValue(new Promise((resolve) => { concluir = resolve; }));
    render(<FinanceiroObra obraId={1} />);
    const input = await screen.findByLabelText("Total recebido acumulado");
    fireEvent.change(input, { target: { value: "200" } });
    const form = input.closest("form")!;
    fireEvent.submit(form);
    fireEvent.submit(form);
    expect(financeiroService.atualizar).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Salvando..." })).toBeDisabled();
    await act(async () => concluir({ totalRecebido: 199.99, totalGasto: 150, saldo: 49.99 }));
    expect(input).toHaveValue("199.99");
    expect(screen.getByRole("button", { name: "Salvar total recebido" })).toBeEnabled();
  });
  it("ignora resposta de salvamento da obra anterior", async () => {
    let concluir!: (valor: { totalRecebido: number; totalGasto: number; saldo: number }) => void;
    vi.mocked(financeiroService.atualizar).mockReturnValue(new Promise((resolve) => { concluir = resolve; }));
    const { rerender } = render(<FinanceiroObra obraId={1} />);
    const input = await screen.findByLabelText("Total recebido acumulado");
    fireEvent.submit(input.closest("form")!);
    vi.mocked(financeiroService.buscar).mockResolvedValue({ totalRecebido: 300, totalGasto: 0, saldo: 300 });
    rerender(<FinanceiroObra obraId={2} />);
    await waitFor(() => expect(screen.getByLabelText("Total recebido acumulado")).toHaveValue("300.00"));
    await act(async () => concluir({ totalRecebido: 999, totalGasto: 0, saldo: 999 }));
    expect(screen.getByLabelText("Total recebido acumulado")).toHaveValue("300.00");
    expect(toast.success).not.toHaveBeenCalled();
  });
  it("reinicia resumo ao trocar obra e ignora leitura atrasada", async () => {
    let concluir!: (valor: []) => void;
    vi.mocked(financeiroService.resumo).mockReturnValueOnce(new Promise((resolve) => { concluir = resolve; }));
    const { rerender } = render(<ResumoReembolsos obraId={1} />);
    vi.mocked(financeiroService.resumo).mockResolvedValue([{ funcionarioId: 3, funcionarioNome: "Pessoa da segunda obra", valorTotal: 20, totalPago: 0, saldoPendente: 20 }]);
    rerender(<ResumoReembolsos obraId={2} />);
    await screen.findByText("Pessoa da segunda obra");
    await act(async () => concluir([]));
    expect(screen.getByText("Pessoa da segunda obra")).toBeInTheDocument();
    let rejeitar!: (erro: Error) => void;
    vi.mocked(financeiroService.resumo).mockReturnValue(new Promise((_, reject) => { rejeitar = reject; }));
    rerender(<ResumoReembolsos obraId={3} />);
    expect(screen.getByRole("status")).toHaveTextContent("Carregando resumo");
    expect(screen.queryByText("Pessoa da segunda obra")).not.toBeInTheDocument();
    await act(async () => rejeitar(new Error("Falha")));
    expect(screen.getByRole("alert")).toBeInTheDocument();
  });
  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("não mostra nem consulta financeiro como %s", (cargo) => {
    validarSessao(cargo);
    const { container } = render(<FinanceiroObra obraId={1} orcamento="999" />);
    expect(container).toBeEmptyDOMElement();
    expect(financeiroService.buscar).not.toHaveBeenCalled();
  });
  it("separa orçamento/recebido, mostra saldo negativo e salva zero", async () => {
    render(<FinanceiroObra obraId={1} orcamento="500" />);
    const input = await screen.findByLabelText("Total recebido acumulado");
    expect(screen.getByText("Orçamento planejado (separado)")).toBeInTheDocument();
    expect(screen.getByText(/-.*50,00/)).toBeInTheDocument();
    fireEvent.change(input, { target: { value: "0,00" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar total recebido" }));
    await waitFor(() => expect(financeiroService.atualizar).toHaveBeenCalledWith(1, 0));
  });
  it("não envia recebido negativo ou com três casas", async () => {
    render(<FinanceiroObra obraId={1} />);
    const input = await screen.findByLabelText("Total recebido acumulado");
    for (const valor of ["-1", "1.001"]) {
      fireEvent.change(input, { target: { value: valor } });
      fireEvent.click(screen.getByRole("button", { name: "Salvar total recebido" }));
    }
    expect(financeiroService.atualizar).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledTimes(2);
  });
  it("exibe falha de leitura sem inventar saldo zero", async () => {
    vi.mocked(financeiroService.buscar).mockRejectedValue(new Error("Falha"));
    render(<FinanceiroObra obraId={1} />);
    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível carregar");
    expect(screen.queryByLabelText("Total recebido acumulado")).not.toBeInTheDocument();
  });
  it("exibe resumo monetário por pessoa vindo da API", async () => {
    vi.mocked(financeiroService.resumo).mockResolvedValue([{ funcionarioId: 2, funcionarioNome: "Pessoa A", valorTotal: 100, totalPago: 30, saldoPendente: 70 }]);
    render(<ResumoReembolsos obraId={1} />);
    expect(await screen.findByText("Pessoa A")).toBeInTheDocument();
    expect(screen.getByText(/70,00/)).toBeInTheDocument();
    expect(financeiroService.resumo).toHaveBeenCalledWith(1);
  });
});