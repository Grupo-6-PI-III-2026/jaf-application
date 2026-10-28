import { isAxiosError } from "axios";

export const moeda = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

// Não arredondar silenciosamente valores digitados nem aceitar notação exponencial.
export function valorDecimal(texto: string, permiteZero = false): number | null {
  const normalizado = texto.trim().replace(/\s/g, "");
  if (normalizado.includes(",") && !/^(?:\d+|\d{1,3}(?:\.\d{3})+),\d{1,2}$/.test(normalizado)) return null;
  const decimal = normalizado.includes(",")
    ? normalizado.replace(/\./g, "").replace(",", ".")
    : normalizado;
  if (!/^\d{1,17}(\.\d{1,2})?$/.test(decimal)) return null;
  const [inteiros, fracao = ""] = decimal.split(".");
  const centavos = Number(`${inteiros}${fracao.padEnd(2, "0")}`);
  if (!Number.isSafeInteger(centavos) || centavos < 0 || (!permiteZero && centavos === 0)) return null;
  return centavos / 100;
}

export function dataValida(data: string, hoje: string, inicio?: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(data)) return false;
  const parsed = new Date(`${data}T12:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === data &&
    data <= hoje && (!inicio || data >= inicio);
}

export function erroApi(erro: unknown, padrao: string): string {
  if (isAxiosError<{ message?: string; mensagem?: string; detail?: string }>(erro)) {
    const body = erro.response?.data;
    return body?.message || body?.mensagem || body?.detail || padrao;
  }
  return padrao;
}