import { useEffect, useState } from "react";
import { financeiroService, type ResumoPessoa } from "../../Service/Financeiro/financeiroService";
import { erroApi, moeda } from "../../Utils/financeiro";
import styles from "./Financeiro.module.css";

export default function ResumoReembolsos({ obraId, revisao = 0 }: { obraId: number; revisao?: number }) {
  return <ResumoDaObra key={`${obraId}:${revisao}`} obraId={obraId} />;
}

function ResumoDaObra({ obraId }: { obraId: number }) {
  const [dados, setDados] = useState<ResumoPessoa[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  useEffect(() => {
    let ativo = true;
    financeiroService.resumo(obraId).then((valor) => {
      if (ativo) { setDados(valor); setErro(""); }
    }).catch((e: unknown) => { if (ativo) setErro(erroApi(e, "Não foi possível carregar o resumo. Atualize para tentar novamente.")); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, [obraId]);
  return <section className={styles.painel}>
    <h2>Reembolsos por pessoa</h2><p>Todos os gastos de reembolso da obra, independentemente dos filtros da lista.</p>
    {carregando ? <p role="status">Carregando resumo...</p> : erro ? <p role="alert">{erro}</p> : dados.length === 0 ? <p>Nenhum reembolso nesta obra.</p> :
      <div className={styles.scroll}><table className={styles.tabela}>
        <thead><tr><th>Pessoa</th><th>Total</th><th>Pago</th><th>Pendente</th></tr></thead>
        <tbody>{dados.map((pessoa) => <tr key={pessoa.funcionarioId}><td>{pessoa.funcionarioNome}</td><td>{moeda(pessoa.valorTotal)}</td><td>{moeda(pessoa.totalPago)}</td><td>{moeda(pessoa.saldoPendente)}</td></tr>)}</tbody>
      </table></div>}
  </section>;
}