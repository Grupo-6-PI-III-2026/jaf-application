import { useEffect, useState } from "react";
import { FileText, Trash2 } from "lucide-react";
import { notaFiscalService, type NotaFiscal } from "../../Service/NotasFiscais/notaFiscalService";
import { tratarErroApi } from "../../Utils/erro";
import styles from "./NotasFiscaisGasto.module.css";

const API_BASE_URL = import.meta.env.VITE_API_URL || "/api";

interface NotasFiscaisGastoProps {
  gastoId: number;
  podeExcluir: boolean;
}

export default function NotasFiscaisGasto({ gastoId, podeExcluir }: NotasFiscaisGastoProps) {
  const [notas, setNotas] = useState<NotaFiscal[]>([]);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let ativo = true;
    notaFiscalService.listar(gastoId)
      .then((dados) => { if (ativo) setNotas(dados); })
      .catch((error: unknown) => { if (ativo) setErro(tratarErroApi(error, "Não foi possível carregar as notas fiscais.").mensagem); });
    return () => { ativo = false; };
  }, [gastoId]);

  async function excluir(nota: NotaFiscal) {
    if (!window.confirm(`Excluir a nota fiscal "${nota.nomeArquivo}"?`)) return;
    try {
      await notaFiscalService.deletar(nota.id);
      setNotas((atuais) => atuais.filter((atual) => atual.id !== nota.id));
    } catch (error) {
      setErro(tratarErroApi(error, "Não foi possível excluir a nota fiscal.").mensagem);
    }
  }

  if (!notas.length && !erro) return null;
  return (
    <div className={styles.container} aria-label={`Notas fiscais do gasto ${gastoId}`}>
      <span className={styles.titulo}><FileText size={14} /> Notas fiscais {notas.length > 0 && `(${notas.length})`}</span>
      {erro && <small className={styles.erro}>{erro}</small>}
      {notas.map((nota) => (
        <div className={styles.nota} key={nota.id}>
          <a href={`${API_BASE_URL}/notas-fiscais/${nota.id}/arquivo`} download>{nota.nomeArquivo}</a>
          {nota.numero && <small>Nº {nota.numero}</small>}
          {podeExcluir && <button type="button" onClick={() => excluir(nota)} aria-label={`Excluir ${nota.nomeArquivo}`}><Trash2 size={13} /></button>}
        </div>
      ))}
    </div>
  );
}
