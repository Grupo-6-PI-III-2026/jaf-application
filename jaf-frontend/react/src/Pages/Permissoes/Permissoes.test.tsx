import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Permissoes from "./Permissoes";
import { permissaoService } from "../../Service/Permissoes/permissaoService";
import { validarSessao } from "../../test/authorization";
import { DialogProvider } from "../../Components/Feedback/DialogProvider";

vi.mock("../../Service/Permissoes/permissaoService", () => ({
  permissaoService: {
    listarFuncionarios: vi.fn(),
    listarPermissoesPorCargo: vi.fn(),
    buscarPermissoesFuncionario: vi.fn(),
    atualizarAcesso: vi.fn(),
  },
}));
vi.mock("../../Service/Funcionarios/funcionarioService", () => ({ funcionarioService: {} }));
vi.mock("../../Context/useUser", () => ({ useUser: () => ({ user: { id: 1, nome: "Admin" } }) }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const engenheiro = { id: 5, nome: "Pessoa Eng", email: "eng@exemplo.com", cargo: "ENGENHEIRO" as const, ativo: true };
const padrao = {
  ADMIN: ["CRIAR_OBRA"],
  RESPONSAVEL_ADMINISTRATIVO: ["CRIAR_OBRA", "VISUALIZAR_OBRA"],
  ENGENHEIRO: ["VISUALIZAR_OBRA"],
};

beforeEach(() => {
  vi.resetAllMocks();
  localStorage.clear();
  validarSessao("ADMIN", []);
  vi.mocked(permissaoService.listarFuncionarios).mockResolvedValue([engenheiro]);
  vi.mocked(permissaoService.listarPermissoesPorCargo).mockResolvedValue(padrao);
  vi.mocked(permissaoService.buscarPermissoesFuncionario).mockResolvedValue({
    funcionarioId: 5, cargo: "ENGENHEIRO", permissoes: ["VISUALIZAR_OBRA"], personalizada: false,
  });
});

async function abrirUsuario() {
  render(<DialogProvider><Permissoes /></DialogProvider>);
  fireEvent.click(await screen.findByText("Pessoa Eng"));
  await screen.findByRole("button", { name: "Responsável Administrativo" });
}

describe("perfis de acesso", () => {
  it("não mostra permissões padrão como se fossem as reais quando a consulta do usuário falha", async () => {
    vi.mocked(permissaoService.buscarPermissoesFuncionario).mockRejectedValue(new Error("falhou"));
    await abrirUsuario();

    expect(await screen.findByText(/Não foi possível carregar as permissões deste usuário/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Tentar novamente" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Salvar/ })).toBeDisabled();
    expect(screen.getByRole("checkbox", { name: "Visualizar Lista de Obras" })).not.toBeChecked();
  });

  it("salva cargo e permissões em uma única chamada de acesso", async () => {
    vi.mocked(permissaoService.atualizarAcesso).mockResolvedValue({
      funcionarioId: 5, cargo: "RESPONSAVEL_ADMINISTRATIVO", permissoes: padrao.RESPONSAVEL_ADMINISTRATIVO, personalizada: false,
    });
    await abrirUsuario();

    fireEvent.click(screen.getByRole("button", { name: "Responsável Administrativo" }));
    fireEvent.click(screen.getByRole("button", { name: /Salvar/ }));

    await waitFor(() => expect(permissaoService.atualizarAcesso).toHaveBeenCalledWith(5, {
      cargo: "RESPONSAVEL_ADMINISTRATIVO", permissoes: expect.arrayContaining(["CRIAR_OBRA", "VISUALIZAR_OBRA"]),
    }));
    expect(permissaoService.atualizarAcesso).toHaveBeenCalledTimes(1);
  });

  it("pede confirmação antes de promover a administrador e respeita a recusa", async () => {
    await abrirUsuario();

    fireEvent.click(screen.getByRole("button", { name: "Administrador" }));

    expect(await screen.findByText(/Promover este usuário a Administrador/)).toBeInTheDocument();
    fireEvent.click(within(screen.getByRole("dialog")).getByRole("button", { name: "Cancelar" }));
    expect(screen.getByRole("button", { name: "Engenheiro" })).toHaveAttribute("aria-pressed", "true");
  });

  it("recarrega o que está salvo quando o salvamento falha", async () => {
    vi.mocked(permissaoService.atualizarAcesso).mockRejectedValue(new Error("falhou"));
    await abrirUsuario();

    fireEvent.click(screen.getByRole("button", { name: "Responsável Administrativo" }));
    fireEvent.click(screen.getByRole("button", { name: /Salvar/ }));

    await waitFor(() => expect(permissaoService.buscarPermissoesFuncionario).toHaveBeenCalledTimes(2));
  });
});
