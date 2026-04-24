import { useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Login.module.css";
import { authService } from "../../../Service/Auth/Login/authService";
import { type LoginCredentials } from "../../../Types/auth";

function Login() {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null); 

  const navigate = useNavigate();

  const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  const validateInputs = () => {
    if (!emailRegex.test(email)) return false;

    const hasMinLength = password.length >= 8;
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasSpecial = /[^A-Za-z0-9]/.test(password);

    return hasMinLength && hasUpper && hasLower && hasSpecial;
  };

  const funcLogin = async (evento: React.MouseEvent) => {
    evento.preventDefault();
    setErrorMessage(null);

    if (!validateInputs()) {
      setErrorMessage("E-mail ou senha inválidos. Verifique suas credenciais.");
      return;
    }

    const dadosParaLogar: LoginCredentials = { email, password };

    try {
      const dataUser = await authService.login(dadosParaLogar);

      if (dataUser?.id) {
        localStorage.setItem("userId", dataUser.id.toString());
      }

      navigate("/funcionarios/novo");
    } catch (error) {
      console.error(error);
      setErrorMessage("E-mail ou senha inválidos. Verifique suas credenciais.");
    }
  };

  return (
    <div className={styles.loginRoot}>
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

      <div className={styles.loginRightContainer}>
        <div className={styles.loginRight}>
          <div className={styles.loginCard}>
            <h2 className={styles.cardTitle}>Bem-vindo</h2>
            <p className={styles.cardSub}>
              Entre com suas credenciais para acessar o painel.
            </p>

            
          
            {errorMessage && (
              <div className={styles.errorBanner}>{errorMessage}</div>
            )}

            <div className={styles.fieldGroup}>
              <div className={styles.fieldRow}>
                <label className={styles.fieldLabel}>E-mail</label>
              </div>
              <input
                className={styles.fieldInput}
                type="email"
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
                <label className={styles.fieldLabel}>Senha</label>
                <button className={styles.forgotLink} type="button">
                  Esqueceu a senha?
                </button>
              </div>
              <div className={styles.inputWrap}>
                <input
                  className={`${styles.fieldInput} ${styles.hasIcon}`}
                  type={showPassword ? "text" : "password"}
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
              onClick={funcLogin}
              type="button"
            >
              Entrar <span className={styles.btnArrow}>→</span>
            </button>

            <p className={styles.noAccess}>
              Não possui acesso à plataforma?
              <br />
              <a href="#">Solicite aqui</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
