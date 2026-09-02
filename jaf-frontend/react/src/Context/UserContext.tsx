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

  const refreshUser = useCallback(async () => {
    if (!authService.isAuthenticated()) {
      setUser(null);
      return;
    }

    setIsLoading(true);
    setError(null);

    try {
      const perfil = await funcionarioService.getMeuPerfil();
      setUser(perfil);
    } catch (err) {
      setError("Nao foi possivel carregar os dados do usuario.");
      setUser(null);
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const clearUser = useCallback(() => {
    setUser(null);
  }, []);

  useEffect(() => {
    if (authService.isAuthenticated()) {
      refreshUser();
    }
  }, [refreshUser]);

  return (
    <UserContext.Provider
      value={{ user, isLoading, error, refreshUser, clearUser }}
    >
      {children}
    </UserContext.Provider>
  );
}
