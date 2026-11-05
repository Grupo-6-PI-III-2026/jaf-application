import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { toast } from "sonner";
import NovaObra from "./NovaObra";
import { validarSessao } from "../../../test/authorization";
import { obraService, type Obra } from "../../../Service/Obras/obraService";
import { funcionarioService } from "../../../Service/Funcionarios/funcionarioService";

const { navigate } = vi.hoisted(() => ({ navigate: vi.fn() }));
vi.mock("react-router-dom", () => ({ useNavigate: () => navigate }));
vi.mock("../../../Service/Obras/obraService", () => ({ obraService: { criar: vi.fn() } }));
vi.mock("../../../Service/Funcionarios/funcionarioService", () => ({ funcionarioService: { listarAlocaveis: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() } }));

const obra: Obra = {
  id: 42, titulo: "Obra planejada", orcamento: "1250.5", status: "PLANEJADA",
  dtInicio: "2099-01-01", dtTerminoPrevisto: "2099-12-31",
};

beforeEach(() => {
  vi.resetAllMocks();
  validarSessao("ENGENHEIRO", ["CRIAR_OBRA", "CRIAR_ALOCACAO"]);
  vi.mocked(funcionarioService.listarAlocaveis).mockResolvedValue([
    { id: 7, nome: "Pessoa responsável", cargo: "ENGENHEIRO" },
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
    expect(screen.getByText("R$")).toBeInTheDocument();
  });

  it("aceita início futuro e envia o responsável na mesma chamada de criação", async () => {
    await renderComResponsavel();
    expect(screen.getByLabelText("ORÇAMENTO PLANEJADO")).toHaveValue("1.250,50");
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/home"));
    expect(funcionarioService.listarAlocaveis).toHaveBeenCalledTimes(1);
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(obraService.criar).toHaveBeenCalledWith({
      titulo: obra.titulo, orcamento: "1250.5", status: obra.status,
      dtInicio: obra.dtInicio, dtTerminoPrevisto: obra.dtTerminoPrevisto, responsavelId: 7,
    }, expect.any(String));
    expect(toast.error).not.toHaveBeenCalled();
  });

  it("aplica a máscara ao valor digitado com ponto decimal", async () => {
    await renderComResponsavel();
    const orcamento = screen.getByLabelText("ORÇAMENTO PLANEJADO");
    fireEvent.change(orcamento, { target: { value: "1250.5" } });
    expect(orcamento).toHaveValue("1.250,5");
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
    expect(funcionarioService.listarAlocaveis).not.toHaveBeenCalled();
    expect(obraService.criar).toHaveBeenCalledWith(expect.not.objectContaining({ responsavelId: expect.anything() }), expect.any(String));
    expect(toast.success).toHaveBeenCalledWith(expect.stringMatching(/Solicite a um usuário com permissão de alocação/));
  });

  it("bloqueia submits simultâneos durante a criação", async () => {
    let concluirCriacao!: (value: Obra) => void;
    vi.mocked(obraService.criar).mockReturnValue(new Promise((resolve) => { concluirCriacao = resolve; }));
    await renderComResponsavel();
    const form = screen.getByRole("form", { name: "Criar obra" });
    act(() => { fireEvent.submit(form); fireEvent.submit(form); });
    expect(obraService.criar).toHaveBeenCalledTimes(1);
    expect(screen.getByRole("button", { name: "Criando..." })).toBeDisabled();
    await act(async () => { concluirCriacao(obra); });
    expect(obraService.criar).toHaveBeenCalledTimes(1);
  });

  it("não silencia erro de criação e reutiliza a chave de idempotência na nova tentativa", async () => {
    vi.mocked(obraService.criar).mockRejectedValueOnce({ isAxiosError: true, response: { status: 400, data: { message: "Título inválido" } } });
    await renderComResponsavel();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(toast.error).toHaveBeenCalledWith("Título inválido"));
    expect(navigate).not.toHaveBeenCalled();
    fireEvent.submit(screen.getByRole("form", { name: "Criar obra" }));
    await waitFor(() => expect(navigate).toHaveBeenCalledWith("/home"));
    expect(obraService.criar).toHaveBeenCalledTimes(2);
    const chaves = vi.mocked(obraService.criar).mock.calls.map((chamada) => chamada[1]);
    expect(chaves[0]).toBe(chaves[1]);
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