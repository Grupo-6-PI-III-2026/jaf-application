import { Calendar, X } from "lucide-react";
import { useMemo, useState } from "react";
import styles from "./ControlePresenca.module.css";

type Colaborador = {
  id: number;
  nome: string;
  cargo: string;
  foto: string;
  presente: boolean;
  desabilitado?: boolean;
};

type ControlePresencaProps = {
  aberto: boolean;
  onFechar: () => void;
};

const colaboradoresIniciais: Colaborador[] = [
  {
    id: 1,
    nome: "Joao Silva",
    cargo: "Pintor Especialista",
    foto: "https://images.unsplash.com/photo-1607746882042-944635dfe10e?w=96&q=80",
    presente: true,
  },
  {
    id: 2,
    nome: "Marcos Pereira",
    cargo: "Gesseiro",
    foto: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=96&q=80",
    presente: true,
  },
  {
    id: 3,
    nome: "Carlos Antunes",
    cargo: "Mestre de Obras",
    foto: "https://images.unsplash.com/photo-1521119989659-a83eee488004?w=96&q=80",
    presente: true,
  },
  {
    id: 4,
    nome: "Ricardo Mendes",
    cargo: "Eletricista",
    foto: "https://images.unsplash.com/photo-1633332755192-727a05c4013d?w=96&q=80",
    presente: false,
    desabilitado: true,
  },
];

export default function ControlePresenca({ aberto, onFechar }: ControlePresencaProps) {
  const [colaboradores, setColaboradores] = useState(colaboradoresIniciais);

  const totalPresentes = useMemo(
    () => colaboradores.filter((colaborador) => colaborador.presente).length,
    [colaboradores],
  );

  function alternarPresenca(id: number) {
    setColaboradores((listaAtual) =>
      listaAtual.map((colaborador) => {
        if (colaborador.id !== id || colaborador.desabilitado) {
          return colaborador;
        }

        return { ...colaborador, presente: !colaborador.presente };
      }),
    );
  }

  if (!aberto) {
    return null;
  }

  return (
    <div className={styles.overlay} onClick={onFechar}>
      <section className={styles.modal} onClick={(evento) => evento.stopPropagation()}>
        <header className={styles.header}>
          <button
            type="button"
            onClick={onFechar}
            className={styles.botaoFechar}
            aria-label="Fechar controle de presenca"
          >
            <X size={20} />
          </button>

          <h2 className={styles.titulo}>CONTROLE DE PRESENCA</h2>
          <p className={styles.subtitulo}>Edificio Horizonte</p>

          <button type="button" className={styles.botaoData}>
            <Calendar size={14} className={styles.iconeData} />
            Data
          </button>
        </header>

        <div className={styles.corpo}>
          <div className={styles.cabecalhoLista}>
            <span>COLABORADOR</span>
            <span>STATUS</span>
          </div>

          <ul className={styles.lista}>
            {colaboradores.map((colaborador) => (
              <li key={colaborador.id} className={styles.itemColaborador}>
                <div className={styles.infoColaborador}>
                  <img
                    src={colaborador.foto}
                    alt={colaborador.nome}
                    className={`${styles.avatar} ${colaborador.desabilitado ? styles.avatarCinza : ""}`}
                  />
                  <div>
                    <p className={`${styles.nome} ${colaborador.desabilitado ? styles.textoDesabilitado : ""}`}>
                      {colaborador.nome}
                    </p>
                    <p className={`${styles.cargo} ${colaborador.desabilitado ? styles.cargoDesabilitado : ""}`}>
                      {colaborador.cargo}
                    </p>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={() => alternarPresenca(colaborador.id)}
                  disabled={colaborador.desabilitado}
                  className={`${styles.toggle} ${
                    colaborador.desabilitado
                      ? styles.toggleDesabilitado
                      : colaborador.presente
                        ? styles.toggleAtivo
                        : styles.toggleInativo
                  }`}
                >
                  <span className={`${styles.bolinhaToggle} ${colaborador.presente ? styles.bolinhaAtiva : ""}`} />
                </button>
              </li>
            ))}
          </ul>
        </div>

        <footer className={styles.rodape}>
          <p className={styles.contador}>
            {totalPresentes} DE {colaboradores.length} PRESENTES
          </p>
          <button type="button" className={styles.botaoRegistrar}>
            Registrar Presenca
          </button>
        </footer>
      </section>
    </div>
  );
}
