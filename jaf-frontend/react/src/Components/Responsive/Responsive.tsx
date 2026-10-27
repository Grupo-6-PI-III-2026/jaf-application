import type { ReactNode } from "react";
import { breakpointNoIntervalo, type Breakpoint } from "../../Constants/breakpoints";
import { useBreakpoint } from "../../Hooks/useBreakpoint";

interface ResponsiveProps {
  min?: Breakpoint;
  max?: Breakpoint;
  children: ReactNode;
}

// Fora do intervalo os filhos não são montados (diferente de display: none, que ainda executa hooks e renders).
export function Responsive({ min, max, children }: ResponsiveProps) {
  const breakpoint = useBreakpoint();
  return breakpointNoIntervalo(breakpoint, min, max) ? <>{children}</> : null;
}
