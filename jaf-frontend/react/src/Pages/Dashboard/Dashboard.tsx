import { useState, useEffect } from "react";
import { ArrowLeft, Plus, Search, Tag, ChevronDown } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import ChartCard from "../../Components/ChartCard/ChartCard";
import StatCard from "../../Components/StatCard/StatCard";
import { dashboardService, type DashboardStats } from "../../Service/Dashboard/dashboardService";
import styles from "./Dashboard.module.css";

const PIE_COLORS = ["#F5C518", "#5A6B7B"];

const formatBRL = (v: number) =>
  `R$${v.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;

const etapas = ["ETAPA 1", "ETAPA 2", "TODAS"];

const limitarPercentual = (valor: number) => Math.min(100, Math.max(0, valor));

export default function Dashboard() {
  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();
  const obraId = id ? parseInt(id) : NaN;

  const [etapa, setEtapa] = useState("ETAPA 1");
  const [busca, setBusca] = useState("");
  const [ordenacao, setOrdenacao] = useState<"valor" | "categoria">("valor");
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

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
  const percentualPendente = totalReembolsos
    ? limitarPercentual(((stats?.reembolsosPizza[0]?.value ?? 0) / totalReembolsos) * 100)
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

  function trocarEtapa() {
    const indiceAtual = etapas.indexOf(etapa);
    setEtapa(etapas[(indiceAtual + 1) % etapas.length]);
  }

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
      {/* Header / Breadcrumb */}
      <div className={styles.header}>
        <div className={styles.breadcrumb}>
          <button className={styles.botaoVoltarBreadcrumb} onClick={() => navegar(`/obras/detalhamento/${obraId}`)} aria-label="Voltar para detalhes da obra">
            <ArrowLeft size={16} />
          </button>
          <span>Obras</span>
          <span className={styles.separator}>›</span>
          <span>Detalhes da Obra</span>
          <span className={styles.separator}>›</span>
          <span>Financeiro da Obra</span>
        </div>
        <button className={styles.addBtn} onClick={() => navegar(`/obras/detalhamento/${obraId}`)}>
          <Plus size={16} /> Adicionar gasto
        </button>
      </div>

      {/* Filters */}
      <div className={styles.filters}>
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
          action={<button className={styles.etapaBtn} onClick={trocarEtapa}>Trocar etapa</button>}
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
                  background: `conic-gradient(${PIE_COLORS[0]} 0 ${percentualPendente}%, ${PIE_COLORS[1]} ${percentualPendente}% 100%)`,
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

        {/* Line */}
        <ChartCard title="Gastos imprevistos">
          <div className={styles.chartContainer}>
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
          </div>
        </ChartCard>
      </div>

      {/* Bar chart full width */}
      <div className={styles.chartFull}>
        <ChartCard
          title="Gastos por categoria na obra"
          right={
            <div className={styles.etapaTabs}>
              {etapas.map((e) => (
                <button
                  key={e}
                  onClick={() => setEtapa(e)}
                  className={`${styles.etapaTab} ${
                    etapa === e ? styles.etapaTabActive : styles.etapaTabInactive
                  }`}
                >
                  {e === "TODAS" ? "Todas as etapas" : e}
                </button>
              ))}
            </div>
          }
        >
          <div className={styles.chartContainerLarge}>
            <div className={styles.barList}>
              {categoriasExibidas.map((item) => (
                <div key={item.categoria} className={styles.barItem}>
                  <div className={styles.barHeader}>
                    <span>{item.categoria}</span>
                    <strong>{formatBRL(item.valor)}</strong>
                  </div>
                  <div className={styles.barTrack}>
                    <div
                      className={styles.barFill}
                      style={{ width: `${limitarPercentual((item.valor / maxCategoria) * 100)}%` }}
                    />
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
