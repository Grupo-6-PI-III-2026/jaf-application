const FUSO = "America/Sao_Paulo";

/** "2026-10-30" -> "30/10/2026" sem passar por Date: não muda de dia conforme o fuso do aparelho. */
export function formatarDataIso(iso: string | null | undefined): string {
  if (!iso) return "";
  const [ano, mes, dia] = iso.slice(0, 10).split("-");
  return ano && mes && dia ? `${dia}/${mes}/${ano}` : iso;
}

/** Data de hoje (AAAA-MM-DD) em São Paulo, a mesma convenção do backend. */
export function hojeSaoPaulo(): string {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: FUSO,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const parte = (tipo: string) => partes.find((p) => p.type === tipo)?.value ?? "";
  return `${parte("year")}-${parte("month")}-${parte("day")}`;
}

/** Dias de `de` até `ate` (datas AAAA-MM-DD), contados em UTC para não sofrer com horário de verão/fuso. */
export function diasEntreIso(de: string, ate: string): number {
  const paraUtc = (iso: string) => {
    const [ano, mes, dia] = iso.split("-").map(Number);
    return Date.UTC(ano, (mes ?? 1) - 1, dia ?? 1);
  };
  return Math.round((paraUtc(ate) - paraUtc(de)) / 86_400_000);
}