import { useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { ArrowLeft, LogOut, Menu, X } from "lucide-react";
import styles from "./Navbar.module.css";
import { authService } from "../../../Service/Auth/Login/authService";
import { useUser } from "../../../Context/useUser";

const getBreadcrumb = (pathname: string) => {
  if (pathname === "/home") return ["Obras"];
  if (pathname === "/perfil") return ["Perfil"];
  if (pathname === "/presencas") return ["Presenças"];
  if (pathname === "/funcionarios/novo") return ["Administração", "Novo usuário"];
  if (pathname === "/obras/criar") return ["Obras", "Nova obra"];
  if (pathname === "/permissoes") return ["Administração", "Perfis de acesso"];
  if (/^\/obras\/\d+\/alocacoes$/.test(pathname)) return ["Obras", "Alocações"];
  if (/^\/obras\/detalhamento\/\d+\/financeiro$/.test(pathname)) return ["Obras", "Detalhes da obra", "Financeiro"];
  if (/^\/obras\/detalhamento(\/\d+)?$/.test(pathname)) return ["Obras", "Detalhes da obra"];
  return ["JAF"];
};

interface NavbarProps {
  menuAberto?: boolean;
  onAbrirMenu?: () => void;
}

export default function Navbar({ menuAberto = false, onAbrirMenu }: NavbarProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { clearUser } = useUser();
  const [modalLogoutAberto, setModalLogoutAberto] = useState(false);
  const [saindo, setSaindo] = useState(false);
  const cancelarRef = useRef<HTMLButtonElement>(null);
  const breadcrumb = useMemo(() => getBreadcrumb(location.pathname), [location.pathname]);

  useEffect(() => {
    if (!modalLogoutAberto) return;
    cancelarRef.current?.focus();
    const fecharComEsc = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") setModalLogoutAberto(false);
    };
    document.addEventListener("keydown", fecharComEsc);
    return () => document.removeEventListener("keydown", fecharComEsc);
  }, [modalLogoutAberto]);

  const handleLogout = () => {
    setSaindo(true);
    clearUser();
    void authService.logout();
  };

  const handleVoltar = () => {
    // idx do BrowserRouter: 0 significa que não há página interna anterior (login usa replace).
    const indice = (window.history.state as { idx?: number } | null)?.idx ?? 0;
    if (indice > 0) navigate(-1);
    else navigate("/home", { replace: true });
  };

  return (
    <>
      <header className={styles.navbar}>
        {onAbrirMenu && (
          <button
            type="button"
            className={styles.backButton}
            onClick={onAbrirMenu}
            aria-label="Abrir menu"
            aria-controls="menu-principal"
            aria-expanded={menuAberto}
          >
            <Menu size={20} />
          </button>
        )}
        <button className={styles.backButton} onClick={handleVoltar} aria-label="Voltar">
          <ArrowLeft size={20} />
        </button>

        <nav className={styles.breadcrumb} aria-label="Breadcrumb">
          {breadcrumb.map((item, index) => (
            <span key={`${item}-${index}`} className={index === breadcrumb.length - 1 ? styles.breadcrumbAtivo : undefined}>
              {item}
            </span>
          ))}
        </nav>

        <div className={styles.spacer}></div>

        <button onClick={() => setModalLogoutAberto(true)} className={styles.logoutButton} title="Sair" aria-label="Sair">
          <LogOut size={16} />
          <span className={styles.logoutTexto}>Sair</span>
        </button>
      </header>

      {modalLogoutAberto && (
        <div className={styles.modalOverlay} onClick={() => setModalLogoutAberto(false)}>
          <div
            className={styles.modalLogout}
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="titulo-sair"
            aria-describedby="descricao-sair"
            onClick={(evento) => evento.stopPropagation()}
          >
            <header className={styles.modalHeader}>
              <div>
                <h2 id="titulo-sair">Deseja sair da sua conta?</h2>
                <p id="descricao-sair">Você precisará entrar novamente para acessar o sistema.</p>
              </div>
              <button type="button" className={styles.modalClose} onClick={() => setModalLogoutAberto(false)} aria-label="Fechar aviso de saída">
                <X size={18} />
              </button>
            </header>
            <footer className={styles.modalFooter}>
              <button ref={cancelarRef} type="button" className={styles.cancelButton} onClick={() => setModalLogoutAberto(false)} disabled={saindo}>
                Cancelar
              </button>
              <button type="button" className={styles.confirmButton} onClick={handleLogout} disabled={saindo}>
                Confirmar
              </button>
            </footer>
          </div>
        </div>
      )}
    </>
  );
}
