import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Home from "./Home";
import { obraService } from "../../Service/Obras/obraService";
import { gastoService } from "../../Service/Gastos/gastoService";
import { alocacaoService } from "../../Service/Alocacoes/alocacaoService";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Obras/obraService", () => ({ obraService: { listar: vi.fn() } }));
vi.mock("../../Service/Gastos/gastoService", () => ({ gastoService: { listarPorObra: vi.fn() } }));
vi.mock("../../Service/Alocacoes/alocacaoService", () => ({ alocacaoService: { listarPorObra: vi.fn() } }));
function RotaAtual() {
  return <output aria-label="Rota atual">{useLocation().pathname}</output>;
}
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear();
  vi.mocked(obraService.listar).mockResolvedValue([{ id: 1, titulo: "Obra segura", orcamento: "999.99", status: "EM_ANDAMENTO", dtInicio: "2020-01-01", dtTerminoPrevisto: "2030-01-01" }]);
  vi.mocked(gastoService.listarPorObra).mockResolvedValue([]);
  vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([]);
});
describe("Home sem vazamento financeiro", () => {
  it("permite acessar detalhes de todas as obras, inclusive além das duas recentes", async () => {
    validarSessao("ENGENHEIRO", ["VISUALIZAR_ALOCACOES"]);
    vi.mocked(obraService.listar).mockResolvedValue(Array.from({ length: 4 }, (_, index) => ({
      id: index + 1, titulo: `Obra ${index + 1}`, status: index === 0 ? "CONCLUÍDA" : "EM_ANDAMENTO",
      dtInicio: `2020-01-0${index + 1}`, dtTerminoPrevisto: "2030-01-01",
    })));
    render(<MemoryRouter><Home /><RotaAtual /></MemoryRouter>);
    const lista = await screen.findByRole("region", { name: "Todas as obras" });
    expect(within(lista).getAllByRole("button", { name: "Detalhes" })).toHaveLength(4);
    // A ordem é da mais recente para a mais antiga; todos os IDs devem ser explícitos.
    within(lista).getAllByRole("button", { name: "Detalhes" }).forEach((botao, index) => {
      fireEvent.click(botao);
      expect(screen.getByLabelText("Rota atual")).toHaveTextContent(`/obras/detalhamento/${4 - index}`);
    });
    fireEvent.change(screen.getByLabelText("Buscar por título"), { target: { value: "  OBRA 1 " } });
    expect(within(lista).getAllByRole("button", { name: "Detalhes" })).toHaveLength(1);
    expect(within(lista).getByText("Obra 1")).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Status da obra"), { target: { value: "em andamento" } });
    expect(within(lista).queryByRole("button", { name: "Detalhes" })).not.toBeInTheDocument();
    expect(within(lista).getByText(/Nenhuma obra corresponde/)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Buscar por título"), { target: { value: "" } });
    expect(within(lista).getAllByRole("button", { name: "Detalhes" })).toHaveLength(3);
    fireEvent.click(screen.getByRole("button", { name: "Limpar filtros" }));
    expect(within(lista).getAllByRole("button", { name: "Detalhes" })).toHaveLength(4);
    // KPIs não devem variar com a busca.
    expect(screen.getByText("Obras ativas")).toBeInTheDocument();
  });
  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("esconde totais e dashboard para %s mesmo com orçamento na resposta", async (cargo) => {
    validarSessao(cargo, ["VISUALIZAR_GASTOS"]);
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect(await screen.findByText("Obra segura")).toBeInTheDocument();
    expect(screen.queryByText("TOTAL GASTO")).not.toBeInTheDocument();
    expect(screen.queryByText(/999,99/)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Dashboard" })).not.toBeInTheDocument();
    expect(gastoService.listarPorObra).not.toHaveBeenCalled();
  });
  it("preserva dashboard e total para administrador", async () => {
    validarSessao("ADMIN", ["VISUALIZAR_GASTOS"]);
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect(await screen.findByRole("button", { name: "Dashboard" })).toBeInTheDocument();
    expect(screen.getAllByText("TOTAL GASTO")).toHaveLength(2);
    expect(gastoService.listarPorObra).toHaveBeenCalledWith(1);
  });
});