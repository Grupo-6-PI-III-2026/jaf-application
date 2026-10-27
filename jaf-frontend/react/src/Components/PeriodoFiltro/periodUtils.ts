export type PeriodoPreset = "TODOS" | "HOJE" | "7_DIAS" | "PERSONALIZADO";

export interface PeriodoIntervalo {
  from: string | null;
  to: string | null;
}

export interface PeriodoFiltroValue {
  preset: PeriodoPreset;
  from: string;
  to: string;
}

export const PERIODO_OPCOES: { value: PeriodoPreset; label: string }[] = [
  { value: "TODOS", label: "Todos" },
  { value: "HOJE", label: "Hoje" },
  { value: "7_DIAS", label: "Últimos 7 dias" },
  { value: "PERSONALIZADO", label: "Personalizado" },
];

export const periodoFiltroInicial = (): PeriodoFiltroValue => ({
  preset: "TODOS",
  from: hojeIso(),
  to: hojeIso(),
});

/** Data de hoje em yyyy-MM-dd no fuso America/Sao_Paulo. */
export function hojeIso(): string {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Sao_Paulo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const getParte = (tipo: string) => partes.find((parte) => parte.type === tipo)?.value ?? "";
  return `${getParte("year")}-${getParte("month")}-${getParte("day")}`;
}

function adicionarDiasIso(dataIso: string, dias: number): string {
  const data = new Date(`${dataIso}T12:00:00`);
  data.setDate(data.getDate() + dias);
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
}

/** Resolve o intervalo efetivo a partir do estado do filtro. */
export function resolverPeriodo(value: PeriodoFiltroValue): PeriodoIntervalo {
  const hoje = hojeIso();

  switch (value.preset) {
    case "HOJE":
      return { from: hoje, to: hoje };
    case "7_DIAS":
      return { from: adicionarDiasIso(hoje, -6), to: hoje };
    case "PERSONALIZADO":
      return {
        from: value.from || null,
        to: value.to || null,
      };
    case "TODOS":
    default:
      return { from: null, to: null };
  }
}

/** Verifica se uma data ISO (yyyy-MM-dd) está dentro do intervalo. */
export function dataNoPeriodo(dataIso: string | null | undefined, intervalo: PeriodoIntervalo): boolean {
  if (!intervalo.from && !intervalo.to) return true;
  if (!dataIso) return false;

  if (intervalo.from && dataIso < intervalo.from) return false;
  if (intervalo.to && dataIso > intervalo.to) return false;
  return true;
}
