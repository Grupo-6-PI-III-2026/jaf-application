import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import NovaObra from "./NovaObra";
import { validarSessao } from "../../../test/authorization";
import { obraService, type Obra } from "../../../Service/Obras/obraService";
import { alocacaoService } from "../../../Service/Alocacoes/alocacaoService";
import { funcionarioService } from "../../../Service/Funcionarios/funcionarioService";

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }));
vi.mock("react-router-dom", () => ({ useNavigate: () => navigate }));
vi.mock("../../../Service/Obras/obraService", () => ({ obraService: { criar: vi.fn() } }));
vi.mock("../../../Service/Alocacoes/alocacaoService", () => ({ alocacaoService: { criar: vi.fn() } }));
vi.mock("../../../Service/Funcionarios/funcionarioService", () => ({ funcionarioService: { listar: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }));

const obra: Obra = {
  id: 42, titulo: "Obra planejada", orcamento: "1250.5", status: "PLANEJADA",
  dtInicio: "2099-01-01", dtTerminoPrevisto: "2099-12-31",
};

beforeEach(() => {
  vi.resetAllMocks();
  validarSessao("ENGENHEIRO", ["CRIAR_OBRA", "CRIAR_ALOCACAO"]);
  vi.mocked(funcionarioService.listar).mockResolvedValue([
    { id: 7, nome: "Pessoa responsável", email: "pessoa@exemplo.com", cargo: "ENGENHEIRO" },
  ]);
  vi.mocked(obraService.criar).mockResolvedValue(obra);
});

function preencher() {
  fireEvent.change(screen.getByLabelText("NOME DA OBRA"), { target: { value: " Obra planejada " } });
  fireEvent.change(screen.getByLabelText("DATA DE INÍCIO"), { target: { value: obra.dtInicio } });
  fireEvent.change(screen.getByLabelText("DATA DE TÉRMINO"), { target: { value: obra.dtTerminoPrevisto } });
  fireEvent.change(screen.getByLabelText("ORÇAMENTO PLANEJADO"), { target: { value: "1.250,50" } });
}

async function renderComResponsavel() {
  render(<NovaObra />);
  await waitFor(() => expect(screen.getByLabelText("RESPONSÁVEL")).toHaveValue("7"));
  preencher();
}

describe("nova obra", () => {
  it("mostra apenas campos persistidos e distingue orçamento de recebido", () => {
    validarSessao("ENGENHEIRO", ["CRIAR_OBRA"]);
    const { container } = render(<NovaObra />);
    expect(screen.getByRole("region", { name: "Informações Gerais" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Financeiro" })).toBeInTheDocument();
    expect(screen.getByText(/Orçamento planejado não é dinheiro recebido/)).toBeInTheDocument();
    expect(screen.queryByText(/Localização|Documentação|OCR|Upload/i)).not.toBeInTheDocument();
    expect(screen.queryByLabelText(/ENDEREÇO|CIDADE|ESTADO/i)).not.toBeInTheDocument();
    expect(container.querySelector('input[type="file"]')).not.toBeInTheDocument();
    expect(container.querySelectorAll("input, select")).toHaveLength(5);
  });

  it("aceita início futuro, envia somente o contrato e aloca com CRIAR_ALOCACAO sem VISUALIZAR_FUNCIONARIOS", async () => {
    await renderComResponsavel();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/home"));
    expect(funcionarioService.listar).toHaveBeenCalledTimes(1);
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(obraService.criar).toHaveBeenCalledWith({
      titulo: obra.titulo, orcamento: "1250.5", status: obra.status,
      dtInicio: obra.dtInicio, dtTerminoPrevisto: obra.dtTerminoPrevisto,
    });
    expect(alocacaoService.criar).toHaveBeenCalledTimes(1);
    expect(alocacaoService.criar).toHaveBeenCalledWith({ funcionarioId: 7, obraId: 42, cargoNaObra: "GESTOR_OBRA" });
    expect(toast.error).not.toHaveBeenCalled();
  });

  it.each([{ permissoes: [] }, { permissoes: ["VISUALIZAR_FUNCIONARIOS"] }])("cria sem responsável quando não pode alocar ($permissoes)", async ({ permissoes }) => {
    validarSessao("ENGENHEIRO", ["CRIAR_OBRA", ...permissoes]);
    render(<NovaObra />);
    preencher();
    expect(screen.queryByLabelText("RESPONSÁVEL")).not.toBeInTheDocument();
    expect(screen.getByText(/Você pode criar a obra sem responsável/)).toBeInTheDocument();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/home"));
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(funcionarioService.listar).not.toHaveBeenCalled();
    expect(alocacaoService.criar).not.toHaveBeenCalled();
    expect(toast.success).toHaveBeenCalledWith(expect.stringMatching(/Solicite a um usuário com permissão de alocação/));
  });

  it("avisa que a obra foi criada quando a alocação falha e nunca repete a criação", async () => {
    vi.mocked(alocacaoService.criar).mockRejectedValue(new Error("Falha na alocação"));
    await renderComResponsavel();
    const form = screen.getByRole("form", { name: "Criar obra" });
    fireEvent.submit(form);
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/obras/42/alocacoes"));
    expect(toast.warning).toHaveBeenCalledWith(expect.stringMatching(/criada.*Finaliz.*não é necessário criar a obra novamente/));
    expect(toast.error).not.toHaveBeenCalled();
    fireEvent.submit(form);
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(alocacaoService.criar).toHaveBeenCalledTimes(1);
  });

  it("bloqueia submits simultâneos durante criação e alocação", async () => {
    let concluirCriacao!: (value: Obra) => void;
    let concluirAlocacao!: () => void;
    vi.mocked(obraService.criar).mockReturnValue(new Promise((resolve) => { concluirCriacao = resolve; }));
    vi.mocked(alocacaoService.criar).mockImplementation(() => new Promise((resolve) => {
      concluirAlocacao = () => resolve({
        id: 1, obra, cargo: "GESTOR_OBRA",
        funcionario: { id: 7, nome: "Pessoa responsável", email: "pessoa@exemplo.com", fotoUrl: null, cargoGlobal: "ENGENHEIRO" },
      });
    }));
    await renderComResponsavel();
    const form = screen.getByRole("form", { name: "Criar obra" });
    act(() => { fireEvent.submit(form); fireEvent.submit(form); });
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Criando..." })).toBeDisabled();
    await act(async () => { concluirCriacao(obra); });
    fireEvent.submit(form);
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(alocacaoService.criar).toHaveBeenCalledTimes(1);
    await act(async () => { concluirAlocacao(); });
    fireEvent.submit(form);
    expect(obraService.criar).toHaveBeenCalledTimes(1);
  });

  it("não silencia erro de criação e permite tentar novamente quando não houve criação confirmada", async () => {
    vi.mocked(obraService.criar).mockRejectedValueOnce({ isAxiosError: true, response: { status: 400, data: { message: "Título inválido" } } });
    await renderComResponsavel();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Título inválido"));
    expect(alocacaoService.criar).not.toHaveBeenCalled();
    expect(navigate).not.toHaveBeenCalled();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/home"));
    expect(obraService.criar).toHaveBeenCalledTimes(2);
  });

  it.each(["", "2099-02-29", "2099-13-01"])("rejeita início inválido %j sem chamar a API", async (inicio) => {
    await renderComResponsavel();
    // Alterar o tipo permite exercitar valores ISO impossíveis que um date input pode sanitizar.
    const input = screen.getByLabelText("DATA DE INÍCIO");
    input.setAttribute("type", "text");
    fireEvent.change(input, { target: { value: inicio } });
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    expect(screen.getByText("Informe uma data de início válida.")).toBeInTheDocument();
    expect(input).toHaveAttribute("aria-invalid", "true");
    expect(obraService.criar).not.toHaveBeenCalled();
  });

  it("rejeita término inválido ou anterior ao início e orçamento inválido", async () => {
    await renderComResponsavel();
    const fim = screen.getByLabelText("DATA DE TÉRMINO");
    fim.setAttribute("type", "text");
    fireEvent.change(fim, { target: { value: "2099-02-30" } });
    fireEvent.change(screen.getByLabelText("ORÇAMENTO PLANEJADO"), { target: { value: "0" } });
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    expect(screen.getByText("Informe uma data de término válida.")).toBeInTheDocument();
    expect(screen.getByText(/Informe um orçamento positivo/)).toBeInTheDocument();
    fireEvent.change(fim, { target: { value: "2098-12-31" } });
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    expect(screen.getByText(/não pode ser anterior/)).toBeInTheDocument();
    expect(obraService.criar).not.toHaveBeenCalled();
  });
});