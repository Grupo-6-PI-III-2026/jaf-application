import { useNavigate } from "react-router-dom";
import { ArrowLeft} from "lucide-react";
import styles from "./Navbar.module.css";

export default function Navbar() {
  const navigate = useNavigate();

  return (
    <header className={styles.navbar}>
      <button className={styles.backButton} onClick={() => navigate(-1)}>
        <ArrowLeft size={20} />
      </button>

      <div className={styles.spacer}></div>
    </header>
  );
}