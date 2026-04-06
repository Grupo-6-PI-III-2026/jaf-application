import {
  LayoutGrid,
  Pickaxe,
  Wallet,
  ChartNoAxesCombined,
  Settings,
  Bell,
} from "lucide-react";
import { NavLink } from "react-router-dom";
import styles from "./Sidebar.module.css";

// Vetor com os itens do menu da sidebar
const menuItems = [
  { icon: LayoutGrid, label: "Dashboard", path: "/dashboard" },
  { icon: Pickaxe, label: "Obras", path: "/obras" },
  { icon: Wallet, label: "Financeiro", path: "/financeiro" },
  { icon: ChartNoAxesCombined, label: "Relatórios", path: "/relatorios" },
  { icon: Settings, label: "Configurações", path: "/configuracoes" },
];

export function Sidebar() {
    return (
        <div className={styles.sidebar}>
            {/* Logo e nome da empresa */}
            <div className={styles.margin}>
                <div className={styles.container}>
                    <div className={styles.circuloLogo}>
                        <img src="/assets/Gera/logo.png"/>
                    </div>
                    <div className={styles.nomeEmpresa}>
                        <h1>JAF CONSTRUTORA</h1>
                        <p> Gestão de gastos</p>
                    </div>
                </div>
            </div>
            {/* Menu de navegação */}
            <div className={styles.nav}>
                {menuItems.map(item => (
                    <div key={item.label} className={styles.menuItem}>
                        <NavLink to={item.path}>
                            <item.icon />
                            <span>{item.label}</span>
                        </NavLink>
                    </div>
                ))}
            </div>
            <div className={styles.perfil}>
                <div className={styles.circuloPerfil}></div>
                <div className={styles.nomePerfil}>
                        <h1>Guilherme Peres</h1>
                        <p>Administrador da Obra</p>
                </div>
                <div><Bell /></div>
            </div>
        </div>
    );
}