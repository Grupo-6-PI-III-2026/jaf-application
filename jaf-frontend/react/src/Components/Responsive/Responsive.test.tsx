import { useEffect } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, render, screen } from "@testing-library/react";
import { Responsive } from "./Responsive";
import { breakpointNoIntervalo } from "../../Constants/breakpoints";

let larguraAtual = 1280;
const ouvintes = new Set<() => void>();
const matchMediaOriginal = window.matchMedia;

function matchMediaFalso(query: string): MediaQueryList {
  const minimo = Number(/min-width:\s*(\d+)px/.exec(query)?.[1] ?? 0);
  return {
    media: query,
    matches: larguraAtual >= minimo,
    onchange: null,
    addEventListener: (_tipo: string, ouvinte: () => void) => ouvintes.add(ouvinte),
    removeEventListener: (_tipo: string, ouvinte: () => void) => ouvintes.delete(ouvinte),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  } as unknown as MediaQueryList;
}

function redimensionar(largura: number) {
  act(() => {
    larguraAtual = largura;
    ouvintes.forEach((ouvinte) => ouvinte());
  });
}

function Tela() {
  return (
    <>
      <Responsive max="md">
        <span>lista</span>
      </Responsive>
      <Responsive min="lg">
        <span>tabela</span>
      </Responsive>
    </>
  );
}

beforeEach(() => {
  larguraAtual = 1280;
  ouvintes.clear();
  window.matchMedia = matchMediaFalso;
});

afterEach(() => {
  window.matchMedia = matchMediaOriginal;
});

describe("Responsive", () => {
  it("renderiza só a versão do breakpoint atual", () => {
    render(<Tela />);

    expect(screen.queryByText("tabela")).not.toBeNull();
    expect(screen.queryByText("lista")).toBeNull();
  });

  it("troca a versão ao cruzar o breakpoint", () => {
    render(<Tela />);

    redimensionar(375);
    expect(screen.queryByText("lista")).not.toBeNull();
    expect(screen.queryByText("tabela")).toBeNull();

    redimensionar(1023);
    expect(screen.queryByText("lista")).not.toBeNull();

    redimensionar(1024);
    expect(screen.queryByText("tabela")).not.toBeNull();
    expect(screen.queryByText("lista")).toBeNull();
  });

  it("não monta o filho oculto, então seus efeitos não executam", () => {
    const efeito = vi.fn();
    function ComEfeito() {
      useEffect(efeito, []);
      return null;
    }
    larguraAtual = 375;

    render(
      <Responsive min="lg">
        <ComEfeito />
      </Responsive>,
    );

    expect(efeito).not.toHaveBeenCalled();
  });

  it("remove os ouvintes do matchMedia ao desmontar", () => {
    const { unmount } = render(<Tela />);
    expect(ouvintes.size).toBeGreaterThan(0);

    unmount();

    expect(ouvintes.size).toBe(0);
  });
});

describe("breakpointNoIntervalo", () => {
  it("considera limites inclusivos e opcionais", () => {
    expect(breakpointNoIntervalo("md", "sm", "md")).toBe(true);
    expect(breakpointNoIntervalo("lg", undefined, "md")).toBe(false);
    expect(breakpointNoIntervalo("base", undefined, "sm")).toBe(true);
    expect(breakpointNoIntervalo("2xl", "lg")).toBe(true);
    expect(breakpointNoIntervalo("sm", "lg")).toBe(false);
  });
});
