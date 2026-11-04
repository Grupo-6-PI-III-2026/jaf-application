import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DialogProvider } from "./DialogProvider";
import { useDialogs } from "./DialogContext";

function TriggerDialog() {
  const { confirm } = useDialogs();
  return <button type="button" onClick={() => void confirm("Excluir gasto?")}>Abrir confirmação</button>;
}

describe("DialogProvider", () => {
  it("fecha o diálogo após confirmar", async () => {
    const showModal = vi.fn(function (this: HTMLDialogElement) {
      this.setAttribute("open", "");
    });
    const close = vi.fn(function (this: HTMLDialogElement) {
      this.removeAttribute("open");
    });
    vi.spyOn(HTMLDialogElement.prototype, "showModal").mockImplementation(showModal);
    vi.spyOn(HTMLDialogElement.prototype, "close").mockImplementation(close);

    render(
      <DialogProvider>
        <TriggerDialog />
      </DialogProvider>,
    );

    fireEvent.click(screen.getByRole("button", { name: "Abrir confirmação" }));
    const dialog = screen.getByRole("dialog");
    expect(await screen.findByText("Excluir gasto?")).toBeInTheDocument();
    expect(dialog).toHaveAttribute("open");

    fireEvent.click(screen.getByRole("button", { name: "Confirmar" }));

    await waitFor(() => expect(dialog).not.toHaveAttribute("open"));
    expect(close).toHaveBeenCalledTimes(1);
  });
});
