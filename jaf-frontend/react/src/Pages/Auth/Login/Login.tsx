import { useEffect, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import styles from "./Login.module.css";
import { authService } from "../../../Service/Auth/Login/authService";
import { useUser } from "../../../Context/useUser";
import { toast } from "sonner";
import { Responsive } from "../../../Components/Responsive/Responsive";

function Login() {
  const navigate = useNavigate();
  const { user, error, refreshUser, clearUser } = useUser();
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const sessaoLocal = authService.isAuthenticated();

  useEffect(() => {
    // Sessão local inválida (ex.: cookie expirado): descartar e exibir o formulário.
    if (error && !loading) {
      authService.clearSession();
      clearUser();
    }
  }, [error, loading, clearUser]);

  const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  const funcLogin = async (evento: React.FormEvent<HTMLFormElement>) => {
    evento.preventDefault();
    if (loading) return;
    setErrorMessage(null);
    const emailNormalizado = email.trim().toLowerCase();

    if (!emailRegex.test(emailNormalizado) || password.trim().length === 0) {
      const message = "E-mail ou senha inválidos. Verifique suas credenciais.";
      setErrorMessage(message);
      toast.error(message);
      return;
    }

    setLoading(true);
    try {
      await authService.login({ email: emailNormalizado, senha: password });
      if (!(await refreshUser())) {
        throw new Error("Não foi possível validar seu acesso. Tente novamente.");
      }
      toast.success("Login realizado com sucesso!");
      navigate("/home", { replace: true });
    } catch (error: unknown) {
      console.error(error);
      const message = error instanceof Error ? error.message : "Erro inesperado. Tente novamente.";
      setErrorMessage(message);
      toast.error(message);
    } finally {
      setLoading(false);
    }
  };

  if (!loading && sessaoLocal && user) return <Navigate to="/home" replace />;
  if (!loading && sessaoLocal && !error) {
    return <p role="status" className={styles.loginRoot}>Verificando sessão...</p>;
  }

  return (
    <div className={styles.loginRoot}>
      <Responsive min="lg">
      <div className={styles.loginLeft}>
        <div className={styles.loginLeftBg} />

        <div className={styles.logo}>
          <img src="/assets/Geral/Logo.png" alt="JAF Construtora" />
          <span className={styles.logoName}>JAF Construtora</span>
        </div>

        <div className={styles.leftContent}>
          <h1 className={styles.leftHeadline}>
            Elevando o padrão
            <br />
            da construção
            <br />
            civil.
          </h1>
          <p className={styles.leftSub}>
            Acesse sua plataforma exclusiva de gestão de obras e acompanhe cada
            detalhe do seu projeto em tempo real.
          </p>
        </div>

        <p className={styles.leftFooter}>
          © 2026 JAF Construtora. Todos os direitos reservados.
        </p>
      </div>
      </Responsive>

      <div className={styles.loginRightContainer}>
        <div className={styles.loginRight}>
          <form className={styles.loginCard} onSubmit={funcLogin}>
            <Responsive max="md">
              <div className={styles.logoCompacto}>
                <img src="/assets/Geral/Logo.png" alt="" />
                <span>JAF Construtora</span>
              </div>
            </Responsive>
            <h2 className={styles.cardTitle}>Bem-vindo</h2>
            <p className={styles.cardSub}>
              Entre com suas credenciais para acessar o painel.
            </p>

            {errorMessage && (
              <div className={styles.errorBanner}>{errorMessage}</div>
            )}

            <div className={styles.fieldGroup}>
              <div className={styles.fieldRow}>
                <label htmlFor="login-email" className={styles.fieldLabel}>E-mail</label>
              </div>
              <input
                className={styles.fieldInput}
                type="email"
                id="login-email"
                required
                placeholder="exemplo@jaf.com.br"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  setErrorMessage(null);
                }}
                autoComplete="email"
              />
            </div>

            <div className={styles.fieldGroup}>
              <div className={styles.fieldRow}>
                <label htmlFor="login-senha" className={styles.fieldLabel}>Senha</label>
                <button className={styles.forgotLink} type="button" onClick={() => toast.info("A recuperação de senha não está disponível nesta plataforma. Entre em contato com o administrador da sua empresa.")}>
                  Esqueceu a senha?
                </button>
              </div>
              <div className={styles.inputWrap}>
                <input
                  className={`${styles.fieldInput} ${styles.hasIcon}`}
                  type={showPassword ? "text" : "password"}
                  id="login-senha"
                  required
                  placeholder="Digite sua senha"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setErrorMessage(null);
                  }}
                  autoComplete="current-password"
                />
                <button
                  className={styles.eyeBtn}
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  aria-label={showPassword ? "Ocultar senha" : "Mostrar senha"}
                >
                  {showPassword ? (
                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                      <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                      <line x1="1" y1="1" x2="23" y2="23" />
                    </svg>
                  ) : (
                    <svg
                      width="20"
                      height="20"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="2"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                      <circle cx="12" cy="12" r="3" />
                    </svg>
                  )}
                </button>
              </div>
            </div>

            <button
              className={styles.btnEntrar}
              type="submit"
              disabled={loading}
            >
              {loading ? "Entrando..." : "Entrar"}{" "}
              <span className={styles.btnArrow}>→</span>
            </button>

            <p className={styles.noAccess}>
              Não possui acesso à plataforma?
              <br />
              <span>Solicite seu cadastro ao administrador da sua empresa.</span>
            </p>
          </form>
        </div>
      </div>
    </div>
  );
}

export default Login;
