import { useState, useRef } from "react";
import {
  FileText,
  MapPin,
  Calendar,
  User,
  DollarSign,
  Upload,
  ChevronDown,
  X,
} from "lucide-react";
import { useNavigate } from "react-router-dom";
import styles from "./NovaObra.module.css";

interface ErrosFormulario {
  nomeObra?: string;
  status?: string;
  responsavel?: string;
  dataInicio?: string;
  dataFim?: string;
  endereco?: string;
  cidade?: string;
  estado?: string;
  orcamento?: string;
  arquivos?: string;
}

interface ArquivoUpload {
  id: string;
  nome: string;
  tipo: string;
  tamanho: number;
}

const ESTADOS = [
  "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA",
  "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN",
  "RS", "RO", "RR", "SC", "SP", "SE", "TO"
];

const STATUS_OPCOES = [
  "Em Planejamento",
  "Em Progresso",
  "Pausada",
  "Finalizada",
];

export default function NovaObra() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errors, setErrors] = useState<ErrosFormulario>({});
  const [arquivos, setArquivos] = useState<ArquivoUpload[]>([]);

  // Form states
  const [nomeObra, setNomeObra] = useState("");
  const [status, setStatus] = useState("Em Planejamento");
  const [responsavel, setResponsavel] = useState("");
  const [dataInicio, setDataInicio] = useState("");
  const [dataFim, setDataFim] = useState("");
  const [endereco, setEndereco] = useState("");
  const [cidade, setCidade] = useState("");
  const [estado, setEstado] = useState("");
  const [orcamento, setOrcamento] = useState("");

  const validateInputs = () => {
    const nextErrors: ErrosFormulario = {};

    if (!nomeObra.trim()) {
      nextErrors.nomeObra = "O nome da obra é obrigatório.";
    }

    if (!responsavel.trim()) {
      nextErrors.responsavel = "Selecione um responsável.";
    }

    if (!dataInicio) {
      nextErrors.dataInicio = "A data de início é obrigatória.";
    }

    if (!dataFim) {
      nextErrors.dataFim = "A data de término é obrigatória.";
    }

    if (dataInicio && dataFim && new Date(dataInicio) > new Date(dataFim)) {
      nextErrors.dataFim = "A data de término deve ser após a data de início.";
    }

    if (!endereco.trim()) {
      nextErrors.endereco = "O endereço é obrigatório.";
    }

    if (!cidade.trim()) {
      nextErrors.cidade = "A cidade é obrigatória.";
    }

    if (!estado) {
      nextErrors.estado = "Selecione um estado.";
    }

    if (!orcamento.trim()) {
      nextErrors.orcamento = "Defina o orçamento inicial.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleArquivoUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files) return;

    if (arquivos.length + files.length > 3) {
      setErrors((prev) => ({
        ...prev,
        arquivos: "Máximo de 3 arquivos permitidos.",
      }));
      return;
    }

    const novoArquivos: ArquivoUpload[] = Array.from(files).map((file) => ({
      id: Math.random().toString(36).substr(2, 9),
      nome: file.name,
      tipo: file.type,
      tamanho: file.size,
    }));

    setArquivos((prev) => [...prev, ...novoArquivos]);
    setErrors((prev) => ({ ...prev, arquivos: undefined }));
  };

  const removeArquivo = (id: string) => {
    setArquivos((prev) => prev.filter((arq) => arq.id !== id));
  };

  const handleCriarObra = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!validateInputs()) {
      return;
    }

    setIsLoading(true);

    try {
      // Simulação de API call
      console.log("Dados da nova obra:", {
        nomeObra,
        status,
        responsavel,
        dataInicio,
        dataFim,
        endereco,
        cidade,
        estado,
        orcamento,
        arquivos: arquivos.map((a) => a.nome),
      });

      alert("Obra criada com sucesso!");
      
      // Limpar formulário
      setNomeObra("");
      setStatus("Em Planejamento");
      setResponsavel("");
      setDataInicio("");
      setDataFim("");
      setEndereco("");
      setCidade("");
      setEstado("");
      setOrcamento("");
      setArquivos([]);
      setErrors({});

      // Redirecionar para lista de obras
      navigate("/obras");
    } catch (error) {
      console.error("Erro ao criar obra:", error);
      alert("Falha ao criar obra. Tente novamente.");
    } finally {
      setIsLoading(false);
    }
  };

  const formatarTamanhoArquivo = (bytes: number) => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + " " + sizes[i];
  };

  return (
    <div className={styles.pagina}>
      <div className={styles.navegacao}>
        <span>OBRAS</span>
        <span className={styles.separador}>&gt;</span>
        <span className={styles.ativo}>NOVA OBRA</span>
      </div>

      <h1 className={styles.titulo}>Criação de Obra</h1>
      <p className={styles.subtitulo}>
        Cadastre uma nova estrutura no sistema e defina os parâmetros iniciais de gerenciamento.
      </p>

      <form className={styles.formulario} onSubmit={handleCriarObra}>
        <div className={styles.gridContainer}>
          {/* Coluna esquerda - Formulário principal */}
          <div className={styles.colunaEsquerda}>
            {/* Seção: Informações Gerais */}
            <div className={styles.secao}>
              <div className={styles.secaoHeader}>
                <FileText size={20} className={styles.iconeSecao} />
                <h2 className={styles.secaoTitulo}>Informações Gerais</h2>
              </div>

              <div className={styles.gruposCampos}>
                {/* Nome da Obra */}
                <div className={styles.campoBlocoInteiro}>
                  <label className={styles.rotulo}>NOME DA OBRA</label>
                  <div className={styles.caixaCampo}>
                    <input
                      type="text"
                      placeholder="Ex: Residencial Mirante do Sol"
                      value={nomeObra}
                      onChange={(e) => {
                        setNomeObra(e.target.value);
                        if (errors.nomeObra)
                          setErrors((prev) => ({ ...prev, nomeObra: undefined }));
                      }}
                      className={styles.campo}
                      disabled={isLoading}
                    />
                  </div>
                  {errors.nomeObra && (
                    <span className={styles.errorText}>{errors.nomeObra}</span>
                  )}
                </div>

                {/* Status e Responsável */}
                <div className={styles.linhaGrupos}>
                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>STATUS</label>
                    <div className={styles.selectWrapper}>
                      <select
                        value={status}
                        onChange={(e) => setStatus(e.target.value)}
                        className={styles.select}
                        disabled={isLoading}
                      >
                        {STATUS_OPCOES.map((opt) => (
                          <option key={opt} value={opt}>
                            {opt}
                          </option>
                        ))}
                      </select>
                      <ChevronDown size={20} className={styles.iconSelect} />
                    </div>
                    {errors.status && (
                      <span className={styles.errorText}>{errors.status}</span>
                    )}
                  </div>

                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>RESPONSÁVEL</label>
                    <div className={styles.caixaCampo}>
                      <User size={18} className={styles.icone} />
                      <input
                        type="text"
                        placeholder="Ex: Rafael Pereira"
                        value={responsavel}
                        onChange={(e) => {
                          setResponsavel(e.target.value);
                          if (errors.responsavel)
                            setErrors((prev) => ({
                              ...prev,
                              responsavel: undefined,
                            }));
                        }}
                        className={styles.campo}
                        disabled={isLoading}
                      />
                    </div>
                    {errors.responsavel && (
                      <span className={styles.errorText}>
                        {errors.responsavel}
                      </span>
                    )}
                  </div>
                </div>

                {/* Datas */}
                <div className={styles.linhaGrupos}>
                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>DATA DE INÍCIO</label>
                    <div className={styles.caixaCampo}>
                      <Calendar size={18} className={styles.icone} />
                      <input
                        type="date"
                        value={dataInicio}
                        onChange={(e) => {
                          setDataInicio(e.target.value);
                          if (errors.dataInicio)
                            setErrors((prev) => ({
                              ...prev,
                              dataInicio: undefined,
                            }));
                        }}
                        className={styles.campo}
                        disabled={isLoading}
                      />
                    </div>
                    {errors.dataInicio && (
                      <span className={styles.errorText}>
                        {errors.dataInicio}
                      </span>
                    )}
                  </div>

                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>DATA DE TÉRMINO</label>
                    <div className={styles.caixaCampo}>
                      <Calendar size={18} className={styles.icone} />
                      <input
                        type="date"
                        value={dataFim}
                        onChange={(e) => {
                          setDataFim(e.target.value);
                          if (errors.dataFim)
                            setErrors((prev) => ({
                              ...prev,
                              dataFim: undefined,
                            }));
                        }}
                        className={styles.campo}
                        disabled={isLoading}
                      />
                    </div>
                    {errors.dataFim && (
                      <span className={styles.errorText}>{errors.dataFim}</span>
                    )}
                  </div>
                </div>
              </div>
            </div>

            {/* Seção: Localização */}
            <div className={styles.secao}>
              <div className={styles.secaoHeader}>
                <MapPin size={20} className={styles.iconeSecao} />
                <h2 className={styles.secaoTitulo}>Localização</h2>
              </div>

              <div className={styles.gruposCampos}>
                {/* Endereço completo */}
                <div className={styles.campoBlocoInteiro}>
                  <label className={styles.rotulo}>ENDEREÇO COMPLETO</label>
                  <textarea
                    placeholder="Rua, Número, Bairro, Complemento"
                    value={endereco}
                    onChange={(e) => {
                      setEndereco(e.target.value);
                      if (errors.endereco)
                        setErrors((prev) => ({ ...prev, endereco: undefined }));
                    }}
                    className={styles.textarea}
                    disabled={isLoading}
                  />
                  {errors.endereco && (
                    <span className={styles.errorText}>{errors.endereco}</span>
                  )}
                </div>

                {/* Cidade e Estado */}
                <div className={styles.linhaGrupos}>
                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>CIDADE</label>
                    <div className={styles.caixaCampo}>
                      <input
                        type="text"
                        placeholder="Ex: São Paulo"
                        value={cidade}
                        onChange={(e) => {
                          setCidade(e.target.value);
                          if (errors.cidade)
                            setErrors((prev) => ({
                              ...prev,
                              cidade: undefined,
                            }));
                        }}
                        className={styles.campo}
                        disabled={isLoading}
                      />
                    </div>
                    {errors.cidade && (
                      <span className={styles.errorText}>{errors.cidade}</span>
                    )}
                  </div>

                  <div className={styles.grupoCampo}>
                    <label className={styles.rotulo}>ESTADO</label>
                    <div className={styles.selectWrapper}>
                      <select
                        value={estado}
                        onChange={(e) => {
                          setEstado(e.target.value);
                          if (errors.estado)
                            setErrors((prev) => ({ ...prev, estado: undefined }));
                        }}
                        className={styles.select}
                        disabled={isLoading}
                      >
                        <option value="">Selecione um estado</option>
                        {ESTADOS.map((uf) => (
                          <option key={uf} value={uf}>
                            {uf}
                          </option>
                        ))}
                      </select>
                      <ChevronDown size={20} className={styles.iconSelect} />
                    </div>
                    {errors.estado && (
                      <span className={styles.errorText}>{errors.estado}</span>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Coluna direita - Cards auxiliares */}
          <div className={styles.colunaDireita}>
            {/* Card: Financeiro */}
            <div className={styles.card}>
              <div className={styles.cardHeader}>
                <DollarSign size={20} className={styles.iconeCard} />
                <h3 className={styles.cardTitulo}>Financeiro</h3>
              </div>

              <div className={styles.cardBody}>
                <label className={styles.rotulo}>ORÇAMENTO INICIAL</label>
                <div className={styles.caixaCampo}>
                  <DollarSign size={18} className={styles.icone} />
                  <input
                    type="number"
                    placeholder="0.000,00"
                    value={orcamento}
                    onChange={(e) => {
                      setOrcamento(e.target.value);
                      if (errors.orcamento)
                        setErrors((prev) => ({
                          ...prev,
                          orcamento: undefined,
                        }));
                    }}
                    step="0.01"
                    min="0"
                    className={styles.campo}
                    disabled={isLoading}
                  />
                </div>
                {errors.orcamento && (
                  <span className={styles.errorText}>{errors.orcamento}</span>
                )}
                <p className={styles.infoTexto}>
                  Este valor servirá como base para o controle de<br />
                  gastos e projeção de lucros da obra.
                </p>
              </div>
            </div>

            {/* Card: Documentação */}
            <div className={styles.card}>
              <div className={styles.cardHeader}>
                <Upload size={20} className={styles.iconeCard} />
                <h3 className={styles.cardTitulo}>Documentação</h3>
              </div>

              <div className={styles.cardBody}>
                <label className={styles.rotulo}>
                  UPLOAD DE ARQUIVOS
                </label>
                <label className={styles.areaUpload}>
                  <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept="image/*,.pdf"
                    onChange={handleArquivoUpload}
                    disabled={isLoading || arquivos.length >= 3}
                    className={styles.inputArquivo}
                  />
                  <div className={styles.uploadConteudo}>
                    <Upload size={32} className={styles.uploadIcone} />
                    <p className={styles.uploadTexto}>
                      Upload de Arquivos
                    </p>
                    <p className={styles.uploadSubtexto}>
                      Arraste fotos ou projetos<br />
                      (.pdf, .jpg, .png)
                    </p>
                    <button
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className={styles.botaoSelecionarArquivos}
                      disabled={isLoading || arquivos.length >= 3}
                    >
                      Selecionar Arquivos
                    </button>
                  </div>
                </label>

                {errors.arquivos && (
                  <span className={styles.errorText}>{errors.arquivos}</span>
                )}

                {/* Lista de arquivos */}
                {arquivos.length > 0 && (
                  <div className={styles.listaArquivos}>
                    {arquivos.map((arq) => (
                      <div key={arq.id} className={styles.itemArquivo}>
                        <div className={styles.infoArquivo}>
                          <FileText size={16} />
                          <div>
                            <p className={styles.nomeArquivo}>{arq.nome}</p>
                            <p className={styles.tamanhoArquivo}>
                              {formatarTamanhoArquivo(arq.tamanho)}
                            </p>
                          </div>
                        </div>
                        <button
                          type="button"
                          onClick={() => removeArquivo(arq.id)}
                          className={styles.botaoRemover}
                          disabled={isLoading}
                        >
                          <X size={16} />
                        </button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* Card: Botões de Ação */}
            <div className={styles.card}>
              <button
                type="submit"
                className={styles.botaoCriar}
                disabled={isLoading}
              >
                {isLoading ? "Criando..." : "Criar Obra"}
              </button>
              <button
                type="button"
                onClick={() => navigate(-1)}
                className={styles.botaoCancelar}
                disabled={isLoading}
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      </form>
    </div>
  );
}
