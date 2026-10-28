import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { toast } from "sonner";
import { financeiroService, type Reembolso } from "../../Service/Financeiro/financeiroService";
import type { Gasto } from "../../Service/Gastos/gastoService";
import { authService } from "../../Service/Auth/Login/authService";
import { hojeIso } from "../PeriodoFiltro/periodUtils";
import { dataValida, erroApi, moeda, valorDecimal } from "../../Utils/financeiro";
import styles from "./Financeiro.module.css";

const CLASSE_STATUS: Record<Reembolso["status"], string> = {
  PENDENTE: styles.statusPendente,
  PARCIAL: styles.statusParcial,
  PAGO: styles.statusPago,
};

export default function ReembolsoModal({ gasto, onFechar, onAtualizado }: { gasto: Gasto; onFechar: () => void; onAtualizado: () => void }) {
  const [dados, setDados] = useState<Reembolso | null>(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [valor, setValor] = useState("");
  const [data, setData] = useState(hojeIso());
  const [observacao, setObservacao] = useState("");
  const [requerConsulta, setRequerConsulta] = useState(false);
  const enviando = useRef(false);
  const dialogo = useRef<HTMLDialogElement>(null);
  const podePagar = authService.podeRegistrarPagamento();
  const carregar = useCallback(async () => {
    setCarregando(true);
    try { setDados(await financeiroService.historico(gasto.id)); setErro(""); setRequerConsulta(false); }
    catch (e) { setErro(erroApi(e, "Não foi possível carregar o histórico")); }
    finally { setCarregando(false); }
  }, [gasto.id]);
  useEffect(() => { void carregar(); }, [carregar]);
  useEffect(() => {
    const modal = dialogo.current;
    modal?.showModal();
    return () => { modal?.close(); };
  }, []);

  async function registrar(e: FormEvent) {
    e.preventDefault();
    if (!dados || !podePagar || enviando.current || requerConsulta) return;
    const numero = valorDecimal(valor);
    if (numero === null || Math.round(numero * 100) > Math.round(dados.saldoPendente * 100)) {
      toast.error("Informe um valor positivo, com até duas casas decimais, não superior ao saldo pendente."); return;
    }
    if (!dataValida(data, hojeIso(), gasto.dtGasto) || observacao.length > 1000) {
      toast.error("A data deve estar entre a data do gasto e hoje; observação com até 1000 caracteres."); return;
    }
    enviando.current = true;
    setSalvando(true);
    try {
      setDados(await financeiroService.registrar(gasto.id, { valor: numero, dataPagamento: data, observacao: observacao.trim() || undefined }));
      setValor(""); setObservacao("");
      onAtualizado();
      toast.success("Pagamento registrado");
    } catch (e) {
      // Sem idempotência: nunca repetir POST automaticamente após falha de rede.
      setRequerConsulta(true);
      toast.error(erroApi(e, "Resultado do pagamento desconhecido. Consulte o histórico antes de tentar novamente."));
    } finally { enviando.current = false; setSalvando(false); }
  }

  return <dialog ref={dialogo} className={`${styles.painel} ${styles.modal}`} aria-labelledby="titulo-reembolso" onCancel={(e) => { e.preventDefault(); if (!salvando) onFechar(); }}>
    <div className={styles.topo}><h2 id="titulo-reembolso">Reembolso · {gasto.descricao}</h2><button type="button" onClick={onFechar} disabled={salvando} aria-label="Fechar reembolso">Fechar</button></div>
    <p>{gasto.funcionario.nome} · Gasto em {gasto.dtGasto}</p>
    {carregando ? <p role="status">Carregando histórico...</p> : erro ? <p role="alert">{erro}</p> : dados && <>
      <p className={styles.resumoReembolso}><span className={`${styles.status} ${CLASSE_STATUS[dados.status]}`}>{dados.status}</span> Total {moeda(dados.valorTotal)} · Pago {moeda(dados.totalPago)} · Pendente {moeda(dados.saldoPendente)}</p>
      <h3>Histórico de pagamentos</h3>
      {dados.pagamentos.length === 0 ? <p>Nenhum pagamento registrado.</p> : <ol className={styles.historico}>{dados.pagamentos.map((p) => <li key={p.id}><strong>{moeda(p.valor)}</strong> · {p.dataPagamento}{p.observacao && <p>{p.observacao}</p>}</li>)}</ol>}
      {podePagar && dados.saldoPendente > 0 && <form className={styles.form} onSubmit={registrar}>
        <h3>Registrar pagamento</h3>
        <label>Valor da parcela<input inputMode="decimal" value={valor} onChange={(e) => setValor(e.target.value)} required disabled={salvando || requerConsulta} /></label>
        <label>Data do pagamento<input type="date" min={gasto.dtGasto} max={hojeIso()} value={data} onChange={(e) => setData(e.target.value)} required disabled={salvando || requerConsulta} /></label>
        <label>Observação (opcional)<textarea value={observacao} onChange={(e) => setObservacao(e.target.value)} maxLength={1000} disabled={salvando || requerConsulta} /></label>
        <p>Parcelas não podem ser editadas ou excluídas. Confira os dados antes de confirmar.</p>
        <button className={styles.botaoPrimario} disabled={salvando || requerConsulta}>{salvando ? "Registrando..." : "Confirmar pagamento"}</button>
      </form>}
      {!podePagar && <p>Somente administrador ou responsável administrativo alocado pode registrar pagamentos.</p>}
    </>}
    {requerConsulta && <p role="alert">Consulte o histórico e confira se a parcela foi registrada antes de preencher outro pagamento.</p>}
    <button type="button" onClick={() => void carregar()} disabled={salvando || carregando}>Consultar histórico atualizado</button>
  </dialog>;
}