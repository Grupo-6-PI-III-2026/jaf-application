import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import ImagemComPadrao from "./ImagemComPadrao";

describe("ImagemComPadrao", () => {
  it("usa a imagem padrão sem foto e quando a URL temporária falha", () => {
    const { rerender } = render(<ImagemComPadrao src={null} padrao="/padrao.svg" alt="Obra" />);
    expect(screen.getByAltText("Obra")).toHaveAttribute("src", "/padrao.svg");

    rerender(<ImagemComPadrao src="https://bucket/foto.png" padrao="/padrao.svg" alt="Obra" />);
    expect(screen.getByAltText("Obra")).toHaveAttribute("src", "https://bucket/foto.png");

    fireEvent.error(screen.getByAltText("Obra"));
    expect(screen.getByAltText("Obra")).toHaveAttribute("src", "/padrao.svg");

    rerender(<ImagemComPadrao src="https://bucket/nova.png" padrao="/padrao.svg" alt="Obra" />);
    expect(screen.getByAltText("Obra")).toHaveAttribute("src", "https://bucket/nova.png");
  });
});
