import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import AlocacaoFuncionario from "./AlocacaoFuncionario";
import { obraService } from "../../Service/Obras/obraService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { funcionarioService } from "../../Service/Funcionarios/funcionarioService";
import { validarSessao } from "../../test/authorization";
import { toast } from "sonner";
import { DialogProvider } from "../../Components/Feedback/DialogProvider";

vi.mock("../../Service/Obras/obraService", () => ({ obraService: { buscarPorId: vi.fn() } }));
vi.mock("../../Service/Alocacoes/alocacaoService", () => ({
  alocacaoService: { listarPorObra: vi.fn(), criar: vi.fn(), deletar: vi.fn() },
}));
vi.mock("../../Service/Funcionarios/funcionarioService", () => ({ funcionarioService: { listarAlocaveis: vi.fn() } }));
vi.mock("../../Components/Responsive/Responsive", () => ({
  Responsive: ({ min, children }: { min?: string; children: React.ReactNode }) => min ? null : <>{children}</>,
}));
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: vi.fn() } }));

const alocacao: AlocacaoObra = {
  id: 8,
  funcionario: { id: 3, nome: "Pessoa alocada", email: "pessoa@example.com", fotoUrl: null, cargoGlobal: "ENGENHEIRO" },
  obra: { id: 1, titulo: "Obra de teste", status: "EM_ANDAMENTO", dtInicio: "2026-01-01", dtTerminoPrevisto: "2026-12-01" },
  cargo: "ENGENHEIRO",
  dtInicio: "2026-01-01",
  dtFim: null,
};

function abrir() {
  return render(
    <DialogProvider>
      <MemoryRouter initialEntries={["/obras/1/alocacoes"]}>
        <Routes>
          <Route path="/obras/:id/alocacoes" element={<AlocacaoFuncionario />} />
        </Routes>
      </MemoryRouter>
    </DialogProvider>,
  );
}

beforeEach(() => {
  vi.resetAllMocks();
  localStorage.clear();
  validarSessao("ADMIN");
  vi.mocked(obraService.buscarPorId).mockResolvedValue(alocacao.obra);
  vi.mocked(alocacaoService.listarPorObra).mockResolvedValue([alocacao]);
  vi.mocked(funcionarioService.listarAlocaveis).mockResolvedValue([]);
});

describe("alocações da obra", () => {
  it("admin pode remover uma alocação mesmo sem permissões duplicadas na sessão local", async () => {
    abrir();
    const remover = await screen.findByRole("button", { name: "Remover alocação de Pessoa alocada" });

    fireEvent.click(remover);
    expect(await screen.findByText("Remover Pessoa alocada da equipe desta obra?")).toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Confirmar" }));

    await waitFor(() => expect(alocacaoService.deletar).toHaveBeenCalledWith(8));
    expect(await screen.findByText("Nenhum funcionário alocado nesta obra.")).toBeInTheDocument();
    expect(toast.success).toHaveBeenCalledWith("Funcionário removido da equipe da obra.");
  });
});
