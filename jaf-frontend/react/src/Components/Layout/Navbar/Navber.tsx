import { useNavigate } from "react-router-dom";
import { ArrowLeft, LogOut, User } from "lucide-react";
import styles from "./Navbar.module.css";
import { authService } from "../../../Service/Auth/Login/authService";

export default function Navbar() {
  const navigate = useNavigate();
  const userEmail = authService.getUserEmail();

  const handleLogout = () => {
    if (confirm("Deseja realmente sair?")) {
      authService.logout();
    }
  };

  return (
    <header className={styles.navbar}>
      <button className={styles.backButton} onClick={() => navigate(-1)}>
        <ArrowLeft size={20} />
      </button>

      <div className={styles.spacer}></div>
      
      {/* Informações do usuário */}
      <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#666' }}>
          <User size={18} />
          <span style={{ fontSize: '14px' }}>{userEmail}</span>
        </div>
        
        <button 
          onClick={handleLogout}
          style={{ 
            display: 'flex', 
            alignItems: 'center', 
            gap: '8px',
            padding: '8px 12px',
            background: '#ff4444',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '14px'
          }}
          title="Sair"
        >
          <LogOut size={16} />
          Sair
        </button>
      </div>
    </header>
  );
}