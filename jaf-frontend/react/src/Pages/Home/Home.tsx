import {
  TrendingUp,
  Building2,
  CheckCircle,
  DollarSign,
} from "lucide-react";
import styles from "./Home.module.css";

import CardObra from "../../Components/CardObra/cardObra";

const obrasRecentes = [
  {
    nome: "Obra Alphaville",
    local: "Residencial 1, Barueri - SP",
    valor: 149500,
    imagem:
      "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
  },
  {
    nome: "Obra Osasco",
    local: "Centro, Osasco - SP",
    valor: 98000,
    imagem:
      "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800",
  },
];

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

export default function Home() {
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
            <span className={styles.valorDestaque}>08</span>
          </div>
          <span className={styles.sub}>+2 esse mês</span>
        </div>

        <div className={styles.card}>
          <span className={styles.rotulo}>OBRAS FINALIZADAS</span>
          <div className={styles.valorLinha}>
            <CheckCircle size={18} />
            <span className={styles.valor}>42</span>
          </div>
          <span className={styles.sub}>+5 esse mês</span>
        </div>

        <div className={styles.card}>
          <span className={styles.rotulo}>TOTAL GASTO</span>
          <div className={styles.valorLinha}>
            <DollarSign size={18} />
            <span className={styles.valorDestaque}>
              {formatarMoeda(2609500)}
            </span>
          </div>
          <div className={styles.tendencia}>
            <TrendingUp size={14} />
            8% acima do mês anterior
          </div>
        </div>
      </div>

      {/* OBRAS RECENTES */}
      <div className={styles.secao}>
        <h2 className={styles.subtitulo}>Obras Recentes</h2>

        <div className={styles.lista}>
          {obrasRecentes.map((obra, index) => (
            <CardObra
              key={index}
              nome={obra.nome}
              local={obra.local}
              valor={obra.valor}
              imagem={obra.imagem}
            />
          ))}
        </div>
      </div>
    </div>
  );
}