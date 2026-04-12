import { Outlet } from "react-router-dom";
import { Sidebar } from "./Sidebar/Sidebar";
import Navbar from "./Navbar/Navber";
import styles from "./Layout.module.css";

export default function Layout() {
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
