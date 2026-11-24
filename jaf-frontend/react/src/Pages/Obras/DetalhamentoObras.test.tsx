import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import DetalhamentoObras from "./DetalhamentoObras";
import { obraService } from "../../Service/Obras/obraService";
import { gastoService } from "../../Service/Gastos/gastoService";
import { alocacaoService } from "../../Service/Alocacoes/alocacaoService";
import { ocrService } from "../../Service/Gastos/ocrService";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Service/Obras/obraService", () => ({ obraService: { buscarPorId: vi.fn(), listar: vi.fn() } }));
vi.mock("../../Service/Gastos/gastoService", () => ({ gastoService: { listarPorObra: vi.fn(), criar: vi.fn(), atualizar: vi.fn(), deletar: vi.fn(), anexarNotaFiscal: vi.fn(), obterUrlNotaFiscal: vi.fn(), removerNotaFiscal: vi.fn() } }));
vi.mock("../../Service/Gastos/ocrService", () => ({ ocrService: { processarNotaFiscal: vi.fn() } }));
vi.mock("../../Service/Alocacoes/alocacaoService", () => ({ alocacaoService: { listarPorObra: vi.fn() } }));
vi.mock("../../Components/Financeiro/FinanceiroObra", () => ({ default: () => <div>Financeiro restrito</div> }));
vi.mock("../../Components/Financeiro/ResumoReembolsos", () => ({ default: () => <div>Resumo por pessoa</div> }));
vi.mock("../../Components/Responsive/Responsive", () => ({ Responsive: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }));
const funcionario = { id: 2, nome: "Pessoa A", email: "", cargoGlobal: "ENGENHEIRO", fotoUrl: null };
const obra = { id: 1, titulo: "Obra de teste", status: "EM_ANDAMENTO", dtInicio: "2020-01-01", dtTerminoPrevisto: "2030-01-01" };
const gasto = { id: 4, descricao: "Nota fiscal paga", categoria: "MATERIAL", metodoPagamento: "REEMBOLSO", etapa: "ETAPA 1", valor: 100, dtGasto: "2020-01-01", funcionario, obra, reembolsoConcluido: true };
const abrir = () => render(<MemoryRouter initialEntries={["/obras/detalhamento/1"]}><Routes><Route path="/obras/detalhamento/:id" element={<DetalhamentoObras />} /></Routes></MemoryRouter>);
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear(); validarSessao("ENGENHEIRO", ["CRIAR_GASTO", "EDITAR_GASTO", "VISUALIZAR_GASTOS", "VISUALIZAR_ALOCACOES"]);
  vi.mocked(obraService.buscarPorId).mockResolvedValue(obra);
  vi.mocked(gastoService.listarPorObra).mockResolvedValue([gasto]);
  vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([{ id: 3, funcionario, obra, cargo: "ENGENHEIRO" }]);
  vi.mocked(gastoService.criar).mockResolvedValue(gasto);
  vi.mocked(gastoService.atualizar).mockResolvedValue(gasto);
});
describe("notinhas na obra", () => {
  it("mantém donos históricos no filtro pessoa mesmo sem alocação e sem resultados filtrados", async () => {
    vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([]);
    vi.mocked(gastoService.listarPorObra).mockImplementation(async (_id, filtros) => filtros?.statusReembolso ? [] : [gasto]);
    abrir();
    await screen.findByRole("option", { name: "Pessoa A" });
    fireEvent.change(screen.getByLabelText("Status do reembolso"), { target: { value: "PENDENTE" } });
    await waitFor(() => expect(screen.queryByText("Nota fiscal paga")).not.toBeInTheDocument());
    expect(screen.getByRole("option", { name: "Pessoa A" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Pessoa"), { target: { value: "2" } });
    await waitFor(() => expect(gastoService.listarPorObra).toHaveBeenLastCalledWith(1, { funcionarioId: 2, statusReembolso: "PENDENTE" }));
  });
  it("não expõe financeiro, orçamento ou links de dashboard ao engenheiro", async () => {
    abrir(); await screen.findByText("Gastos");
    expect(screen.queryByText("Financeiro restrito")).not.toBeInTheDocument();
    expect(screen.queryByText("Ver estatísticas")).not.toBeInTheDocument();
    expect(screen.queryByText("CUSTO TOTAL ACUMULADO")).not.toBeInTheDocument();
    expect(screen.getByText("Resumo por pessoa")).toBeInTheDocument();
    expect(await screen.findAllByRole("button", { name: "Ver pagamentos" })).toHaveLength(2);
  });
  it("envia filtros pessoa e status para a API", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.change(screen.getByLabelText("Pessoa"), { target: { value: "2" } });
    fireEvent.change(screen.getByLabelText("Status do reembolso"), { target: { value: "PARCIAL" } });
    await waitFor(() => expect(gastoService.listarPorObra).toHaveBeenLastCalledWith(1, { funcionarioId: 2, statusReembolso: "PARCIAL" }));
  });
  it("filtro de método REEMBOLSO mantém gastos pagos (não usa flag antiga)", async () => {
    abrir(); await screen.findByText("Gastos");
    const option = screen.getByRole("option", { name: "Todos os pagamentos" });
    fireEvent.change(option.parentElement!, { target: { value: "REEMBOLSO" } });
    expect(await screen.findAllByText("Nota fiscal paga")).toHaveLength(2);
  });
  it("cria reembolso com método correto sem enviar flag de quitação", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar gasto" }));
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Compra de cimento" } });
    fireEvent.change(screen.getByLabelText("Valor"), { target: { value: "10,25" } });
    fireEvent.click(screen.getByRole("checkbox", { name: /Reembolso ao funcionário/ }));
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.criar).toHaveBeenCalledWith(expect.objectContaining({ valor: 10.25, metodoPagamento: "REEMBOLSO", obraId: 1, funcionarioId: 2 })));
    expect(gastoService.criar).not.toHaveBeenCalledWith(expect.objectContaining({ reembolsoConcluido: expect.anything() }));
  });
  it("mantém edição do gasto disponível sem alternar quitação", async () => {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click((await screen.findAllByRole("button", { name: "Editar gasto" }))[0]);
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Descrição revisada" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.atualizar).toHaveBeenCalledWith(4, expect.objectContaining({ descricao: "Descrição revisada", metodoPagamento: "REEMBOLSO" })));
  });
  it("lista vazia de obras não quebra acesso sem id", async () => {
    vi.mocked(obraService.listar).mockResolvedValue([]);
    render(<MemoryRouter><DetalhamentoObras /></MemoryRouter>);
    expect(await screen.findByText("Obra não encontrada.")).toBeInTheDocument();
  });
});

describe("notas fiscais dos gastos", () => {
  const nota = new File(["%PDF-1.7"], "nota.pdf", { type: "application/pdf" });

  async function abrirNovoGasto() {
    abrir(); await screen.findByText("Gastos");
    fireEvent.click(screen.getByRole("button", { name: "Adicionar gasto" }));
    fireEvent.change(screen.getByLabelText("Escolher arquivo"), { target: { files: [nota] } });
  }

  it("anexa a nota lida pelo OCR enviando o jobId junto com o gasto", async () => {
    vi.mocked(ocrService.processarNotaFiscal).mockResolvedValue({ jobId: "job-9", status: "CONCLUIDO", sucesso: true, mensagem: "Lida", valor: 50 });
    await abrirNovoGasto();
    fireEvent.click(screen.getByRole("button", { name: "Extrair campos" }));
    await waitFor(() => expect(screen.getByLabelText("Valor")).toHaveValue("50,00"));
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Cimento" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.criar).toHaveBeenCalledWith(expect.objectContaining({ ocrJobId: "job-9" })));
    expect(gastoService.anexarNotaFiscal).not.toHaveBeenCalled();
  });

  it("anexa o arquivo diretamente quando a nota não passou pelo OCR", async () => {
    await abrirNovoGasto();
    fireEvent.change(screen.getByLabelText("Descrição"), { target: { value: "Cimento" } });
    fireEvent.change(screen.getByLabelText("Valor"), { target: { value: "10,00" } });
    fireEvent.click(screen.getByRole("button", { name: "Salvar gasto" }));
    await waitFor(() => expect(gastoService.anexarNotaFiscal).toHaveBeenCalledWith(4, nota));
    expect(gastoService.criar).not.toHaveBeenCalledWith(expect.objectContaining({ ocrJobId: expect.anything() }));
  });

  it("abre a nota anexada em nova aba com a URL temporária", async () => {
    vi.mocked(gastoService.listarPorObra).mockResolvedValue([{ ...gasto, possuiNotaFiscal: true }]);
    vi.mocked(gastoService.obterUrlNotaFiscal).mockResolvedValue("https://bucket/nota.pdf?assinatura");
    const aba = { opener: {} as unknown, location: { href: "" }, close: vi.fn() };
    const open = vi.spyOn(window, "open").mockReturnValue(aba as unknown as Window);
    abrir();
    fireEvent.click((await screen.findAllByRole("button", { name: "Ver nota fiscal" }))[0]);
    await waitFor(() => expect(aba.location.href).toBe("https://bucket/nota.pdf?assinatura"));
    expect(aba.opener).toBeNull();
    expect(gastoService.obterUrlNotaFiscal).toHaveBeenCalledWith(4);
    open.mockRestore();
  });

  it("gasto sem nota não mostra botão de visualização", async () => {
    abrir(); await screen.findAllByText("Nota fiscal paga");
    expect(screen.queryByRole("button", { name: "Ver nota fiscal" })).not.toBeInTheDocument();
  });
});