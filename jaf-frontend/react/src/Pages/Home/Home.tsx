import { useEffect, useState } from "react";
import {
  TrendingUp,
  Building2,
  CheckCircle,
  DollarSign,
} from "lucide-react";
import styles from "./Home.module.css";

import CardObra from "../../Components/CardObra/cardObra";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { gastoService, type Gasto } from "../../Service/Gastos/gastoService";

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

// Array de imagens placeholder para as obras
const imagensObras = [
  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
  "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800",
  "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=800",
  "https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=800",
];

export default function Home() {
  const [obras, setObras] = useState<Obra[]>([]);
  const [gastos, setGastos] = useState<Gasto[]>([]);
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    const carregarDados = async () => {
      try {
        setCarregando(true);
        const [obrasData, gastosData] = await Promise.all([
          obraService.listar(),
          gastoService.listar(),
        ]);
        setObras(obrasData);
        setGastos(gastosData);
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
      } finally {
        setCarregando(false);
      }
    };

    carregarDados();
  }, []);

  // Calcular KPIs
  const obrasEmAndamento = obras.filter(
    (obra) => obra.status === "EM_ANDAMENTO" || obra.status === "EM ANDAMENTO"
  ).length;

  const obrasFinalizadas = obras.filter(
    (obra) => obra.status === "CONCLUIDA" || obra.status === "CONCLUÍDA"
  ).length;

  const totalGasto = gastos.reduce((total, gasto) => total + gasto.valor, 0);

  // Pegar as obras mais recentes (últimas 2)
  const obrasRecentes = obras
    .sort((a, b) => new Date(b.dtInicio).getTime() - new Date(a.dtInicio).getTime())
    .slice(0, 2);

  return (
    <div className={styles.pagina}>
      
      {/* HEADER */}
      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>Dashboard</h1>
      </div>

      {carregando ? (
        <div style={{ textAlign: "center", padding: "2rem" }}>Carregando...</div>
      ) : (
        <>
          {/* MÉTRICAS */}
          <div className={styles.metricas}>
            <div className={styles.card}>
              <span className={styles.rotulo}>OBRAS EM ANDAMENTO</span>
              <div className={styles.valorLinha}>
                <Building2 size={18} />
                <span className={styles.valorDestaque}>{obrasEmAndamento}</span>
              </div>
              <span className={styles.sub}>Obras ativas</span>
            </div>

            <div className={styles.card}>
              <span className={styles.rotulo}>OBRAS FINALIZADAS</span>
              <div className={styles.valorLinha}>
                <CheckCircle size={18} />
                <span className={styles.valor}>{obrasFinalizadas}</span>
              </div>
              <span className={styles.sub}>Obras concluídas</span>
            </div>

            <div className={styles.card}>
              <span className={styles.rotulo}>TOTAL GASTO</span>
              <div className={styles.valorLinha}>
                <DollarSign size={18} />
                <span className={styles.valorDestaque}>
                  {formatarMoeda(totalGasto)}
                </span>
              </div>
              <div className={styles.tendencia}>
                <TrendingUp size={14} />
                Gastos acumulados
              </div>
            </div>
          </div>

          {/* OBRAS RECENTES */}
          <div className={styles.secao}>
            <h2 className={styles.subtitulo}>Obras Recentes</h2>

            <div className={styles.lista}>
              {obrasRecentes.length > 0 ? (
                obrasRecentes.map((obra, index) => (
                  <CardObra
                    key={obra.id}
                    id={obra.id}
                    nome={obra.titulo}
                    local={obra.status}
                    valor={parseFloat(obra.orcamento) || 0}
                    imagem={imagensObras[index % imagensObras.length]}
                  />
                ))
              ) : (
                <p>Nenhuma obra encontrada.</p>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}