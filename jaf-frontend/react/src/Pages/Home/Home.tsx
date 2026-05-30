import {
  TrendingUp,
  Building2,
  CheckCircle,
  DollarSign,
} from "lucide-react";
import styles from "./Home.module.css";

import CardObra from "../../Components/CardObra/cardObra";
import { obraService } from "../../Service/Obras/obraService";
import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

export default function Home() {
  const [obras, setObras] = useState<any[]>([]);
  const [carregando, setCarregando] = useState(true);
  const navegar = useNavigate();

  useEffect(() => {
    async function carregarObras() {
      try {
        setCarregando(true);
        const obrasData = await obraService.listar();
        setObras(obrasData);
      } catch (error) {
        console.error("Erro ao carregar obras:", error);
      } finally {
        setCarregando(false);
      }
    }

    carregarObras();
  }, []);

  const handleCardClick = (obraId: number) => {
    navegar(`/obras/${obraId}`);
  };

  return (
    <div className={styles.pagina}>
      
      {/* HEADER */}
      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>Dashboard</h1>
      </div>

      {/* MÉTRICAS */}
      <div className={styles.metricas}>
        <div className={styles.card}>
          <span className={styles.rotulo}>OBRAS EM ANDAMENTO</span>
          <div className={styles.valorLinha}>
            <Building2 size={18} />
            <span className={styles.valorDestaque}>
              {obras.filter((o) => o.status === "EM_ANDAMENTO").length || 0}
            </span>
          </div>
          <span className={styles.sub}>Obras ativas</span>
        </div>

        <div className={styles.card}>
          <span className={styles.rotulo}>OBRAS FINALIZADAS</span>
          <div className={styles.valorLinha}>
            <CheckCircle size={18} />
            <span className={styles.valor}>
              {obras.filter((o) => o.status === "FINALIZADA").length || 0}
            </span>
          </div>
          <span className={styles.sub}>Obras completadas</span>
        </div>

        <div className={styles.card}>
          <span className={styles.rotulo}>TOTAL OBRAS</span>
          <div className={styles.valorLinha}>
            <DollarSign size={18} />
            <span className={styles.valorDestaque}>
              {obras.length}
            </span>
          </div>
          <div className={styles.tendencia}>
            <TrendingUp size={14} />
            Total cadastrado
          </div>
        </div>
      </div>

      {/* OBRAS RECENTES */}
      <div className={styles.secao}>
        <h2 className={styles.subtitulo}>Obras Recentes</h2>

        {carregando ? (
          <p>Carregando obras...</p>
        ) : (
          <div className={styles.lista}>
            {obras.length > 0 ? (
              obras.map((obra) => (
                <CardObra
                  key={obra.id}
                  nome={obra.titulo}
                  local={obra.status || "Status não definido"}
                  valor={parseFloat(obra.orcamento) || 0}
                  imagem="https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800"
                  onClick={() => handleCardClick(obra.id)}
                />
              ))
            ) : (
              <p>Nenhuma obra encontrada</p>
            )}
          </div>
        )}
      </div>
    </div>
  );
}