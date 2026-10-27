import { useMemo, useState } from "react";
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
  const breadcrumb = useMemo(() => getBreadcrumb(location.pathname), [location.pathname]);

  const handleLogout = () => {
    clearUser();
    authService.logout();
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
        <button className={styles.backButton} onClick={() => navigate(-1)} aria-label="Voltar">
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
          <div className={styles.modalLogout} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>Sair da aplicação?</h2>
                <p>Confirme apenas se deseja encerrar sua sessão atual.</p>
              </div>
              <button type="button" className={styles.modalClose} onClick={() => setModalLogoutAberto(false)} aria-label="Fechar aviso de saída">
                <X size={18} />
              </button>
            </header>
            <footer className={styles.modalFooter}>
              <button type="button" className={styles.cancelButton} onClick={() => setModalLogoutAberto(false)}>
                Continuar no sistema
              </button>
              <button type="button" className={styles.confirmButton} onClick={handleLogout}>
                Sair
              </button>
            </footer>
          </div>
        </div>
      )}
    </>
  );
}
