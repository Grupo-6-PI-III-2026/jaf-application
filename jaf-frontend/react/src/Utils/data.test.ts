import { afterEach, describe, expect, it, vi } from "vitest";
import { diasEntreIso, formatarDataIso, hojeSaoPaulo } from "./data";

afterEach(() => {
  vi.useRealTimers();
});

describe("datas", () => {
  it("formata a data ISO sem depender do fuso do aparelho", () => {
    expect(formatarDataIso("2026-10-30")).toBe("30/10/2026");
    expect(formatarDataIso("2026-01-01")).toBe("01/01/2026");
    expect(formatarDataIso("2026-10-30T12:00:00")).toBe("30/10/2026");
    expect(formatarDataIso(null)).toBe("");
  });

  it("usa a data de São Paulo na virada do dia", () => {
    // 01:30 UTC de 31/10 ainda é 22:30 de 30/10 em São Paulo.
    vi.useFakeTimers();
    vi.setSystemTime(new Date("2026-10-31T01:30:00Z"));
    expect(hojeSaoPaulo()).toBe("2026-10-30");
    vi.setSystemTime(new Date("2026-10-31T03:00:00Z"));
    expect(hojeSaoPaulo()).toBe("2026-10-31");
  });

  it("conta dias entre datas ISO", () => {
    expect(diasEntreIso("2026-03-01", "2026-03-31")).toBe(30);
    expect(diasEntreIso("2026-03-31", "2026-03-01")).toBe(-30);
    expect(diasEntreIso("2026-02-28", "2026-03-01")).toBe(1);
  });
});