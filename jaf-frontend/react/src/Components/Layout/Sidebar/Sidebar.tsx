import {
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

// Vetor com os itens do menu da sidebar
const menuItems = [
  { icon: Pickaxe, label: "Obras", path: "/home" },
  { icon: HardHat, label: "Nova Obra", path: "/obras/criar", permissao: "CRIAR_OBRA" },
  { icon: UserPlus, label: "Novo Funcionário", path: "/funcionarios/novo", permissao: "CRIAR_FUNCIONARIO" },
  { icon: Shield, label: "Permissões", path: "/permissoes", permissao: "EDITAR_FUNCIONARIO" },
  { icon: Settings, label: "Configurações", path: "/perfil" },
];

export function Sidebar() {
  const { user } = useUser();
  const avatarUrl = user?.fotoUrl || DEFAULT_AVATAR_URL;
  const userName = user?.nome ?? "Usuario";
  const userCargo = user ? CargoLabel[user.cargo] : "Sem cargo";
  const visibleMenuItems = menuItems.filter(
    (item) => !item.permissao || authService.hasAuthority(item.permissao)
  );

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
        {visibleMenuItems.map((item) => (
          <div key={item.label} className={styles.menuItem}>
            <NavLink to={item.path}>
              <item.icon />
              <span>{item.label}</span>
            </NavLink>
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
