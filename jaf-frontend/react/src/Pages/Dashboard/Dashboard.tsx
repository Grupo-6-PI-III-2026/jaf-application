import { useState, useEffect } from "react";
import { Plus, Search, Tag, ChevronDown, FileText } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import ChartCard from "../../Components/ChartCard/ChartCard";
import StatCard from "../../Components/StatCard/StatCard";
import { dashboardService, type DashboardStats } from "../../Service/Dashboard/dashboardService";
import { authService } from "../../Service/Auth/Login/authService";
import styles from "./Dashboard.module.css";

const PIE_COLORS = ["#5A6B7B", "#F5C518"];

const formatBRL = (v: number) =>
  `R$${v.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;

const etapas = ["TODAS", "ETAPA 1", "ETAPA 2"];

const limitarPercentual = (valor: number) => Math.min(100, Math.max(0, valor));

const escaparHtml = (valor: string) =>
  valor
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");

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
  const podeGerarRelatorio = authService.hasAuthority("GERAR_RELATORIO");

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
  const maxCategoria = Math.max(...categoriasExibidas.map((item) => item.valor), 1);

  const gerarRelatorio = () => {
    if (!stats) return;

    const dataGeracao = new Date().toLocaleString("pt-BR", {
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
    const etapaLabel = etapa === "TODAS" ? "Todas as etapas" : etapa;
    const categoriasRelatorio = [...stats.gastosPorCategoria].sort((a, b) => b.valor - a.valor);
    const maxCategoriaRelatorio = Math.max(...categoriasRelatorio.map((item) => item.valor), 1);
    const reembolsosHtml = stats.reembolsosPizza
      .map(
        (item, index) => `
          <div class="legend-row">
            <span class="legend-dot" style="background:${PIE_COLORS[index] ?? "#F5C518"}"></span>
            <span>${escaparHtml(item.name)}</span>
            <strong>${item.value}%</strong>
          </div>`
      )
      .join("");
    const imprevistosHtml = stats.gastosImprevistos.length
      ? stats.gastosImprevistos
          .map((item) => {
            const largura = Math.max(6, limitarPercentual((item.valor / maxImprevisto) * 100));
            return `
              <div class="bar-row">
                <div class="bar-info"><span>${escaparHtml(item.mes)}</span><strong>${formatBRL(item.valor)}</strong></div>
                <div class="bar-track"><div class="bar-fill" style="width:${largura}%"></div></div>
              </div>`;
          })
          .join("")
      : `<p class="empty">Nenhum gasto imprevisto na etapa selecionada.</p>`;
    const categoriasHtml = categoriasRelatorio.length
      ? categoriasRelatorio
          .map((item) => {
            const largura = Math.max(4, limitarPercentual((item.valor / maxCategoriaRelatorio) * 100));
            return `
              <div class="bar-row category-row">
                <div class="bar-info"><span>${escaparHtml(item.categoria)}</span><strong>${formatBRL(item.valor)}</strong></div>
                <div class="bar-track"><div class="bar-fill" style="width:${largura}%"></div></div>
              </div>`;
          })
          .join("")
      : `<p class="empty">Nenhuma categoria encontrada.</p>`;

    const janelaRelatorio = window.open("", "_blank", "width=960,height=720");

    if (!janelaRelatorio) {
      window.alert("Não foi possível abrir o relatório. Verifique se o navegador bloqueou pop-ups.");
      return;
    }

    janelaRelatorio.document.write(`<!doctype html>
      <html lang="pt-BR">
        <head>
          <meta charset="utf-8" />
          <title>Relatório financeiro JAF</title>
          <style>
            * { box-sizing: border-box; }
            body { margin: 0; background: #f8f9fa; color: #1F2A33; font-family: Arial, Helvetica, sans-serif; }
            .page { width: 210mm; min-height: 297mm; margin: 0 auto; padding: 22mm; background: #f8f9fa; }
            .hero { background: #243038; color: #fff; border-radius: 8px; padding: 24px 28px; border-top: 6px solid #F5C518; }
            .brand { color: #F5C518; font-size: 12px; font-weight: 800; letter-spacing: 0.12em; text-transform: uppercase; margin-bottom: 8px; }
            h1 { margin: 0; font-size: 26px; line-height: 1.2; }
            .subtitle { margin: 8px 0 0; color: rgba(255,255,255,0.72); font-size: 13px; }
            .meta { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin: 18px 0; }
            .meta-card, .stat, .panel { border: 1px solid #e5e7eb; border-radius: 8px; background: #fff; }
            .meta-card { padding: 12px; }
            .label { display: block; color: #6c757d; font-size: 10px; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 5px; }
            .value { font-size: 13px; font-weight: 800; }
            .stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 14px; }
            .stat { padding: 16px; border-left: 5px solid #F5C518; }
            .stat strong { display: block; font-size: 18px; margin-top: 6px; }
            .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
            .panel { padding: 18px; margin-bottom: 12px; break-inside: avoid; }
            .panel.dark { background: #243038; color: #fff; border-color: #243038; }
            h2 { margin: 0 0 14px; color: #F5C518; font-size: 12px; letter-spacing: 0.06em; text-transform: uppercase; }
            .legend-row { display: grid; grid-template-columns: 12px 1fr auto; align-items: center; gap: 8px; padding: 8px 0; font-size: 12px; text-transform: uppercase; }
            .legend-dot { width: 12px; height: 12px; border-radius: 3px; }
            .bar-row { display: grid; grid-template-columns: 130px 1fr; align-items: center; gap: 14px; padding: 7px 0; }
            .bar-info { display: flex; flex-direction: column; gap: 3px; font-size: 11px; }
            .bar-info span { color: rgba(255,255,255,0.72); text-transform: uppercase; }
            .panel:not(.dark) .bar-info span { color: #6c757d; }
            .bar-track { height: 24px; border-radius: 7px; background: rgba(255,255,255,0.08); overflow: hidden; }
            .panel:not(.dark) .bar-track { background: #eef0f2; }
            .bar-fill { height: 100%; border-radius: 7px; background: linear-gradient(180deg, #F5C518, #f7dc72); }
            .category-row { grid-template-columns: 190px 1fr; }
            .empty { color: rgba(255,255,255,0.7); font-size: 12px; margin: 0; }
            .footer { margin-top: 18px; color: #6c757d; font-size: 10px; text-align: right; }
            @media print {
              body { background: #fff; }
              .page { width: auto; min-height: auto; margin: 0; padding: 12mm; }
              @page { size: A4; margin: 0; }
            }
          </style>
        </head>
        <body>
          <main class="page">
            <section class="hero">
              <div class="brand">JAF • Dashboard financeiro</div>
              <h1>Relatório financeiro da obra</h1>
              <p class="subtitle">Resumo de gastos, reembolsos, imprevistos e categorias.</p>
            </section>

            <section class="meta">
              <div class="meta-card"><span class="label">Obra</span><span class="value">#${obraId}</span></div>
              <div class="meta-card"><span class="label">Etapa</span><span class="value">${escaparHtml(etapaLabel)}</span></div>
              <div class="meta-card"><span class="label">Gerado em</span><span class="value">${dataGeracao}</span></div>
            </section>

            <section class="stats">
              <div class="stat"><span class="label">Gastos da ${etapa === "TODAS" ? "obra" : etapa.toLowerCase()}</span><strong>${formatBRL(stats.gastosEtapa)}</strong></div>
              <div class="stat"><span class="label">Reembolsos pendentes</span><strong>${formatBRL(stats.reembolsosPendentes)}</strong></div>
              <div class="stat"><span class="label">Saldo restante</span><strong>${formatBRL(stats.saldoRestante)}</strong></div>
            </section>

            <section class="grid">
              <div class="panel dark">
                <h2>Reembolsos pendentes x concluídos</h2>
                ${reembolsosHtml}
              </div>
              <div class="panel dark">
                <h2>Gastos imprevistos</h2>
                ${imprevistosHtml}
              </div>
            </section>

            <section class="panel dark">
              <h2>Gastos por categoria</h2>
              ${categoriasHtml}
            </section>

            <div class="footer">Relatório gerado pelo sistema JAF.</div>
          </main>
          <script>
            window.onload = () => {
              window.focus();
              window.print();
            };
          </script>
        </body>
      </html>`);
    janelaRelatorio.document.close();
  };

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
        <div className={styles.headerActions}>
          {podeGerarRelatorio && (
            <button className={styles.reportBtn} onClick={gerarRelatorio}>
              <FileText size={16} /> Gerar relatório
            </button>
          )}
          {podeCriarGasto && (
            <button className={styles.addBtn} onClick={() => navegar(`/obras/detalhamento/${obraId}`)}>
              <Plus size={16} /> Adicionar gasto
            </button>
          )}
        </div>
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
              <div className={styles.unplannedList}>
                {stats.gastosImprevistos.map((item) => (
                  <div key={`${item.mes}-${item.valor}`} className={styles.unplannedItem}>
                    <div className={styles.unplannedInfo}>
                      <span>{item.mes}</span>
                      <strong>{formatBRL(item.valor)}</strong>
                    </div>
                    <div className={styles.unplannedBarWrap}>
                      <div
                        className={styles.unplannedBar}
                        style={{ width: `${Math.max(6, limitarPercentual((item.valor / maxImprevisto) * 100))}%` }}
                      />
                    </div>
                  </div>
                ))}
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
            <div className={styles.categoryList}>
              {categoriasExibidas.map((item) => (
                <div key={item.categoria} className={styles.categoryItem}>
                  <div className={styles.categoryInfo}>
                    <span>{item.categoria}</span>
                    <strong>{formatBRL(item.valor)}</strong>
                  </div>
                  <div className={styles.categoryBarWrap}>
                    <div
                      className={styles.categoryBar}
                      style={{ width: `${Math.max(4, limitarPercentual((item.valor / maxCategoria) * 100))}%` }}
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
