import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { funcionarioService } from "../Service/Funcionarios/funcionarioService";
import { authService, limparDadosLegados } from "../Service/Auth/Login/authService";
import { canalAuth } from "../Service/Auth/Login/canalAuth";
import { EVENTO_SESSAO_ALTERADA, EVENTO_SESSAO_EXPIRADA } from "../Service/Auth/Login/Api/Api";
import type { FuncionarioResponseDto } from "../Types/user";
import { UserContext } from "./UserContextDefinition";
import { logger } from "../Utils/logger";

const REVALIDAR_APOS_MS = 30_000;

const statusDe = (erro: unknown) => (erro as { response?: { status?: number } } | null)?.response?.status;

export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<FuncionarioResponseDto | null>(null);
  // Começa carregando: sem localStorage, só o servidor diz se há sessão válida.
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const requisicao = useRef(0);
  const iniciou = useRef(false);
  const ultimaValidacao = useRef(0);
  const userIdAtual = useRef<number | null>(null);

  const refreshUser = useCallback(async (): Promise<boolean> => {
    const id = ++requisicao.current;
    const atual = () => id === requisicao.current;
    authService.clearAuthorization();
    setUser(null);
    setError(null);
    setIsLoading(true);

    try {
      const perfil = await funcionarioService.getMeuPerfil();
      if (!atual()) return false;
      const acesso = await funcionarioService.getMinhasPermissoes();
      if (!atual()) return false;
      if (!Number.isInteger(perfil.id) || perfil.id <= 0 || !perfil.email ||
          !["ADMIN", "RESPONSAVEL_ADMINISTRATIVO", "ENGENHEIRO"].includes(perfil.cargo) ||
          acesso.funcionarioId !== perfil.id || acesso.cargo !== perfil.cargo ||
          !Array.isArray(acesso.permissoes) || !acesso.permissoes.every((p) => typeof p === "string")) {
        throw new Error("Perfil e permissões incompatíveis.");
      }
      authService.setValidatedSession(perfil, acesso.permissoes);
      userIdAtual.current = perfil.id;
      ultimaValidacao.current = Date.now();
      setUser(perfil);
      canalAuth.publicar({ tipo: "login", userId: perfil.id });
      return true;
    } catch (err) {
      if (!atual()) return false;
      authService.clearAuthorization();
      userIdAtual.current = null;
      setUser(null);
      // 401: não há sessão; a rota privada volta ao login. Outros erros permitem tentar de novo.
      if (statusDe(err) !== 401) {
        logger.erro("Erro ao carregar dados do usuário", err);
        setError("Nao foi possivel carregar os dados do usuario.");
      }
      return false;
    } finally {
      if (atual()) setIsLoading(false);
    }
  }, []);

  const clearUser = useCallback(() => {
    requisicao.current += 1;
    authService.clearAuthorization();
    userIdAtual.current = null;
    setUser(null);
    setError(null);
    setIsLoading(false);
  }, []);

  useEffect(() => {
    if (iniciou.current) return;
    iniciou.current = true;
    limparDadosLegados();
    void refreshUser();
  }, [refreshUser]);

  useEffect(() => {
    const aoExpirar = () => clearUser();
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    return () => window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
  }, [clearUser]);

  useEffect(() => {
    // Outra aba entrou, saiu ou trocou de usuário: o cookie é compartilhado, o estado desta aba não.
    return canalAuth.ouvir((mensagem) => {
      if (mensagem.tipo === "logout") {
        if (userIdAtual.current === null) return;
        clearUser();
        window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ALTERADA, { detail: { motivo: "saiu" } }));
      } else if (userIdAtual.current !== null && mensagem.userId !== userIdAtual.current) {
        clearUser();
        window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ALTERADA, { detail: { motivo: "trocou" } }));
      }
    });
  }, [clearUser]);

  useEffect(() => {
    const aoVoltar = async () => {
      if (document.visibilityState !== "visible" || userIdAtual.current === null) return;
      if (Date.now() - ultimaValidacao.current < REVALIDAR_APOS_MS) return;
      ultimaValidacao.current = Date.now();
      try {
        const perfil = await funcionarioService.getMeuPerfil();
        if (userIdAtual.current !== null && perfil.id !== userIdAtual.current) {
          clearUser();
          window.dispatchEvent(new CustomEvent(EVENTO_SESSAO_ALTERADA, { detail: { motivo: "trocou" } }));
        }
      } catch (err) {
        if (statusDe(err) === 401) {
          clearUser();
          window.dispatchEvent(new Event(EVENTO_SESSAO_EXPIRADA));
        }
      }
    };
    document.addEventListener("visibilitychange", aoVoltar);
    return () => document.removeEventListener("visibilitychange", aoVoltar);
  }, [clearUser]);

  useEffect(() => {
    // Página restaurada do bfcache (seta voltar) não reexecuta o app: revalidar a sessão.
    const aoRestaurar = (evento: PageTransitionEvent) => {
      if (evento.persisted) void refreshUser();
    };
    window.addEventListener("pageshow", aoRestaurar);
    return () => window.removeEventListener("pageshow", aoRestaurar);
  }, [refreshUser]);

  const value = useMemo(
    () => ({ user, isLoading, error, refreshUser, clearUser }),
    [user, isLoading, error, refreshUser, clearUser],
  );

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}
