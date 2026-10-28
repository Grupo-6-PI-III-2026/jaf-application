import { beforeEach, describe, expect, it, vi } from "vitest";
import api from "./Api/Api";
import { authService } from "./authService";
import { validarSessao } from "../../../test/authorization";

vi.mock("./Api/Api", () => ({ default: { post: vi.fn(), get: vi.fn() } }));

beforeEach(() => {
  vi.resetAllMocks();
  localStorage.clear();
  authService.clearAuthorization();
});

describe("cache de autoridades da sessão", () => {
  it("não usa cargo estático nem localStorage como autoridade ou ADMIN validado", () => {
    localStorage.setItem("userId", "1");
    localStorage.setItem("userEmail", "pessoa@exemplo.com");
    localStorage.setItem("userCargo", "ADMIN");
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
    expect(authService.podeRegistrarPagamento()).toBe(false);
  });

  it("usa permissões customizadas exatas e consulta apenas memória, inclusive em verificações repetidas", () => {
    const permissions = ["DELETAR_OBRA", "VISUALIZAR_PRESENCAS"];
    validarSessao("ENGENHEIRO", permissions);
    permissions.push("REGISTRAR_PRESENCA");
    for (let i = 0; i < 10; i += 1) {
      expect(authService.hasAuthority("DELETAR_OBRA")).toBe(true);
      expect(authService.hasAuthority("REGISTRAR_PRESENCA")).toBe(false);
      expect(authService.hasAuthority("INEXISTENTE")).toBe(false);
    }
    expect(api.get).not.toHaveBeenCalled();
    expect(authService.isAdmin()).toBe(false);
  });

  it("lista efetiva vazia não recupera padrões, nem mesmo para ADMIN", () => {
    validarSessao("ADMIN", []);
    expect(authService.isAdmin()).toBe(true);
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
  });

  it.each(["userId", "userEmail"])("descarta cache ao trocar %s", (key) => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    localStorage.setItem(key, "outra-pessoa");
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
    expect(authService.getCargo()).toBeNull();
  });

  it("isAdmin e pagamento usam cargo do perfil validado, não cargo editado no armazenamento", () => {
    validarSessao("ENGENHEIRO", ["EDITAR_GASTO"]);
    localStorage.setItem("userCargo", "ADMIN");
    expect(authService.isAdmin()).toBe(false);
    expect(authService.podeRegistrarPagamento()).toBe(false);
  });

  it("reinicia cache imediatamente ao tentar login, inclusive se falhar", async () => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    vi.mocked(api.post).mockRejectedValue(new Error("Falha"));
    const login = authService.login({ email: "outra@exemplo.com", senha: "senha" });
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
    await expect(login).rejects.toThrow("Erro ao fazer login");
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
  });

  it("login bem-sucedido não libera permissões nem ADMIN antes da validação de /me", async () => {
    validarSessao("ENGENHEIRO", ["DELETAR_OBRA"]);
    vi.mocked(api.post).mockResolvedValue({ data: { id: 2, email: "admin@exemplo.com", nome: "Admin", cargo: "ADMIN" } });
    await authService.login({ email: "admin@exemplo.com", senha: "senha" });
    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.isAdmin()).toBe(false);
    expect(authService.hasAuthority("DELETAR_OBRA")).toBe(false);
  });

  it("logout limpa antes da resposta e permanece fechado se o endpoint falhar", async () => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    let reject!: (error: Error) => void;
    vi.mocked(api.post).mockReturnValue(new Promise((_, rejeitar) => { reject = rejeitar; }));
    const logout = authService.logout();
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
    expect(authService.isAuthenticated()).toBe(false);
    reject(new Error("Falha"));
    await logout;
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(api.post).toHaveBeenCalledWith("/funcionarios/logout");
  });
});