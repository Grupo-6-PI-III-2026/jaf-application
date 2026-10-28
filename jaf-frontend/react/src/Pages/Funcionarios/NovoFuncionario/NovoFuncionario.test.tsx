import { act, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import NovoFuncionario from "./NovoFuncionario";
import { validarSessao } from "../../../test/authorization";
import { funcionarioService } from "../../../Service/Funcionarios/funcionarioService";
import { toast } from "sonner";

vi.mock("../../../Service/Funcionarios/funcionarioService", () => ({ funcionarioService: { cadastrar: vi.fn() } }));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

beforeEach(() => { vi.resetAllMocks(); localStorage.clear(); });
function preencher() {
  fireEvent.change(screen.getByPlaceholderText("Ex: Carlos Almeida"), { target: { value: "Carlos Almeida" } });
  fireEvent.change(screen.getByPlaceholderText("nome@jafconstrutora.com.br"), { target: { value: "carlos@exemplo.com" } });
  fireEvent.change(screen.getByPlaceholderText("••••••••"), { target: { value: "Senha@123" } });
}

describe("cargos no cadastro", () => {
  it("associa labels e impede submissões simultâneas sem silenciar falhas", async () => {
    validarSessao("ADMIN", ["CRIAR_FUNCIONARIO"]);
    let rejeitar!: (error: Error) => void;
    vi.mocked(funcionarioService.cadastrar).mockReturnValue(new Promise((_, reject) => { rejeitar = reject; }));
    render(<NovoFuncionario />);
    preencher();
    expect(screen.getByLabelText("NOME COMPLETO")).toHaveValue("Carlos Almeida");
    expect(screen.getByLabelText("E-MAIL CORPORATIVO")).toHaveValue("carlos@exemplo.com");
    expect(screen.getByLabelText("SENHA DE ACESSO")).toHaveValue("Senha@123");
    fireEvent.change(screen.getByLabelText("PERFIL DE ACESSO"), { target: { value: "ENGENHEIRO" } });
    const form = screen.getByRole("form", { name: "Cadastrar funcionário" });
    act(() => { fireEvent.submit(form); fireEvent.submit(form); });
    expect(funcionarioService.cadastrar).toHaveBeenCalledTimes(1);
    await act(async () => { rejeitar(new Error("Erro inesperado")); });
    expect(toast.error).toHaveBeenCalledWith("Erro ao cadastrar funcionário. Tente novamente.");
    expect(screen.getByRole("button", { name: "Cadastrar Funcionário" })).toBeEnabled();
  });

  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("%s não pode escolher ADMIN mas mantém cadastro não-admin", async (cargo) => {
    validarSessao(cargo, ["CRIAR_FUNCIONARIO", "EDITAR_FUNCIONARIO"]);
    render(<NovoFuncionario />);
    expect(screen.queryByRole("option", { name: "Administrador" })).not.toBeInTheDocument();
    preencher();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "ENGENHEIRO" } });
    fireEvent.click(screen.getByRole("button", { name: "Cadastrar Funcionário" }));
    await waitFor(() => expect(funcionarioService.cadastrar).toHaveBeenCalledWith(expect.objectContaining({ cargo: "ENGENHEIRO" })));
  });

  it("ADMIN validado mantém todas as opções e pode cadastrar ADMIN", async () => {
    validarSessao("ADMIN", ["CRIAR_FUNCIONARIO"]);
    render(<NovoFuncionario />);
    expect(screen.getByRole("option", { name: "Administrador" })).toBeInTheDocument();
    preencher();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "ADMIN" } });
    fireEvent.click(screen.getByRole("button", { name: "Cadastrar Funcionário" }));
    await waitFor(() => expect(funcionarioService.cadastrar).toHaveBeenCalledWith(expect.objectContaining({ cargo: "ADMIN" })));
  });

  it("bloqueia submissão ADMIN se a sessão deixar de ser admin depois da seleção", () => {
    validarSessao("ADMIN", ["CRIAR_FUNCIONARIO"]);
    render(<NovoFuncionario />);
    preencher();
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "ADMIN" } });
    validarSessao("RESPONSAVEL_ADMINISTRATIVO", ["CRIAR_FUNCIONARIO"]);
    fireEvent.click(screen.getByRole("button", { name: "Cadastrar Funcionário" }));
    expect(funcionarioService.cadastrar).not.toHaveBeenCalled();
  });
});