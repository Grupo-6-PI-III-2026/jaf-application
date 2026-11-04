import { describe, expect, it } from "vitest";
import { formatarValorOuTraco, paraCentavos, somarCentavos } from "./dinheiro";

describe("dinheiro", () => {
  it("converte para centavos sem erro de ponto flutuante", () => {
    expect(paraCentavos(19.99)).toBe(1999);
    expect(paraCentavos("1000.10")).toBe(100010);
  });

  it("retorna null para valores inválidos", () => {
    expect(paraCentavos("abc")).toBeNull();
    expect(paraCentavos(undefined)).toBeNull();
  });

  it("soma em inteiros", () => {
    expect(somarCentavos(0.1, 0.2)).toBe(30);
  });

  it("mostra traço para orçamento inválido", () => {
    expect(formatarValorOuTraco("x")).toBe("—");
  });
});
