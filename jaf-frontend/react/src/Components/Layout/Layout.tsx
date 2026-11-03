import { useEffect, useState } from "react";
import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar/Sidebar";
import Navbar from "./Navbar/Navber";
import styles from "./Layout.module.css";
import { useBreakpoint } from "../../Hooks/useBreakpoint";
import { breakpointNoIntervalo } from "../../Constants/breakpoints";
import ErrorBoundary from "../ErrorBoundary/ErrorBoundary";

export default function Layout() {
  const breakpoint = useBreakpoint();
  const menuRecolhivel = !breakpointNoIntervalo(breakpoint, "lg");
  const [menuAberto, setMenuAberto] = useState(false);
  const menuVisivel = menuRecolhivel && menuAberto;

  useEffect(() => {
    if (!menuVisivel) return;
    const fecharComEsc = (evento: KeyboardEvent) => {
      if (evento.key === "Escape") setMenuAberto(false);
    };
    document.addEventListener("keydown", fecharComEsc);
    return () => document.removeEventListener("keydown", fecharComEsc);
  }, [menuVisivel]);

  return (
    <div className={styles.layout}>
      <Sidebar recolhivel={menuRecolhivel} aberta={menuVisivel} onNavegar={() => setMenuAberto(false)} />
      {menuVisivel && <div className={styles.fundoMenu} onClick={() => setMenuAberto(false)} aria-hidden="true" />}
      <div className={styles.mainArea}>
        <Navbar
          menuAberto={menuVisivel}
          onAbrirMenu={menuRecolhivel ? () => setMenuAberto(true) : undefined}
        />

        <main className={styles.content}>
          <ErrorBoundary><Outlet /></ErrorBoundary>
        </main>
      </div>
    </div>
  );
}
