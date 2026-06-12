import { useCallback, useState, useEffect, type FormEvent } from "react";
import {
  ArrowLeft,
  Calendar,
  Search,
  Filter,
  CalendarDays,
  TrendingUp,
  ChevronLeft,
  ChevronRight,
  Plus,
  Pencil,
  X,
} from "lucide-react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import styles from "./DetalhamentoObras.module.css";
import ControlePresenca from "../ControlePresenca/ControlePresenca";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { gastoService, type Gasto } from "../../Service/Gastos/gastoService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { toast } from "sonner";

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

const dataHoje = () => new Date().toISOString().split("T")[0];

export default function DetalhamentoObras() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [controlePresencaAberto, setControlePresencaAberto] = useState(false);
  const [modalGastoAberto, setModalGastoAberto] = useState(false);
  const [modalEdicaoAberto, setModalEdicaoAberto] = useState(false);
  const [salvandoGasto, setSalvandoGasto] = useState(false);
  const [salvandoObra, setSalvandoObra] = useState(false);
  const [buscaFinanceira, setBuscaFinanceira] = useState("");
  const [ordemFinanceira, setOrdemFinanceira] = useState<"data" | "valor">("data");
  const [dataSelecionada, setDataSelecionada] = useState<string>(dataHoje());
  const [novoGasto, setNovoGasto] = useState({
    descricao: "",
    categoria: "MATERIAL",
    metodoPagamento: "PIX",
    etapa: "ETAPA 1",
    valor: "",
    dtGasto: dataHoje(),
    funcionarioId: "",
  });
  const [edicaoObra, setEdicaoObra] = useState({
    titulo: "",
    orcamento: "",
    status: "EM_ANDAMENTO",
    dtInicio: dataHoje(),
    dtTerminoPrevisto: dataHoje(),
  });
  const [obra, setObra] = useState<Obra | null>(null);
  const [gastos, setGastos] = useState<Gasto[]>([]);
  const [alocacoes, setAlocacoes] = useState<AlocacaoObra[]>([]);
  const [carregando, setCarregando] = useState(true);
  
  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  const carregarDados = useCallback(async () => {
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
        setEdicaoObra({
          titulo: obraData.titulo,
          orcamento: obraData.orcamento,
          status: obraData.status,
          dtInicio: obraData.dtInicio,
          dtTerminoPrevisto: obraData.dtTerminoPrevisto,
        });

        if (obraData) {
          const [gastosData, alocacoesData] = await Promise.all([
            gastoService.listarPorObra(obraData.id),
            alocacaoService.listarPorObra(obraData.id),
          ]);

          setGastos(gastosData);
          setAlocacoes(alocacoesData);
          setNovoGasto((gastoAtual) => ({
            ...gastoAtual,
            funcionarioId:
              gastoAtual.funcionarioId || String(alocacoesData[0]?.funcionario.id ?? ""),
          }));
        }
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
        toast.error("Erro ao carregar detalhes da obra");
      } finally {
        setCarregando(false);
      }
  }, [id]);

  useEffect(() => {
    carregarDados();
  }, [carregarDados]);

  useEffect(() => {
    if (searchParams.get("editar") === "1") {
      setModalEdicaoAberto(true);
    }
  }, [searchParams]);

  useEffect(() => {
    setPaginaAtual(1);
  }, [buscaFinanceira, ordemFinanceira]);

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

  const gastosFiltrados = gastos
    .filter((gasto) => {
      const termo = buscaFinanceira.trim().toLowerCase();
      if (!termo) return true;

      return [
        gasto.descricao,
        gasto.categoria,
        gasto.metodoPagamento,
        gasto.etapa,
        gasto.funcionario.nome,
      ]
        .filter(Boolean)
        .some((campo) => campo.toLowerCase().includes(termo));
    })
    .sort((a, b) => {
      if (ordemFinanceira === "valor") {
        return b.valor - a.valor;
      }
      return new Date(b.dtGasto).getTime() - new Date(a.dtGasto).getTime();
    });

  const totalPaginas = Math.ceil(gastosFiltrados.length / 5);
  const gastosExibidos = gastosFiltrados.slice((paginaAtual - 1) * 5, paginaAtual * 5);

  function atualizarNovoGasto(campo: keyof typeof novoGasto, valor: string) {
    setNovoGasto((gastoAtual) => ({ ...gastoAtual, [campo]: valor }));
  }

  function atualizarEdicaoObra(campo: keyof typeof edicaoObra, valor: string) {
    setEdicaoObra((obraAtual) => ({ ...obraAtual, [campo]: valor }));
  }

  function fecharModalEdicao() {
    setModalEdicaoAberto(false);
    if (searchParams.get("editar")) {
      setSearchParams({});
    }
  }

  async function salvarEdicaoObra(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!obra) return;

    const orcamento = Number(edicaoObra.orcamento.replace(",", "."));
    if (!orcamento || orcamento <= 0) {
      toast.error("Informe um orçamento maior que zero");
      return;
    }

    try {
      setSalvandoObra(true);
      const obraAtualizada = await obraService.atualizar(obra.id, {
        titulo: edicaoObra.titulo.trim(),
        orcamento: String(orcamento),
        status: edicaoObra.status,
        dtInicio: edicaoObra.dtInicio,
        dtTerminoPrevisto: edicaoObra.dtTerminoPrevisto,
      });
      setObra(obraAtualizada);
      fecharModalEdicao();
      toast.success("Obra atualizada com sucesso");
    } catch (error) {
      console.error("Erro ao atualizar obra:", error);
      toast.error("Não foi possível atualizar a obra");
    } finally {
      setSalvandoObra(false);
    }
  }

  async function cadastrarGasto(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();

    if (!obra || !novoGasto.funcionarioId) {
      toast.error("Selecione um funcionário alocado na obra");
      return;
    }

    const valor = Number(novoGasto.valor.replace(",", "."));
    if (!valor || valor <= 0) {
      toast.error("Informe um valor maior que zero");
      return;
    }

    try {
      setSalvandoGasto(true);
      await gastoService.criar({
        descricao: novoGasto.descricao.trim(),
        categoria: novoGasto.categoria,
        metodoPagamento: novoGasto.metodoPagamento,
        etapa: novoGasto.etapa,
        valor,
        dtGasto: novoGasto.dtGasto,
        funcionarioId: Number(novoGasto.funcionarioId),
        obraId: obra.id,
      });

      const gastosAtualizados = await gastoService.listarPorObra(obra.id);
      setGastos(gastosAtualizados);
      setModalGastoAberto(false);
      setNovoGasto({
        descricao: "",
        categoria: "MATERIAL",
        metodoPagamento: "PIX",
        etapa: "ETAPA 1",
        valor: "",
        dtGasto: dataHoje(),
        funcionarioId: String(alocacoes[0]?.funcionario.id ?? ""),
      });
      toast.success("Gasto adicionado ao financeiro da obra");
    } catch (error) {
      console.error("Erro ao cadastrar gasto:", error);
      toast.error("Não foi possível adicionar o gasto");
    } finally {
      setSalvandoGasto(false);
    }
  }

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
        obraId={obra.id}
        obraTitulo={obra.titulo}
        data={dataSelecionada}
      />

      {modalEdicaoAberto && (
        <div className={styles.modalOverlay} onClick={fecharModalEdicao}>
          <form className={styles.modalGasto} onSubmit={salvarEdicaoObra} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>Editar obra</h2>
                <p>{obra.titulo}</p>
              </div>
              <button type="button" className={styles.botaoFecharModal} onClick={fecharModalEdicao} aria-label="Fechar modal de edição">
                <X size={20} />
              </button>
            </header>

            <div className={styles.modalGrid}>
              <label>
                Título
                <input value={edicaoObra.titulo} onChange={(evento) => atualizarEdicaoObra("titulo", evento.target.value)} required minLength={5} maxLength={150} />
              </label>
              <label>
                Orçamento
                <input value={edicaoObra.orcamento} onChange={(evento) => atualizarEdicaoObra("orcamento", evento.target.value)} required inputMode="decimal" />
              </label>
              <label>
                Status
                <select value={edicaoObra.status} onChange={(evento) => atualizarEdicaoObra("status", evento.target.value)}>
                  <option value="EM_ANDAMENTO">Em andamento</option>
                  <option value="CONCLUIDA">Concluída</option>
                  <option value="PAUSADA">Pausada</option>
                  <option value="CANCELADA">Cancelada</option>
                </select>
              </label>
              <label>
                Início
                <input type="date" value={edicaoObra.dtInicio} onChange={(evento) => atualizarEdicaoObra("dtInicio", evento.target.value)} required />
              </label>
              <label>
                Término previsto
                <input type="date" value={edicaoObra.dtTerminoPrevisto} onChange={(evento) => atualizarEdicaoObra("dtTerminoPrevisto", evento.target.value)} required />
              </label>
            </div>

            <footer className={styles.modalFooter}>
              <button type="button" className={styles.botaoSecundario} onClick={fecharModalEdicao}>Cancelar</button>
              <button type="submit" className={styles.botaoAdicionarGasto} disabled={salvandoObra}>
                {salvandoObra ? "Salvando..." : "Salvar obra"}
              </button>
            </footer>
          </form>
        </div>
      )}

      {modalGastoAberto && (
        <div className={styles.modalOverlay} onClick={() => setModalGastoAberto(false)}>
          <form className={styles.modalGasto} onSubmit={cadastrarGasto} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>Adicionar gasto</h2>
                <p>{obra.titulo}</p>
              </div>
              <button type="button" className={styles.botaoFecharModal} onClick={() => setModalGastoAberto(false)} aria-label="Fechar modal de gasto">
                <X size={20} />
              </button>
            </header>

            <div className={styles.modalGrid}>
              <label>
                Descrição
                <input value={novoGasto.descricao} onChange={(evento) => atualizarNovoGasto("descricao", evento.target.value)} required maxLength={255} placeholder="Ex.: Compra de cimento" />
              </label>
              <label>
                Valor
                <input value={novoGasto.valor} onChange={(evento) => atualizarNovoGasto("valor", evento.target.value)} required inputMode="decimal" placeholder="0,00" />
              </label>
              <label>
                Responsável
                <select value={novoGasto.funcionarioId} onChange={(evento) => atualizarNovoGasto("funcionarioId", evento.target.value)} required>
                  <option value="" disabled>Selecione</option>
                  {alocacoes.map((alocacao) => (
                    <option key={alocacao.id} value={alocacao.funcionario.id}>
                      {alocacao.funcionario.nome}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Categoria
                <select value={novoGasto.categoria} onChange={(evento) => atualizarNovoGasto("categoria", evento.target.value)}>
                  <option value="MATERIAL">Material</option>
                  <option value="MAO_DE_OBRA">Mão de obra</option>
                  <option value="TRANSPORTE">Transporte</option>
                  <option value="EQUIPAMENTO">Equipamento</option>
                  <option value="IMPREVISTO">Imprevisto</option>
                </select>
              </label>
              <label>
                Método de pagamento
                <select value={novoGasto.metodoPagamento} onChange={(evento) => atualizarNovoGasto("metodoPagamento", evento.target.value)}>
                  <option value="PIX">Pix</option>
                  <option value="CARTAO_CREDITO">Cartão de crédito</option>
                  <option value="CARTAO_DEBITO">Cartão de débito</option>
                  <option value="DINHEIRO">Dinheiro</option>
                  <option value="TRANSFERENCIA">Transferência</option>
                </select>
              </label>
              <label>
                Etapa
                <select value={novoGasto.etapa} onChange={(evento) => atualizarNovoGasto("etapa", evento.target.value)}>
                  <option value="ETAPA 1">Etapa 1</option>
                  <option value="ETAPA 2">Etapa 2</option>
                  <option value="ETAPA 3">Etapa 3</option>
                </select>
              </label>
              <label>
                Data
                <input type="date" value={novoGasto.dtGasto} max={dataHoje()} onChange={(evento) => atualizarNovoGasto("dtGasto", evento.target.value)} required />
              </label>
            </div>

            <footer className={styles.modalFooter}>
              <button type="button" className={styles.botaoSecundario} onClick={() => setModalGastoAberto(false)}>Cancelar</button>
              <button type="submit" className={styles.botaoAdicionarGasto} disabled={salvandoGasto || alocacoes.length === 0}>
                {salvandoGasto ? "Salvando..." : "Salvar gasto"}
              </button>
            </footer>
          </form>
        </div>
      )}

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
          <input
            type="date"
            value={dataSelecionada}
            onChange={(e) => setDataSelecionada(e.target.value)}
            style={{
              padding: '8px 12px',
              border: '1px solid #d1d5db',
              borderRadius: '6px',
              marginRight: '8px',
              fontSize: '14px'
            }}
          />
          <button
            className={styles.botaoAdicionarGasto}
            onClick={() => setControlePresencaAberto(true)}
          >
            Controle de presença
          </button>
          <button className={styles.botaoAdicionarGasto} onClick={() => setModalGastoAberto(true)}>
            <Plus size={16} />
            Adicionar gasto
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
            <button className={styles.botaoEditar} onClick={() => setModalEdicaoAberto(true)} aria-label="Editar obra">
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
            <button
              className={styles.botaoAdicionarMembro}
              onClick={() => navegar(`/obras/${obra.id}/alocacoes`)}
            >
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
            <button className={styles.botaoFiltro} onClick={() => navegar(`/obras/detalhamento/${obra.id}/financeiro`)}>
              Ver dashboard
            </button>
            <div className={styles.campoBusca}>
              <Search size={15} className={styles.iconeBusca} />
              <input
                type="text"
                placeholder="Buscar lançamentos..."
                className={styles.inputBusca}
                value={buscaFinanceira}
                onChange={(evento) => setBuscaFinanceira(evento.target.value)}
              />
            </div>
            <button className={`${styles.botaoFiltro} ${ordemFinanceira === "valor" ? styles.botaoFiltroAtivo : ""}`} onClick={() => setOrdemFinanceira("valor")}>
              <Filter size={14} />
              Valor
            </button>
            <button className={`${styles.botaoFiltro} ${ordemFinanceira === "data" ? styles.botaoFiltroAtivo : ""}`} onClick={() => setOrdemFinanceira("data")}>
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
                    {buscaFinanceira ? "Nenhum lançamento encontrado para a busca." : "Nenhum gasto registrado para esta obra."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {gastosExibidos.length} de {gastosFiltrados.length} lançamentos
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
