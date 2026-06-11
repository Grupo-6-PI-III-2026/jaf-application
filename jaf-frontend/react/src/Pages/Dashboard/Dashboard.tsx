import { useState, useEffect } from "react";
import { ArrowLeft, Plus, Search, Calendar, ChevronDown } from "lucide-react";
import {
  PieChart, Pie, Cell, ResponsiveContainer, Legend,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  BarChart, Bar,
} from "recharts";
import { useParams } from "react-router-dom";
import ChartCard from "../../Components/ChartCard/ChartCard";
import StatCard from "../../Components/StatCard/StatCard";
import { dashboardService, type DashboardStats } from "../../Service/Dashboard/dashboardService";
import styles from "./Dashboard.module.css";

const PIE_COLORS = ["#F5C518", "#5A6B7B"];

const formatBRL = (v: number) =>
  `R$${v.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;

const etapas = ["ETAPA 1", "ETAPA 2", "TODAS"];

const formatarPercentual = ({ value }: { value?: number | string }) => `${value ?? 0}%`;
const formatarEixoMoeda = (valor: number) => `R$${(valor / 1000).toFixed(0)}.000,00`;
const formatarTooltipMoeda = (valor: unknown) => formatBRL(Number(valor ?? 0));
const formatarLegenda = (valor: string | number) => (
  <span style={{ fontSize: 12, textTransform: "uppercase", color: "#fff" }}>
    {valor}
  </span>
);

export default function Dashboard() {
  const { id } = useParams<{ id: string }>();
  const obraId = id ? parseInt(id) : 1;

  const [etapa, setEtapa] = useState("ETAPA 1");
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  useEffect(() => {
    const carregarStats = async () => {
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
          <ArrowLeft size={16} />
          <span>Obras</span>
          <span className={styles.separator}>›</span>
          <span>Detalhes da Obra</span>
          <span className={styles.separator}>›</span>
          <span>Financeiro da Obra</span>
        </div>
        <button className={styles.addBtn}>
          <Plus size={16} /> Adicionar gasto
        </button>
      </div>

      {/* Filters */}
      <div className={styles.filters}>
        <div className={styles.searchWrapper}>
          <Search className={styles.searchIcon} />
          <input placeholder="Buscar lançamentos..." className={styles.searchInput} />
        </div>
        <button className={styles.filterBtn}>
          <ChevronDown size={16} color="#F5C518" /> Valor
        </button>
        <button className={styles.filterBtn}>
          <Calendar size={16} /> Data
        </button>
      </div>

      {/* Stats */}
      <div className={styles.statsRow}>
        <StatCard
          label={`Gastos da ${etapa === "TODAS" ? "obra" : etapa.toLowerCase()}`}
          value={formatBRL(stats.gastosEtapa)}
          progress={stats.progressoEtapa}
          action={<button className={styles.etapaBtn}>Trocar etapa</button>}
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
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={stats.reembolsosPizza}
                  dataKey="value"
                  cx="45%"
                  cy="50%"
                  outerRadius={100}
                  label={formatarPercentual}
                  labelLine={false}
                >
                  {stats.reembolsosPizza.map((_, i) => (
                    <Cell key={i} fill={PIE_COLORS[i]} />
                  ))}
                </Pie>
                <Legend
                  verticalAlign="middle"
                  align="right"
                  layout="vertical"
                  iconType="square"
                  formatter={formatarLegenda}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>

        {/* Line */}
        <ChartCard title="Gastos imprevistos">
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={stats.gastosImprevistos} margin={{ top: 10, right: 20, left: 10, bottom: 10 }}>
                <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis
                  dataKey="mes"
                  tick={{ fill: "rgba(255,255,255,0.6)", fontSize: 10 }}
                  angle={-25}
                  textAnchor="end"
                  height={50}
                />
                <YAxis
                  tick={{ fill: "rgba(255,255,255,0.6)", fontSize: 10 }}
                  tickFormatter={formatarEixoMoeda}
                />
                <Tooltip
                  contentStyle={{ background: "#1a1a1a", border: "none" }}
                  formatter={formatarTooltipMoeda}
                />
                <Line
                  type="monotone"
                  dataKey="valor"
                  stroke="#ffffff"
                  strokeWidth={2}
                  dot={{ fill: "#fff", r: 3 }}
                />
              </LineChart>
            </ResponsiveContainer>
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
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={stats.gastosPorCategoria} margin={{ top: 10, right: 20, left: 10, bottom: 30 }}>
                <CartesianGrid stroke="rgba(255,255,255,0.08)" vertical={false} />
                <XAxis
                  dataKey="categoria"
                  tick={{ fill: "rgba(255,255,255,0.7)", fontSize: 11 }}
                  angle={-25}
                  textAnchor="end"
                />
                <YAxis
                  tick={{ fill: "rgba(255,255,255,0.6)", fontSize: 10 }}
                  tickFormatter={formatarEixoMoeda}
                />
                <Tooltip
                  contentStyle={{ background: "#1a1a1a", border: "none" }}
                  formatter={formatarTooltipMoeda}
                />
                <Bar dataKey="valor" fill="#e5e7eb" radius={[2, 2, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </ChartCard>
      </div>
    </div>
  );
}
