import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import Profile from "./Profile";

vi.mock("../../Context/useUser", () => ({
  useUser: () => ({
    user: { id: 1, nome: "Pessoa", email: "pessoa@exemplo.com", cargo: "ENGENHEIRO", fotoUrl: null },
    isLoading: false, error: null, refreshUser: vi.fn(), clearUser: vi.fn(),
  }),
}));

describe("semântica do perfil", () => {
  it("não aninha main dentro do main do layout", () => {
    render(<main aria-label="Conteúdo principal"><Profile /></main>);
    expect(screen.getAllByRole("main")).toHaveLength(1);
    expect(screen.getByRole("region", { name: "Configurações do perfil" }).tagName).toBe("SECTION");
  });
});