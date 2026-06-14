import { Outlet, useNavigationType } from "react-router-dom";
import { useEffect } from "react";
import { Sidebar } from "./Sidebar/Sidebar";
import Navbar from "./Navbar/Navber";
import styles from "./Layout.module.css";

export default function Layout() {
  const navigationType = useNavigationType();

  useEffect(() => {
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };

    window.addEventListener("beforeunload", handleBeforeUnload);

    return () => {
      window.removeEventListener("beforeunload", handleBeforeUnload);
    };
  }, []);

  useEffect(() => {
    if (navigationType === "POP") {
      const confirmLeave = window.confirm("Você tem certeza que deseja sair? Isso pode efetuar o logout.");
      if (!confirmLeave) {
        window.history.pushState(null, "", window.location.href);
      }
    }
  }, [navigationType]);

  return (
    <div className={styles.layout}>
      <Sidebar />
      <div className={styles.mainArea}>
        <Navbar />

        <main className={styles.content}>
          <Outlet />
        </main>
      </div>
    </div>
  );
}
