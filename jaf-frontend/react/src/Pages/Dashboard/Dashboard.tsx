import { useState } from "react";
import { ArrowLeft, Plus, Search, Calendar, ChevronDown } from "lucide-react";
import {
  PieChart, Pie, Cell, ResponsiveContainer, Legend,
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip,
  BarChart, Bar,
} from "recharts";
import ChartCard from "../../Components/ChartCard/ChartCard";
import StatCard from "../../Components/StatCard/StatCard";
import { pieData, PIE_COLORS, lineData, barData, formatBRL } from "./data";
import styles from "./Dashboard.module.css";

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
  const [etapa, setEtapa] = useState("ETAPA 1");

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
          label="Gastos da primeira etapa"
          value="R$ 29.857,00"
          progress={65}
          action={<button className={styles.etapaBtn}>Trocar etapa</button>}
        />
        <StatCard label="Total de reembolsos pendentes" value="R$ 5.600,00" />
        <StatCard label="Saldo restante do orçamento" value="R$ 1.955,00" progress={85} />
      </div>

      {/* Charts row */}
      <div className={styles.chartsGrid}>
        {/* Pie */}
        <ChartCard title="Reembolsos pendentes x concluídos">
          <div className={styles.chartContainer}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={pieData}
                  dataKey="value"
                  cx="45%"
                  cy="50%"
                  outerRadius={100}
                  label={formatarPercentual}
                  labelLine={false}
                >
                  {pieData.map((_, i) => (
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
              <LineChart data={lineData} margin={{ top: 10, right: 20, left: 10, bottom: 10 }}>
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
              <BarChart data={barData} margin={{ top: 10, right: 20, left: 10, bottom: 30 }}>
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
