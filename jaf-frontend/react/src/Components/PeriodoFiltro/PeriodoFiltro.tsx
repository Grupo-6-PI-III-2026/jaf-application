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
  /** Agrupa os presets em um controle segmentado (altura padrão de 40px). */
  segmentado?: boolean;
  className?: string;
}

export default function PeriodoFiltro({
  value,
  onChange,
  ocultarTodos = false,
  temaEscuro = false,
  segmentado = false,
  className,
}: PeriodoFiltroProps) {
  const opcoes = ocultarTodos
    ? PERIODO_OPCOES.filter((opcao) => opcao.value !== "TODOS")
    : PERIODO_OPCOES;

  const selecionar = (preset: PeriodoPreset) => {
    onChange({ ...value, preset });
  };

  const botoes = opcoes.map((opcao) => {
    const ativo = value.preset === opcao.value;
    const classe = segmentado
      ? `${styles.segmento} ${ativo ? styles.segmentoAtivo : ""}`
      : `${styles.chip} ${ativo ? styles.chipAtivo : ""}`;
    return (
      <button
        key={opcao.value}
        type="button"
        className={classe}
        aria-pressed={ativo}
        onClick={() => selecionar(opcao.value)}
      >
        {opcao.label}
      </button>
    );
  });

  return (
    <div
      className={[
        styles.periodoFiltro,
        temaEscuro ? styles.temaEscuro : "",
        segmentado ? styles.periodoSegmentado : "",
        className ?? "",
      ]
        .filter(Boolean)
        .join(" ")}
    >
      {segmentado ? (
        <div className={styles.segmentos} role="group" aria-label="Período">
          {botoes}
        </div>
      ) : (
        botoes
      )}

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
