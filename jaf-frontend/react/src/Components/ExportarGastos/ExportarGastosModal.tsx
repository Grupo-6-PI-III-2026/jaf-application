import { useState } from "react";
import { X } from "lucide-react";
import styles from "./ExportarGastosModal.module.css";
import { exportarGastos, type FormatoExportacao } from "../../Service/Gastos/exportacaoService";
import { tratarErroApiBlob } from "../../Utils/erro";

interface ExportarGastosModalProps {
  obraId: number;
  periodoInicial: { from: string; to: string };
  funcionarioId?: number;
  metodoPagamento?: string;
  statusReembolso?: string;
  onClose: () => void;
}

export default function ExportarGastosModal({
  obraId,
  periodoInicial,
  funcionarioId,
  metodoPagamento,
  statusReembolso,
  onClose,
}: ExportarGastosModalProps) {
  const [formato, setFormato] = useState<FormatoExportacao>("csv");
  const [from, setFrom] = useState(periodoInicial.from);
  const [to, setTo] = useState(periodoInicial.to);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");

  async function confirmar() {
    setCarregando(true);
    setErro("");
    try {
      await exportarGastos({
        obraId,
        from: from || undefined,
        to: to || undefined,
        funcionarioId,
        metodoPagamento,
        statusReembolso,
        formato,
      });
      onClose();
    } catch (error) {
      setErro((await tratarErroApiBlob(error, "Não foi possível exportar os gastos.")).mensagem);
    } finally {
      setCarregando(false);
    }
  }

  return (
    <div className={styles.fundo} role="presentation" onMouseDown={(evento) => {
      if (evento.target === evento.currentTarget && !carregando) onClose();
    }}>
      <section className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="titulo-exportacao">
        <header className={styles.cabecalho}>
          <div>
            <h2 id="titulo-exportacao">Exportar gastos</h2>
            <p>Baixe a lista de gastos com os filtros selecionados.</p>
          </div>
          <button type="button" className={styles.fechar} onClick={onClose} disabled={carregando} aria-label="Fechar">
            <X size={20} />
          </button>
        </header>
        <div className={styles.formulario}>
          <label>
            Formato
            <select value={formato} onChange={(evento) => setFormato(evento.target.value as FormatoExportacao)} disabled={carregando}>
              <option value="csv">CSV</option>
              <option value="xlsx">Excel (XLSX)</option>
            </select>
          </label>
          <div className={styles.periodo}>
            <label>De<input type="date" value={from} onChange={(evento) => setFrom(evento.target.value)} disabled={carregando} /></label>
            <label>Até<input type="date" value={to} onChange={(evento) => setTo(evento.target.value)} disabled={carregando} /></label>
          </div>
          {erro && <p className={styles.erro} role="alert">{erro}</p>}
        </div>
        <footer className={styles.acoes}>
          <button type="button" className={styles.secundario} onClick={onClose} disabled={carregando}>Cancelar</button>
          <button type="button" className={styles.primario} onClick={confirmar} disabled={carregando || (!!from && !!to && from > to)}>
            {carregando ? "Gerando..." : "Baixar arquivo"}
          </button>
        </footer>
      </section>
    </div>
  );
}
