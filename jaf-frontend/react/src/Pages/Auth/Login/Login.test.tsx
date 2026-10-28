import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import Login from "./Login";
import { authService } from "../../../Service/Auth/Login/authService";
import { toast } from "sonner";

const contexto = vi.hoisted(() => ({ user: null as null | { id: number }, sessao: false }));
vi.mock("../../../Context/useUser", () => ({
  useUser: () => ({ user: contexto.user, error: null, refreshUser: vi.fn().mockResolvedValue(true), clearUser: vi.fn() }),
}));
vi.mock("../../../Service/Auth/Login/authService", () => ({
  authService: { login: vi.fn(), isAuthenticated: () => contexto.sessao, clearSession: vi.fn() },
}));
vi.mock("../../../Components/Responsive/Responsive", () => ({ Responsive: ({ children }: { children: React.ReactNode }) => <>{children}</> }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn(), info: vi.fn() } }));
beforeEach(() => { vi.clearAllMocks(); contexto.user = null; contexto.sessao = false; });
describe("login", () => {
  it("redireciona sessão já validada para /home sem exibir o formulário", () => {
    contexto.sessao = true;
    contexto.user = { id: 1 };
    render(<MemoryRouter><Routes><Route path="/" element={<Login />} /><Route path="/home" element={<div>Obras autorizadas</div>} /></Routes></MemoryRouter>);
    expect(screen.getByText("Obras autorizadas")).toBeInTheDocument();
    expect(screen.queryByLabelText("Senha")).not.toBeInTheDocument();
  });
  it("orienta recuperação sem inventar endpoint e remove link sem destino", () => {
    render(<MemoryRouter><Login /></MemoryRouter>);
    fireEvent.click(screen.getByRole("button", { name: "Esqueceu a senha?" }));
    expect(toast.info).toHaveBeenCalledWith(expect.stringContaining("não está disponível"));
    expect(screen.getByText("Solicite seu cadastro ao administrador da sua empresa.")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Solicite aqui" })).not.toBeInTheDocument();
  });
  it("aceita submit de formulário e navega sem espera artificial", async () => {
    vi.mocked(authService.login).mockResolvedValue({ id: 1, email: "usuario@exemplo.com", nome: "Pessoa", cargo: "ADMIN", token: null });
    render(<MemoryRouter><Routes><Route path="/" element={<Login />} /><Route path="/home" element={<div>Obras autorizadas</div>} /></Routes></MemoryRouter>);
    fireEvent.change(screen.getByLabelText("E-mail"), { target: { value: "usuario@exemplo.com" } });
    fireEvent.change(screen.getByLabelText("Senha"), { target: { value: "senha-ficticia-de-teste" } });
    fireEvent.submit(screen.getByLabelText("E-mail").closest("form")!);
    await waitFor(() => expect(screen.getByText("Obras autorizadas")).toBeInTheDocument());
    expect(authService.login).toHaveBeenCalledTimes(1);
  });
});