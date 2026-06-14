import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarDays, CheckCircle2, CircleSlash, Search, Trash2, UsersRound } from "lucide-react";
import { toast } from "sonner";
import styles from "./GestaoPresenca.module.css";
import { authService } from "../../Service/Auth/Login/authService";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { presencaService, type Colaborador } from "../../Service/Presencas/presencaService";

const hojeIso = () => new Date().toISOString().slice(0, 10);

const formatarCargoObra = (cargo: string) => cargo.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (letra) => letra.toUpperCase());

export default function GestaoPresenca() {
  const [obras, setObras] = useState<Obra[]>([]);
  const [obraId, setObraId] = useState<number | null>(null);
  const [data, setData] = useState(hojeIso());
  const [colaboradores, setColaboradores] = useState<Colaborador[]>([]);
  const [busca, setBusca] = useState("");
  const [statusFiltro, setStatusFiltro] = useState("TODOS");
  const [carregando, setCarregando] = useState(true);
  const [salvandoId, setSalvandoId] = useState<number | null>(null);

  const podeRegistrar = authService.hasAuthority("REGISTRAR_PRESENCA");
  const podeEditar = authService.hasAuthority("EDITAR_PRESENCA");
  const podeDeletar = authService.hasAuthority("DELETAR_PRESENCA");

  const obraSelecionada = obras.find((obra) => obra.id === obraId) ?? null;

  const carregarPresencas = useCallback(async (obraSelecionadaId: number, dataSelecionada: string) => {
    const dados = await presencaService.listarPorObraEData(obraSelecionadaId, dataSelecionada);
    setColaboradores(dados);
  }, []);

  useEffect(() => {
    async function carregarObras() {
      try {
        setCarregando(true);
        const dados = await obraService.listar();
        setObras(dados);
        const primeiraObraId = dados[0]?.id ?? null;
        setObraId(primeiraObraId);
      } catch (error) {
        console.error("Erro ao carregar gestão de presença:", error);
        toast.error("Erro ao carregar controle de presença");
      } finally {
        setCarregando(false);
      }
    }

    carregarObras();
  }, []);

  useEffect(() => {
    if (!obraId) return;

    carregarPresencas(obraId, data).catch((error) => {
      console.error("Erro ao atualizar lista de presença:", error);
      toast.error("Erro ao atualizar lista de presença");
    });
  }, [obraId, data, carregarPresencas]);

  const colaboradoresFiltrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();

    return colaboradores.filter((colaborador) => {
      const correspondeBusca = !termo || [colaborador.funcionarioNome, colaborador.funcionarioCargo]
        .some((campo) => campo.toLowerCase().includes(termo));
      const temRegistro = Boolean(colaborador.id);
      const correspondeStatus =
        statusFiltro === "TODOS" ||
        (statusFiltro === "PRESENTES" && colaborador.presente) ||
        (statusFiltro === "AUSENTES" && temRegistro && !colaborador.presente) ||
        (statusFiltro === "SEM_REGISTRO" && !temRegistro);

      return correspondeBusca && correspondeStatus;
    });
  }, [busca, colaboradores, statusFiltro]);

  const totais = useMemo(() => {
    const presentes = colaboradores.filter((colaborador) => colaborador.presente).length;
    const comRegistro = colaboradores.filter((colaborador) => colaborador.id).length;

    return {
      total: colaboradores.length,
      presentes,
      ausentes: comRegistro - presentes,
      semRegistro: colaboradores.length - comRegistro,
    };
  }, [colaboradores]);

  async function salvarStatus(colaborador: Colaborador, presente: boolean) {
    if (!obraId) return;
    if (!colaborador.id && !podeRegistrar) {
      toast.error("Você não tem permissão para registrar presença");
      return;
    }
    if (colaborador.id && !podeEditar) {
      toast.error("Você não tem permissão para editar presença");
      return;
    }

    try {
      setSalvandoId(colaborador.funcionarioId);
      const payload = {
        funcionarioId: colaborador.funcionarioId,
        obraId,
        data,
        presente,
      };

      if (colaborador.id) {
        await presencaService.atualizarPresenca(colaborador.id, payload);
      } else {
        await presencaService.criarPresenca(payload);
      }

      await carregarPresencas(obraId, data);
      toast.success(presente ? "Presença marcada" : "Ausência registrada");
    } catch (error) {
      console.error("Erro ao salvar presença:", error);
      toast.error("Não foi possível salvar o registro");
    } finally {
      setSalvandoId(null);
    }
  }

  async function excluirRegistro(colaborador: Colaborador) {
    if (!obraId || !colaborador.id) return;
    if (!podeDeletar) {
      toast.error("Você não tem permissão para excluir presença");
      return;
    }

    try {
      setSalvandoId(colaborador.funcionarioId);
      await presencaService.deletarPresenca(colaborador.id);
      await carregarPresencas(obraId, data);
      toast.success("Registro removido");
    } catch (error) {
      console.error("Erro ao excluir presença:", error);
      toast.error("Não foi possível remover o registro");
    } finally {
      setSalvandoId(null);
    }
  }

  return (
    <main className={styles.pagina}>
      <header className={styles.header}>
        <div>
          <span className={styles.eyebrow}>OPERAÇÃO</span>
          <h1>Gestão de Presença</h1>
          <p>Consulte e ajuste a presença dos colaboradores operacionais alocados em cada obra.</p>
        </div>
      </header>

      <section className={styles.filtros}>
        <label>
          Obra
          <select value={obraId ?? ""} onChange={(evento) => setObraId(Number(evento.target.value))}>
            {obras.map((obra) => (
              <option key={obra.id} value={obra.id}>{obra.titulo}</option>
            ))}
          </select>
        </label>
        <label>
          Data
          <input type="date" value={data} onChange={(evento) => setData(evento.target.value)} />
        </label>
        <label>
          Status
          <select value={statusFiltro} onChange={(evento) => setStatusFiltro(evento.target.value)}>
            <option value="TODOS">Todos</option>
            <option value="PRESENTES">Presentes</option>
            <option value="AUSENTES">Ausentes</option>
            <option value="SEM_REGISTRO">Sem registro</option>
          </select>
        </label>
        <div className={styles.busca}>
          <Search size={18} />
          <input value={busca} onChange={(evento) => setBusca(evento.target.value)} placeholder="Buscar colaborador ou função" />
        </div>
      </section>

      <section className={styles.metricas}>
        <div><UsersRound /><span>Total</span><strong>{totais.total}</strong></div>
        <div><CheckCircle2 /><span>Presentes</span><strong>{totais.presentes}</strong></div>
        <div><CircleSlash /><span>Ausentes</span><strong>{totais.ausentes}</strong></div>
        <div><CalendarDays /><span>Sem registro</span><strong>{totais.semRegistro}</strong></div>
      </section>

      <section className={styles.tabelaContainer}>
        <div className={styles.tabelaHeader}>
          <div>
            <h2>{obraSelecionada?.titulo ?? "Obra"}</h2>
            <p>Controle de ponto para equipe de campo sem necessidade de login no sistema</p>
          </div>
        </div>

        {carregando ? (
          <div className={styles.estado}>Carregando presença...</div>
        ) : colaboradoresFiltrados.length === 0 ? (
          <div className={styles.estado}>Nenhum colaborador encontrado para os filtros selecionados.</div>
        ) : (
          <div className={styles.tabelaScroll}>
            <table className={styles.tabela}>
              <thead>
                <tr>
                  <th>Colaborador</th>
                  <th>Função na obra</th>
                  <th>Registro</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {colaboradoresFiltrados.map((colaborador) => {
                  const salvando = salvandoId === colaborador.funcionarioId;
                  const status = colaborador.id ? (colaborador.presente ? "Presente" : "Ausente") : "Sem registro";

                  return (
                    <tr key={colaborador.funcionarioId}>
                      <td>
                        <div className={styles.funcionarioCell}>
                          <span>{colaborador.funcionarioNome.charAt(0).toUpperCase()}</span>
                          <strong>{colaborador.funcionarioNome}</strong>
                        </div>
                      </td>
                      <td>{formatarCargoObra(colaborador.funcionarioCargo)}</td>
                      <td><span className={`${styles.status} ${colaborador.presente ? styles.presente : colaborador.id ? styles.ausente : styles.semRegistro}`}>{status}</span></td>
                      <td>
                        <div className={styles.acoes}>
                          <button type="button" onClick={() => salvarStatus(colaborador, true)} disabled={salvando || (!colaborador.id && !podeRegistrar) || (Boolean(colaborador.id) && !podeEditar)}>
                            Presente
                          </button>
                          <button type="button" onClick={() => salvarStatus(colaborador, false)} disabled={salvando || (!colaborador.id && !podeRegistrar) || (Boolean(colaborador.id) && !podeEditar)}>
                            Ausente
                          </button>
                          {colaborador.id && (
                            <button type="button" className={styles.botaoPerigo} onClick={() => excluirRegistro(colaborador)} disabled={salvando || !podeDeletar} aria-label={`Excluir presença de ${colaborador.funcionarioNome}`}>
                              <Trash2 size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </main>
  );
}