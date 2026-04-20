import { useState } from "react";
import { useNavigate } from "react-router-dom";
import styles from "./Login.module.css";
import { authService } from "../../../Service/Auth/Login/authService";
import { type LoginCredentials } from "../../../Types/auth";

function Login() {
  const navigate = useNavigate();
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errors, setErrors] = useState<{ email?: string; password?: string }>(
    {},
  );

  const dadosParaLogar: LoginCredentials = {
    email: email,
    senha: password, 
  };

  const emailRegex =
    /^[A-Z0-9._%+-]+@(gmail|hotmail|outlook|yahoo|live)\.[A-Z]{2,}(\.[A-Z]{2,})?$/i;

  const validateInputs = () => {
    const nextErrors: { email?: string; password?: string } = {};

    if (!emailRegex.test(email)) {
      nextErrors.email =
        "Use um email valido (gmail, hotmail, outlook, yahoo, live).";
    }

    const hasMinLength = password.length >= 8;
    const hasUpper = /[A-Z]/.test(password);
    const hasLower = /[a-z]/.test(password);
    const hasSpecial = /[^A-Za-z0-9]/.test(password);

    if (!hasMinLength || !hasUpper || !hasLower || !hasSpecial) {
      nextErrors.password =
        "A senha precisa de 8 caracteres, 1 maiuscula, 1 minuscula e 1 especial.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const funcLogin = async (evento: React.MouseEvent) => {
    evento.preventDefault();

    if (!validateInputs()) {
      return;
    }

    setLoading(true);

    try {
      const dataUser = await authService.login(dadosParaLogar);

      console.log("Login concluído com sucesso", dataUser);
      
      // Redireciona para a página de funcionários após login
      navigate("/funcionarios/novo");
    } catch (error: any) {
      console.error("Erro no login:", error);
      alert(error.message || "Usuário ou senha inválidos");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.loginRoot}>
      <div className={styles.loginLeft}>
        <div className={styles.loginLeftBg} />

        <div className={styles.logo}>
          <img src="src\assets\Geral\Logo.png" alt="" />
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

      {/* Direita */}
      <div className={styles.loginRightContainer}>
        <div className={styles.loginRight}>
          <div className={styles.loginCard}>
            <h2 className={styles.cardTitle}>Bem-vindo</h2>
            <p className={styles.cardSub}>
              Entre com suas credenciais para acessar o painel.
            </p>

            <div className={styles.fieldGroup}>
              <div className={styles.fieldRow}>
                <label className={styles.fieldLabel}>E-mail</label>
              </div>
              <input
                className={`${styles.fieldInput} ${errors.email ? styles.inputError : ""}`}
                type="email"
                placeholder="exemplo@jaf.com.br"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  if (errors.email) {
                    setErrors((prev) => ({ ...prev, email: undefined }));
                  }
                }}
                autoComplete="email"
              />
              {errors.email && (
                <p className={styles.errorText}>{errors.email}</p>
              )}
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
                  className={`${styles.fieldInput} ${styles.hasIcon} ${
                    errors.password ? styles.inputError : ""
                  }`}
                  type={showPassword ? "text" : "password"}
                  placeholder="Digite sua senha"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (errors.password) {
                      setErrors((prev) => ({ ...prev, password: undefined }));
                    }
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
              {errors.password && (
                <p className={styles.errorText}>{errors.password}</p>
              )}
            </div>

            <button
              className={styles.btnEntrar}
              onClick={funcLogin}
              type="button"
              disabled={loading}
            >
              {loading ? "Entrando..." : "Entrar"} <span className={styles.btnArrow}>→</span>
            </button>

            <p className={styles.noAccess}>
              Não possui acesso à plataforma?
              <br />
              <a href="#">Solicite aqui</a>
            </p>
            
            {/* Credenciais de teste */}
            <div style={{ marginTop: '20px', padding: '10px', background: '#f0f0f0', borderRadius: '5px', fontSize: '12px' }}>
              <strong>Usuário de teste:</strong><br />
              Email: admin@jaf.com<br />
              Senha: Admin@123
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Login;
