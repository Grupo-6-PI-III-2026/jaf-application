import { useSyncExternalStore } from "react";
import { BREAKPOINTS_ORDER, BREAKPOINTS_WIDTH, type Breakpoint } from "../Constants/breakpoints";

const CONSULTAS = BREAKPOINTS_ORDER.filter((breakpoint) => breakpoint !== "base").map((breakpoint) => ({
  breakpoint,
  query: `(min-width: ${BREAKPOINTS_WIDTH[breakpoint]}px)`,
}));

function calcularBreakpoint(): Breakpoint {
  for (let indice = CONSULTAS.length - 1; indice >= 0; indice--) {
    if (window.matchMedia(CONSULTAS[indice].query).matches) {
      return CONSULTAS[indice].breakpoint;
    }
  }
  return "base";
}

// matchMedia só notifica ao cruzar um breakpoint, ao contrário de "resize", que dispara a cada pixel.
function inscrever(notificar: () => void) {
  const listas = CONSULTAS.map(({ query }) => window.matchMedia(query));
  listas.forEach((lista) => lista.addEventListener("change", notificar));
  return () => listas.forEach((lista) => lista.removeEventListener("change", notificar));
}

export function useBreakpoint(): Breakpoint {
  return useSyncExternalStore(inscrever, calcularBreakpoint, () => "lg");
}
