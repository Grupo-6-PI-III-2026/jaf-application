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

export default function NovoFuncionario() {

  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [cargo, setCargo] = useState("");
  const [mostrarSenha, setMostrarSenha] = useState(false);

  const cargos = [
    "Administrador",
    "Gerente de Projetos",
    "Engenheiro",
    "Mestre de Obras",
    "Auxiliar Administrativo",
  ];

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

      <form className={styles.formulario}>
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
                  onChange={(informacao) => setNome(informacao.target.value)}
                  className={styles.campo}
                />
              </div>
            </div>

            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>E-MAIL CORPORATIVO</label>
              <div className={styles.caixaCampo}>
                <Mail size={18} className={styles.icone} />
                <input
                  type="email"
                  placeholder="nome@jafconstrutora.com.br"
                  value={email}
                  onChange={(informacao) => setEmail(informacao.target.value)}
                  className={styles.campo}
                />
              </div>
            </div>

            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>SENHA DE ACESSO</label>
              <div className={styles.caixaCampo}>
                <Lock size={18} className={styles.icone} />
                <input
                  type={mostrarSenha ? "text" : "password"}
                  placeholder="••••••••"
                  value={senha}
                  onChange={(informacao) => setSenha(informacao.target.value)}
                  className={styles.campo}
                />
                <button
                  type="button"
                  onClick={() => setMostrarSenha(!mostrarSenha)}
                  className={styles.botaoOlho}
                >
                  {mostrarSenha ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
              <span className={styles.dica}>
                Mínimo de 8 caracteres, incluindo letras e números.
              </span>
            </div>
          </div>

          <div className={styles.coluna}>
            <div className={styles.grupoCampo}>
              <label className={styles.rotulo}>CARGO / FUNÇÃO</label>
              <div className={styles.caixaCampo}>
                <User size={18} className={styles.icone} />
                <select
                  value={cargo}
                  onChange={(informacao) => setCargo(informacao.target.value)}
                  className={styles.seletor}
                >
                  <option value="">Selecione uma função</option>
                  {cargos.map((cargo) => (
                    <option key={cargo} value={cargo}>
                      {cargo}
                    </option>
                  ))}
                </select>
                <ChevronDown size={18} className={styles.iconeSeletor} />
              </div>
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

            <button className={styles.botaoCadastrar}>
              <UserPlus size={18} />
              Cadastrar Funcionário
            </button>

            <button className={styles.botaoCancelar}>
              CANCELAR OPERAÇÃO
            </button>
          </div>
        </div>
      </form>

      <div className={styles.cardsRodape}>
        <div className={styles.cardRodape}>
          <ShieldCheck size={24} className={styles.cardRodapeIcone} />
          <h3>Segurança de Dados</h3>
          <p>
            Todos os perfis são criptografados e seguem as normas da LGPD para
            proteção de dados corporativos.
          </p>
        </div>

        <div className={styles.cardRodape}>
          <Network size={24} className={styles.cardRodapeIcone} />
          <h3>Hierarquia de Obras</h3>
          <p>
            Defina permissões específicas para cada canteiro de obras através do
            painel de controle.
          </p>
        </div>

        <div className={styles.cardRodape}>
          <History size={24} className={styles.cardRodapeIcone} />
          <h3>Log de Atividades</h3>
          <p>
            Acompanhe as ações realizadas pelo novo usuário em tempo real para
            auditoria de processos.
          </p>
        </div>
      </div>
    </div>
  );
}