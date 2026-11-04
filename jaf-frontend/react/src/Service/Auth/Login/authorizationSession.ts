import type { FuncionarioResponseDto } from "../../../Types/user";

let generation = 0;
let session: { user: FuncionarioResponseDto; permissions: Set<string> } | null = null;
const ouvintes = new Set<() => void>();

const notificar = () => ouvintes.forEach((ouvinte) => ouvinte());

// Apenas memória: nunca reconstruir autoridades a partir do cargo/localStorage.
export const authorizationSession = {
  clear: () => {
    session = null;
    generation += 1;
    notificar();
  },
  version: () => generation,
  set: (user: FuncionarioResponseDto, permissions: string[]) => {
    session = { user: { ...user }, permissions: new Set(permissions) };
    generation += 1;
    notificar();
  },
  current: () => session,
  subscribe: (ouvinte: () => void) => {
    ouvintes.add(ouvinte);
    return () => { ouvintes.delete(ouvinte); };
  },
};