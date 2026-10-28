import { Calendar, X } from "lucide-react";
import { useCallback, useMemo, useRef, useState, useEffect } from "react";
import styles from "./ControlePresenca.module.css";
import { presencaService } from "../../Service/Presencas/presencaService";
import type { Colaborador } from "../../Service/Presencas/presencaService";
import { toast } from "sonner";
import { authService } from "../../Service/Auth/Login/authService";
import { hojeIso } from "../../Components/PeriodoFiltro/periodUtils";
import { dataValida, erroApi } from "../../Utils/financeiro";

type ControlePresencaProps = {
  aberto: boolean;
  onFechar: () => void;
  obraId?: number;
  obraTitulo?: string;
  data?: string;
};

export default function ControlePresenca({ 
  aberto, 
  onFechar, 
  obraId, 
  obraTitulo = "Obra", 
  data 
}: ControlePresencaProps) {
  const [colaboradores, setColaboradores] = useState<Colaborador[]>([]);
  const [carregando, setCarregando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [alterados, setAlterados] = useState<Set<number>>(new Set());
  const [erro, setErro] = useState("");
  const originais = useRef(new Map<number, boolean>());
  const podeRegistrar = authService.hasAuthority("REGISTRAR_PRESENCA");
  const podeEditar = authService.hasAuthority("EDITAR_PRESENCA");
  const dataPermitida = !!data && dataValida(data, hojeIso());

  const carregarColaboradores = useCallback(async () => {
    if (!obraId || !data) return;
    
    try {
      setCarregando(true);
      const dados = await presencaService.listarPorObraEData(obraId, data);
      setColaboradores(dados);
      originais.current = new Map(dados.map((pessoa) => [pessoa.funcionarioId, pessoa.presente]));
      setAlterados(new Set());
      setErro("");
    } catch (error) {
      console.error("Erro ao carregar colaboradores:", error);
      toast.error("Erro ao carregar lista de presença");
      setColaboradores([]);
      setErro("Não foi possível carregar a lista de presença. Feche e abra novamente para tentar.");
    } finally {
      setCarregando(false);
    }
  }, [obraId, data]);

  // Carregar colaboradores quando o modal abrir
  useEffect(() => {
    if (aberto && obraId && data) {
      carregarColaboradores();
    }
  }, [aberto, obraId, data, carregarColaboradores]);

  const totalPresentes = useMemo(
    () => colaboradores.filter((colaborador) => colaborador.presente).length,
    [colaboradores],
  );

  async function alternarPresenca(colaborador: Colaborador) {
    if (salvando || !dataPermitida || colaborador.desabilitado || (colaborador.id ? !podeEditar : !podeRegistrar)) return;

    // Atualização otimista da UI
    setColaboradores((listaAtual) =>
      listaAtual.map((colab) => {
        if (colab.funcionarioId !== colaborador.funcionarioId) {
          return colab;
        }
        return { ...colab, presente: !colab.presente };
      }),
    );

    setAlterados((atuais) => new Set(atuais).add(colaborador.funcionarioId));
  }

  async function registrarPresenca() {
    if (salvando || !obraId || !data || !dataPermitida) {
      toast.error("Dados da obra incompletos");
      return;
    }

    try {
      setSalvando(true);
      
      // Toda seleção (inclusive ausência explícita) é persistida só na confirmação.
      const promessas = colaboradores
        .filter(colab => alterados.has(colab.funcionarioId) &&
          authService.hasAuthority(colab.id ? "EDITAR_PRESENCA" : "REGISTRAR_PRESENCA") &&
          (!colab.id || originais.current.get(colab.funcionarioId) !== colab.presente))
        .map(colab => 
          colab.id ? presencaService.alternarPresenca(colab.id) : presencaService.criarPresenca({
            funcionarioId: colab.funcionarioId,
            obraId: obraId,
            data: data,
            presente: colab.presente,
          })
        );

      const resultados = await Promise.allSettled(promessas);
      await carregarColaboradores();
      if (resultados.some((r) => r.status === "rejected")) {
        toast.error("Alguns registros falharam. A lista foi recarregada; confira antes de tentar novamente.");
      } else {
        toast.success("Registros de presença salvos");
        onFechar();
      }
    } catch (error) {
      console.error("Erro ao registrar presença:", error);
      toast.error(erroApi(error, "Erro ao registrar presença"));
    } finally {
      setSalvando(false);
    }
  }

  function formatarData(dataStr?: string): string {
    if (!dataStr) return "Selecionar data";
    const data = new Date(`${dataStr}T00:00:00`);
    return data.toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" });
  }

  if (!aberto) {
    return null;
  }

  return (
    <div className={styles.overlay} onClick={() => { if (!salvando) onFechar(); }}>
      <section className={styles.modal} onClick={(evento) => evento.stopPropagation()}>
        <header className={styles.header}>
          <button
            type="button"
            onClick={onFechar}
            disabled={salvando}
            className={styles.botaoFechar}
            aria-label="Fechar controle de presenca"
          >
            <X size={20} />
          </button>

          <h2 className={styles.titulo}>CONTROLE DE PRESENÇA</h2>
          <p className={styles.subtitulo}>{obraTitulo}</p>

          <button type="button" className={styles.botaoData}>
            <Calendar size={14} className={styles.iconeData} />
            {formatarData(data)}
          </button>
        </header>

        <div className={styles.corpo}>
          <p>Registro manual por usuário autenticado com permissão. Selecione a pessoa e confirme para salvar; fechar descarta a seleção. A pessoa deve estar alocada na data.</p>
          {!dataPermitida && <p role="alert">Selecione uma data válida até hoje para registrar.</p>}
          {erro && <p role="alert">{erro}</p>}
          <div className={styles.cabecalhoLista}>
            <span>COLABORADOR</span>
            <span>STATUS</span>
          </div>

          {carregando ? (
            <div className={styles.carregando}>
              <p>Carregando...</p>
            </div>
          ) : (
            <ul className={styles.lista}>
              {colaboradores.map((colaborador) => (
                <li key={colaborador.funcionarioId} className={styles.itemColaborador}>
                  <div className={styles.infoColaborador}>
                    <div className={`${styles.avatar} ${colaborador.desabilitado ? styles.avatarCinza : ""}`}>
                      {colaborador.funcionarioNome.charAt(0).toUpperCase()}
                    </div>
                    <div>
                      <p className={`${styles.nome} ${colaborador.desabilitado ? styles.textoDesabilitado : ""}`}>
                        {colaborador.funcionarioNome}
                      </p>
                      <p className={`${styles.cargo} ${colaborador.desabilitado ? styles.cargoDesabilitado : ""}`}>
                        {colaborador.funcionarioCargo}
                      </p>
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => alternarPresenca(colaborador)}
                    disabled={salvando || !dataPermitida || colaborador.desabilitado || (colaborador.id ? !podeEditar : !podeRegistrar)}
                    aria-label={`Presença de ${colaborador.funcionarioNome}`}
                    aria-pressed={colaborador.presente}
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
          )}
        </div>

        <footer className={styles.rodape}>
          <p className={styles.contador}>
            {totalPresentes} DE {colaboradores.length} PRESENTES
          </p>
          {(podeRegistrar || podeEditar) && <button
            type="button" 
            className={styles.botaoRegistrar}
            onClick={registrarPresenca}
            disabled={salvando || carregando || !!erro || !dataPermitida || alterados.size === 0}
          >
            {salvando ? "Salvando..." : podeRegistrar ? "Registrar Presença" : "Salvar alterações"}
          </button>}
        </footer>
      </section>
    </div>
  );
}
