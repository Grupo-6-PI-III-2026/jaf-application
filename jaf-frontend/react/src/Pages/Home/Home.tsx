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
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { authService } from "../../Service/Auth/Login/authService";
import { toast } from "sonner";

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

const normalizar = (valor: string) => valor.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/_/g, " ").trim().toLowerCase();

// Array de imagens placeholder para as obras
const imagensObras = [
  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
  "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800",
  "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=800",
  "https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=800",
];

export default function Home() {
  const admin = authService.isAdmin();
  const [obras, setObras] = useState<Obra[]>([]);
  const [gastos, setGastos] = useState<Gasto[]>([]);
  const [responsaveisPorObra, setResponsaveisPorObra] = useState<Map<number, AlocacaoObra>>(new Map());
  const [carregando, setCarregando] = useState(true);
  const [busca, setBusca] = useState("");
  const [status, setStatus] = useState("");

  useEffect(() => {
    const carregarDados = async () => {
      try {
        setCarregando(true);
        const obrasData = await obraService.listar();
        const gastosPorObra = await Promise.allSettled(
          admin && authService.hasAuthority("VISUALIZAR_GASTOS") ? obrasData.map((obra) => gastoService.listarPorObra(obra.id)) : []
        );
        const alocacoesPorObra = await Promise.allSettled(
          authService.hasAuthority("VISUALIZAR_ALOCACOES") ? obrasData.map((obra) => alocacaoService.listarPorObra(obra.id)) : []
        );
        const gastosData = gastosPorObra.flatMap((resultado) =>
          resultado.status === "fulfilled" ? resultado.value : []
        );
        const responsaveis = new Map<number, AlocacaoObra>();
        alocacoesPorObra.forEach((resultado, index) => {
          if (resultado.status === "fulfilled" && resultado.value.length > 0) {
            responsaveis.set(obrasData[index].id, resultado.value[0]);
          }
        });
        setObras(obrasData);
        setGastos(gastosData);
        setResponsaveisPorObra(responsaveis);
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
        toast.error("Não foi possível carregar as obras. Atualize a página para tentar novamente.");
      } finally {
        setCarregando(false);
      }
    };

    carregarDados();
  }, [admin]);

  // Calcular KPIs
  const obrasEmAndamento = obras.filter(
    (obra) => obra.status === "EM_ANDAMENTO" || obra.status === "EM ANDAMENTO"
  ).length;

  const obrasFinalizadas = obras.filter(
    (obra) => obra.status === "CONCLUIDA" || obra.status === "CONCLUÍDA"
  ).length;

  const totalGasto = gastos.reduce((total, gasto) => total + gasto.valor, 0);
  const totalGastoPorObra = new Map<number, number>();
  gastos.forEach((gasto) => {
    totalGastoPorObra.set(gasto.obra.id, (totalGastoPorObra.get(gasto.obra.id) ?? 0) + gasto.valor);
  });

  const statusDisponiveis = Array.from(new Set(obras.map((obra) => normalizar(obra.status)))).sort();
  const obrasFiltradas = obras
    .slice()
    .sort((a, b) => new Date(b.dtInicio).getTime() - new Date(a.dtInicio).getTime())
    .filter((obra) => normalizar(obra.titulo).includes(normalizar(busca)) && (!status || normalizar(obra.status) === status));

  return (
    <div className={styles.pagina}>
      
      {/* HEADER */}
      <div className={styles.cabecalho}>
        <h1 className={styles.titulo}>Visão geral das obras</h1>
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

            {admin && <div className={styles.card}>
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
            </div>}
          </div>

          <section className={styles.secao} aria-labelledby="todas-obras">
            <h2 id="todas-obras" className={styles.subtitulo}>Todas as obras</h2>
            <div className={styles.filtros}>
              <label>Buscar por título
                <input type="search" value={busca} onChange={(e) => setBusca(e.target.value)} placeholder="Digite o título da obra" />
              </label>
              <label>Status da obra
                <select value={status} onChange={(e) => setStatus(e.target.value)}>
                  <option value="">Todos os status</option>
                  {statusDisponiveis.map((item) => <option key={item} value={item}>{item.toLocaleUpperCase("pt-BR")}</option>)}
                </select>
              </label>
              {(busca || status) && <button type="button" onClick={() => { setBusca(""); setStatus(""); }}>Limpar filtros</button>}
            </div>
            <p role="status">{obrasFiltradas.length} de {obras.length} obras</p>

            <div className={styles.lista}>
              {obrasFiltradas.length > 0 ? (
                obrasFiltradas.map((obra, index) => {
                  const responsavel = responsaveisPorObra.get(obra.id);
                  return (
                    <CardObra
                      key={obra.id}
                      id={obra.id}
                      nome={obra.titulo}
                      status={obra.status}
                      valor={totalGastoPorObra.get(obra.id) ?? 0}
                      imagem={imagensObras[index % imagensObras.length]}
                      responsavelNome={responsavel?.funcionario.nome}
                      responsavelCargo={responsavel?.cargo?.replace(/_/g, " ")}
                    />
                  );
                })
              ) : (
                <p>{obras.length ? "Nenhuma obra corresponde aos filtros. Tente outro título ou status." : "Nenhuma obra encontrada."}</p>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}