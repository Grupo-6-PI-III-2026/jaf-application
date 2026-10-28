import { useEffect, useRef, useState } from "react";
import { isAxiosError } from "axios";
import { FileText, Calendar, User, DollarSign, ChevronDown } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { toast } from "sonner";
import styles from "./NovaObra.module.css";
import { obraService, type Obra } from "../../../Service/Obras/obraService";
import { alocacaoService } from "../../../Service/Alocacoes/alocacaoService";
import { funcionarioService } from "../../../Service/Funcionarios/funcionarioService";
import { authService } from "../../../Service/Auth/Login/authService";
import type { FuncionarioPermissoes } from "../../../Types/permissoes";
import { dataValida, erroApi, valorDecimal } from "../../../Utils/financeiro";

interface ErrosFormulario {
  nomeObra?: string;
  responsavel?: string;
  dataInicio?: string;
  dataFim?: string;
  orcamento?: string;
}

const STATUS_OPCOES = [
  { label: "Planejada", value: "PLANEJADA" },
  { label: "Em Andamento", value: "EM_ANDAMENTO" },
  { label: "Concluída", value: "CONCLUIDA" },
  { label: "Cancelada", value: "CANCELADA" },
];

export default function NovaObra() {
  const navigate = useNavigate();
  // Trava síncrona: não depender de um novo render para impedir envios duplicados.
  const enviandoRef = useRef(false);
  const obraCriadaRef = useRef<Obra | null>(null);
  const podeAlocar = authService.hasAuthority("CRIAR_ALOCACAO");
  const podeListarFuncionarios = podeAlocar || authService.hasAuthority("VISUALIZAR_FUNCIONARIOS");
  const [isLoading, setIsLoading] = useState(false);
  const [carregandoFuncionarios, setCarregandoFuncionarios] = useState(false);
  const [errors, setErrors] = useState<ErrosFormulario>({});
  const [funcionarios, setFuncionarios] = useState<FuncionarioPermissoes[]>([]);
  const [nomeObra, setNomeObra] = useState("");
  const [status, setStatus] = useState("PLANEJADA");
  const [responsavelId, setResponsavelId] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [orcamento, setOrcamento] = useState("");

  useEffect(() => {
    // A lista só é necessária para alocar; CRIAR_ALOCACAO também autoriza a consulta na API.
    if (!podeAlocar || !podeListarFuncionarios) return;
    let ativo = true;
    setCarregandoFuncionarios(true);
    funcionarioService.listar().then((data) => {
      if (!ativo) return;
      setFuncionarios(data);
      setResponsavelId((atual) => atual || String(data[0]?.id ?? ""));
    }).catch((error: unknown) => {
      if (ativo) toast.error(erroApi(error, "Não foi possível carregar os usuários do sistema."));
    }).finally(() => {
      if (ativo) setCarregandoFuncionarios(false);
    });
    return () => { ativo = false; };
  }, [podeAlocar, podeListarFuncionarios]);

  const validateInputs = () => {
    const nextErrors: ErrosFormulario = {};
    if (!nomeObra.trim()) nextErrors.nomeObra = "O nome da obra é obrigatório.";
    // O helper exige um limite superior: planejamento admite qualquer data ISO válida, inclusive futura.
    const inicioValido = dataValida(dataInicio, "9999-12-31");
    const fimValido = dataValida(dataFim, "9999-12-31");
    if (!inicioValido) nextErrors.dataInicio = "Informe uma data de início válida.";
    if (!fimValido) nextErrors.dataFim = "Informe uma data de término válida.";
    else if (inicioValido && dataFim < dataInicio) {
      nextErrors.dataFim = "A data de término não pode ser anterior à data de início.";
    }
    if (valorDecimal(orcamento) === null) {
      nextErrors.orcamento = "Informe um orçamento positivo com até duas casas decimais.";
    }
    if (podeAlocar && !funcionarios.some((funcionario) => String(funcionario.id) === responsavelId)) {
      nextErrors.responsavel = "Selecione um usuário responsável pela obra.";
    }
    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleCriarObra = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (enviandoRef.current) return;
    if (obraCriadaRef.current) {
      navigate(podeAlocar ? `/obras/${obraCriadaRef.current.id}/alocacoes` : "/home");
      return;
    }
    if (!validateInputs()) return;
    enviandoRef.current = true;
    setIsLoading(true);

    try {
      let obraCriada: Obra;
      try {
        obraCriada = await obraService.criar({
          titulo: nomeObra.trim(),
          orcamento: String(valorDecimal(orcamento)),
          status,
          dtInicio: dataInicio,
          dtTerminoPrevisto: dataFim,
        });
        // Nunca repetir o POST após uma criação confirmada, mesmo se a alocação falhar.
        obraCriadaRef.current = obraCriada;
      } catch (error: unknown) {
        if (isAxiosError(error) && error.response?.status === 401) {
          toast.error("Sessão expirada. Faça login novamente.");
          navigate("/");
        } else {
          toast.error(erroApi(error, isAxiosError(error) && error.response?.status === 403
            ? "Você não tem permissão para criar obras."
            : "Falha ao criar obra. Tente novamente."));
        }
        return;
      }

      if (podeAlocar) {
        try {
          await alocacaoService.criar({
            funcionarioId: Number(responsavelId),
            obraId: obraCriada.id,
            cargoNaObra: "GESTOR_OBRA",
          });
        } catch {
          toast.warning(`Obra "${obraCriada.titulo}" criada, mas não foi possível alocar o responsável. Finalize a alocação na próxima tela; não é necessário criar a obra novamente.`);
          navigate(`/obras/${obraCriada.id}/alocacoes`);
          return;
        }
        toast.success(`Obra "${obraCriada.titulo}" criada com sucesso!`);
      } else {
        toast.success(`Obra "${obraCriada.titulo}" criada. Solicite a um usuário com permissão de alocação que vincule o responsável.`);
      }
      navigate("/home");
    } finally {
      enviandoRef.current = false;
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.pagina}>
      <div className={styles.navegacao}>
        <span>OBRAS</span><span className={styles.separador}>&gt;</span>
        <span className={styles.ativo}>NOVA OBRA</span>
      </div>
      <h1 className={styles.titulo}>Criação de Obra</h1>
      <p className={styles.subtitulo}>Cadastre uma nova estrutura no sistema e defina os parâmetros iniciais de gerenciamento.</p>
      <form aria-label="Criar obra" className={styles.formulario} onSubmit={handleCriarObra} noValidate>
        <div className={styles.gridContainer}>
          <div className={styles.colunaEsquerda}>
            <section className={styles.secao} aria-labelledby="informacoes-obra">
              <div className={styles.secaoHeader}>
                <FileText size={20} className={styles.iconeSecao} aria-hidden="true" />
                <h2 id="informacoes-obra" className={styles.secaoTitulo}>Informações Gerais</h2>
              </div>
              <div className={styles.gruposCampos}>
                <div className={styles.campoBlocoInteiro}>
                  <label htmlFor="nome-obra" className={styles.rotulo}>NOME DA OBRA</label>
                  <div className={styles.caixaCampo}>
                    <input id="nome-obra" type="text" placeholder="Ex: Residencial Mirante do Sol" value={nomeObra}
                      onChange={(e) => setNomeObra(e.target.value)} className={styles.campo} disabled={isLoading}
                      aria-invalid={!!errors.nomeObra} aria-describedby={errors.nomeObra ? "erro-nome-obra" : undefined} />
                  </div>
                  {errors.nomeObra && <span id="erro-nome-obra" role="alert" className={styles.errorText}>{errors.nomeObra}</span>}
                </div>
                <div className={styles.linhaGrupos}>
                  <div className={styles.grupoCampo}>
                    <label htmlFor="status-obra" className={styles.rotulo}>STATUS</label>
                    <div className={styles.selectWrapper}>
                      <select id="status-obra" value={status} onChange={(e) => setStatus(e.target.value)} className={styles.select} disabled={isLoading}>
                        {STATUS_OPCOES.map((opt) => <option key={opt.value} value={opt.value}>{opt.label}</option>)}
                      </select>
                      <ChevronDown size={20} className={styles.iconSelect} aria-hidden="true" />
                    </div>
                  </div>
                  {podeAlocar && <div className={styles.grupoCampo}>
                    <label htmlFor="responsavel-obra" className={styles.rotulo}>RESPONSÁVEL</label>
                    <div className={styles.selectWrapperComIcone}>
                      <User size={18} className={styles.icone} aria-hidden="true" />
                      <select id="responsavel-obra" value={responsavelId} onChange={(e) => setResponsavelId(e.target.value)}
                        className={styles.selectComIcone} disabled={isLoading || carregandoFuncionarios || !funcionarios.length}
                        aria-invalid={!!errors.responsavel} aria-describedby={errors.responsavel ? "erro-responsavel-obra" : undefined}>
                        <option value="">{carregandoFuncionarios ? "Carregando usuários..." : "Selecione um responsável"}</option>
                        {funcionarios.map((funcionario) => <option key={funcionario.id} value={funcionario.id}>{funcionario.nome} - {funcionario.email}</option>)}
                      </select>
                      <ChevronDown size={20} className={styles.iconSelect} aria-hidden="true" />
                    </div>
                    {errors.responsavel && <span id="erro-responsavel-obra" role="alert" className={styles.errorText}>{errors.responsavel}</span>}
                  </div>}
                </div>
                {!podeAlocar && <p className={styles.infoTexto}>Você pode criar a obra sem responsável. Solicite a um usuário com permissão de alocação que faça o vínculo depois.</p>}
                <div className={styles.linhaGrupos}>
                  <div className={styles.grupoCampo}>
                    <label htmlFor="inicio-obra" className={styles.rotulo}>DATA DE INÍCIO</label>
                    <div className={styles.caixaCampo}>
                      <Calendar size={18} className={styles.icone} aria-hidden="true" />
                      <input id="inicio-obra" type="date" value={dataInicio} onChange={(e) => setDataInicio(e.target.value)}
                        className={styles.campo} disabled={isLoading} aria-invalid={!!errors.dataInicio}
                        aria-describedby={errors.dataInicio ? "erro-inicio-obra" : undefined} />
                    </div>
                    {errors.dataInicio && <span id="erro-inicio-obra" role="alert" className={styles.errorText}>{errors.dataInicio}</span>}
                  </div>
                  <div className={styles.grupoCampo}>
                    <label htmlFor="fim-obra" className={styles.rotulo}>DATA DE TÉRMINO</label>
                    <div className={styles.caixaCampo}>
                      <Calendar size={18} className={styles.icone} aria-hidden="true" />
                      <input id="fim-obra" type="date" value={dataFim} onChange={(e) => setDataFim(e.target.value)}
                        className={styles.campo} disabled={isLoading} aria-invalid={!!errors.dataFim}
                        aria-describedby={errors.dataFim ? "erro-fim-obra" : undefined} />
                    </div>
                    {errors.dataFim && <span id="erro-fim-obra" role="alert" className={styles.errorText}>{errors.dataFim}</span>}
                  </div>
                </div>
              </div>
            </section>
          </div>
          <div className={styles.colunaDireita}>
            <section className={styles.card} aria-labelledby="financeiro-obra">
              <div className={styles.cardHeader}>
                <DollarSign size={20} className={styles.iconeCard} aria-hidden="true" />
                <h2 id="financeiro-obra" className={styles.cardTitulo}>Financeiro</h2>
              </div>
              <div className={styles.cardBody}>
                <label htmlFor="orcamento-obra" className={styles.rotulo}>ORÇAMENTO PLANEJADO</label>
                <div className={styles.caixaCampo}>
                  <DollarSign size={18} className={styles.icone} aria-hidden="true" />
                  <input id="orcamento-obra" type="text" inputMode="decimal" placeholder="0,00" value={orcamento}
                    onChange={(e) => setOrcamento(e.target.value)} className={styles.campo} disabled={isLoading}
                    aria-invalid={!!errors.orcamento} aria-describedby={`info-orcamento${errors.orcamento ? " erro-orcamento-obra" : ""}`} />
                </div>
                {errors.orcamento && <span id="erro-orcamento-obra" role="alert" className={styles.errorText}>{errors.orcamento}</span>}
                <p id="info-orcamento" className={styles.infoTexto}>Orçamento planejado não é dinheiro recebido. O total recebido começa em zero e deve ser atualizado no financeiro da obra.</p>
              </div>
            </section>
            <div className={styles.card}>
              <button type="submit" className={styles.botaoCriar} disabled={isLoading}>{isLoading ? "Criando..." : "Criar Obra"}</button>
              <button type="button" onClick={() => navigate(-1)} className={styles.botaoCancelar} disabled={isLoading}>Cancelar</button>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
