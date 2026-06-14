import { Pencil, MapPin, Plus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import styles from "./CardObra.module.css";
import { authService } from "../../Service/Auth/Login/authService";

type Props = {
  id?: number;
  nome: string;
  status: string;
  valor: number;
  imagem: string;
  responsavelNome?: string;
  responsavelCargo?: string;
};

const formatarMoeda = (valor: number) =>
  valor.toLocaleString("pt-BR", {
    style: "currency",
    currency: "BRL",
  });

const formatarStatus = (status: string) => status.replace(/_/g, " ");

const getIniciais = (nome: string) => {
  const partes = nome.trim().split(/\s+/);
  return partes.length > 1
    ? `${partes[0][0]}${partes[1][0]}`.toUpperCase()
    : nome.substring(0, 2).toUpperCase();
};

export default function CardObra({ id, nome, status, valor, imagem, responsavelNome, responsavelCargo }: Props) {
  const navigate = useNavigate();
  const podeEditarObra = authService.hasAuthority("EDITAR_OBRA");
  const podeVisualizarAlocacoes = authService.hasAuthority("VISUALIZAR_ALOCACOES");

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
          <span className={styles.status}>{formatarStatus(status)}</span>

          {podeEditarObra && (
            <button
              className={styles.botaoEditar}
              onClick={() => navigate(id ? `/obras/detalhamento/${id}?editar=1` : "/obras/detalhamento?editar=1")}
              aria-label="Editar obra"
            >
              <Pencil size={16} />
            </button>
          )}
        </div>

        {/* TITULO */}
        <h2 className={styles.titulo}>{nome}</h2>

        <div className={styles.local}>
          <MapPin size={14} />
          {formatarStatus(status)}
        </div>

        {/* RESPONSÁVEL */}
        <div className={styles.responsavelBloco}>
          <span className={styles.label}>FUNCIONÁRIO RESPONSÁVEL</span>

          <div className={styles.responsavel}>
            <div className={styles.avatar}>{responsavelNome ? getIniciais(responsavelNome) : "--"}</div>
            <div>
              <span className={styles.nome}>{responsavelNome ?? "Sem responsável"}</span>
              <span className={styles.cargo}>{responsavelCargo ?? "Alocação pendente"}</span>
            </div>

            {podeVisualizarAlocacoes && (
              <button className={styles.botaoAdd} onClick={() => id && navigate(`/obras/${id}/alocacoes`)} aria-label="Alocar funcionário">
                <Plus size={14} />
              </button>
            )}
          </div>
        </div>

        {/* LINHA */}
        <div className={styles.divisor} />

        {/* RODAPÉ */}
        <div className={styles.rodape}>
          <div>
            <span className={styles.label}>TOTAL GASTO</span>
            <div className={styles.valor}>
              {formatarMoeda(valor)}
            </div>
          </div>

          <button className={styles.botaoDetalhes} onClick={() => navigate(id ? `/obras/detalhamento/${id}` : '/obras/detalhamento')}>
            VISUALIZAR DETALHES
          </button>
        </div>

      </div>
    </div>
  );
}