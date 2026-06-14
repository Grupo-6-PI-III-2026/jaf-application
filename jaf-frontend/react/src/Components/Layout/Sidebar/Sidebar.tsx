import {
  CalendarCheck,
  Pickaxe,
  Settings,
  Bell,
  UserPlus,
  HardHat,
  Shield,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import styles from "./Sidebar.module.css";
import { useUser } from "../../../Context/useUser";
import { CargoLabel, DEFAULT_AVATAR_URL } from "../../../Types/user";
import { authService } from "../../../Service/Auth/Login/authService";

const menuSections = [
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
      { icon: Shield, label: "Perfis de Acesso", path: "/permissoes", permissao: "EDITAR_FUNCIONARIO" },
    ],
  },
  {
    title: "Conta",
    items: [
      { icon: Settings, label: "Perfil", path: "/perfil" },
    ],
  },
];

export function Sidebar() {
  const { user } = useUser();
  const avatarUrl = user?.fotoUrl || DEFAULT_AVATAR_URL;
  const userName = user?.nome ?? "Usuario";
  const userCargo = user ? CargoLabel[user.cargo] : "Sem cargo";
  const visibleMenuSections = menuSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => !item.permissao || authService.hasAuthority(item.permissao)),
    }))
    .filter((section) => section.items.length > 0);

  return (
    <div className={styles.sidebar}>
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
        </div>
      </div>
      {/* Menu de navegação */}
      <div className={styles.nav}>
        {visibleMenuSections.map((section) => (
          <div key={section.title} className={styles.navSection}>
            <span className={styles.navSectionTitle}>{section.title}</span>
            {section.items.map((item) => (
              <div key={item.label} className={styles.menuItem}>
                <NavLink to={item.path}>
                  <item.icon />
                  <span>{item.label}</span>
                </NavLink>
              </div>
            ))}
          </div>
        ))}
      </div>
      <div className={styles.perfil}>
        <div className={styles.circuloPerfil}>
          <img src={avatarUrl} alt={userName} />
        </div>
        <div className={styles.nomePerfil}>
          <h1>{userName}</h1>
          <p>{userCargo}</p>
        </div>
        <NavLink
          className={styles.notificationBtn}
          to="/perfil"
          title="Perfil"
          aria-label="Ir para perfil"
        >
          <Bell />
        </NavLink>
      </div>
    </div>
  );
}
