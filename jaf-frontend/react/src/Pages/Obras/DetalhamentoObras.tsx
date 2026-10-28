import { useCallback, useState, useEffect, type FormEvent } from "react";
import {
  Calendar,
  Search,
  CalendarDays,
  ChevronLeft,
  ChevronRight,
  Plus,
  Pencil,
  Upload,
  X,
  Trash2,
} from "lucide-react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import styles from "./DetalhamentoObras.module.css";
import ControlePresenca from "../ControlePresenca/ControlePresenca";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { gastoService, type Gasto, type StatusReembolso } from "../../Service/Gastos/gastoService";
import { ocrService } from "../../Service/Gastos/ocrService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { authService } from "../../Service/Auth/Login/authService";
import PeriodoFiltro from "../../Components/PeriodoFiltro/PeriodoFiltro";
import {
  dataNoPeriodo,
  periodoFiltroInicial,
  resolverPeriodo,
  type PeriodoFiltroValue,
} from "../../Components/PeriodoFiltro/periodUtils";
import { toast } from "sonner";
import { Responsive } from "../../Components/Responsive/Responsive";
import FinanceiroObra from "../../Components/Financeiro/FinanceiroObra";
import ReembolsoModal from "../../Components/Financeiro/ReembolsoModal";
import ResumoReembolsos from "../../Components/Financeiro/ResumoReembolsos";
import { dataValida, erroApi, valorDecimal } from "../../Utils/financeiro";

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

const formatarData = (data: string) => {
  return new Date(`${data}T00:00:00`).toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" });
};

const getIniciais = (nome: string) => {
  const palavras = nome.split(" ");
  if (palavras.length >= 2) {
    return (palavras[0][0] + palavras[1][0]).toUpperCase();
  }
  return nome.substring(0, 2).toUpperCase();
};

const cargoLabel = (cargo: string | null | undefined) =>
  cargo ? cargo.replace(/_/g, " ") : "Não definido";

const STATUS_REEMBOLSO: Record<StatusReembolso, { rotulo: string; classe: string }> = {
  PENDENTE: { rotulo: "Pendente", classe: styles.statusPendente },
  PARCIAL: { rotulo: "Parcial", classe: styles.statusParcial },
  PAGO: { rotulo: "Pago", classe: styles.statusPago },
};

const dataHoje = () => {
  const partes = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Sao_Paulo",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date());
  const getParte = (tipo: string) => partes.find((parte) => parte.type === tipo)?.value ?? "";
  return `${getParte("year")}-${getParte("month")}-${getParte("day")}`;
};

const METODOS_PAGAMENTO = [
  { value: "PIX", label: "Pix" },
  { value: "CARTAO_CREDITO", label: "Cartão de crédito" },
  { value: "CARTAO_DEBITO", label: "Cartão de débito" },
  { value: "DINHEIRO", label: "Dinheiro" },
];

const FILTROS_PAGAMENTO = [
  ...METODOS_PAGAMENTO,
  { value: "REEMBOLSO", label: "Reembolso" },
];

const normalizarMetodoPagamento = (metodo: string) => {
  const metodoNormalizado = metodo
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/_/g, " ")
    .toLowerCase();

  if (metodoNormalizado.includes("pix")) return "PIX";
  if (metodoNormalizado.includes("debito")) return "CARTAO_DEBITO";
  if (metodoNormalizado.includes("credito") || metodoNormalizado.includes("cartao")) return "CARTAO_CREDITO";
  if (metodoNormalizado.includes("dinheiro") || metodoNormalizado.includes("especie")) return "DINHEIRO";
  if (metodoNormalizado.includes("reembolso")) return "REEMBOLSO";
  return metodo;
};

const metodoPagamentoLabel = (metodo: string) =>
  FILTROS_PAGAMENTO.find((item) => item.value === normalizarMetodoPagamento(metodo))?.label ??
  metodo
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/(^|\s)\S/g, (letra) => letra.toUpperCase());

const normalizarMetodoPagamentoOcr = (metodo: string | null | undefined) => {
  if (!metodo) return null;
  const metodoNormalizado = metodo
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/_/g, " ")
    .toLowerCase();

  if (metodoNormalizado.includes("pix")) return "PIX";
  if (metodoNormalizado.includes("debito")) return "CARTAO_DEBITO";
  if (metodoNormalizado.includes("cartao") || metodoNormalizado.includes("credito")) return "CARTAO_CREDITO";
  if (metodoNormalizado.includes("dinheiro") || metodoNormalizado.includes("especie")) return "DINHEIRO";
  return null;
};

const formatarValorInput = (valor: number) =>
  valor.toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

export default function DetalhamentoObras() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [controlePresencaAberto, setControlePresencaAberto] = useState(false);
  const [modalGastoAberto, setModalGastoAberto] = useState(false);
  const [modalEdicaoAberto, setModalEdicaoAberto] = useState(false);
  const [modalDeleteAberto, setModalDeleteAberto] = useState(false);
  const [salvandoGasto, setSalvandoGasto] = useState(false);
  const [salvandoObra, setSalvandoObra] = useState(false);
  const [deletandoObra, setDeletandoObra] = useState(false);
  const [gastoReembolso, setGastoReembolso] = useState<Gasto | null>(null);
  const [gastoEditando, setGastoEditando] = useState<Gasto | null>(null);
  const [excluindoGastoId, setExcluindoGastoId] = useState<number | null>(null);
  const [revisaoFinanceira, setRevisaoFinanceira] = useState(0);
  const [filtroPessoa, setFiltroPessoa] = useState("");
  const [filtroStatus, setFiltroStatus] = useState<StatusReembolso | "">("");
  const [carregandoGastos, setCarregandoGastos] = useState(false);
  const [erroGastos, setErroGastos] = useState("");
  const [buscaFinanceira, setBuscaFinanceira] = useState("");
  const [ordemFinanceira, setOrdemFinanceira] = useState<"data" | "valor" | "alfabetica">("data");
  const [filtroMetodoPagamento, setFiltroMetodoPagamento] = useState("TODOS");
  const [periodoFinanceiro, setPeriodoFinanceiro] = useState<PeriodoFiltroValue>(periodoFiltroInicial);
  const [dataSelecionada, setDataSelecionada] = useState<string>(dataHoje());
  const [notaFiscalArquivo, setNotaFiscalArquivo] = useState<File | null>(null);
  const [processandoNotaFiscal, setProcessandoNotaFiscal] = useState(false);
  const [mensagemNotaFiscal, setMensagemNotaFiscal] = useState<string | null>(null);
  const [novoGasto, setNovoGasto] = useState({
    descricao: "",
    categoria: "MATERIAL",
    metodoPagamento: "PIX",
    etapa: "ETAPA 1",
    valor: "",
    dtGasto: dataHoje(),
    funcionarioId: "",
    reembolsoFuncionario: false,
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
  const [pessoasHistoricas, setPessoasHistoricas] = useState<Gasto["funcionario"][]>([]);
  const [alocacoes, setAlocacoes] = useState<AlocacaoObra[]>([]);
  const [carregando, setCarregando] = useState(true);
  
  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const admin = authService.isAdmin();
  // O PUT legado exige orçamento: não reenviar valor oculto ou inventar zero.
  const podeEditarObra = admin && authService.hasAuthority("EDITAR_OBRA");
  const podeDeletarObra = authService.hasAuthority("DELETAR_OBRA");
  const podeCriarGasto = authService.hasAuthority("CRIAR_GASTO");
  const podeEditarGasto = authService.hasAuthority("EDITAR_GASTO");
  const podeDeletarGasto = authService.hasAuthority("DELETAR_GASTO");
  const podeVisualizarGastos = authService.hasAuthority("VISUALIZAR_GASTOS");
  const podeVisualizarAlocacoes = authService.hasAuthority("VISUALIZAR_ALOCACOES");
  const podeRegistrarPresenca = authService.hasAuthority("VISUALIZAR_PRESENCAS");

  const carregarDados = useCallback(async () => {
      try {
        setCarregando(true);
        
        // Se não houver ID, pega a primeira obra
        let obraData: Obra | null;
        if (id) {
          obraData = await obraService.buscarPorId(parseInt(id));
        } else {
          const obras = await obraService.listar();
          obraData = obras[0] || null;
        }
        
        setObra(obraData);
        if (!obraData) { setGastos([]); setAlocacoes([]); return; }
        setEdicaoObra({
          titulo: obraData.titulo,
          orcamento: obraData.orcamento ?? "",
          status: obraData.status,
          dtInicio: obraData.dtInicio,
          dtTerminoPrevisto: obraData.dtTerminoPrevisto,
        });

        if (obraData) {
          const [alocacoesResultado] = await Promise.allSettled([
            podeVisualizarAlocacoes ? alocacaoService.listarPorObra(obraData.id) : Promise.resolve([]),
          ]);
          const alocacoesData = alocacoesResultado.status === "fulfilled" ? alocacoesResultado.value : [];
          if (alocacoesResultado.status === "rejected") toast.error("Não foi possível carregar as alocações");
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
  }, [id, podeVisualizarAlocacoes]);

  const obraId = obra?.id;
  useEffect(() => {
    setFiltroPessoa("");
    setFiltroStatus("");
    setGastoReembolso(null);
    setPessoasHistoricas([]);
  }, [obraId]);

  useEffect(() => {
    if (!obraId || !podeVisualizarGastos) { setGastos([]); return; }
    let ativo = true;
    setCarregandoGastos(true);
    setErroGastos("");
    // As opções não podem depender da resposta filtrada (nem das alocações atuais).
    const historico = filtroPessoa || filtroStatus ? gastoService.listarPorObra(obraId) : null;
    const consulta = gastoService.listarPorObra(obraId, {
      funcionarioId: filtroPessoa ? Number(filtroPessoa) : undefined,
      statusReembolso: filtroStatus || undefined,
    });
    Promise.all([historico ?? consulta, consulta]).then(([todos, dados]) => {
      if (ativo) { setGastos(dados); setPessoasHistoricas(todos.map((gasto) => gasto.funcionario)); }
    })
      .catch((e: unknown) => { if (ativo) { setGastos([]); setErroGastos(erroApi(e, "Não foi possível carregar os gastos. Atualize para tentar novamente.")); } })
      .finally(() => { if (ativo) setCarregandoGastos(false); });
    return () => { ativo = false; };
  }, [obraId, podeVisualizarGastos, filtroPessoa, filtroStatus, revisaoFinanceira]);

  useEffect(() => {
    carregarDados();
  }, [carregarDados, id]);

  useEffect(() => {
    if (searchParams.get("editar") === "1" && podeEditarObra) {
      setModalEdicaoAberto(true);
    }
  }, [podeEditarObra, searchParams]);

  useEffect(() => {
    setPaginaAtual(1);
  }, [buscaFinanceira, ordemFinanceira, filtroMetodoPagamento, periodoFinanceiro, filtroPessoa, filtroStatus]);

  // Calcular dias restantes
  const calcularDiasRestantes = () => {
    if (!obra?.dtTerminoPrevisto) return 0;
    const hoje = new Date(`${dataHoje()}T00:00:00`);
    const dataTermino = new Date(`${obra.dtTerminoPrevisto}T00:00:00`);
    const diffTime = dataTermino.getTime() - hoje.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  };

  const intervaloFinanceiro = resolverPeriodo(periodoFinanceiro);

  const gastosFiltrados = gastos
    .filter((gasto) => {
      const termo = buscaFinanceira.trim().toLowerCase();
      const correspondeBusca = !termo || [
        gasto.descricao,
        gasto.categoria,
        gasto.metodoPagamento,
        gasto.etapa,
        gasto.funcionario.nome,
      ]
        .filter(Boolean)
        .some((campo) => campo.toLowerCase().includes(termo));

      const correspondeMetodo =
        filtroMetodoPagamento === "TODOS" ||
        normalizarMetodoPagamento(gasto.metodoPagamento) === filtroMetodoPagamento;

      const correspondePeriodo = dataNoPeriodo(gasto.dtGasto, intervaloFinanceiro);

      return correspondeBusca && correspondeMetodo && correspondePeriodo;
    })
    .sort((a, b) => {
      if (ordemFinanceira === "valor") {
        return b.valor - a.valor;
      }
      if (ordemFinanceira === "alfabetica") {
        return a.descricao.localeCompare(b.descricao);
      }
      return new Date(b.dtGasto).getTime() - new Date(a.dtGasto).getTime();
    });

  const totalPaginas = Math.ceil(gastosFiltrados.length / 5);
  const paginaVisivel = Math.min(paginaAtual, Math.max(1, totalPaginas));
  const gastosExibidos = gastosFiltrados.slice((paginaVisivel - 1) * 5, paginaVisivel * 5);

  function atualizarNovoGasto(campo: keyof typeof novoGasto, valor: string | boolean) {
    setNovoGasto((gastoAtual) => ({ ...gastoAtual, [campo]: valor }));
  }

  function limparNotaFiscal() {
    setNotaFiscalArquivo(null);
    setMensagemNotaFiscal(null);
  }

  function fecharModalGasto() {
    if (salvandoGasto || processandoNotaFiscal) return;
    setModalGastoAberto(false);
    setGastoEditando(null);
    limparNotaFiscal();
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

    const orcamento = valorDecimal(edicaoObra.orcamento);
    if (orcamento === null) {
      toast.error("Informe um orçamento positivo com até duas casas decimais");
      return;
    }
    if (!admin || edicaoObra.titulo.trim().length < 5 || edicaoObra.dtTerminoPrevisto < edicaoObra.dtInicio) {
      toast.error("Verifique o título e o intervalo de datas da obra"); return;
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
      toast.error(erroApi(error, "Não foi possível atualizar a obra"));
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

    const valor = valorDecimal(novoGasto.valor);
    if (valor === null) {
      toast.error("Informe um valor positivo com até duas casas decimais e dentro do limite seguro");
      return;
    }
    if (!novoGasto.descricao.trim() || !dataValida(novoGasto.dtGasto, dataHoje())) {
      toast.error("Informe uma descrição e uma data válida até hoje");
      return;
    }

    try {
      setSalvandoGasto(true);
      const payload = {
        descricao: novoGasto.descricao.trim(),
        categoria: novoGasto.categoria,
        metodoPagamento: novoGasto.reembolsoFuncionario ? "REEMBOLSO" : novoGasto.metodoPagamento,
        etapa: novoGasto.etapa,
        valor,
        dtGasto: novoGasto.dtGasto,
        funcionarioId: Number(novoGasto.funcionarioId),
        obraId: obra.id,
      };
      if (gastoEditando) await gastoService.atualizar(gastoEditando.id, payload);
      else await gastoService.criar(payload);
      setRevisaoFinanceira((r) => r + 1);
      setModalGastoAberto(false);
      setGastoEditando(null);
      limparNotaFiscal();
      setNovoGasto({
        descricao: "",
        categoria: "MATERIAL",
        metodoPagamento: "PIX",
        etapa: "ETAPA 1",
        valor: "",
        dtGasto: dataHoje(),
        funcionarioId: String(alocacoes[0]?.funcionario.id ?? ""),
        reembolsoFuncionario: false,
      });
      toast.success(gastoEditando ? "Gasto atualizado" : "Gasto adicionado à obra");
    } catch (error) {
      console.error("Erro ao cadastrar gasto:", error);
      toast.error(erroApi(error, "Não foi possível salvar o gasto"));
    } finally {
      setSalvandoGasto(false);
    }
  }

  async function processarNotaFiscal() {
    if (!notaFiscalArquivo) {
      toast.error("Selecione a foto ou PDF da nota fiscal");
      return;
    }

    try {
      setProcessandoNotaFiscal(true);
      setMensagemNotaFiscal(null);
      const resultado = await ocrService.processarNotaFiscal(notaFiscalArquivo);

      if (!resultado.sucesso) {
        setMensagemNotaFiscal(resultado.mensagem);
        toast.error(resultado.mensagem || "Não foi possível processar a nota fiscal");
        return;
      }

      const metodoPagamento = normalizarMetodoPagamentoOcr(resultado.metodoPagamento);
      const descricao = [resultado.materialInsumo, resultado.descricaoAdicional]
        .filter(Boolean)
        .join(" - ")
        .slice(0, 255);

      setNovoGasto((gastoAtual) => ({
        ...gastoAtual,
        descricao: descricao || gastoAtual.descricao,
        valor: resultado.valor ? formatarValorInput(resultado.valor) : gastoAtual.valor,
        dtGasto: resultado.dtGasto || gastoAtual.dtGasto,
        metodoPagamento: metodoPagamento || gastoAtual.metodoPagamento,
      }));

      setMensagemNotaFiscal(resultado.mensagem);
      toast.success("Campos preenchidos a partir da nota fiscal");
    } catch (error) {
      console.error("Erro ao processar nota fiscal:", error);
      toast.error("Não foi possível processar a nota fiscal");
    } finally {
      setProcessandoNotaFiscal(false);
    }
  }

  function editarGasto(gasto: Gasto) {
    setGastoEditando(gasto);
    setNovoGasto({ descricao: gasto.descricao, categoria: gasto.categoria,
      metodoPagamento: gasto.metodoPagamento === "REEMBOLSO" ? "PIX" : gasto.metodoPagamento,
      etapa: gasto.etapa, valor: gasto.valor.toFixed(2), dtGasto: gasto.dtGasto,
      funcionarioId: String(gasto.funcionario.id), reembolsoFuncionario: gasto.metodoPagamento === "REEMBOLSO" });
    setModalGastoAberto(true);
  }

  async function excluirGasto(gasto: Gasto) {
    if (!podeDeletarGasto || excluindoGastoId || !window.confirm(`Excluir o gasto “${gasto.descricao}”? Gastos com pagamentos não podem ser excluídos.`)) return;
    setExcluindoGastoId(gasto.id);
    try { await gastoService.deletar(gasto.id); setRevisaoFinanceira((r) => r + 1); toast.success("Gasto excluído"); }
    catch (e) { toast.error(erroApi(e, "Não foi possível excluir o gasto")); }
    finally { setExcluindoGastoId(null); }
  }

  function renderizarAcoesGasto(gasto: Gasto) {
    if (!podeEditarGasto && !podeDeletarGasto) return null;
    return <div className={styles.acoesGasto}>
      {podeEditarGasto && (
        <button className={styles.botaoIcone} type="button" aria-label="Editar gasto" title="Editar gasto" onClick={() => editarGasto(gasto)}>
          <Pencil size={15} aria-hidden="true" />
        </button>
      )}
      {podeDeletarGasto && (
        <button
          className={`${styles.botaoIcone} ${styles.botaoIconePerigo}`}
          type="button"
          aria-label="Excluir gasto"
          title={excluindoGastoId === gasto.id ? "Excluindo..." : "Excluir gasto"}
          aria-busy={excluindoGastoId === gasto.id}
          disabled={excluindoGastoId !== null}
          onClick={() => void excluirGasto(gasto)}
        >
          <Trash2 size={15} aria-hidden="true" />
        </button>
      )}
    </div>;
  }

  function irParaPaginaAnterior() {
    setPaginaAtual(Math.max(1, paginaVisivel - 1));
  }

  function irParaProximaPagina() {
    setPaginaAtual(Math.min(totalPaginas, paginaVisivel + 1));
  }

  function irParaPagina(pagina: number) {
    setPaginaAtual(pagina);
  }

  async function deletarObra() {
    if (!obra) return;

    try {
      setDeletandoObra(true);
      await obraService.deletar(obra.id);
      toast.success("Obra excluída com sucesso");
      navegar("/home");
    } catch (error) {
      console.error("Erro ao deletar obra:", error);
      toast.error("Não foi possível excluir a obra");
    } finally {
      setDeletandoObra(false);
    }
  }

  function renderizarReembolso(gasto: Gasto) {
    if (gasto.metodoPagamento !== "REEMBOLSO") {
      return (
        <span className={styles.semReembolso} title="Não é reembolso">
          <span aria-hidden="true">—</span>
          <span className={styles.textoOculto}>Não</span>
        </span>
      );
    }

    // O gasto não traz status próprio; só é confiável quando filtrado no servidor.
    const status = filtroStatus ? STATUS_REEMBOLSO[filtroStatus] : null;
    return (
      <div className={styles.celulaReembolso}>
        {status && <span className={`${styles.statusBadge} ${status.classe}`}>{status.rotulo}</span>}
        <button
          type="button"
          className={`${styles.botaoSecundario} ${styles.botaoCompacto}`}
          onClick={() => setGastoReembolso(gasto)}
          title="Consultar parcelas e saldo do reembolso"
        >
          {authService.podeRegistrarPagamento() ? "Pagamentos / Registrar" : "Ver pagamentos"}
        </button>
      </div>
    );
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
      {gastoReembolso && <ReembolsoModal key={gastoReembolso.id} gasto={gastoReembolso} onFechar={() => setGastoReembolso(null)} onAtualizado={() => setRevisaoFinanceira((r) => r + 1)} />}
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
                  <option value="PLANEJADA">Planejada</option>
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
              <button type="submit" className={styles.botaoPrimario} disabled={salvandoObra}>
                {salvandoObra ? "Salvando..." : "Salvar obra"}
              </button>
            </footer>
          </form>
        </div>
      )}

      {modalGastoAberto && (
        <div className={styles.modalOverlay} onClick={fecharModalGasto}>
          <form className={styles.modalGasto} onSubmit={cadastrarGasto} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>{gastoEditando ? "Editar gasto" : "Adicionar gasto"}</h2>
                <p>{obra.titulo}</p>
              </div>
              <button type="button" className={styles.botaoFecharModal} onClick={fecharModalGasto} aria-label="Fechar modal de gasto">
                <X size={20} />
              </button>
            </header>

            <div className={styles.modalGrid}>
              <div className={styles.notaFiscalBox}>
                <div>
                  <strong>Nota fiscal</strong>
                  <span>{notaFiscalArquivo ? notaFiscalArquivo.name : "Selecione uma imagem ou PDF para preencher os campos automaticamente."}</span>
                  {mensagemNotaFiscal && <small>{mensagemNotaFiscal}</small>}
                </div>
                <label className={styles.botaoUploadNota}>
                  <Upload size={16} />
                  Escolher arquivo
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/gif,image/bmp,image/tiff,image/webp,application/pdf"
                    onChange={(evento) => setNotaFiscalArquivo(evento.target.files?.[0] ?? null)}
                  />
                </label>
                <button
                  type="button"
                  className={styles.botaoProcessarNota}
                  onClick={processarNotaFiscal}
                  disabled={!notaFiscalArquivo || processandoNotaFiscal}
                >
                  {processandoNotaFiscal ? "Lendo nota..." : "Extrair campos"}
                </button>
              </div>

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
                  {gastoEditando && !alocacoes.some((a) => a.funcionario.id === gastoEditando.funcionario.id) && <option value={gastoEditando.funcionario.id}>{gastoEditando.funcionario.nome}</option>}
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
                </select>
              </label>
              <label className={styles.checkboxLinha}>
                <input
                  type="checkbox"
                  checked={novoGasto.reembolsoFuncionario}
                  onChange={(evento) => atualizarNovoGasto("reembolsoFuncionario", evento.target.checked)}
                />
                <span>
                  Reembolso ao funcionário
                  <small>Use quando o funcionário pagou com cartão, Pix ou dinheiro pessoal.</small>
                </span>
              </label>
              <label>
                Etapa
                <select value={novoGasto.etapa} onChange={(evento) => atualizarNovoGasto("etapa", evento.target.value)}>
                  <option value="ETAPA 1">Etapa 1</option>
                  <option value="ETAPA 2">Etapa 2</option>
                </select>
              </label>
              <label>
                Data
                <input type="date" value={novoGasto.dtGasto} max={dataHoje()} onChange={(evento) => atualizarNovoGasto("dtGasto", evento.target.value)} required />
              </label>
            </div>

            <footer className={styles.modalFooter}>
              <button type="button" className={styles.botaoSecundario} onClick={fecharModalGasto}>Cancelar</button>
              <button type="submit" className={styles.botaoPrimario} disabled={salvandoGasto || processandoNotaFiscal || (!gastoEditando && alocacoes.length === 0)}>
                {salvandoGasto ? "Salvando..." : "Salvar gasto"}
              </button>
            </footer>
          </form>
        </div>
      )}

      {modalDeleteAberto && (
        <div className={styles.modalOverlay} onClick={() => setModalDeleteAberto(false)}>
          <div className={styles.modalGasto} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>Excluir obra</h2>
                <p>{obra.titulo}</p>
              </div>
              <button type="button" className={styles.botaoFecharModal} onClick={() => setModalDeleteAberto(false)} aria-label="Fechar modal de exclusão">
                <X size={20} />
              </button>
            </header>
            <div className={styles.modalGrid}>
              <p className={styles.textoModal}>
                Tem certeza que deseja excluir esta obra? Esta ação é irreversível e todos os dados associados (gastos, alocações, presenças) serão permanentemente removidos.
              </p>
            </div>
            <footer className={styles.modalFooter}>
              <button type="button" className={styles.botaoSecundario} onClick={() => setModalDeleteAberto(false)}>Cancelar</button>
              <button type="button" className={styles.botaoPerigo} onClick={deletarObra} disabled={deletandoObra}>
                {deletandoObra ? "Excluindo..." : "Confirmar exclusão"}
              </button>
            </footer>
          </div>
        </div>
      )}

      {/* Cabeçalho */}
      <div className={styles.cabecalho}>
        <div className={styles.cabecalhoEsquerda}></div>
        <div className={styles.cabecalhoAcoes}>
          {podeRegistrarPresenca && (
            <>
              <input
                type="date"
                className={styles.inputDataPresenca}
                max={dataHoje()}
                value={dataSelecionada}
                onChange={(e) => setDataSelecionada(e.target.value)}
                aria-label="Data do controle de presença"
              />
              <button
                type="button"
                className={styles.botaoSecundario}
                onClick={() => setControlePresencaAberto(true)}
              >
                Controle de presença
              </button>
            </>
          )}
          {podeCriarGasto && (
            <button type="button" className={styles.botaoPrimario} onClick={() => {
              setGastoEditando(null);
              setNovoGasto({ descricao: "", categoria: "MATERIAL", metodoPagamento: "PIX", etapa: "ETAPA 1", valor: "", dtGasto: dataHoje(), funcionarioId: String(alocacoes[0]?.funcionario.id ?? ""), reembolsoFuncionario: false });
              setModalGastoAberto(true);
            }}>
              <Plus size={16} />
              Adicionar gasto
            </button>
          )}
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
            <div className={styles.cardObraAcoes}>
              {podeEditarObra && (
                <button className={styles.botaoEditar} onClick={() => setModalEdicaoAberto(true)} aria-label="Editar obra">
                  <Pencil size={16} />
                </button>
              )}
              {podeDeletarObra && (
                <button className={styles.botaoDeletar} onClick={() => setModalDeleteAberto(true)} aria-label="Excluir obra">
                  <Trash2 size={16} />
                </button>
              )}
            </div>
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
              alocacoes.map((alocacao) => (
                <div key={alocacao.id} className={styles.membroCard}>
                  <div className={styles.membroAvatar} aria-hidden="true">
                    {getIniciais(alocacao.funcionario.nome)}
                  </div>
                  <div className={styles.membroInformacoes}>
                    <span className={styles.membroNome}>{alocacao.funcionario.nome}</span>
                    <span className={styles.membroCargo}>
                      {cargoLabel(alocacao.cargo)}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <p className={styles.equipeVazia}>Nenhum funcionário alocado</p>
            )}
            {podeVisualizarAlocacoes && (
              <button
                type="button"
                className={styles.botaoAdicionarMembro}
                onClick={() => navegar(`/obras/${obra.id}/alocacoes`)}
                aria-label="Gerenciar equipe da obra"
                title="Gerenciar equipe da obra"
              >
                <Plus size={18} />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Cards de métricas */}
      {admin && <FinanceiroObra key={obra.id} obraId={obra.id} orcamento={obra.orcamento} revisao={revisaoFinanceira} />}
      <div className={styles.metricas}>
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
      {podeVisualizarGastos && <>
      <ResumoReembolsos key={obra.id} obraId={obra.id} revisao={revisaoFinanceira} />
      <div className={styles.secaoFinanceiro}>
        <div className={styles.financeiroTopo}>
          <div className={styles.financeiroCabecalho}>
            <div>
              <h2 className={styles.financeiroTitulo}>Gastos</h2>
              {!carregandoGastos && !erroGastos && (
                <span className={styles.financeiroContador}>
                  {gastosFiltrados.length} {gastosFiltrados.length === 1 ? "lançamento" : "lançamentos"}
                </span>
              )}
            </div>
            {admin && (
              <button type="button" className={styles.botaoSecundario} onClick={() => navegar(`/obras/detalhamento/${obra.id}/financeiro`)}>
                Ver estatísticas
              </button>
            )}
          </div>
          <div className={styles.toolbar} role="group" aria-label="Filtros de gastos">
            <label className={`${styles.campoFiltro} ${styles.campoBusca}`}>
              <span className={styles.rotuloFiltro}>Buscar</span>
              <span className={styles.buscaControle}>
                <Search size={15} className={styles.iconeBusca} aria-hidden="true" />
                <input
                  type="text"
                  placeholder="Buscar lançamentos..."
                  aria-label="Buscar lançamentos"
                  className={styles.inputBusca}
                  value={buscaFinanceira}
                  onChange={(evento) => setBuscaFinanceira(evento.target.value)}
                />
              </span>
            </label>
            <div className={styles.campoFiltro}>
              <span className={styles.rotuloFiltro} aria-hidden="true">Ordenar por</span>
              <div className={styles.segmentado} role="group" aria-label="Ordenar por">
                {([
                  { valor: "valor", rotulo: "Valor" },
                  { valor: "alfabetica", rotulo: "Alfabética" },
                  { valor: "data", rotulo: "Data" },
                ] as const).map((opcao) => (
                  <button
                    key={opcao.valor}
                    type="button"
                    className={`${styles.segmento} ${ordemFinanceira === opcao.valor ? styles.segmentoAtivo : ""}`}
                    aria-pressed={ordemFinanceira === opcao.valor}
                    onClick={() => setOrdemFinanceira(opcao.valor)}
                  >
                    {opcao.rotulo}
                  </button>
                ))}
              </div>
            </div>
            <label className={styles.campoFiltro}>
              <span className={styles.rotuloFiltro}>Pagamento</span>
              <select className={styles.inputFiltro} aria-label="Pagamento" value={filtroMetodoPagamento} onChange={(evento) => setFiltroMetodoPagamento(evento.target.value)}>
                <option value="TODOS">Todos os pagamentos</option>
                {FILTROS_PAGAMENTO.map((metodo) => (
                  <option key={metodo.value} value={metodo.value}>{metodo.label}</option>
                ))}
              </select>
            </label>
            <label className={styles.campoFiltro}>
              <span className={styles.rotuloFiltro}>Pessoa</span>
              <select className={styles.inputFiltro} aria-label="Pessoa" value={filtroPessoa} onChange={(e) => setFiltroPessoa(e.target.value)}>
                <option value="">Todas as pessoas</option>
                {Array.from(new Map([...alocacoes.map((a) => [a.funcionario.id, a.funcionario.nome] as const), ...pessoasHistoricas.map((pessoa) => [pessoa.id, pessoa.nome] as const)]).entries()).map(([pessoaId, nome]) => <option key={pessoaId} value={pessoaId}>{nome}</option>)}
              </select>
            </label>
            <label className={styles.campoFiltro}>
              <span className={styles.rotuloFiltro}>Status do reembolso</span>
              <select className={styles.inputFiltro} aria-label="Status do reembolso" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value as StatusReembolso | "")}>
                <option value="">Todos os status</option>
                <option value="PENDENTE">Pendente</option><option value="PARCIAL">Parcial</option><option value="PAGO">Pago</option>
              </select>
            </label>
            <div className={`${styles.campoFiltro} ${styles.campoPeriodo}`}>
              <span className={styles.rotuloFiltro} aria-hidden="true">Período</span>
              <PeriodoFiltro value={periodoFinanceiro} onChange={setPeriodoFinanceiro} segmentado />
            </div>
          </div>
        </div>

        {/* Tabela */}
        {carregandoGastos && <p role="status" className={styles.estadoLista}>Carregando gastos...</p>}
        {erroGastos && <p role="alert" className={styles.mensagemErro}>{erroGastos}</p>}
        {!carregandoGastos && !erroGastos &&
        <div className={styles.tabelaWrapper}>
          <Responsive max="md">
            <ul className={styles.listaCards}>
              {gastosExibidos.length > 0 ? (
                gastosExibidos.map((gasto) => (
                  <li key={gasto.id} className={styles.itemCard}>
                    <div className={styles.itemCardTopo}>
                      <span className={styles.descricao}>{gasto.descricao}</span>
                      <span className={styles.celulaValor}>
                        {formatarMoeda(gasto.valor)}
                      </span>
                    </div>
                    <div className={styles.itemCardTopo}>
                      <div className={styles.celulaFuncionario}>
                        <div className={styles.avatarPequeno} aria-hidden="true">
                          {getIniciais(gasto.funcionario.nome)}
                        </div>
                        {gasto.funcionario.nome}
                      </div>
                      <span className={styles.itemCardData}>{formatarData(gasto.dtGasto)}</span>
                    </div>
                    <dl className={styles.itemCardDetalhes}>
                      <div>
                        <dt>Tipo</dt>
                        <dd className={gasto.metodoPagamento === "REEMBOLSO" ? styles.metodoReembolso : undefined}>
                          {metodoPagamentoLabel(gasto.metodoPagamento)}
                        </dd>
                      </div>
                      <div>
                        <dt>Reembolso</dt>
                        <dd>{renderizarReembolso(gasto)}</dd>
                      </div>
                      <div>
                        <dt>Etapa</dt>
                        <dd>{gasto.etapa || "-"}</dd>
                      </div>
                      <div>
                        <dt>Material</dt>
                        <dd>{gasto.categoria || "-"}</dd>
                      </div>
                    </dl>
                    {renderizarAcoesGasto(gasto)}
                  </li>
                ))
              ) : (
                <li className={styles.itemCardVazio}>
                  {buscaFinanceira ? "Nenhum lançamento encontrado para a busca." : "Nenhum gasto registrado para esta obra."}
                </li>
              )}
            </ul>
          </Responsive>

          <Responsive min="lg">
          <div className={styles.tabelaScroll}>
          <table className={styles.tabela}>
            <thead>
              <tr>
                <th>VALOR</th>
                <th>FUNCIONÁRIO</th>
                <th>TIPO</th>
                <th>REEMBOLSO</th>
                <th>DESCRIÇÃO</th>
                <th>ETAPA</th>
                <th>MATERIAL</th>
                <th>DATA</th>
                <th>AÇÕES</th>
              </tr>
            </thead>
            <tbody>
              {gastosExibidos.length > 0 ? (
                gastosExibidos.map((gasto) => (
                  <tr key={gasto.id}>
                    <td>
                      <span className={styles.celulaValor}>
                        {formatarMoeda(gasto.valor)}
                      </span>
                    </td>
                    <td>
                      <div className={styles.celulaFuncionario}>
                        <div className={styles.avatarPequeno} aria-hidden="true">
                          {getIniciais(gasto.funcionario.nome)}
                        </div>
                        {gasto.funcionario.nome}
                      </div>
                    </td>
                    <td>
                      <span className={gasto.metodoPagamento === "REEMBOLSO" ? styles.metodoReembolso : undefined}>
                        {metodoPagamentoLabel(gasto.metodoPagamento)}
                      </span>
                    </td>
                    <td>{renderizarReembolso(gasto)}</td>
                    <td>
                      <span className={styles.descricao}>{gasto.descricao}</span>
                    </td>
                    <td>{gasto.etapa || "-"}</td>
                    <td>{gasto.categoria || "-"}</td>
                    <td>{formatarData(gasto.dtGasto)}</td>
                    <td>{renderizarAcoesGasto(gasto)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={9} className={styles.estadoLista}>
                    {buscaFinanceira ? "Nenhum lançamento encontrado para a busca." : "Nenhum gasto registrado para esta obra."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          </div>
          </Responsive>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {gastosExibidos.length} de {gastosFiltrados.length} lançamentos
            </span>
            <div className={styles.paginacaoBotoes}>
              <button
                type="button"
                className={styles.botaoPaginacao}
                onClick={irParaPaginaAnterior}
                disabled={paginaVisivel === 1}
                aria-label="Página anterior"
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: totalPaginas }, (_, i) => i + 1).map((numeroPagina) => (
                <button
                  key={numeroPagina}
                  type="button"
                  className={`${styles.botaoPaginacao} ${paginaVisivel === numeroPagina ? styles.botaoPaginacaoAtivo : ""}`}
                  aria-current={paginaVisivel === numeroPagina ? "page" : undefined}
                  onClick={() => irParaPagina(numeroPagina)}
                >
                  {numeroPagina}
                </button>
              ))}
              <button
                type="button"
                className={styles.botaoPaginacao}
                onClick={irParaProximaPagina}
                disabled={paginaVisivel === totalPaginas || totalPaginas === 0}
                aria-label="Próxima página"
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>}
      </div>
      </>}
    </div>
  );
}
