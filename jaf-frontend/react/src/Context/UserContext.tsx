import {
  useCallback,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import { funcionarioService } from "../Service/Funcionarios/funcionarioService";
import { authService } from "../Service/Auth/Login/authService";
import type { FuncionarioResponseDto } from "../Types/user";
import { UserContext } from "./UserContextDefinition";

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<FuncionarioResponseDto | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshUser = useCallback(async (): Promise<boolean> => {
    authService.clearAuthorization();
    const version = authService.getAuthorizationVersion();
    const email = authService.getEmail();
    const id = authService.getId();
    const isCurrent = () => version === authService.getAuthorizationVersion() &&
      email === authService.getEmail() && id === authService.getId();
    setUser(null);
    setError(null);
    if (!authService.isAuthenticated()) {
      setIsLoading(false);
      return false;
    }

    setIsLoading(true);
    setError(null);

    try {
      const perfil = await funcionarioService.getMeuPerfil();
      if (!isCurrent()) return false;
      const acesso = await funcionarioService.getMinhasPermissoes();
      if (!isCurrent()) return false;
      if (!Number.isInteger(perfil.id) || perfil.id <= 0 || !perfil.email ||
          !["ADMIN", "RESPONSAVEL_ADMINISTRATIVO", "ENGENHEIRO"].includes(perfil.cargo) ||
          acesso.funcionarioId !== perfil.id || acesso.cargo !== perfil.cargo ||
          !Array.isArray(acesso.permissoes) || !acesso.permissoes.every((p) => typeof p === "string")) {
        throw new Error("Perfil e permissões incompatíveis.");
      }
      // Revalidar os dados de apresentação antes de liberar as rotas privadas.
      localStorage.setItem("userCargo", perfil.cargo);
      localStorage.setItem("userId", String(perfil.id));
      localStorage.setItem("userEmail", perfil.email);
      localStorage.setItem("userName", perfil.nome);
      authService.setValidatedSession(perfil, acesso.permissoes);
      setUser(perfil);
      return true;
    } catch (err) {
      if (!isCurrent()) return false;
      authService.clearAuthorization();
      setUser(null);
      setIsLoading(false);
      console.error(err);
      // Cookie expirado/encerrado: tentar de novo não resolve, a rota privada volta ao login.
      if ((err as { response?: { status?: number } } | null)?.response?.status === 401) {
        authService.clearSession();
        return false;
      }
      setError("Nao foi possivel carregar os dados do usuario.");
      return false;
    } finally {
      if (version === authService.getAuthorizationVersion()) setIsLoading(false);
    }
  }, []);

  const clearUser = useCallback(() => {
    authService.clearAuthorization();
    setUser(null);
    setError(null);
    setIsLoading(false);
  }, []);

  useEffect(() => {
    if (authService.isAuthenticated()) {
      refreshUser();
    }
    return () => authService.clearAuthorization();
  }, [refreshUser]);

  useEffect(() => {
    // Página restaurada do bfcache (seta voltar) não reexecuta o app: revalidar a sessão.
    const aoRestaurar = (evento: PageTransitionEvent) => {
      if (!evento.persisted) return;
      if (authService.isAuthenticated()) {
        void refreshUser();
      } else if (window.location.pathname !== "/") {
        window.location.replace("/");
      }
    };
    window.addEventListener("pageshow", aoRestaurar);
    return () => window.removeEventListener("pageshow", aoRestaurar);
  }, [refreshUser]);

  return (
    <UserContext.Provider
      value={{ user, isLoading, error, refreshUser, clearUser }}
    >
      {children}
    </UserContext.Provider>
  );
}
