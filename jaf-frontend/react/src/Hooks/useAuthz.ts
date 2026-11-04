import { useCallback, useSyncExternalStore } from "react";
import { authorizationSession } from "../Service/Auth/Login/authorizationSession";

// Permissões vêm da sessão validada pelo servidor; a API continua sendo quem autoriza de fato.
export function useAuthz() {
  useSyncExternalStore(authorizationSession.subscribe, authorizationSession.version);
  const sessao = authorizationSession.current();
  const can = useCallback((permissao: string) =>
    sessao?.user.cargo === "ADMIN" || sessao?.permissions.has(permissao) === true, [sessao]);
  const cargo = sessao?.user.cargo ?? null;
  return {
    can,
    cargo,
    isAdmin: cargo === "ADMIN",
    podeRegistrarPagamento: cargo === "ADMIN" || cargo === "RESPONSAVEL_ADMINISTRATIVO",
  };
}
