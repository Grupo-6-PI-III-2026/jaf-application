import type { FuncionarioResponseDto } from "../../../Types/user";

let generation = 0;
let session: { user: FuncionarioResponseDto; permissions: Set<string> } | null = null;

// Apenas memória: nunca reconstruir autoridades a partir do cargo/localStorage.
export const authorizationSession = {
  clear: () => {
    session = null;
    generation += 1;
  },
  version: () => generation,
  set: (user: FuncionarioResponseDto, permissions: string[]) => {
    session = { user: { ...user }, permissions: new Set(permissions) };
  },
  current: () => {
    if (session && (localStorage.getItem("userId") !== String(session.user.id) ||
        localStorage.getItem("userEmail") !== session.user.email)) {
      authorizationSession.clear();
    }
    return session;
  },
};