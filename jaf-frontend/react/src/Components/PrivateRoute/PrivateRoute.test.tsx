import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import PrivateRoute from "./PrivateRoute";
import { authService } from "../../Service/Auth/Login/authService";
import { validarSessao } from "../../test/authorization";

vi.mock("../../Context/useUser", () => ({ useUser: () => ({ user: { id: 1 }, isLoading: false, error: null }) }));
beforeEach(() => { localStorage.clear(); authService.clearAuthorization(); localStorage.setItem("userEmail", "usuario@exemplo.com"); });
describe("autorização da UI", () => {
  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("bloqueia acesso direto ao dashboard para %s", (cargo) => {
    validarSessao(cargo, ["VISUALIZAR_GASTOS", "EDITAR_FUNCIONARIO"]);
    render(<MemoryRouter initialEntries={["/restrito"]}><Routes><Route path="/restrito" element={<PrivateRoute adminOnly requiredPermission="VISUALIZAR_GASTOS"><div>Financeiro secreto</div></PrivateRoute>} /><Route path="/home" element={<div>Home segura</div>} /></Routes></MemoryRouter>);
    expect(screen.getByText("Home segura")).toBeInTheDocument();
    expect(screen.queryByText("Financeiro secreto")).not.toBeInTheDocument();
  });
  it("permissão de editar gasto não concede registrar pagamento", () => {
    validarSessao("ENGENHEIRO", ["EDITAR_GASTO"]);
    expect(authService.hasAuthority("EDITAR_GASTO")).toBe(true);
    expect(authService.podeRegistrarPagamento()).toBe(false);
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
    expect(authService.hasAuthority("INEXISTENTE")).toBe(false);
  });
  it("admin pode acessar financeiro", () => {
    validarSessao("ADMIN");
    render(<MemoryRouter><PrivateRoute adminOnly><div>Financeiro autorizado</div></PrivateRoute></MemoryRouter>);
    expect(screen.getByText("Financeiro autorizado")).toBeInTheDocument();
  });
  it.each(["ENGENHEIRO", "RESPONSAVEL_ADMINISTRATIVO"] as const)("EDITAR_FUNCIONARIO não abre configuração para %s", (cargo) => {
    validarSessao(cargo, ["EDITAR_FUNCIONARIO"]);
    render(<MemoryRouter initialEntries={["/permissoes"]}><Routes><Route path="/permissoes" element={<PrivateRoute adminOnly><div>Configuração restrita</div></PrivateRoute>} /><Route path="/home" element={<div>Home segura</div>} /></Routes></MemoryRouter>);
    expect(screen.getByText("Home segura")).toBeInTheDocument();
    expect(screen.queryByText("Configuração restrita")).not.toBeInTheDocument();
  });
});