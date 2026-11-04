import { useEffect, useState } from "react";
import {
  TrendingUp,
  Building2,
  CheckCircle,
  DollarSign,
} from "lucide-react";
import styles from "./Home.module.css";

import { somarCentavos } from "../../Utils/dinheiro";
import CardObra from "../../Components/CardObra/CardObra";
import { obraService, type ObraResumo } from "../../Service/Obras/obraService";
import { toast } from "sonner";
import { logger } from "../../Utils/logger";
import { useAuthz } from "../../Hooks/useAuthz";

// Uma página basta para a Home; acima disso o aviso abaixo informa que a lista está parcial.
const OBRAS_POR_PAGINA = 100;

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

const normalizar = (valor: string) => valor.normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/_/g, " ").trim().toLowerCase();

const STATUS_OBRA = ["PLANEJADA", "EM_ANDAMENTO", "CONCLUIDA", "CANCELADA"];

// Array de imagens placeholder para as obras
const imagensObras = [
  "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
  "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?w=800",
  "https://images.unsplash.com/photo-1600566753190-17f0baa2a6c3?w=800",
  "https://images.unsplash.com/photo-1600047509807-ba8f99d2cdde?w=800",
];

export default function Home() {
  const { isAdmin } = useAuthz();
  const admin = isAdmin;
  const [obras, setObras] = useState<ObraResumo[]>([]);
  const [totalObras, setTotalObras] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState(false);
  const [tentativa, setTentativa] = useState(0);
  const [busca, setBusca] = useState("");
  const [status, setStatus] = useState("");

  useEffect(() => {
    const controle = { ativo: true };
    const carregarDados = async () => {
      try {
        setCarregando(true);
        setErro(false);
        // Uma única chamada traz obras, responsável e (para ADMIN) o total gasto já somado no servidor.
        const pagina = await obraService.resumo({ page: 0, size: OBRAS_POR_PAGINA, sortBy: "dtInicio", sortDir: "desc" });
        if (!controle.ativo) return;
        setObras(pagina.content);
        setTotalObras(pagina.totalElements);
      } catch (error) {
        if (!controle.ativo) return;
        logger.erro("Erro ao carregar obras", error);
        setErro(true);
        toast.error("Não foi possível carregar as obras. Atualize a página para tentar novamente.");
      } finally {
        if (controle.ativo) setCarregando(false);
      }
    };

    void carregarDados();
    return () => { controle.ativo = false; };
  }, [tentativa]);
  // Calcular KPIs
  const obrasEmAndamento = obras.filter(
    (obra) => obra.status === "EM_ANDAMENTO" || obra.status === "EM ANDAMENTO"
  ).length;

  const obrasFinalizadas = obras.filter(
    (obra) => obra.status === "CONCLUIDA" || obra.status === "CONCLUÍDA"
  ).length;

  // Nunca mostrar soma parcial: se faltar o total de alguma obra (ou a lista foi cortada), o valor fica indisponível.
  const totalGastoDisponivel = obras.every((obra) => obra.totalGasto != null) && totalObras <= obras.length;
  const totalGasto = totalGastoDisponivel
    ? somarCentavos(...obras.map((obra) => obra.totalGasto)) / 100
    : null;
  const statusDisponiveis = Array.from(new Set([...STATUS_OBRA, ...obras.map((obra) => obra.status)].map(normalizar))).sort();
  const obrasFiltradas = obras
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
                  {totalGasto === null ? "Total indisponível" : formatarMoeda(totalGasto)}
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
            {totalObras > obras.length && (
              <p role="note">Exibindo as primeiras {obras.length} de {totalObras} obras.</p>
            )}

            <div className={styles.lista}>
              {obrasFiltradas.length > 0 ? (
                obrasFiltradas.map((obra, index) => {
                  const responsavel = obra.responsavel;
                  return (
                    <CardObra
                      key={obra.id}
                      id={obra.id}
                      nome={obra.titulo}
                      status={obra.status}
                      valor={obra.totalGasto ?? null}
                      imagem={imagensObras[index % imagensObras.length]}
                      responsavelNome={responsavel?.nome}
                    />
                  );
                })
              ) : (
                erro ? (
                  <p role="alert">Não foi possível carregar as obras. <button type="button" onClick={() => setTentativa((n) => n + 1)}>Tentar novamente</button></p>
                ) : <p>{
                  status && !busca
                    ? "Nenhuma obra encontrada com esse status."
                    : obras.length
                      ? "Nenhuma obra corresponde aos filtros. Tente outro título ou status."
                      : "Nenhuma obra encontrada."
                }</p>
              )}
            </div>
          </section>
        </>
      )}
    </div>
  );
}
