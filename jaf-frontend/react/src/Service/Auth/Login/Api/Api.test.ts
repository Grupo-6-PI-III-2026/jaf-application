import { AxiosError, type AxiosAdapter } from "axios";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import api, { EVENTO_SESSAO_EXPIRADA } from "./Api";
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
  it.each(["/funcionarios/me", "/funcionarios/me/permissoes"])("propaga falha de %s sem emitir evento de expiração (o Provider decide)", async (url) => {
    const aoExpirar = vi.fn();
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    await expect(api.get(url)).rejects.toMatchObject({ response: { status: 401 } });
    window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    expect(aoExpirar).not.toHaveBeenCalled();
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });

  it("401 fora da validação limpa o cache em memória e avisa a aplicação sem recarregar a página", async () => {
    validarSessao("ADMIN", ["CRIAR_OBRA"]);
    const aoExpirar = vi.fn();
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    await expect(api.get("/obras")).rejects.toMatchObject({ response: { status: 401 } });
    window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    expect(aoExpirar).toHaveBeenCalledTimes(1);
    expect(authService.isAuthenticated()).toBe(false);
    expect(authService.hasAuthority("CRIAR_OBRA")).toBe(false);
    expect(authService.isAdmin()).toBe(false);
  });
});