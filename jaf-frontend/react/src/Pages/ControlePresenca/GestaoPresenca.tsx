import { useCallback, useEffect, useMemo, useState } from "react";
import { CalendarDays, CheckCircle2, CircleSlash, Search, Trash2, UsersRound } from "lucide-react";
import { toast } from "sonner";
import styles from "./GestaoPresenca.module.css";
import { authService } from "../../Service/Auth/Login/authService";
import { obraService, type Obra } from "../../Service/Obras/obraService";
import { presencaService, type Colaborador } from "../../Service/Presencas/presencaService";
import PeriodoFiltro from "../../Components/PeriodoFiltro/PeriodoFiltro";
import {
  hojeIso,
  periodoFiltroInicial,
  resolverPeriodo,
  type PeriodoFiltroValue,
} from "../../Components/PeriodoFiltro/periodUtils";

const formatarCargoObra = (cargo: string) =>
  cargo.replace(/_/g, " ").toLowerCase().replace(/\b\w/g, (letra) => letra.toUpperCase());

const formatarData = (data: string) =>
  new Date(`${data}T00:00:00`).toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" });

export default function GestaoPresenca() {
  const [obras, setObras] = useState<Obra[]>([]);
  const [obraId, setObraId] = useState<number | null>(null);
  const [data, setData] = useState(hojeIso());
  const [periodo, setPeriodo] = useState<PeriodoFiltroValue>({
    ...periodoFiltroInicial(),
    preset: "HOJE",
  });
  const [colaboradores, setColaboradores] = useState<Colaborador[]>([]);
  const [busca, setBusca] = useState("");
  const [statusFiltro, setStatusFiltro] = useState("TODOS");
  const [carregando, setCarregando] = useState(true);
  const [salvandoId, setSalvandoId] = useState<string | null>(null);

  const podeRegistrar = authService.hasAuthority("REGISTRAR_PRESENCA");
  const podeEditar = authService.hasAuthority("EDITAR_PRESENCA");
  const podeDeletar = authService.hasAuthority("DELETAR_PRESENCA");

  const obraSelecionada = obras.find((obra) => obra.id === obraId) ?? null;
  const modoPeriodo = periodo.preset === "7_DIAS" || periodo.preset === "PERSONALIZADO";
  const intervalo = resolverPeriodo(periodo);

  const chaveLinha = (colaborador: Colaborador) =>
    `${colaborador.id ?? "n"}-${colaborador.funcionarioId}-${colaborador.data}`;

  const carregarPresencas = useCallback(async () => {
    if (!obraId) return;

    if (modoPeriodo) {
      if (!intervalo.from || !intervalo.to) {
        setColaboradores([]);
        return;
      }
      const dados = await presencaService.listarPorObraEPeriodo(obraId, intervalo.from, intervalo.to);
      setColaboradores(dados);
      return;
    }

    const dataConsulta = periodo.preset === "HOJE" ? hojeIso() : data;
    const dados = await presencaService.listarPorObraEData(obraId, dataConsulta);
    setColaboradores(dados);
  }, [obraId, modoPeriodo, intervalo.from, intervalo.to, periodo.preset, data]);

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
    if (periodo.preset === "HOJE") {
      setData(hojeIso());
    }
  }, [periodo.preset]);

  useEffect(() => {
    if (!obraId) return;

    setCarregando(true);
    carregarPresencas()
      .catch((error) => {
        console.error("Erro ao atualizar lista de presença:", error);
        toast.error("Erro ao atualizar lista de presença");
      })
      .finally(() => setCarregando(false));
  }, [obraId, carregarPresencas]);

  const colaboradoresFiltrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();

    return colaboradores.filter((colaborador) => {
      const correspondeBusca =
        !termo ||
        [colaborador.funcionarioNome, colaborador.funcionarioCargo].some((campo) =>
          campo.toLowerCase().includes(termo),
        );
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
    if (modoPeriodo && !colaborador.id) {
      toast.error("No modo período, use a visão diária para registrar novos pontos");
      return;
    }
    if (!colaborador.id && !podeRegistrar) {
      toast.error("Você não tem permissão para registrar presença");
      return;
    }
    if (colaborador.id && !podeEditar) {
      toast.error("Você não tem permissão para editar presença");
      return;
    }

    const dataRegistro = modoPeriodo ? colaborador.data : periodo.preset === "HOJE" ? hojeIso() : data;

    try {
      setSalvandoId(chaveLinha(colaborador));
      const payload = {
        funcionarioId: colaborador.funcionarioId,
        obraId,
        data: dataRegistro,
        presente,
      };

      if (colaborador.id) {
        await presencaService.atualizarPresenca(colaborador.id, payload);
      } else {
        await presencaService.criarPresenca(payload);
      }

      await carregarPresencas();
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
      setSalvandoId(chaveLinha(colaborador));
      await presencaService.deletarPresenca(colaborador.id);
      await carregarPresencas();
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
              <option key={obra.id} value={obra.id}>
                {obra.titulo}
              </option>
            ))}
          </select>
        </label>
        {!modoPeriodo && (
          <label>
            Data
            <input
              type="date"
              value={periodo.preset === "HOJE" ? hojeIso() : data}
              disabled={periodo.preset === "HOJE"}
              onChange={(evento) => setData(evento.target.value)}
            />
          </label>
        )}
        <label>
          Status
          <select value={statusFiltro} onChange={(evento) => setStatusFiltro(evento.target.value)}>
            <option value="TODOS">Todos</option>
            <option value="PRESENTES">Presentes</option>
            <option value="AUSENTES">Ausentes</option>
            {!modoPeriodo && <option value="SEM_REGISTRO">Sem registro</option>}
          </select>
        </label>
        <div className={styles.busca}>
          <Search size={18} />
          <input
            value={busca}
            onChange={(evento) => setBusca(evento.target.value)}
            placeholder="Buscar colaborador ou função"
          />
        </div>
        <div className={styles.periodoWrap}>
          <PeriodoFiltro value={periodo} onChange={setPeriodo} temaEscuro ocultarTodos />
        </div>
      </section>

      <section className={styles.metricas}>
        <div>
          <UsersRound />
          <span>Total</span>
          <strong>{totais.total}</strong>
        </div>
        <div>
          <CheckCircle2 />
          <span>Presentes</span>
          <strong>{totais.presentes}</strong>
        </div>
        <div>
          <CircleSlash />
          <span>Ausentes</span>
          <strong>{totais.ausentes}</strong>
        </div>
        {!modoPeriodo && (
          <div>
            <CalendarDays />
            <span>Sem registro</span>
            <strong>{totais.semRegistro}</strong>
          </div>
        )}
      </section>

      <section className={styles.tabelaContainer}>
        <div className={styles.tabelaHeader}>
          <div>
            <h2>{obraSelecionada?.titulo ?? "Obra"}</h2>
            <p>
              {modoPeriodo
                ? "Visão agregada dos registros no período selecionado"
                : "Controle de ponto para equipe de campo sem necessidade de login no sistema"}
            </p>
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
                  {modoPeriodo && <th>Data</th>}
                  <th>Registro</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {colaboradoresFiltrados.map((colaborador) => {
                  const salvando = salvandoId === chaveLinha(colaborador);
                  const status = colaborador.id
                    ? colaborador.presente
                      ? "Presente"
                      : "Ausente"
                    : "Sem registro";

                  return (
                    <tr key={chaveLinha(colaborador)}>
                      <td>
                        <div className={styles.funcionarioCell}>
                          <span>{colaborador.funcionarioNome.charAt(0).toUpperCase()}</span>
                          <strong>{colaborador.funcionarioNome}</strong>
                        </div>
                      </td>
                      <td>{formatarCargoObra(colaborador.funcionarioCargo)}</td>
                      {modoPeriodo && <td>{formatarData(colaborador.data)}</td>}
                      <td>
                        <span
                          className={`${styles.status} ${
                            colaborador.presente
                              ? styles.presente
                              : colaborador.id
                                ? styles.ausente
                                : styles.semRegistro
                          }`}
                        >
                          {status}
                        </span>
                      </td>
                      <td>
                        <div className={styles.acoes}>
                          <button
                            type="button"
                            onClick={() => salvarStatus(colaborador, true)}
                            disabled={
                              salvando ||
                              (!colaborador.id && (!podeRegistrar || modoPeriodo)) ||
                              (Boolean(colaborador.id) && !podeEditar)
                            }
                          >
                            Presente
                          </button>
                          <button
                            type="button"
                            onClick={() => salvarStatus(colaborador, false)}
                            disabled={
                              salvando ||
                              (!colaborador.id && (!podeRegistrar || modoPeriodo)) ||
                              (Boolean(colaborador.id) && !podeEditar)
                            }
                          >
                            Ausente
                          </button>
                          {colaborador.id && (
                            <button
                              type="button"
                              className={styles.botaoPerigo}
                              onClick={() => excluirRegistro(colaborador)}
                              disabled={salvando || !podeDeletar}
                              aria-label={`Excluir presença de ${colaborador.funcionarioNome}`}
                            >
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
