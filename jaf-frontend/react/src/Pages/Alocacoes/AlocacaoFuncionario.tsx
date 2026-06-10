import { useState } from "react";
import {
  ArrowLeft,
  Calendar,
  Filter,
  CalendarDays,
  Pencil,
  Plus,
  ChevronLeft,
  ChevronRight,
  UserCheck,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import styles from "./AlocacaoFuncionario.module.css";

const formatarData = (data: string) =>
  new Date(data).toLocaleDateString("pt-BR");

const getIniciais = (nome: string) => {
  const palavras = nome.split(" ");
  if (palavras.length >= 2) {
    return (palavras[0][0] + palavras[1][0]).toUpperCase();
  }
  return nome.substring(0, 2).toUpperCase();
};

const cores = ["#6C63FF", "#FF6584", "#43B89C", "#ffc107", "#9c27b0"];

const obraEstatica = {
  titulo: "Obra Alphaville",
  status: "EM PROGRESSO",
  dtInicio: "2026-01-01",
  dtTerminoPrevisto: "2026-06-01",
  imagem:
    "https://s2.glbimg.com/YgImWrcpJX6FAHRJOlZtEnTaHYc=/e.glbimg.com/og/ed/f/original/2020/12/23/apartamento-sao-paulo-260-m2-atemporal3.jpg",
};

const equipeEstatica = [
  { id: 1, nome: "Rafael Pereira", cargo: "Mestre de Obras" },
  { id: 2, nome: "Gabriel Junior", cargo: "Engenheiro Civil" },
  { id: 3, nome: "Ana Souza", cargo: "Arquiteta" },
];

const funcionariosEstaticos = Array.from({ length: 24 }, (_, i) => ({
  id: i + 1,
  nome: "Elias Santos Souza",
  funcao: "Pintor",
  quantidadeAlocacoes: 12,
}));

const ITENS_POR_PAGINA = 8;

export default function AlocacaoFuncionario() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [dataSelecionada, setDataSelecionada] = useState<string>(
    new Date().toISOString().split("T")[0]
  );

  const navegar = useNavigate();

  const totalPaginas = Math.ceil(funcionariosEstaticos.length / ITENS_POR_PAGINA);
  const funcionariosExibidos = funcionariosEstaticos.slice(
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

  return (
    <div className={styles.pagina}>
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
            <span className={styles.navegacaoAtivo}>Alocações de funcionários</span>
          </span>
        </div>
        <div className={styles.cabecalhoAcoes}>
          <button className={styles.botaoAdicionarGasto}>
            <UserCheck size={16} />
            Adicionar gasto
          </button>
        </div>
      </div>

      {/* Card da obra */}
      <div className={styles.cardObra}>
        <div className={styles.cardObraImagem}>
          <img src={obraEstatica.imagem} alt={obraEstatica.titulo} />
        </div>
        <div className={styles.cardObraInformacoes}>
          <div className={styles.badgeStatus}>
            <span className={styles.badgePonto} />
            {obraEstatica.status}
          </div>
          <div className={styles.cardObraTitulo}>
            <h1>{obraEstatica.titulo}</h1>
            <button className={styles.botaoEditar}>
              <Pencil size={16} />
            </button>
          </div>
          <div className={styles.cardObraData}>
            <Calendar size={14} />
            <span>
              {formatarData(obraEstatica.dtInicio)} -{" "}
              {formatarData(obraEstatica.dtTerminoPrevisto)}
            </span>
          </div>

          <div className={styles.equipeRotulo}>EQUIPE RESPONSÁVEL</div>
          <div className={styles.equipe}>
            {equipeEstatica.map((membro, index) => (
              <div key={membro.id} className={styles.membroCard}>
                <div
                  className={styles.membroAvatar}
                  style={{ backgroundColor: cores[index % cores.length] }}
                >
                  {getIniciais(membro.nome)}
                </div>
                <div className={styles.membroInformacoes}>
                  <span className={styles.membroNome}>{membro.nome}</span>
                  <span className={styles.membroCargo}>{membro.cargo}</span>
                </div>
              </div>
            ))}
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
                <th>FUNÇÃO</th>
                <th>QUANTIDADE DE ALOCAÇÕES</th>
                <th>AÇÕES</th>
              </tr>
            </thead>
            <tbody>
              {funcionariosExibidos.map((funcionario, indice) => (
                <tr key={funcionario.id}>
                  <td>
                    <div className={styles.celulaFuncionario}>
                      <div
                        className={styles.avatarPequeno}
                        style={{ backgroundColor: cores[indice % cores.length] }}
                      >
                        {getIniciais(funcionario.nome)}
                      </div>
                      {funcionario.nome}
                    </div>
                  </td>
                  <td>{funcionario.funcao}</td>
                  <td>{funcionario.quantidadeAlocacoes}</td>
                  <td>
                    <button className={styles.botaoAcao}>Editar perfil</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>
              Mostrando {funcionariosExibidos.length} de{" "}
              {funcionariosEstaticos.length} lançamentos
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
                      paginaAtual === numeroPagina ? styles.botaoPaginacaoAtivo : ""
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
                disabled={paginaAtual === totalPaginas}
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
