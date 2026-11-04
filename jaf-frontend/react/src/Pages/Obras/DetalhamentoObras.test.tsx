import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DetalhamentoObras from "./DetalhamentoObras";
import { obraService } from "../../Service/Obras/obraService";
import { gastoService } from "../../Service/Gastos/gastoService";
import { alocacaoService } from "../../Service/Alocacoes/alocacaoService";
import { validarSessao } from "../../test/authorization";
import { toast } from "sonner";
import { DialogProvider } from "../../Components/Feedback/DialogProvider";

vi.mock("../../Service/Obras/obraService", () => ({ obraService: { buscarPorId: vi.fn(), listar: vi.fn(), atualizar: vi.fn(), atualizarParcial: vi.fn(), listarPessoas: vi.fn(), deletar: vi.fn() } }));
vi.mock("../../Service/Gastos/gastoService", () => ({ gastoService: { listarPaginado: vi.fn(), criar: vi.fn(), atualizar: vi.fn(), deletar: vi.fn() } }));
vi.mock("../../Service/Alocacoes/alocacaoService", () => ({ alocacaoService: { listarPorObra: vi.fn() } }));
vi.mock("../../Components/Financeiro/FinanceiroObra", () => ({ default: () => <div>Financeiro restrito</div> }));
vi.mock("../../Components/Financeiro/ResumoReembolsos", () => ({ default: () => <div>Resumo por pessoa</div> }));
vi.mock("../../Components/Responsive/Responsive", () => ({ Responsive: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));
const funcionario = { id: 2, nome: "Pessoa A", email: "", cargoGlobal: "ENGENHEIRO", fotoUrl: null };
const obra = { id: 1, titulo: "Obra de teste", status: "EM_ANDAMENTO", dtInicio: "2020-01-01", dtTerminoPrevisto: "2030-01-01" };
const gasto = { id: 4, descricao: "Nota fiscal paga", categoria: "MATERIAL", metodoPagamento: "REEMBOLSO", etapa: "ETAPA 1", valor: 100, dtGasto: "2020-01-01", funcionario, obra, statusReembolso: "PAGO" as const, saldoPendente: 0 };
const pagina = (content: unknown[], totalElements = content.length) =>
  ({ content, number: 0, size: 5, totalElements, totalPages: Math.max(1, Math.ceil(totalElements / 5)) }) as never;
const abrir = () => render(
  <DialogProvider>
    <MemoryRouter initialEntries={["/obras/detalhamento/1"]}>
      <Routes><Route path="/obras/detalhamento/:id" element={<DetalhamentoObras />} /></Routes>
    </MemoryRouter>
  </DialogProvider>,
);
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); validarSessao("ENGENHEIRO", ["CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS", "VISUALIZAR_ALOCACOES"]);
  vi.mocked(obraService.buscarPorId).mockResolvedValue(obra);
  vi.mocked(gastoService.listarPaginado).mockResolvedValue(pagina([gasto]));
  vi.mocked(obraService.listarPessoas).mockResolvedValue([{ id: 2, nome: "Pessoa A", funcaoNaObra: "ENGENHEIRO" }]);
  vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([{ id: 3, funcionario, obra, cargo: "ENGENHEIRO" }]);
  vi.mocked(gastoService.criar).mockResolvedValue(gasto);
  vi.mocked(gastoService.atualizar).mockResolvedValue(gasto);
});
describe("notinhas na obra", () => {
  it("mantém donos históricos no filtro pessoa mesmo sem alocação e sem resultados filtrados", async () => {
    vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([]);
    vi.mocked(gastoService.listarPaginado).mockImplementation(async (params) => params.statusReembolso ? pagina([]) : pagina([gasto]));
    abrir();
    await screen.findByRole("option", { name: "Pessoa A" });
    fireEvent.change(screen.getByLabelText("Status do reembolso"), { target: { value: "PENDENTE" } });
    await waitFor(() => expect(screen.queryByText("Nota fiscal paga")).not.toBeInTheDocument());
    expect(screen.getByRole("option", { name: "Pessoa A" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Pessoa"), { target: { value: "2" } });
    await waitFor(() => expect(gastoService.listarPaginado).toHaveBeenLastCalledWith(
      expect.objectContaining({ obraId: 1, funcionarioId: 2, statusReembolso: "PENDENTE" }), expect.anything()));
  });  it("não expõe financeiro, orçamento ou links de dashboard ao engenheiro", async () => {
    abrir(); await screen.findByText("Gastos");
    expect(screen.queryByText("Financeiro restrito")).not.toBeInTheDocument();
    expect(screen.queryByText("Ver estatísticas")).not.toBeInTheDocument();
    expect(screen.queryByText("CUSTO TOTAL ACUMULADO")).not.toBeInTheDocument();
    expect(screen.getByText("Resumo por pessoa")).toBeInTheDocument();
    expect(await screen.findAllByRole("button", { name: "Ver pagamentos" })).toHaveLength(2);
  });
  it("envia filtros pessoa e status para a API", async () => {
    abrir(); await screen.findByText("Gastos");
    await screen.findByRole("option", { name: "Pessoa A" });
    fireEvent.change(screen.getByLabelText("Pessoa"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("Status do reembolso"), { target: { value: "PARCIAL" } });
    await waitFor(() => expect(gastoService.listarPaginado).toHaveBeenLastCalledWith(
      expect.objectContaining({ obraId: 1, funcionarioId: 2, statusReembolso: "PARCIAL", page: 0, size: 5 }), expect.anything()));
  });
  it("filtro de método REEMBOLSO é enviado ao servidor e mantém gastos pagos (não usa flag antiga)", async () => {
    abrir(); await screen.findByText("Gastos");
    const option = screen.getByRole("option", { name: "Todos os pagamentos" });
    fireEvent.change(option.parentElement!, { target: { value: "REEMBOLSO" } });
    await waitFor(() => expect(gastoService.listarPaginado).toHaveBeenLastCalledWith(
      expect.objectContaining({ metodoPagamento: "REEMBOLSO" }), expect.anything()));
    expect(await screen.findAllByText("Nota fiscal paga")).toHaveLength(2);
  });
  it("usa o total real do servidor e pede a página escolhida", async () => {
    vi.mocked(gastoService.listarPaginado).mockImplementation(async (params) =>
      pagina(params.page === 0 ? [gasto] : [{ ...gasto, id: 9, descricao: "Segunda página" }], 12));
    abrir(); await screen.findByText("Gastos");
    expect(await screen.findByText(/Mostrando 1 de 12 lançamentos/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "2" }));
    await waitFor(() => expect(gastoService.listarPaginado).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 1 }), expect.anything()));
    expect((await screen.findAllByText("Segunda página")).length).toBeGreaterThan(0);
  });
  it("envia a ordenação ao servidor", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click(screen.getByRole("button", { name: "Valor" }));
    await waitFor(() => expect(gastoService.listarPaginado).toHaveBeenLastCalledWith(
      expect.objectContaining({ sortBy: "valor", sortDir: "desc" }), expect.anything()));
  });
  it("cria reembolso com método correto sem enviar flag de quitação", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar gasto" }));
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Compra de cimento" } });
    fireEvent.change(screen.getByLabelText("Valor"), { target: { value: "10,25" } });
    fireEvent.click(screen.getByRole("checkbox", { name: /Reembolso ao funcionário/ }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.criar).toHaveBeenCalledWith(expect.objectContaining({ valor: 10.25, metodoPagamento: "REEMBOLSO", obraId: 1, funcionarioId: 2 }), expect.any(String)));
    expect(gastoService.criar).not.toHaveBeenCalledWith(expect.objectContaining({ reembolsoConcluido: expect.anything() }));
  });
  it("mantém edição do gasto disponível sem alternar quitação", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click((await screen.findAllByRole("button", { name: "Editar gasto" }))[0]);
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Descrição revisada" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.atualizar).toHaveBeenCalledWith(4, expect.objectContaining({ descricao: "Descrição revisada", metodoPagamento: "REEMBOLSO" })));
  });
  it("mostra sempre o selo e o saldo pendente do reembolso, sem filtro", async () => {
    vi.mocked(gastoService.listarPaginado).mockResolvedValue(pagina([
      { ...gasto, id: 5, descricao: "Nota parcial", statusReembolso: "PARCIAL" as const, totalPago: 40, saldoPendente: 60 },
    ]));
    abrir(); await screen.findByText("Gastos");
    expect((await screen.findAllByText("Parcial")).length).toBeGreaterThan(0);
    expect((await screen.findAllByText(/Falta/)).length).toBeGreaterThan(0);
  });
  it("permite ao responsável administrativo editar a obra sem orçamento (PATCH)", async () => {
    validarSessao("RESPONSAVEL_ADMINISTRATIVO", ["EDITAR_OBRA", "VISUALIZAR_GASTOS", "VISUALIZAR_ALOCACOES"]);
    vi.mocked(obraService.atualizarParcial).mockResolvedValue({ ...obra, titulo: "Obra renomeada" });
    abrir(); await screen.findByText("Gastos");
    fireEvent.click(await screen.findByRole("button", { name: "Editar obra" }));
    expect(screen.queryByLabelText("Orçamento")).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Título"), { target: { value: "Obra renomeada" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar obra" }));
    await waitFor(() => expect(obraService.atualizarParcial).toHaveBeenCalledWith(1, expect.not.objectContaining({ orcamento: expect.anything() })));
    expect(obraService.atualizar).not.toHaveBeenCalled();
  });
  it("exibe a mensagem da API ao impedir exclusão por alocações vinculadas", async () => {
    validarSessao("RESPONSAVEL_ADMINISTRATIVO", ["DELETAR_OBRA", "VISUALIZAR_GASTOS", "VISUALIZAR_ALOCACOES"]);
    vi.mocked(obraService.deletar).mockRejectedValueOnce({
      isAxiosError: true,
      response: {
        status: 409,
        data: { message: "Não é possível excluir esta obra pois existem alocações vinculadas a ela. Remova as alocações antes de excluir a obra." },
      },
    });

    abrir();
    fireEvent.click(await screen.findByRole("button", { name: "Excluir obra" }));
    fireEvent.click(screen.getByRole("button", { name: "Confirmar exclusão" }));

    await waitFor(() => expect(toast.error).toHaveBeenCalledTimes(1));
    expect(toast.error).toHaveBeenCalledWith("Não é possível excluir esta obra pois existem alocações vinculadas a ela. Remova as alocações antes de excluir a obra.");
  });
  it("lista vazia de obras não quebra acesso sem id", async () => {
    vi.mocked(obraService.listar).mockResolvedValue([]);
    render(<DialogProvider><MemoryRouter><DetalhamentoObras /></MemoryRouter></DialogProvider>);
    expect(await screen.findByText("Obra não encontrada.")).toBeInTheDocument();
  });
});