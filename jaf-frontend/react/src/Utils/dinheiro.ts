export const paraCentavos = (valor: number | string | null | undefined): number | null => {
  if (valor === null || valor === undefined || valor === "") return null;
  const n = Number(valor);
  return Number.isFinite(n) ? Math.round(n * 100) : null;
};

export const somarCentavos = (...valores: Array<number | string | null | undefined>): number =>
  valores.reduce<number>((total, valor) => total + (paraCentavos(valor) ?? 0), 0);

export const formatarBRL = (centavos: number): string =>
  (centavos / 100).toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export const formatarValorOuTraco = (valor: number | string | null | undefined): string => {
  const centavos = paraCentavos(valor);
  return centavos === null ? "—" : formatarBRL(centavos);
};
