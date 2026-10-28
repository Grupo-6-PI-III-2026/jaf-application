import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { UserProvider } from "./UserContext";
import { useUser } from "./useUser";
import PrivateRoute from "../Components/PrivateRoute/PrivateRoute";
import { authService } from "../Service/Auth/Login/authService";
import api from "../Service/Auth/Login/Api/Api";
import type { FuncionarioResponseDto } from "../Types/user";
import { validarSessao } from "../test/authorization";

vi.mock("../Service/Auth/Login/Api/Api", () => ({ default: { get: vi.fn(), post: vi.fn() } }));

const perfil: FuncionarioResponseDto = { id: 42, email: "engenheiro@exemplo.com", nome: "Engenheiro", cargo: "ENGENHEIRO", fotoUrl: null };
const acesso = { funcionarioId: perfil.id, cargo: perfil.cargo, permissoes: ["VISUALIZAR_PRESENCAS", "DELETAR_OBRA"] };

function Controle() {
  const { refreshUser, clearUser, user } = useUser();
  return <>
    <button onClick={() => void refreshUser()}>Revalidar</button>
    <button onClick={clearUser}>Limpar</button>
    <span aria-label="Usuário validado">{user?.nome ?? "nenhum"}</span>
    <PrivateRoute>
      <div>Conteúdo protegido</div>
      {authService.hasAuthority("DELETAR_OBRA") && <button>Excluir obra customizado</button>}
      {authService.hasAuthority("REGISTRAR_PRESENCA") && <button>Criar presença</button>}
    </PrivateRoute>
  </>;
}
const montar = () => render(<MemoryRouter><UserProvider><Controle /></UserProvider></MemoryRouter>);

beforeEach(() => {
  vi.resetAllMocks();
  localStorage.clear();
  authService.clearAuthorization();
  localStorage.setItem("userEmail", perfil.email);
  localStorage.setItem("userId", String(perfil.id));
  // Um cargo forjado no armazenamento não deve liberar ADMIN.
  localStorage.setItem("userCargo", "ADMIN");
  vi.mocked(api.get).mockResolvedValueOnce({ data: perfil }).mockResolvedValueOnce({ data: acesso });
});

describe("validação de perfil e permissões antes das rotas", () => {
  it("carrega /me e depois /me/permissoes; bloqueia enquanto pendente e usa customizações", async () => {
    let concluir!: (response: { data: typeof acesso }) => void;
    vi.mocked(api.get).mockReset().mockResolvedValueOnce({ data: perfil })
      .mockReturnValueOnce(new Promise((resolve) => { concluir = resolve; }));
    montar();
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2));
    expect(api.get).toHaveBeenNthCalledWith(1, "/funcionarios/me");
    expect(api.get).toHaveBeenNthCalledWith(2, "/funcionarios/me/permissoes");
    expect(screen.getByRole("status")).toHaveTextContent("Validando acesso");
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(authService.isAdmin()).toBe(false);
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
    await act(async () => concluir({ data: acesso }));
    expect(await screen.findByText("Conteúdo protegido")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Excluir obra customizado" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Criar presença" })).not.toBeInTheDocument();
    expect(authService.getCargo()).toBe("ENGENHEIRO");
    for (let i = 0; i < 20; i += 1) expect(authService.hasAuthority("DELETAR_OBRA")).toBe(true);
    expect(api.get).toHaveBeenCalledTimes(2);
  });

  it("sessão expirada (401) encerra a sessão local em vez de oferecer nova tentativa", async () => {
    vi.mocked(api.get).mockReset().mockRejectedValueOnce({ response: { status: 401 } });
    montar();
    await waitFor(() => expect(localStorage.getItem("userEmail")).toBeNull());
    expect(screen.queryByRole("button", { name: "Tentar novamente" })).not.toBeInTheDocument();
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(authService.isAuthenticated()).toBe(false);
  });

  it.each([403, 500])("falha %s em /me/permissoes é fail-closed e retry da PrivateRoute funciona", async (status) => {
    validarSessao("ADMIN", ["CRIAR_OBRA"], perfil.id);
    vi.mocked(api.get).mockReset().mockResolvedValueOnce({ data: perfil })
      .mockRejectedValueOnce({ response: { status } });
    montar();
    await screen.findByRole("alert");
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Usuário validado")).toHaveTextContent("nenhum");
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.hasAuthority("REGISTRAR_PRESENCA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
    vi.mocked(api.get).mockResolvedValueOnce({ data: perfil }).mockResolvedValueOnce({ data: acesso });
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(await screen.findByText("Conteúdo protegido")).toBeInTheDocument();
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(true);
    expect(api.get).toHaveBeenCalledTimes(4);
  });

  it("falha de /me não consulta permissões e limpa a sessão validada anterior", async () => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    vi.mocked(api.get).mockReset().mockRejectedValue(new Error("Perfil inválido"));
    montar();
    await screen.findByRole("alert");
    expect(api.get).toHaveBeenCalledTimes(1);
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });

  it.each([
    { ...acesso, funcionarioId: 99 },
    { ...acesso, cargo: "ADMIN" },
    { ...acesso, permissoes: null },
    { ...acesso, permissoes: [1] },
  ])("recusa resposta incompatível ou inválida: %j", async (invalido) => {
    vi.mocked(api.get).mockReset().mockResolvedValueOnce({ data: perfil }).mockResolvedValueOnce({ data: invalido });
    montar();
    await screen.findByRole("alert");
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });

  it("refresh limpa imediatamente permissões antigas e não recupera defaults ao falhar", async () => {
    montar();
    await screen.findByText("Conteúdo protegido");
    let rejeitar!: (error: Error) => void;
    vi.mocked(api.get).mockReturnValueOnce(new Promise((_, reject) => { rejeitar = reject; }));
    fireEvent.click(screen.getByRole("button", { name: "Revalidar" }));
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    await act(async () => rejeitar(new Error("Falha")));
    await screen.findByRole("alert");
    expect(authService.hasAuthority("REGISTRAR_PRESENCA")).toBe(false);
  });

  it("clearUser limpa memória e ignora resposta atrasada de validação", async () => {
    let concluir!: (response: { data: typeof acesso }) => void;
    vi.mocked(api.get).mockReset().mockResolvedValueOnce({ data: perfil })
      .mockReturnValueOnce(new Promise((resolve) => { concluir = resolve; }));
    montar();
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2));
    fireEvent.click(screen.getByRole("button", { name: "Limpar" }));
    await act(async () => concluir({ data: acesso }));
    expect(screen.queryByText("Conteúdo protegido")).not.toBeInTheDocument();
    expect(screen.getByLabelText("Usuário validado")).toHaveTextContent("nenhum");
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
  });

  it("não aplica resposta da sessão anterior depois de um novo login", async () => {
    let concluir!: (response: { data: typeof acesso }) => void;
    vi.mocked(api.get).mockReset().mockResolvedValueOnce({ data: perfil })
      .mockReturnValueOnce(new Promise((resolve) => { concluir = resolve; }));
    montar();
    await waitFor(() => expect(api.get).toHaveBeenCalledTimes(2));
    const novo = { ...perfil, id: 7, nome: "Outro usuário", email: "outro@exemplo.com" };
    vi.mocked(api.post).mockResolvedValue({ data: novo });
    await act(async () => { await authService.login({ email: novo.email, senha: "senha" }); });
    vi.mocked(api.get).mockResolvedValueOnce({ data: novo })
      .mockResolvedValueOnce({ data: { funcionarioId: novo.id, cargo: novo.cargo, permissoes: [] } });
    fireEvent.click(screen.getByRole("button", { name: "Revalidar" }));
    await screen.findByText("Outro usuário");
    await act(async () => concluir({ data: acesso }));
    expect(screen.getByLabelText("Usuário validado")).toHaveTextContent("Outro usuário");
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
  });
});