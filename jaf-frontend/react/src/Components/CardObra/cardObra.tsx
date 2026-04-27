import { Pencil, MapPin, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import styles from "./CardObra.module.css";

type Props = {
  nome: string;
  local: string;
  valor: number;
  imagem: string;
};

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

export default function CardObra({ nome, local, valor, imagem }: Props) {
  const navigate = useNavigate();

  return (
    <div className={styles.card}>

      {/* IMAGEM */}
      <div className={styles.imagemWrapper}>
        <img src={imagem} alt={nome} />
      </div>

      {/* CONTEÚDO */}
      <div className={styles.conteudo}>

        {/* TOPO */}
        <div className={styles.topo}>
          <span className={styles.status}>EM PROGRESSO</span>

          <button className={styles.botaoEditar}>
            <Pencil size={16} />
          </button>
        </div>

        {/* TITULO */}
        <h2 className={styles.titulo}>{nome}</h2>

        <div className={styles.local}>
          <MapPin size={14} />
          {local}
        </div>

        {/* RESPONSÁVEL */}
        <div className={styles.responsavelBloco}>
          <span className={styles.label}>FUNCIONÁRIO RESPONSÁVEL</span>

          <div className={styles.responsavel}>
            <div className={styles.avatar}>RP</div>
            <div>
              <span className={styles.nome}>Rafael Pereira</span>
              <span className={styles.cargo}>Mestre de Obras</span>
            </div>

            <button className={styles.botaoAdd}>
              <Plus size={14} />
            </button>
          </div>
        </div>

        {/* LINHA */}
        <div className={styles.divisor} />

        {/* RODAPÉ */}
        <div className={styles.rodape}>
          <div>
            <span className={styles.label}>GASTO DA OBRA</span>
            <div className={styles.valor}>
              {formatarMoeda(valor)}
            </div>
          </div>

          <button className={styles.botaoDetalhes} onClick={() => navigate('/obras/detalhamento')}>
            VISUALIZAR DETALHES
          </button>
        </div>

      </div>
    </div>
  );
}