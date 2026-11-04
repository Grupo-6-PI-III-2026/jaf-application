import { renderHook } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { useAuthz } from "./useAuthz";
import { validarSessao } from "../test/authorization";

describe("useAuthz", () => {
  it("garante permissões administrativas pela sessão validada do perfil ADMIN", () => {
    validarSessao("ADMIN");

    const { result } = renderHook(() => useAuthz());

    expect(result.current.can("CRIAR_OBRA")).toBe(true);
    expect(result.current.can("CRIAR_FUNCIONARIO")).toBe(true);
    expect(result.current.can("DELETAR_ALOCACAO")).toBe(true);
  });
});
