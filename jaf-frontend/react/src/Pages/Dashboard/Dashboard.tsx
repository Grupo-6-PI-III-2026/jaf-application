import { useState, useEffect } from "react";
import { Plus, Search, Tag, ChevronDown } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import ChartCard from "../../Components/ChartCard/ChartCard";
import StatCard from "../../Components/StatCard/StatCard";
import { dashboardService, type DashboardStats } from "../../Service/Dashboard/dashboardService";
import { authService } from "../../Service/Auth/Login/authService";
import styles from "./Dashboard.module.css";

const PIE_COLORS = ["#5A6B7B", "#F5C518"];

const formatBRL = (v: number) =>
  `R$${v.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;

const etapas = ["TODAS", "ETAPA 1", "ETAPA 2", "ETAPA 3"];

const limitarPercentual = (valor: number) => Math.min(100, Math.max(0, valor));

export default function Dashboard() {
  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();
  const obraId = id ? parseInt(id) : NaN;

  const [etapa, setEtapa] = useState("TODAS");
  const [busca, setBusca] = useState("");
  const [ordenacao, setOrdenacao] = useState<"valor" | "categoria">("valor");
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const podeCriarGasto = authService.hasAuthority("CRIAR_GASTO");

  useEffect(() => {
    const carregarStats = async () => {
      if (!Number.isFinite(obraId)) {
        setErro("Obra não informada para o dashboard financeiro.");
        setCarregando(false);
        return;
      }

      try {
        setCarregando(true);
        setErro(null);
        const data = await dashboardService.buscarStats(obraId, etapa);
        setStats(data);
      } catch {
        setErro("Erro ao carregar dados do dashboard.");
      } finally {
        setCarregando(false);
      }
    };

    carregarStats();
  }, [obraId, etapa]);

  const categoriasExibidas = (stats?.gastosPorCategoria ?? [])
    .filter((item) => item.categoria.toLowerCase().includes(busca.trim().toLowerCase()))
    .sort((a, b) => {
      if (ordenacao === "categoria") {
        return a.categoria.localeCompare(b.categoria);
      }
      return b.valor - a.valor;
    });

  const totalReembolsos = stats?.reembolsosPizza.reduce((total, item) => total + item.value, 0) ?? 0;
  const reembolsosPendentesPizza = stats?.reembolsosPizza.find((item) => item.name === "Pendentes")?.value ?? 0;
  const percentualPendente = totalReembolsos
    ? limitarPercentual((reembolsosPendentesPizza / totalReembolsos) * 100)
    : 0;
  const maxImprevisto = Math.max(...(stats?.gastosImprevistos.map((item) => item.valor) ?? [0]), 1);
  const pontosLinha = (stats?.gastosImprevistos ?? [])
    .map((item, index, lista) => {
      const x = lista.length <= 1 ? 50 : (index / (lista.length - 1)) * 100;
      const y = 90 - (item.valor / maxImprevisto) * 70;
      return `${x},${y}`;
    })
    .join(" ");
  const maxCategoria = Math.max(...categoriasExibidas.map((item) => item.valor), 1);

  if (carregando) {
    return (
      <div className={styles.dashboard}>
        <div style={{ textAlign: "center", padding: "2rem" }}>Carregando...</div>
      </div>
    );
  }

  if (erro || !stats) {
    return (
      <div className={styles.dashboard}>
        <div style={{ textAlign: "center", padding: "2rem" }}>{erro ?? "Sem dados."}</div>
      </div>
    );
  }

  return (
    <div className={styles.dashboard}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.titulo}>Estatísticas financeiras</h1>
          <p className={styles.subtitulo}>Acompanhe gastos, reembolsos e categorias da obra.</p>
        </div>
        {podeCriarGasto && (
          <button className={styles.addBtn} onClick={() => navegar(`/obras/detalhamento/${obraId}`)}>
            <Plus size={16} /> Adicionar gasto
          </button>
        )}
      </div>

      <div className={styles.filters}>
        <label className={styles.selectWrapper}>
          Etapa
          <select value={etapa} onChange={(evento) => setEtapa(evento.target.value)}>
            {etapas.map((item) => (
              <option key={item} value={item}>{item === "TODAS" ? "Todas as etapas" : item}</option>
            ))}
          </select>
        </label>
        <div className={styles.searchWrapper}>
          <Search className={styles.searchIcon} />
          <input placeholder="Buscar categoria..." className={styles.searchInput} value={busca} onChange={(evento) => setBusca(evento.target.value)} />
        </div>
        <button className={`${styles.filterBtn} ${ordenacao === "valor" ? styles.filterBtnActive : ""}`} onClick={() => setOrdenacao("valor")}>
          <ChevronDown size={16} color="#F5C518" /> Valor
        </button>
        <button className={`${styles.filterBtn} ${ordenacao === "categoria" ? styles.filterBtnActive : ""}`} onClick={() => setOrdenacao("categoria")}>
          <Tag size={16} /> Categoria
        </button>
      </div>

      {/* Stats */}
      <div className={styles.statsRow}>
        <StatCard
          label={`Gastos da ${etapa === "TODAS" ? "obra" : etapa.toLowerCase()}`}
          value={formatBRL(stats.gastosEtapa)}
          progress={stats.progressoEtapa}
        />
        <StatCard
          label="Total de reembolsos pendentes"
          value={formatBRL(stats.reembolsosPendentes)}
        />
        <StatCard
          label="Saldo restante do orçamento"
          value={formatBRL(stats.saldoRestante)}
          progress={stats.progressoSaldo}
        />
      </div>

      {/* Charts row */}
      <div className={styles.chartsGrid}>
        {/* Pie */}
        <ChartCard title="Reembolsos pendentes x concluídos">
          <div className={styles.chartContainer}>
            <div className={styles.pieLayout}>
              <div
                className={styles.pieChart}
                style={{
                  background: `conic-gradient(${PIE_COLORS[1]} 0 ${percentualPendente}%, ${PIE_COLORS[0]} ${percentualPendente}% 100%)`,
                }}
                aria-label={`Reembolsos pendentes ${percentualPendente.toFixed(0)}%`}
              >
                <span>{percentualPendente.toFixed(0)}%</span>
              </div>
              <div className={styles.chartLegend}>
                {stats.reembolsosPizza.map((item, index) => (
                  <div key={item.name} className={styles.legendItem}>
                    <span style={{ background: PIE_COLORS[index] }} />
                    <strong>{item.name}</strong>
                    <small>{item.value}%</small>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </ChartCard>

        <ChartCard title="Gastos imprevistos">
          <div className={styles.chartContainer}>
            {stats.gastosImprevistos.length > 0 ? (
              <div className={styles.lineChartWrap}>
                <svg className={styles.lineChart} viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
                  <polyline points={pontosLinha} />
                </svg>
                <div className={styles.lineLabels}>
                  {stats.gastosImprevistos.map((item) => (
                    <span key={item.mes}>{item.mes}</span>
                  ))}
                </div>
                <div className={styles.lineValues}>
                  {stats.gastosImprevistos.map((item) => (
                    <span key={`${item.mes}-${item.valor}`}>{formatBRL(item.valor)}</span>
                  ))}
                </div>
              </div>
            ) : (
              <div className={styles.emptyChart}>Nenhum gasto imprevisto na etapa selecionada.</div>
            )}
          </div>
        </ChartCard>
      </div>

      <div className={styles.chartFull}>
        <ChartCard title="Gastos por categoria">
          <div className={styles.chartContainerLarge}>
            <div className={styles.categoryChart}>
              {categoriasExibidas.map((item) => (
                <div key={item.categoria} className={styles.categoryColumn}>
                  <div className={styles.categoryBarWrap}>
                    <div
                      className={styles.categoryBar}
                      style={{ height: `${Math.max(12, limitarPercentual((item.valor / maxCategoria) * 100))}%` }}
                    />
                  </div>
                  <div className={styles.categoryInfo}>
                    <strong>{formatBRL(item.valor)}</strong>
                    <span>{item.categoria}</span>
                  </div>
                </div>
              ))}
              {categoriasExibidas.length === 0 && (
                <div className={styles.emptyChart}>Nenhuma categoria encontrada.</div>
              )}
            </div>
          </div>
        </ChartCard>
      </div>
    </div>
  );
}
