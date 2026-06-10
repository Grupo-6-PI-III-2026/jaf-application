import { useState, useEffect } from "react";
import {
  ArrowLeft,
  Calendar,
  Filter,
  Pencil,
  Plus,
  ChevronLeft,
  ChevronRight,
  UserCheck,
} from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import styles from "./AlocacaoFuncionario.module.css";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { alocacaoService, type AlocacaoObra } from "../../Service/Alocacoes/alocacaoService";
import { CargoLabel, type Cargo } from "../../Types/user";

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

const cargoLabel = (cargo: string) =>
  CargoLabel[cargo as Cargo] ?? cargo.replace(/_/g, " ");

const cores = ["#6C63FF", "#FF6584", "#43B89C", "#ffc107", "#9c27b0"];

export default function AlocacaoFuncionario() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [dataSelecionada, setDataSelecionada] = useState<string>(
    new Date().toISOString().split("T")[0]
  );
  const [obra, setObra] = useState<Obra | null>(null);
  const [alocacoes, setAlocacoes] = useState<AlocacaoObra[]>([]);
  const [carregando, setCarregando] = useState(true);

  const { id } = useParams<{ id: string }>();
  const navegar = useNavigate();

  useEffect(() => {
    const carregarDados = async () => {
      if (!id) return;
      try {
        setCarregando(true);
        const [obraData, alocacoesData] = await Promise.all([
          obraService.buscarPorId(parseInt(id)),
          alocacaoService.listarPorObra(parseInt(id)),
        ]);
        setObra(obraData);
        setAlocacoes(alocacoesData);
        setPaginaAtual(1);
      } catch (error) {
        console.error("Erro ao carregar dados:", error);
      } finally {
        setCarregando(false);
      }
    };
    carregarDados();
  }, [id]);

  const totalPaginas = Math.ceil(alocacoes.length / ITENS_POR_PAGINA);
  const alocacoesExibidas = alocacoes.slice(
    (paginaAtual - 1) * ITENS_POR_PAGINA,
    paginaAtual * ITENS_POR_PAGINA
  );

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
          <button className={styles.botaoAdicionarGasto}>
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
            <button className={styles.botaoEditar}>
              <Pencil size={16} />
            </button>
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
            <button className={styles.botaoAdicionarMembro}>
              <Plus size={18} />
            </button>
          </div>
        </div>
      </div>

      {/* Seção status da equipe */}
      <div className={styles.secaoEquipe}>
        <div className={styles.equipeTopo}>
          <h2 className={styles.equipeTitulo}>Status da equipe ativa</h2>
          <div className={styles.equipeControles}>
            <input
              type="date"
              value={dataSelecionada}
              onChange={(e) => setDataSelecionada(e.target.value)}
              className={styles.inputData}
            />
            <button className={styles.botaoFiltro}>
              <Filter size={14} />
              Filtrar por obra
            </button>
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
                <th>AÇÕES</th>
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
                    <td>
                      <button className={styles.botaoAcao}>Editar perfil</button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td
                    colSpan={4}
                    style={{ textAlign: "center", padding: "2rem" }}
                  >
                    Nenhum funcionário alocado nesta obra.
                  </td>
                </tr>
              )}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {alocacoesExibidas.length} de {alocacoes.length}{" "}
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
