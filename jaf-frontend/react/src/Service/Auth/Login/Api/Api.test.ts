import { AxiosError, type AxiosAdapter } from "axios";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import api from "./Api";
import { authService } from "../authService";
import { validarSessao } from "../../../../test/authorization";

const originalAdapter = api.defaults.adapter;
const unauthorized: AxiosAdapter = async (config) => {
  throw new AxiosError("Não autorizado", "ERR_BAD_REQUEST", config, undefined, {
    status: 401, statusText: "Unauthorized", data: {}, headers: {}, config,
  });
};

beforeEach(() => { localStorage.clear(); authService.clearAuthorization(); api.defaults.adapter = unauthorized; });
afterEach(() => { api.defaults.adapter = originalAdapter; });

describe("interceptor de autenticação", () => {
  it.each(["/funcionarios/me", "/funcionarios/me/permissoes"])("propaga falha de %s sem remover marcador necessário para retry no Provider", async (url) => {
    localStorage.setItem("userId", "42");
    localStorage.setItem("userEmail", "pessoa@exemplo.com");
    await expect(api.get(url)).rejects.toMatchObject({ response: { status: 401 } });
    expect(authService.isAuthenticated()).toBe(true);
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });

  it("401 fora da validação limpa também o cache em memória", async () => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    await expect(api.get("/obras")).rejects.toMatchObject({ response: { status: 401 } });
    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });
});