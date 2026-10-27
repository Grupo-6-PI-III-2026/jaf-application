// Mesmos valores dos breakpoints do Tailwind. Nos CSS Modules use os equivalentes:
// "a partir de X" -> @media (min-width: Xpx) | "abaixo de X" -> @media (max-width: X-0.02px)
export type Breakpoint = "base" | "sm" | "md" | "lg" | "xl" | "2xl";

export const BREAKPOINTS_ORDER: Breakpoint[] = ["base", "sm", "md", "lg", "xl", "2xl"];

export const BREAKPOINTS_WIDTH: Record<Breakpoint, number> = {
  base: 0,
  sm: 640,
  md: 768,
  lg: 1024,
  xl: 1280,
  "2xl": 1536,
};

export function breakpointNoIntervalo(atual: Breakpoint, min?: Breakpoint, max?: Breakpoint): boolean {
  const indiceAtual = BREAKPOINTS_ORDER.indexOf(atual);
  const indiceMin = min ? BREAKPOINTS_ORDER.indexOf(min) : 0;
  const indiceMax = max ? BREAKPOINTS_ORDER.indexOf(max) : BREAKPOINTS_ORDER.length - 1;
  return indiceAtual >= indiceMin && indiceAtual <= indiceMax;
}
