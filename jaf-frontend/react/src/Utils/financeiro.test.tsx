import { describe, expect, it } from "vitest";
import { dataValida, erroApi, valorDecimal } from "./financeiro";

describe("validação financeira", () => {
  it.each([["10,25", 10.25], ["1.234,56", 1234.56], ["10.2", 10.2], ["0.01", 0.01], ["123", 123]])("converte %s sem perder centavos", (texto, esperado) => {
    expect(valorDecimal(texto)).toBe(esperado);
  });
  it.each(["", "-1", "0", "NaN", "Infinity", "1e3", "10.001", "1,234", "1.2,3", "1,2,3", "99999999999999999.99"])("rejeita %s sem arredondar", (texto) => {
    expect(valorDecimal(texto)).toBeNull();
  });
  it("aceita zero somente para recebido", () => {
    expect(valorDecimal("0,00", true)).toBe(0);
    expect(valorDecimal("-0", true)).toBeNull();
  });
  it("valida datas reais e limites inclusivos", () => {
    expect(dataValida("2026-10-27", "2026-10-27", "2026-10-27")).toBe(true);
    expect(dataValida("2026-10-28", "2026-10-27")).toBe(false);
    expect(dataValida("2026-10-25", "2026-10-27", "2026-10-26")).toBe(false);
    expect(dataValida("2026-02-30", "2026-10-27")).toBe(false);
    expect(dataValida("inválida", "2026-10-27")).toBe(false);
  });
  it("preserva mensagem de conflito da API", () => {
    expect(erroApi({ isAxiosError: true, response: { data: { message: "Histórico protegido" } } }, "Falha")).toBe("Histórico protegido");
    expect(erroApi(new Error(), "Falha")).toBe("Falha");
  });
});