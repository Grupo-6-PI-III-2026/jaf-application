import { render, screen } from "@testing-library/react";
import { Outlet } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppRoutes } from "./routes";
import { Sidebar } from "./Components/Layout/Sidebar/Sidebar";
import { validarSessao } from "./test/authorization";

vi.mock("./Context/useUser", () => ({ useUser: () => ({ user: { id: 1, nome: "Pessoa", cargo: "ENGENHEIRO" }, isLoading: false, error: null }) }));
vi.mock("./Components/Layout/Layout", () => ({ default: () => <><Sidebar /><Outlet /></> }));
vi.mock("./Pages/Home/Home", () => ({ default: () => <div>Home segura</div> }));
vi.mock("./Pages/Permissoes/Permissoes", () => ({ default: () => <div>Escolha gerencial de cargos</div> }));

beforeEach(() => { localStorage.clear(); window.history.replaceState({}, "", "/permissoes"); });

describe("rota real e link de configuração ADMIN-only", () => {
  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("nega rota e link para %s mesmo com EDITAR_FUNCIONARIO", async (cargo) => {
    validarSessao(cargo, ["EDITAR_FUNCIONARIO", "CRIAR_FUNCIONARIO"]);
    render(<AppRoutes />);
    expect(await screen.findByText("Home segura")).toBeInTheDocument();
    expect(screen.queryByText("Escolha gerencial de cargos")).not.toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Perfis de Acesso" })).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Novo Usuário" })).toBeInTheDocument();
  });

  it("permite rota e link a ADMIN validado sem depender de EDITAR_FUNCIONARIO", () => {
    validarSessao("ADMIN", []);
    render(<AppRoutes />);
    expect(screen.getByText("Escolha gerencial de cargos")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Perfis de Acesso" })).toBeInTheDocument();
  });
});