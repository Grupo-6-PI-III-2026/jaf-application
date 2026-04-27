import { useState } from "react";
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
import { useNavigate } from "react-router-dom";
import styles from "./DetalhamentoObras.module.css";
import ControlePresenca from "../ControlePresenca/ControlePresenca";

const equipe = [
  { nome: "Rafael Pereira", cargo: "Mestre de Obras", iniciais: "RP", cor: "#6C63FF" },
  { nome: "Gabriel Junior", cargo: "Engenheiro Civil", iniciais: "GJ", cor: "#FF6584" },
  { nome: "Ana Souza", cargo: "Arquiteta", iniciais: "AS", cor: "#43B89C" },
];

const lancamentos = [
  { valor: 1000, cor: "#43B89C", funcionario: "Isac Newton", iniciais: "IN", corAvatar: "#6C63FF", tipo: "Débito", descricao: "Pagamento mão de obra", etapa: "Pintura", material: "-", data: "12 Jan 2026" },
  { valor: 450, cor: "#ffc107", funcionario: "Rafael Pereira", iniciais: "RP", corAvatar: "#FF6584", tipo: "Débito", descricao: "Silicone e acabamentos", etapa: "Pintura", material: "Silicone", data: "10 Jan 2026" },
  { valor: 2300, cor: "#43B89C", funcionario: "Gabriel Junior", iniciais: "GJ", corAvatar: "#43B89C", tipo: "Débito", descricao: "Compra de insumos", etapa: "Estrutura", material: "Cimento", data: "08 Jan 2026" },
  { valor: 2300, cor: "#43B89C", funcionario: "Gabriel Junior", iniciais: "GJ", corAvatar: "#43B89C", tipo: "Débito", descricao: "Compra de insumos", etapa: "Estrutura", material: "Cimento", data: "08 Jan 2026" },
  { valor: 2300, cor: "#43B89C", funcionario: "Gabriel Junior", iniciais: "GJ", corAvatar: "#43B89C", tipo: "Débito", descricao: "Compra de insumos", etapa: "Estrutura", material: "Cimento", data: "08 Jan 2026" },
];

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", { style: "currency", currency: "BRL" });

export default function DetalhamentoObras() {
  const [paginaAtual, setPaginaAtual] = useState(1);
  const [controlePresencaAberto, setControlePresencaAberto] = useState(false);
  const totalPaginas = 3;
  const navegar = useNavigate();

  function irParaPaginaAnterior() {
    setPaginaAtual((paginaCorrente) => Math.max(1, paginaCorrente - 1));
  }

  function irParaProximaPagina() {
    setPaginaAtual((paginaCorrente) => Math.min(totalPaginas, paginaCorrente + 1));
  }

  function irParaPagina(pagina: number) {
    setPaginaAtual(pagina);
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
          <button className={styles.botaoVoltar} onClick={() => navegar("/obras")}>
            <ArrowLeft size={18} />
          </button>
          <span className={styles.navegacao}>
            <span className={styles.navegacaoLink} onClick={() => navegar("/obras")}>
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
            alt="Obra Alphaville"
          />
        </div>
        <div className={styles.cardObraInformacoes}>
          <div className={styles.badgeStatus}>
            <span className={styles.badgePonto} />
            EM PROGRESSO
          </div>
          <div className={styles.cardObraTitulo}>
            <h1>Obra Alphaville</h1>
            <button className={styles.botaoEditar}>
              <Pencil size={16} />
            </button>
          </div>
          <div className={styles.cardObraData}>
            <Calendar size={14} />
            <span>01/01/26 - 01/06/26</span>
          </div>

          <div className={styles.equipeRotulo}>EQUIPE RESPONSÁVEL</div>
          <div className={styles.equipe}>
            {equipe.map((membro) => (
              <div key={membro.nome} className={styles.membroCard}>
                <div
                  className={styles.membroAvatar}
                  style={{ backgroundColor: membro.cor }}
                >
                  {membro.iniciais}
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

      {/* Cards de métricas */}
      <div className={styles.metricas}>
        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>CUSTO TOTAL ACUMULADO</span>
          <span className={styles.metricaValorDestaque}>R$ 158.400,00</span>
          <div className={styles.metricaTendencia}>
            <TrendingUp size={14} />
            <span>12% em relação ao mês anterior</span>
          </div>
        </div>

        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>LIMITE ATINGIDO</span>
          <span className={styles.metricaValor}>64%</span>
          <div className={styles.barraProgressoWrapper}>
            <div className={styles.barraProgresso}>
              <div className={styles.barraProgressoPreenchimento} style={{ width: "64%" }} />
            </div>
          </div>
        </div>

        <div className={styles.metricaCard}>
          <span className={styles.metricaRotulo}>DIAS PARA ENTREGA</span>
          <span className={styles.metricaValor}>128 Dias</span>
          <div className={styles.metricaSubtitulo}>
            <CalendarDays size={13} />
            <span>Entrega prevista: 01 Jun 2026</span>
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
              {lancamentos.map((lancamento, indice) => (
                <tr key={indice}>
                  <td>
                    <span className={styles.celulaValor} style={{ color: lancamento.cor }}>
                      {formatarMoeda(lancamento.valor)}
                    </span>
                  </td>
                  <td>
                    <div className={styles.celulaFuncionario}>
                      <div
                        className={styles.avatarPequeno}
                        style={{ backgroundColor: lancamento.corAvatar }}
                      >
                        {lancamento.iniciais}
                      </div>
                      {lancamento.funcionario}
                    </div>
                  </td>
                  <td>{lancamento.tipo}</td>
                  <td>
                    <span className={styles.descricaoDestaque}>{lancamento.descricao}</span>
                  </td>
                  <td>{lancamento.etapa}</td>
                  <td>{lancamento.material}</td>
                  <td>{lancamento.data}</td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Paginação */}
          <div className={styles.paginacao}>
            <span className={styles.paginacaoInfo}>Mostrando 5 de 24 lançamentos</span>
            <div className={styles.paginacaoBotoes}>
              <button
                className={styles.botaoPaginacao}
                onClick={irParaPaginaAnterior}
                disabled={paginaAtual === 1}
              >
                <ChevronLeft size={16} />
              </button>
              {[1, 2, 3].map((numeroPagina) => (
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
