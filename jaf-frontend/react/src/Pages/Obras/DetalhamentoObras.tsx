import { useState, useEffect } from "react";
import {
  ArrowLeft,
  Calendar,
  Search,
  Filter,
  CalendarDays,
  Pencil,
  TrendingUp,
  ChevronLeft,
  ChevronRight,
  Plus,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import styles from "./DetalhamentoObras.module.css";
import ControlePresenca from "../ControlePresenca/ControlePresenca";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { gastoService, type Gasto } from "../../Service/Gastos/gastoService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const formatarData = (data: string) => {
  return new Date(data).toLocaleDateString("pt-BR");
};

const getIniciais = (nome: string) => {
  const palavras = nome.split(" ");
  if (palavras.length >= 2) {
    return (palavras[0][0] + palavras[1][0]).toUpperCase();
  }
  return nome.substring(0, 2).toUpperCase();
};

const cores = ["#6C63FF", "#FF6584", "#43B89C", "#ffc107", "#9c27b0"];

export default function DetalhamentoObras() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [controlePresencaAberto, setControlePresencaAberto] = useState(false);
  const [obra, setObra] = useState<Obra | null>(null);
  const [gastos, setGastos] = useState<Gasto[]>([]);
  const [alocacoes, setAlocacoes] = useState<AlocacaoObra[]>([]);
  const [carregando, setCarregando] = useState(true);
  
  const { id } = useParams<{ id: string }>();
  const totalPaginas = Math.ceil(gastos.length / 5);
  const navegar = useNavigate();

  useEffect(() => {
    const carregarDados = async () => {
      try {
        setCarregando(true);
        
        // Se não houver ID, pega a primeira obra
        let obraData: Obra;
        if (id) {
          obraData = await obraService.buscarPorId(parseInt(id));
        } else {
          const obras = await obraService.listar();
          obraData = obras[0] || null;
        }
        
        setObra(obraData);

        if (obraData) {
          // Carregar gastos e alocações
          const [gastosData, alocacoesData] = await Promise.all([
            gastoService.listar(),
            alocacaoService.listar(),
          ]);

          // Filtrar gastos da obra específica
          const gastosDaObra = gastosData.filter(
            (gasto) => gasto.obra.id === obraData.id
          );
          setGastos(gastosDaObra);

          // Filtrar alocações da obra específica
          const alocacoesDaObra = alocacoesData.filter(
            (alocacao) => alocacao.obra.id === obraData.id
          );
          setAlocacoes(alocacoesDaObra);
        }
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
      } finally {
        setCarregando(false);
      }
    };

    carregarDados();
  }, [id]);

  // Calcular métricas
  const totalGastoObra = gastos.reduce((total, gasto) => total + gasto.valor, 0);
  const orcamentoObra = parseFloat(obra?.orcamento || "0");
  const limiteAtingido = orcamentoObra > 0 ? (totalGastoObra / orcamentoObra) * 100 : 0;

  // Calcular dias restantes
  const calcularDiasRestantes = () => {
    if (!obra?.dtTerminoPrevisto) return 0;
    const hoje = new Date();
    const dataTermino = new Date(obra.dtTerminoPrevisto);
    const diffTime = dataTermino.getTime() - hoje.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  };

  // Paginação
  const gastosExibidos = gastos.slice((paginaAtual - 1) * 5, paginaAtual * 5);

  function irParaPaginaAnterior() {
    setPaginaAtual((paginaCorrente) => Math.max(1, paginaCorrente - 1));
  }

  function irParaProximaPagina() {
    setPaginaAtual((paginaCorrente) => Math.min(totalPaginas, paginaCorrente + 1));
  }

  function irParaPagina(pagina: number) {
    setPaginaAtual(pagina);
  }

  if (carregando) {
    return (
      <div className={styles.pagina}>
        <div style={{ textAlign: "center", padding: "2rem" }}>Carregando...</div>
      </div>
    );
  }

  if (!obra) {
    return (
      <div className={styles.pagina}>
        <div style={{ textAlign: "center", padding: "2rem" }}>Obra não encontrada.</div>
      </div>
    );
  }

  return (
    <div className={styles.pagina}>
      <ControlePresenca
        aberto={controlePresencaAberto}
        onFechar={() => setControlePresencaAberto(false)}
      />

      {/* Cabeçalho */}
      <div className={styles.cabecalho}>
        <div className={styles.cabecalhoEsquerda}>
          <button className={styles.botaoVoltar} onClick={() => navegar("/home")}>
            <ArrowLeft size={18} />
          </button>
          <span className={styles.navegacao}>
            <span className={styles.navegacaoLink} onClick={() => navegar("/home")}>
              Obras
            </span>
            <span className={styles.separador}>›</span>
            <span className={styles.navegacaoAtivo}>Detalhes da Obra</span>
          </span>
        </div>
        <div className={styles.cabecalhoAcoes}>
          <button
            className={styles.botaoAdicionarGasto}
            onClick={() => setControlePresencaAberto(true)}
          >
            Controle de presença
          </button>
          <button className={styles.botaoAdicionarGasto}>
            💰 Adicionar gasto
          </button>
        </div>
      </div>

      {/* Card da obra */}
      <div className={styles.cardObra}>
        <div className={styles.cardObraImagem}>
          <img
            src="https://s2.glbimg.com/YgImWrcpJX6FAHRJOlZtEnTaHYc=/e.glbimg.com/og/ed/f/original/2020/12/23/apartamento-sao-paulo-260-m2-atemporal3.jpg"
            alt={obra.titulo}
          />
        </div>
        <div className={styles.cardObraInformacoes}>
          <div className={styles.badgeStatus}>
            <span className={styles.badgePonto} />
            {obra.status.replace("_", " ")}
          </div>
          <div className={styles.cardObraTitulo}>
            <h1>{obra.titulo}</h1>
            <button className={styles.botaoEditar}>
              <Pencil size={16} />
            </button>
          </div>
          <div className={styles.cardObraData}>
            <Calendar size={14} />
            <span>
              {formatarData(obra.dtInicio)} - {formatarData(obra.dtTerminoPrevisto)}
            </span>
          </div>

          <div className={styles.equipeRotulo}>EQUIPE RESPONSÁVEL</div>
          <div className={styles.equipe}>
            {alocacoes.length > 0 ? (
              alocacoes.map((alocacao, index) => (
                <div key={alocacao.id} className={styles.membroCard}>
                  <div
                    className={styles.membroAvatar}
                    style={{ backgroundColor: cores[index % cores.length] }}
                  >
                    {getIniciais(alocacao.funcionario.nome)}
                  </div>
                  <div className={styles.membroInformacoes}>
                    <span className={styles.membroNome}>{alocacao.funcionario.nome}</span>
                    <span className={styles.membroCargo}>
                      {alocacao.cargo.replace("_", " ")}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <p style={{ fontSize: "14px", color: "#666" }}>Nenhum funcionário alocado</p>
            )}
            <button className={styles.botaoAdicionarMembro}>
              <Plus size={18} />
            </button>
          </div>
        </div>
      </div>

      {/* Cards de métricas */}
      <div className={styles.metricas}>
        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>CUSTO TOTAL ACUMULADO</span>
          <span className={styles.metricaValorDestaque}>
            {formatarMoeda(totalGastoObra)}
          </span>
          <div className={styles.metricaTendencia}>
            <TrendingUp size={14} />
            <span>Gastos acumulados</span>
          </div>
        </div>

        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>LIMITE ATINGIDO</span>
          <span className={styles.metricaValor}>{limiteAtingido.toFixed(0)}%</span>
          <div className={styles.barraProgressoWrapper}>
            <div className={styles.barraProgresso}>
              <div
                className={styles.barraProgressoPreenchimento}
                style={{ width: `${Math.min(limiteAtingido, 100)}%` }}
              />
            </div>
          </div>
        </div>

        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>DIAS PARA ENTREGA</span>
          <span className={styles.metricaValor}>{calcularDiasRestantes()} Dias</span>
          <div className={styles.metricaSubtitulo}>
            <CalendarDays size={13} />
            <span>Entrega prevista: {formatarData(obra.dtTerminoPrevisto)}</span>
          </div>
        </div>
      </div>

      {/* Seção financeiro */}
      <div className={styles.secaoFinanceiro}>
        <div className={styles.financeiroTopo}>
          <h2 className={styles.financeiroTitulo}>Financeiro</h2>
          <div className={styles.financeiroControles}>
            <div className={styles.campoBusca}>
              <Search size={15} className={styles.iconeBusca} />
              <input
                type="text"
                placeholder="Buscar lançamentos..."
                className={styles.inputBusca}
              />
            </div>
            <button className={styles.botaoFiltro}>
              <Filter size={14} />
              Valor
            </button>
            <button className={styles.botaoFiltro}>
              <CalendarDays size={14} />
              Data
            </button>
          </div>
        </div>

        {/* Tabela */}
        <div className={styles.tabelaWrapper}>
          <table className={styles.tabela}>
            <thead>
              <tr>
                <th>VALOR</th>
                <th>FUNCIONÁRIO</th>
                <th>TIPO</th>
                <th>DESCRIÇÃO</th>
                <th>ETAPA</th>
                <th>MATERIAL</th>
                <th>DATA</th>
              </tr>
            </thead>
            <tbody>
              {gastosExibidos.length > 0 ? (
                gastosExibidos.map((gasto, indice) => (
                  <tr key={gasto.id}>
                    <td>
                      <span className={styles.celulaValor} style={{ color: "#43B89C" }}>
                        {formatarMoeda(gasto.valor)}
                      </span>
                    </td>
                    <td>
                      <div className={styles.celulaFuncionario}>
                        <div
                          className={styles.avatarPequeno}
                          style={{
                            backgroundColor: cores[indice % cores.length],
                          }}
                        >
                          {getIniciais(gasto.funcionario.nome)}
                        </div>
                        {gasto.funcionario.nome}
                      </div>
                    </td>
                    <td>{gasto.metodoPagamento}</td>
                    <td>
                      <span className={styles.descricaoDestaque}>{gasto.descricao}</span>
                    </td>
                    <td>{gasto.etapa || "-"}</td>
                    <td>{gasto.categoria || "-"}</td>
                    <td>{formatarData(gasto.dtGasto)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={7} style={{ textAlign: "center", padding: "2rem" }}>
                    Nenhum gasto registrado para esta obra.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {gastosExibidos.length} de {gastos.length} lançamentos
            </span>
            <div className={styles.paginacaoBotoes}>
              <button
                className={styles.botaoPaginacao}
                onClick={irParaPaginaAnterior}
                disabled={paginaAtual === 1}
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: totalPaginas }, (_, i) => i + 1).map((numeroPagina) => (
                <button
                  key={numeroPagina}
                  className={`${styles.botaoPaginacao} ${paginaAtual === numeroPagina ? styles.botaoPaginacaoAtivo : ""}`}
                  onClick={() => irParaPagina(numeroPagina)}
                >
                  {numeroPagina}
                </button>
              ))}
              <button
                className={styles.botaoPaginacao}
                onClick={irParaProximaPagina}
                disabled={paginaAtual === totalPaginas || totalPaginas === 0}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
