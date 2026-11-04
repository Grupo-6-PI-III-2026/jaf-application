import { fireEvent, render, screen, within } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Home from "./Home";
import { obraService, type ObraResumo } from "../../Service/Obras/obraService";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Obras/obraService", () => ({ obraService: { resumo: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

function RotaAtual() {
  return <output aria-label="Rota atual">{useLocation().pathname}</output>;
}

const obra = (sobrescritas: Partial<ObraResumo> = {}): ObraResumo => ({
  id: 1, titulo: "Obra segura", status: "EM_ANDAMENTO", dtInicio: "2020-01-01", dtTerminoPrevisto: "2030-01-01", ...sobrescritas,
});
const pagina = (content: ObraResumo[], totalElements = content.length) =>
  ({ content, number: 0, size: 100, totalElements, totalPages: 1 });

beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear();
  vi.mocked(obraService.resumo).mockResolvedValue(pagina([obra()]));
});

describe("Home", () => {
  it("permite acessar detalhes de todas as obras, inclusive além das duas recentes", async () => {
    validarSessao("ENGENHEIRO", ["VISUALIZAR_ALOCACOES"]);
    // O servidor já devolve da obra mais recente para a mais antiga.
    vi.mocked(obraService.resumo).mockResolvedValue(pagina(Array.from({ length: 4 }, (_, index) => obra({
      id: index + 1, titulo: `Obra ${index + 1}`, status: index === 0 ? "CONCLUÍDA" : "EM_ANDAMENTO",
      dtInicio: `2020-01-0${index + 1}`,
    })).reverse()));
    render(<MemoryRouter><Home /><RotaAtual /></MemoryRouter>);
    const lista = await screen.findByRole("region", { name: "Todas as obras" });
    expect(within(lista).getAllByRole("button", { name: "Detalhes" })).toHaveLength(4);
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
    expect(screen.getByText("Obras ativas")).toBeInTheDocument();
    // Uma única chamada traz tudo, sem requisições por obra.
    expect(obraService.resumo).toHaveBeenCalledTimes(1);
  });

  it("lista todos os status do sistema e informa quando não há obras com o status", async () => {
    validarSessao("ENGENHEIRO", ["VISUALIZAR_ALOCACOES"]);
    render(<MemoryRouter><Home /></MemoryRouter>);
    const lista = await screen.findByRole("region", { name: "Todas as obras" });
    const select = screen.getByLabelText("Status da obra");
    ["PLANEJADA", "EM ANDAMENTO", "CONCLUIDA", "CANCELADA"].forEach((rotulo) =>
      expect(within(select).getByRole("option", { name: rotulo })).toBeInTheDocument());
    fireEvent.change(select, { target: { value: "cancelada" } });
    expect(within(lista).getByText("Nenhuma obra encontrada com esse status.")).toBeInTheDocument();
  });

  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("esconde totais e dashboard para %s", async (cargo) => {
    validarSessao(cargo, ["VISUALIZAR_GASTOS"]);
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect(await screen.findByText("Obra segura")).toBeInTheDocument();
    expect(screen.queryByText("TOTAL GASTO")).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Dashboard" })).not.toBeInTheDocument();
  });

  it("mostra dashboard e o total somado pelo servidor para administrador", async () => {
    validarSessao("ADMIN", ["VISUALIZAR_GASTOS"]);
    vi.mocked(obraService.resumo).mockResolvedValue(pagina([
      obra({ id: 1, totalGasto: 100.1 }), obra({ id: 2, titulo: "Outra obra", totalGasto: 0.2 }),
    ]));
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect((await screen.findAllByRole("button", { name: "Dashboard" }))).toHaveLength(2);
    expect(screen.getByText("R$ 100,30")).toBeInTheDocument();
  });

  it("nunca mostra total parcial: falta o total de uma obra, o valor fica indisponível", async () => {
    validarSessao("ADMIN", ["VISUALIZAR_GASTOS"]);
    vi.mocked(obraService.resumo).mockResolvedValue(pagina([obra({ id: 1, totalGasto: 100 }), obra({ id: 2, titulo: "Outra", totalGasto: null })]));
    render(<MemoryRouter><Home /></MemoryRouter>);
    await screen.findByText("Outra");
    expect(screen.getAllByText("Total indisponível").length).toBeGreaterThan(0);
  });

  it("avisa quando a lista foi cortada e não soma o total de uma lista parcial", async () => {
    validarSessao("ADMIN", ["VISUALIZAR_GASTOS"]);
    vi.mocked(obraService.resumo).mockResolvedValue(pagina([obra({ totalGasto: 50 })], 130));
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect(await screen.findByText(/Exibindo as primeiras 1 de 130 obras/)).toBeInTheDocument();
    expect(screen.getAllByText("Total indisponível").length).toBeGreaterThan(0);
  });

  it("falha de carregamento não vira 'Nenhuma obra encontrada' e permite tentar de novo", async () => {
    validarSessao("ENGENHEIRO", []);
    vi.mocked(obraService.resumo).mockRejectedValueOnce(new Error("Rede")).mockResolvedValue(pagina([obra()]));
    render(<MemoryRouter><Home /></MemoryRouter>);
    expect(await screen.findByRole("alert")).toHaveTextContent("Não foi possível carregar as obras");
    expect(screen.queryByText("Nenhuma obra encontrada.")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByText("Obra segura")).toBeInTheDocument();
  });
});
