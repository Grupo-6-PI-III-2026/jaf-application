import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import ControlePresenca from "./ControlePresenca";
import { presencaService } from "../../Service/Presencas/presencaService";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Presencas/presencaService", () => ({ presencaService: { listarPorObraEData: vi.fn(), criarPresenca: vi.fn(), alternarPresenca: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
const pessoa = { funcionarioId: 2, funcionarioNome: "Pessoa A", funcionarioCargo: "PEDREIRO", data: "2020-01-01", presente: false };
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); validarSessao("ENGENHEIRO", ["REGISTRAR_PRESENCA", "EDITAR_PRESENCA"]);
  vi.mocked(presencaService.listarPorObraEData).mockResolvedValue([pessoa]);
  vi.mocked(presencaService.criarPresenca).mockResolvedValue({});
  vi.mocked(presencaService.alternarPresenca).mockResolvedValue();
});
describe("registro manual de presença", () => {
  it("engenheiro sem REGISTRAR_PRESENCA não vê criar, mesmo que o padrão do cargo permita", async () => {
    validarSessao("ENGENHEIRO", ["VISUALIZAR_PRESENCAS"]);
    render(<ControlePresenca aberto obraId={1} data="2020-01-01" onFechar={vi.fn()} />);
    expect(await screen.findByRole("button", { name: "Presença de Pessoa A" })).toBeDisabled();
    expect(screen.queryByRole("button", { name: "Registrar Presença" })).not.toBeInTheDocument();
    expect(presencaService.criarPresenca).not.toHaveBeenCalled();
  });
  it("só persiste após confirmar e fechar não grava seleção", async () => {
    const fechar = vi.fn();
    render(<ControlePresenca aberto obraId={1} data="2020-01-01" onFechar={fechar} />);
    fireEvent.click(await screen.findByRole("button", { name: "Presença de Pessoa A" }));
    expect(presencaService.criarPresenca).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Fechar controle de presenca" }));
    expect(fechar).toHaveBeenCalledTimes(1);
    expect(presencaService.criarPresenca).not.toHaveBeenCalled();
  });
  it("confirma seleção pessoa/data com payload manual", async () => {
    render(<ControlePresenca aberto obraId={1} data="2020-01-01" onFechar={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: "Presença de Pessoa A" }));
    fireEvent.click(screen.getByRole("button", { name: "Registrar Presença" }));
    await waitFor(() => expect(presencaService.criarPresenca).toHaveBeenCalledWith({ funcionarioId: 2, obraId: 1, data: "2020-01-01", presente: true }));
  });
  it("não permite alterar registro existente sem EDITAR_PRESENCA", async () => {
    validarSessao("ENGENHEIRO", ["VISUALIZAR_PRESENCAS"]);
    vi.mocked(presencaService.listarPorObraEData).mockResolvedValue([{ ...pessoa, id: 7 }]);
    render(<ControlePresenca aberto obraId={1} data="2020-01-01" onFechar={vi.fn()} />);
    expect(await screen.findByRole("button", { name: "Presença de Pessoa A" })).toBeDisabled();
    expect(presencaService.alternarPresenca).not.toHaveBeenCalled();
  });
  it("não registra presença futura", async () => {
    render(<ControlePresenca aberto obraId={1} data="2999-01-01" onFechar={vi.fn()} />);
    expect(await screen.findByRole("button", { name: "Presença de Pessoa A" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Registrar Presença" })).toBeDisabled();
  });
  it("alterna registro existente apenas na confirmação", async () => {
    vi.mocked(presencaService.listarPorObraEData).mockResolvedValue([{ ...pessoa, id: 7 }]);
    render(<ControlePresenca aberto obraId={1} data="2020-01-01" onFechar={vi.fn()} />);
    fireEvent.click(await screen.findByRole("button", { name: "Presença de Pessoa A" }));
    expect(presencaService.alternarPresenca).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Registrar Presença" }));
    await waitFor(() => expect(presencaService.alternarPresenca).toHaveBeenCalledWith(7));
  });
});