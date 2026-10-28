import {
  CalendarCheck,
  Pickaxe,
  Settings,
  UserPlus,
  HardHat,
  Shield,
  X,
  type LucideIcon,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import styles from "./Sidebar.module.css";
import { useUser } from "../../../Context/useUser";
import { CargoLabel, DEFAULT_AVATAR_URL } from "../../../Types/user";
import { authService } from "../../../Service/Auth/Login/authService";

interface MenuItem {
  icon: LucideIcon;
  label: string;
  path: string;
  permissao?: string;
  adminOnly?: boolean;
}

const menuSections: { title: string; items: MenuItem[] }[] = [
  {
    title: "Operação",
    items: [
      { icon: Pickaxe, label: "Obras", path: "/home" },
      { icon: CalendarCheck, label: "Presenças", path: "/presencas", permissao: "VISUALIZAR_PRESENCAS" },
    ],
  },
  {
    title: "Administração",
    items: [
      { icon: HardHat, label: "Nova Obra", path: "/obras/criar", permissao: "CRIAR_OBRA" },
      { icon: UserPlus, label: "Novo Usuário", path: "/funcionarios/novo", permissao: "CRIAR_FUNCIONARIO" },
      { icon: Shield, label: "Perfis de Acesso", path: "/permissoes", adminOnly: true },
    ],
  },
];

interface SidebarProps {
  recolhivel?: boolean;
  aberta?: boolean;
  onNavegar?: () => void;
}

export function Sidebar({ recolhivel = false, aberta = false, onNavegar }: SidebarProps) {
  const { user } = useUser();
  const avatarUrl = user?.fotoUrl || DEFAULT_AVATAR_URL;
  const visibleMenuSections = menuSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) =>
        (!item.adminOnly || authService.isAdmin()) &&
        (!item.permissao || authService.hasAuthority(item.permissao))),
    }))
    .filter((section) => section.items.length > 0);

  return (
    <aside
      id="menu-principal"
      aria-label="Menu principal"
      className={`${styles.sidebar} ${recolhivel ? styles.recolhivel : ""} ${aberta ? styles.aberta : ""}`}
      inert={recolhivel && !aberta}
    >
      {/* Logo e nome da empresa */}
      <div className={styles.margin}>
        <div className={styles.container}>
          <div className={styles.circuloLogo}>
            <img src="/assets/Geral/Logo.png" alt="JAF Logo" />
          </div>
          <div className={styles.nomeEmpresa}>
            <h1>JAF CONSTRUTORA</h1>
            <p> Gestão de gastos</p>
          </div>
          {recolhivel && (
            <button type="button" className={styles.botaoFechar} onClick={onNavegar} aria-label="Fechar menu">
              <X size={20} />
            </button>
          )}
        </div>
      </div>
      {/* Menu de navegação */}
      <div className={styles.nav}>
        {visibleMenuSections.map((section) => (
          <div key={section.title} className={styles.navSection}>
            <span className={styles.navSectionTitle}>{section.title}</span>
            {section.items.map((item) => (
              <div key={item.label} className={styles.menuItem}>
                <NavLink to={item.path} onClick={onNavegar}>
                  <item.icon />
                  <span>{item.label}</span>
                </NavLink>
              </div>
            ))}
          </div>
        ))}
      </div>
      {user && (
        <div className={styles.perfil}>
          <div className={styles.circuloPerfil}>
            <img src={avatarUrl} alt={user.nome} />
          </div>
          <div className={styles.nomePerfil}>
            <h1>{user.nome}</h1>
            <p>{CargoLabel[user.cargo] ?? user.cargo}</p>
          </div>
          <NavLink
            className={styles.notificationBtn}
            to="/perfil"
            onClick={onNavegar}
            title="Perfil"
            aria-label="Ir para perfil"
          >
            <Settings />
          </NavLink>
        </div>
      )}
    </aside>
  );
}
