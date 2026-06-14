import { useState, useEffect, type FormEvent } from "react";
import {
  ArrowLeft,
  Calendar,
  ChevronLeft,
  ChevronRight,
  UserCheck,
  Search,
  X,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import styles from "./AlocacaoFuncionario.module.css";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { funcionarioService } from "../../Service/Funcionarios/funcionarioService";
import type { FuncionarioPermissoes } from "../../Types/permissoes";
import { CargoLabel, type Cargo } from "../../Types/user";
import { toast } from "sonner";

const IMAGEM_PLACEHOLDER =
  "https://s2.glbimg.com/YgImWrcpJX6FAHRJOlZtEnTaHYc=/e.glbimg.com/og/ed/f/original/2020/12/23/apartamento-sao-paulo-260-m2-atemporal3.jpg";

const ITENS_POR_PAGINA = 8;

const formatarData = (data: string) =>
  new Date(data).toLocaleDateString("pt-BR");

const getIniciais = (nome: string) => {
  const palavras = nome.split(" ");
  if (palavras.length >= 2) {
    return (palavras[0][0] + palavras[1][0]).toUpperCase();
  }
  return nome.substring(0, 2).toUpperCase();
};

const cargoLabel = (cargo: string | null | undefined) =>
  cargo ? (CargoLabel[cargo as Cargo] ?? cargo.replace(/_/g, " ")) : "Não definido";

const cores = ["#6C63FF", "#FF6584", "#43B89C", "#ffc107", "#9c27b0"];

const cargosObra = [
  { value: "GESTOR_OBRA", label: "Gestor de Obra" },
  { value: "ENGENHEIRO", label: "Engenheiro" },
  { value: "ARQUITETO", label: "Arquiteto" },
  { value: "MESTRE_DE_OBRAS", label: "Mestre de Obras" },
  { value: "OPERADOR_LANCAMENTO", label: "Operador de Lançamento" },
  { value: "PEDREIRO", label: "Pedreiro" },
];

export default function AlocacaoFuncionario() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [busca, setBusca] = useState("");
  const [cargoFiltro, setCargoFiltro] = useState("TODOS");
  const [modalAberto, setModalAberto] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [funcionarios, setFuncionarios] = useState<FuncionarioPermissoes[]>([]);
  const [novaAlocacao, setNovaAlocacao] = useState({ funcionarioId: "", cargoNaObra: "OPERADOR_LANCAMENTO" });
  const [obra, setObra] = useState<Obra | null>(null);
  const [alocacoes, setAlocacoes] = useState<AlocacaoObra[]>([]);
  const [carregando, setCarregando] = useState(true);

  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();

  useEffect(() => {
    const carregarDados = async () => {
      if (!id) {
        toast.error("ID da obra não fornecido");
        return;
      }
      try {
        setCarregando(true);
        const [obraData, alocacoesData, funcionariosData] = await Promise.all([
          obraService.buscarPorId(parseInt(id)),
          alocacaoService.listarPorObra(parseInt(id)),
          funcionarioService.listar(),
        ]);
        setObra(obraData);
        setAlocacoes(alocacoesData);
        setFuncionarios(funcionariosData);
        setPaginaAtual(1);
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
        if (error instanceof Error) {
          toast.error(`Erro ao carregar alocações: ${error.message}`);
        } else {
          toast.error("Erro ao carregar alocações da obra");
        }
      } finally {
        setCarregando(false);
      }
    };
    carregarDados();
  }, [id]);

  useEffect(() => {
    setPaginaAtual(1);
  }, [busca, cargoFiltro]);

  const funcionariosDisponiveis = funcionarios.filter(
    (funcionario) => !alocacoes.some((alocacao) => alocacao.funcionario.id === funcionario.id),
  );

  function abrirModalAlocacao() {
    setNovaAlocacao((valorAtual) => ({
      ...valorAtual,
      funcionarioId: valorAtual.funcionarioId || String(funcionariosDisponiveis[0]?.id ?? ""),
    }));
    setModalAberto(true);
  }

  const alocacoesFiltradas = alocacoes.filter((alocacao) => {
    const termo = busca.trim().toLowerCase();
    const correspondeBusca = !termo || [
      alocacao.funcionario.nome,
      alocacao.funcionario.email,
      cargoLabel(alocacao.funcionario.cargoGlobal),
      cargoLabel(alocacao.cargo),
    ].some((campo) => campo.toLowerCase().includes(termo));

    const correspondeCargo = cargoFiltro === "TODOS" || alocacao.cargo === cargoFiltro;
    return correspondeBusca && correspondeCargo;
  });

  const totalPaginas = Math.ceil(alocacoesFiltradas.length / ITENS_POR_PAGINA);
  const alocacoesExibidas = alocacoesFiltradas.slice(
    (paginaAtual - 1) * ITENS_POR_PAGINA,
    paginaAtual * ITENS_POR_PAGINA
  );

  async function carregarAlocacoes() {
    if (!id) return;
    const alocacoesAtualizadas = await alocacaoService.listarPorObra(parseInt(id));
    setAlocacoes(alocacoesAtualizadas);
  }

  async function criarAlocacao(evento: FormEvent<HTMLFormElement>) {
    evento.preventDefault();
    if (!id || !novaAlocacao.funcionarioId) {
      toast.error("Selecione um funcionário para alocar");
      return;
    }

    try {
      setSalvando(true);
      await alocacaoService.criar({
        funcionarioId: Number(novaAlocacao.funcionarioId),
        obraId: Number(id),
        cargoNaObra: novaAlocacao.cargoNaObra,
      });
      await carregarAlocacoes();
      setNovaAlocacao({ funcionarioId: "", cargoNaObra: "OPERADOR_LANCAMENTO" });
      setModalAberto(false);
      toast.success("Funcionário alocado com sucesso");
    } catch (error) {
      console.error("Erro ao alocar funcionário:", error);
      toast.error("Não foi possível alocar o funcionário");
    } finally {
      setSalvando(false);
    }
  }

  function irParaPaginaAnterior() {
    setPaginaAtual((p) => Math.max(1, p - 1));
  }

  function irParaProximaPagina() {
    setPaginaAtual((p) => Math.min(totalPaginas, p + 1));
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
        <div style={{ textAlign: "center", padding: "2rem" }}>
          Obra não encontrada.
        </div>
      </div>
    );
  }

  return (
    <div className={styles.pagina}>
      {modalAberto && (
        <div className={styles.modalOverlay} onClick={() => setModalAberto(false)}>
          <form className={styles.modalAlocacao} onSubmit={criarAlocacao} onClick={(evento) => evento.stopPropagation()}>
            <header className={styles.modalHeader}>
              <div>
                <h2>Alocar funcionário</h2>
                <p>{obra.titulo}</p>
              </div>
              <button type="button" className={styles.botaoFecharModal} onClick={() => setModalAberto(false)} aria-label="Fechar modal de alocação">
                <X size={20} />
              </button>
            </header>
            <div className={styles.modalGrid}>
              <label>
                Funcionário
                <select value={novaAlocacao.funcionarioId} onChange={(evento) => setNovaAlocacao((valorAtual) => ({ ...valorAtual, funcionarioId: evento.target.value }))} required>
                  <option value="" disabled>Selecione</option>
                  {funcionariosDisponiveis.map((funcionario) => (
                    <option key={funcionario.id} value={funcionario.id}>
                      {funcionario.nome} - {cargoLabel(funcionario.cargo)}
                    </option>
                  ))}
                </select>
                {funcionariosDisponiveis.length === 0 && (
                  <span className={styles.modalAviso}>Todos os funcionários disponíveis já estão alocados nesta obra.</span>
                )}
              </label>
              <label>
                Cargo na obra
                <select value={novaAlocacao.cargoNaObra} onChange={(evento) => setNovaAlocacao((valorAtual) => ({ ...valorAtual, cargoNaObra: evento.target.value }))}>
                  {cargosObra.map((cargo) => (
                    <option key={cargo.value} value={cargo.value}>{cargo.label}</option>
                  ))}
                </select>
              </label>
            </div>
            <footer className={styles.modalFooter}>
              <button type="button" className={styles.botaoSecundario} onClick={() => setModalAberto(false)}>Cancelar</button>
              <button type="submit" className={styles.botaoAdicionarGasto} disabled={salvando || funcionariosDisponiveis.length === 0}>
                {salvando ? "Salvando..." : "Alocar"}
              </button>
            </footer>
          </form>
        </div>
      )}

      {/* Cabeçalho */}
      <div className={styles.cabecalho}>
        <div className={styles.cabecalhoEsquerda}>
          <button
            className={styles.botaoVoltar}
            onClick={() => navegar(`/obras/detalhamento/${id}`)}
          >
            <ArrowLeft size={18} />
          </button>
          <span className={styles.navegacao}>
            <span
              className={styles.navegacaoLink}
              onClick={() => navegar("/home")}
            >
              Obras
            </span>
            <span className={styles.separador}>›</span>
            <span
              className={styles.navegacaoLink}
              onClick={() => navegar(`/obras/detalhamento/${id}`)}
            >
              {obra.titulo}
            </span>
            <span className={styles.separador}>›</span>
            <span className={styles.navegacaoAtivo}>Alocações de funcionários</span>
          </span>
        </div>
        <div className={styles.cabecalhoAcoes}>
          <button className={styles.botaoAdicionarGasto} onClick={abrirModalAlocacao}>
            <UserCheck size={16} />
            Alocar funcionário
          </button>
        </div>
      </div>

      {/* Card da obra */}
      <div className={styles.cardObra}>
        <div className={styles.cardObraImagem}>
          <img src={IMAGEM_PLACEHOLDER} alt={obra.titulo} />
        </div>
        <div className={styles.cardObraInformacoes}>
          <div className={styles.badgeStatus}>
            <span className={styles.badgePonto} />
            {obra.status.replace(/_/g, " ")}
          </div>
          <div className={styles.cardObraTitulo}>
            <h1>{obra.titulo}</h1>
          </div>
          <div className={styles.cardObraData}>
            <Calendar size={14} />
            <span>
              {formatarData(obra.dtInicio)} -{" "}
              {formatarData(obra.dtTerminoPrevisto)}
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
                    <span className={styles.membroNome}>
                      {alocacao.funcionario.nome}
                    </span>
                    <span className={styles.membroCargo}>
                      {cargoLabel(alocacao.cargo)}
                    </span>
                  </div>
                </div>
              ))
            ) : (
              <p style={{ fontSize: "14px", color: "#666" }}>
                Nenhum funcionário alocado
              </p>
            )}
          </div>
        </div>
      </div>

      {/* Seção status da equipe */}
      <div className={styles.secaoEquipe}>
        <div className={styles.equipeTopo}>
          <h2 className={styles.equipeTitulo}>Status da equipe ativa</h2>
          <div className={styles.equipeControles}>
            <div className={styles.campoBusca}>
              <Search size={15} className={styles.iconeBusca} />
              <input value={busca} onChange={(evento) => setBusca(evento.target.value)} className={styles.inputBusca} placeholder="Buscar funcionário..." />
            </div>
            <select className={styles.inputData} value={cargoFiltro} onChange={(evento) => setCargoFiltro(evento.target.value)}>
              <option value="TODOS">Todos os cargos</option>
              {cargosObra.map((cargo) => (
                <option key={cargo.value} value={cargo.value}>{cargo.label}</option>
              ))}
            </select>
          </div>
        </div>

        {/* Tabela */}
        <div className={styles.tabelaWrapper}>
          <table className={styles.tabela}>
            <thead>
              <tr>
                <th>NOME</th>
                <th>FUNÇÃO GLOBAL</th>
                <th>CARGO NA OBRA</th>
              </tr>
            </thead>
            <tbody>
              {alocacoesExibidas.length > 0 ? (
                alocacoesExibidas.map((alocacao, indice) => (
                  <tr key={alocacao.id}>
                    <td>
                      <div className={styles.celulaFuncionario}>
                        <div
                          className={styles.avatarPequeno}
                          style={{
                            backgroundColor: cores[indice % cores.length],
                          }}
                        >
                          {getIniciais(alocacao.funcionario.nome)}
                        </div>
                        {alocacao.funcionario.nome}
                      </div>
                    </td>
                    <td>{cargoLabel(alocacao.funcionario.cargoGlobal)}</td>
                    <td>{cargoLabel(alocacao.cargo)}</td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan={3}
                    style={{ textAlign: "center", padding: "2rem" }}
                  >
                    {busca || cargoFiltro !== "TODOS" ? "Nenhum funcionário encontrado para os filtros." : "Nenhum funcionário alocado nesta obra."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {alocacoesExibidas.length} de {alocacoesFiltradas.length}{" "}
              funcionários
            </span>
            <div className={styles.paginacaoBotoes}>
              <button
                className={styles.botaoPaginacao}
                onClick={irParaPaginaAnterior}
                disabled={paginaAtual === 1}
              >
                <ChevronLeft size={16} />
              </button>
              {Array.from({ length: totalPaginas }, (_, i) => i + 1).map(
                (numeroPagina) => (
                  <button
                    key={numeroPagina}
                    className={`${styles.botaoPaginacao} ${
                      paginaAtual === numeroPagina
                        ? styles.botaoPaginacaoAtivo
                        : ""
                    }`}
                    onClick={() => irParaPagina(numeroPagina)}
                  >
                    {numeroPagina}
                  </button>
                )
              )}
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
