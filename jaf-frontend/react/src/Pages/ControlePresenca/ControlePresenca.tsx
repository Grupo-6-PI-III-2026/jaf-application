import { Calendar, X } from "lucide-react";
import { useMemo, useState, useEffect } from "react";
import styles from "./ControlePresenca.module.css";
import { presencaService, Colaborador } from "../../Service/Presencas/presencaService";
import { toast } from "sonner";

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

  // Carregar colaboradores quando o modal abrir
  useEffect(() => {
    if (aberto && obraId && data) {
      carregarColaboradores();
    }
  }, [aberto, obraId, data]);

  async function carregarColaboradores() {
    if (!obraId || !data) return;
    
    try {
      setCarregando(true);
      const dados = await presencaService.listarPorObraEData(obraId, data);
      setColaboradores(dados);
    } catch (error) {
      console.error("Erro ao carregar colaboradores:", error);
      toast.error("Erro ao carregar lista de presença");
    } finally {
      setCarregando(false);
    }
  }

  const totalPresentes = useMemo(
    () => colaboradores.filter((colaborador) => colaborador.presente).length,
    [colaboradores],
  );

  async function alternarPresenca(colaborador: Colaborador) {
    if (colaborador.desabilitado) return;

    // Atualização otimista da UI
    setColaboradores((listaAtual) =>
      listaAtual.map((colab) => {
        if (colab.funcionarioId !== colaborador.funcionarioId) {
          return colab;
        }
        return { ...colab, presente: !colab.presente };
      }),
    );

    // Se já existe um ID, tenta alternar no backend
    if (colaborador.id) {
      try {
        await presencaService.alternarPresenca(colaborador.id);
      } catch (error) {
        console.error("Erro ao alternar presença:", error);
        toast.error("Erro ao atualizar presença");
        // Reverter a alteração em caso de erro
        carregarColaboradores();
      }
    }
  }

  async function registrarPresenca() {
    if (!obraId || !data) {
      toast.error("Dados da obra incompletos");
      return;
    }

    try {
      setSalvando(true);
      
      // Para cada colaborador presente que não tem ID, criar presença
      const promessas = colaboradores
        .filter(colab => colab.presente && !colab.id)
        .map(colab => 
          presencaService.criarPresenca({
            funcionarioId: colab.funcionarioId,
            obraId: obraId,
            data: data,
            presente: colab.presente,
          })
        );

      await Promise.all(promessas);
      toast.success("Presença registrada com sucesso");
      onFechar();
    } catch (error) {
      console.error("Erro ao registrar presença:", error);
      toast.error("Erro ao registrar presença");
    } finally {
      setSalvando(false);
    }
  }

  function formatarData(dataStr?: string): string {
    if (!dataStr) return "Selecionar data";
    const data = new Date(dataStr);
    return data.toLocaleDateString('pt-BR');
  }

  if (!aberto) {
    return null;
  }

  return (
    <div className={styles.overlay} onClick={onFechar}>
      <section className={styles.modal} onClick={(evento) => evento.stopPropagation()}>
        <header className={styles.header}>
          <button
            type="button"
            onClick={onFechar}
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
                    disabled={colaborador.desabilitado}
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
          <button 
            type="button" 
            className={styles.botaoRegistrar}
            onClick={registrarPresenca}
            disabled={salvando || carregando}
          >
            {salvando ? "Salvando..." : "Registrar Presença"}
          </button>
        </footer>
      </section>
    </div>
  );
}
