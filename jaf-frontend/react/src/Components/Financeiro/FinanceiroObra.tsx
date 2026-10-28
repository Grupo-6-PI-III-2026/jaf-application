import { useEffect, useRef, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { financeiroService, type FinanceiroObra as Financeiro } from "../../Service/Financeiro/financeiroService";
import { authService } from "../../Service/Auth/Login/authService";
import { erroApi, moeda, valorDecimal } from "../../Utils/financeiro";
import styles from "./Financeiro.module.css";

export default function FinanceiroObra({ obraId, orcamento, revisao = 0 }: { obraId: number; orcamento?: string; revisao?: number }) {
  const [dados, setDados] = useState<Financeiro | null>(null);
  const [recebido, setRecebido] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const geracao = useRef(0);
  const envioEmCurso = useRef(false);
  const admin = authService.isAdmin();

  useEffect(() => {
    geracao.current += 1;
    envioEmCurso.current = false;
    setSalvando(false);
    setCarregando(true);
    setDados(null);
    setRecebido("");
    setErro("");
    if (!admin) return;
    let ativo = true;
    financeiroService.buscar(obraId).then((valor) => {
      if (ativo) { setDados(valor); setRecebido(valor.totalRecebido.toFixed(2)); setErro(""); }
    }).catch((e: unknown) => { if (ativo) setErro(erroApi(e, "Não foi possível carregar o financeiro. Atualize a página para tentar novamente.")); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; geracao.current += 1; };
  }, [obraId, admin, revisao]);

  async function salvar(e: FormEvent) {
    e.preventDefault();
    if (!admin || carregando || !dados || envioEmCurso.current) return;
    const valor = valorDecimal(recebido, true);
    if (valor === null) { toast.error("Informe um recebido não negativo com até duas casas decimais e dentro do limite seguro."); return; }
    envioEmCurso.current = true;
    const geracaoEnvio = geracao.current;
    setSalvando(true);
    try {
      const atualizado = await financeiroService.atualizar(obraId, valor);
      if (geracaoEnvio !== geracao.current) return;
      setDados(atualizado);
      setRecebido(atualizado.totalRecebido.toFixed(2));
      setErro("");
      toast.success("Total recebido atualizado");
    } catch (e) { if (geracaoEnvio === geracao.current) toast.error(erroApi(e, "Não foi possível atualizar o recebido")); }
    finally { if (geracaoEnvio === geracao.current) { envioEmCurso.current = false; setSalvando(false); } }
  }

  if (!admin) return null;
  return <section className={styles.painel} aria-label="Financeiro da obra">
    <h2>Financeiro da obra · Dono</h2>
    <p>Recebimentos são independentes do orçamento. O saldo desconta os gastos, sem somar novamente os reembolsos.</p>
    {carregando ? <p role="status">Carregando financeiro...</p> : erro ? <p role="alert" className={styles.erro}>{erro}</p> : dados && <>
      <dl className={styles.metricas}>
        <div><dt>Recebido acumulado</dt><dd>{moeda(dados.totalRecebido)}</dd></div>
        <div><dt>Total gasto</dt><dd>{moeda(dados.totalGasto)}</dd></div>
        <div><dt>Saldo dos recebimentos</dt><dd className={dados.saldo < 0 ? styles.negativo : undefined}>{moeda(dados.saldo)}</dd></div>
        {orcamento !== undefined && <div><dt>Orçamento planejado (separado)</dt><dd>{moeda(Number(orcamento))}</dd></div>}
      </dl>
      <form className={styles.form} onSubmit={salvar}>
        <label>Total recebido acumulado<input inputMode="decimal" value={recebido} onChange={(e) => setRecebido(e.target.value)} required disabled={salvando} /></label>
        <p>Salvar substitui o acumulado; não adiciona uma entrada nem cria histórico de recebimentos.</p>
        <div><button className={styles.botaoPrimario} disabled={salvando}>{salvando ? "Salvando..." : "Salvar total recebido"}</button></div>
      </form>
    </>}
  </section>;
}