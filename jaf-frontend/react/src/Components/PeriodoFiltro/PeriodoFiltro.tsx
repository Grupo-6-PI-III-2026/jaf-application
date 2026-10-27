import styles from "./PeriodoFiltro.module.css";
import {
  PERIODO_OPCOES,
  type PeriodoFiltroValue,
  type PeriodoPreset,
} from "./periodUtils";

interface PeriodoFiltroProps {
  value: PeriodoFiltroValue;
  onChange: (value: PeriodoFiltroValue) => void;
  /** Esconde a opção "Todos" (útil em telas que sempre precisam de intervalo). */
  ocultarTodos?: boolean;
  /** Visual escuro para barras de filtro escuras (ex.: presença / permissões). */
  temaEscuro?: boolean;
  className?: string;
}

export default function PeriodoFiltro({
  value,
  onChange,
  ocultarTodos = false,
  temaEscuro = false,
  className,
}: PeriodoFiltroProps) {
  const opcoes = ocultarTodos
    ? PERIODO_OPCOES.filter((opcao) => opcao.value !== "TODOS")
    : PERIODO_OPCOES;

  const selecionar = (preset: PeriodoPreset) => {
    onChange({ ...value, preset });
  };

  return (
    <div
      className={[
        styles.periodoFiltro,
        temaEscuro ? styles.temaEscuro : "",
        className ?? "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {opcoes.map((opcao) => (
        <button
          key={opcao.value}
          type="button"
          className={`${styles.chip} ${value.preset === opcao.value ? styles.chipAtivo : ""}`}
          onClick={() => selecionar(opcao.value)}
        >
          {opcao.label}
        </button>
      ))}

      {value.preset === "PERSONALIZADO" && (
        <div className={styles.ranges}>
          <label>
            De
            <input
              type="date"
              value={value.from}
              onChange={(evento) => onChange({ ...value, from: evento.target.value })}
            />
          </label>
          <label>
            Até
            <input
              type="date"
              value={value.to}
              min={value.from || undefined}
              onChange={(evento) => onChange({ ...value, to: evento.target.value })}
            />
          </label>
        </div>
      )}
    </div>
  );
}

export type { PeriodoFiltroValue, PeriodoPreset };
