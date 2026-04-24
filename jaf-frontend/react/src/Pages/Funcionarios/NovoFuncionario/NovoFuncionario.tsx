import { useState } from "react";
import {
  User,
  Mail,
  Lock,
  Eye,
  EyeOff,
  ChevronDown,
  UserPlus,
  ShieldCheck,
  Network,
  History,
  Info,
} from "lucide-react";
import styles from "./NovoFuncionario.module.css";
import {
  funcionarioService
} from "../../../Service/Funcionarios/funcionarioService";

import {type CargoApi } from "../../../Types/Register"

interface ErrosFormulario {
  nome?: string;
  email?: string;
  senha?: string;
  cargo?: string;
}

export default function NovoFuncionario() {
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [cargo, setCargo] = useState<string>("");
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const [errors, setErrors] = useState<ErrosFormulario>({});

  const [isLoading, setIsLoading] = useState(false);

  const opcoesCargo: { label: string; value: CargoApi }[] = [
    { label: "Administrador do Sistema", value: "ADMIN" },
    { label: "Gestor de Obra", value: "GESTOR_OBRA" },
    { label: "Operador de Lançamento", value: "OPERADOR_LANCAMENTO" },
  ];

  const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

  const validateInputs = () => {
    const nextErrors: ErrosFormulario = {};

    if (!nome.trim()) {
      nextErrors.nome = "O nome completo é obrigatório.";
    }

    if (!emailRegex.test(email)) {
      nextErrors.email = "Use um formato de e-mail corporativo válido.";
    }

    const hasMinLength = senha.length >= 8;
    const hasUpper = /[A-Z]/.test(senha);
    const hasLower = /[a-z]/.test(senha);
    const hasSpecial = /[^A-Za-z0-9]/.test(senha);

    if (!hasMinLength || !hasUpper || !hasLower || !hasSpecial) {
      nextErrors.senha =
        "A senha precisa de 8 caracteres, 1 maiúscula, 1 minúscula e 1 especial.";
    }

    if (!cargo) {
      nextErrors.cargo = "Selecione uma função para o funcionário.";
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  };

  const handleCadastrar = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!validateInputs()) {
      return;
    }

    setIsLoading(true);

    try {
      const response = await funcionarioService.cadastrar({
        nome,
        email,
        senha,
        cargo: cargo as CargoApi,
      });

      console.log("Sucesso:", response);
      alert("Funcionário cadastrado com sucesso!");

      setNome("");
      setEmail("");
      setSenha("");
      setCargo("");
    } catch (error) {
      console.error("Erro na API:", error);
      alert(
        "Falha ao cadastrar funcionário. Verifique os dados e tente novamente.",
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className={styles.pagina}>
      <div className={styles.navegacao}>
        <span>FUNCIONÁRIOS</span>
        <span className={styles.separador}>&gt;</span>
        <span className={styles.ativo}>NOVO FUNCIONÁRIO</span>
      </div>

      <h1 className={styles.titulo}>Novo Funcionário</h1>
      <p className={styles.subtitulo}>
        Cadastre um novo membro para a equipe da JAF Construtora. Certifique-se
        de preencher todos os dados corporativos corretamente.
      </p>

      <form className={styles.formulario} onSubmit={handleCadastrar}>
        <div className={styles.formularioGrid}>
          <div className={styles.coluna}>
            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>NOME COMPLETO</label>
              <div className={styles.caixaCampo}>
                <User size={18} className={styles.icone} />
                <input
                  type="text"
                  placeholder="Ex: João da Silva"
                  value={nome}
                  onChange={(e) => {
                    setNome(e.target.value);
                    if (errors.nome)
                      setErrors((prev) => ({ ...prev, nome: undefined }));
                  }}
                  className={styles.campo}
                  disabled={isLoading}
                />
              </div>
              {errors.nome && (
                <span
                  className={styles.errorText}
                  style={{
                    color: "red",
                    fontSize: "12px",
                    marginTop: "4px",
                    display: "block",
                  }}
                >
                  {errors.nome}
                </span>
              )}
            </div>

            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>E-MAIL CORPORATIVO</label>
              <div className={styles.caixaCampo}>
                <Mail size={18} className={styles.icone} />
                <input
                  type="email"
                  placeholder="nome@jafconstrutora.com.br"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (errors.email)
                      setErrors((prev) => ({ ...prev, email: undefined }));
                  }}
                  className={styles.campo}
                  disabled={isLoading}
                />
              </div>
              {errors.email && (
                <span
                  className={styles.errorText}
                  style={{
                    color: "red",
                    fontSize: "12px",
                    marginTop: "4px",
                    display: "block",
                  }}
                >
                  {errors.email}
                </span>
              )}
            </div>

            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>SENHA DE ACESSO</label>
              <div className={styles.caixaCampo}>
                <Lock size={18} className={styles.icone} />
                <input
                  type={mostrarSenha ? "text" : "password"}
                  placeholder="••••••••"
                  value={senha}
                  onChange={(e) => {
                    setSenha(e.target.value);
                    if (errors.senha)
                      setErrors((prev) => ({ ...prev, senha: undefined }));
                  }}
                  className={styles.campo}
                  disabled={isLoading}
                />
                <button
                  type="button"
                  onClick={() => setMostrarSenha(!mostrarSenha)}
                  className={styles.botaoOlho}
                  disabled={isLoading}
                >
                  {mostrarSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              {errors.senha ? (
                <span
                  className={styles.errorText}
                  style={{
                    color: "red",
                    fontSize: "12px",
                    marginTop: "4px",
                    display: "block",
                  }}
                >
                  {errors.senha}
                </span>
              ) : (
                <span className={styles.dica}>
                  Mínimo de 8 caracteres, incluindo letras e números.
                </span>
              )}
            </div>
          </div>

          <div className={styles.coluna}>
            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>CARGO / FUNÇÃO</label>
              <div className={styles.caixaCampo}>
                <User size={18} className={styles.icone} />
                <select
                  value={cargo}
                  onChange={(e) => {
                    setCargo(e.target.value);
                    if (errors.cargo)
                      setErrors((prev) => ({ ...prev, cargo: undefined }));
                  }}
                  className={styles.seletor}
                  disabled={isLoading}
                >
                  <option value="">Selecione uma função</option>
                  {opcoesCargo.map((opcao) => (
                    <option key={opcao.value} value={opcao.value}>
                      {opcao.label}
                    </option>
                  ))}
                </select>
                <ChevronDown size={18} className={styles.iconeSeletor} />
              </div>
              {errors.cargo && (
                <span
                  className={styles.errorText}
                  style={{
                    color: "red",
                    fontSize: "12px",
                    marginTop: "4px",
                    display: "block",
                  }}
                >
                  {errors.cargo}
                </span>
              )}
            </div>

            <div className={styles.cardInfo}>
              <div className={styles.cardInfoTopo}>
                <Info size={18} />
                <span>Acesso ao Sistema</span>
              </div>
              <p className={styles.cardInfoTexto}>
                O funcionário receberá um convite por e-mail para validar seu
                acesso e definir suas preferências de segurança após o cadastro.
              </p>
            </div>
            <button
              type="submit"
              className={styles.botaoCadastrar}
              disabled={isLoading}
              style={{
                opacity: isLoading ? 0.7 : 1,
                cursor: isLoading ? "not-allowed" : "pointer",
              }}
            >
              <UserPlus size={18} />
              {isLoading ? "Cadastrando..." : "Cadastrar Funcionário"}
            </button>

            <button
              type="button"
              className={styles.botaoCancelar}
              disabled={isLoading}
            >
              CANCELAR OPERAÇÃO
            </button>
          </div>
        </div>
      </form>

      {/* RODAPÉ MANTIDO */}
      <div className={styles.cardsRodape}>
        <div className={styles.cardRodape}>
          <ShieldCheck size={24} className={styles.cardRodapeIcone} />
          <h3>Segurança de Dados</h3>
          <p>Todos os perfis são criptografados e seguem as normas da LGPD.</p>
        </div>
        <div className={styles.cardRodape}>
          <Network size={24} className={styles.cardRodapeIcone} />
          <h3>Hierarquia de Obras</h3>
          <p>Defina permissões específicas para cada canteiro de obras.</p>
        </div>
        <div className={styles.cardRodape}>
          <History size={24} className={styles.cardRodapeIcone} />
          <h3>Log de Atividades</h3>
          <p>Acompanhe as ações realizadas pelo novo usuário em tempo real.</p>
        </div>
      </div>
    </div>
  );
}
