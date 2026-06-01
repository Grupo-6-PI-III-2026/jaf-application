import { useRef, useState } from "react";
import { useUser } from "../../Context/UserContext";
import { funcionarioService } from "../../Service/Funcionarios/funcionarioService";
import { authService } from "../../Service/Auth/Login/authService";
import {
  CargoLabel,
  DEFAULT_AVATAR_URL,
  type AlterarSenhaDto,
} from "../../Types/user";
import styles from "./Profile.module.css";

const IconEdit = () => (
  <svg
    width="14"
    height="14"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2.5"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
  </svg>
);

const IconLock = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
    <path d="M7 11V7a5 5 0 0 1 10 0v4" />
  </svg>
);

const IconUser = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
    <circle cx="12" cy="7" r="4" />
  </svg>
);

const IconTrash = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <polyline points="3 6 5 6 21 6" />
    <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
    <path d="M10 11v6M14 11v6" />
    <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
  </svg>
);

const IconMail = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z" />
    <polyline points="22,6 12,13 2,6" />
  </svg>
);

const IconBriefcase = () => (
  <svg
    width="16"
    height="16"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
  >
    <rect x="2" y="7" width="20" height="14" rx="2" ry="2" />
    <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16" />
  </svg>
);

const IconShield = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
    <path d="M12 1L3 5v6c0 5.25 3.75 10.15 9 11.35C17.25 21.15 21 16.25 21 11V5l-9-4z" />
  </svg>
);

type Tab = "info" | "seguranca" | "excluir";

export default function Profile() {
  const { user, isLoading, refreshUser, clearUser, error } = useUser();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [activeTab, setActiveTab] = useState<Tab>("info");
  const [isEditing, setIsEditing] = useState(false);
  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [fotoPreview, setFotoPreview] = useState<string | null>(null);
  const [fotoFile, setFotoFile] = useState<File | null>(null);
  const [isSaving, setIsSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmacaoSenha, setConfirmacaoSenha] = useState("");
  const [senhaError, setSenhaError] = useState<string | null>(null);
  const [senhaSuccess, setSenhaSuccess] = useState(false);
  const [isSavingSenha, setIsSavingSenha] = useState(false);

  const [confirmacaoExclusao, setConfirmacaoExclusao] = useState("");
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleStartEdit = () => {
    if (!user) return;
    setNome(user.nome);
    setEmail(user.email);
    setFotoPreview(null);
    setFotoFile(null);
    setSaveError(null);
    setSaveSuccess(false);
    setIsEditing(true);
  };

  const handleCancelEdit = () => {
    setIsEditing(false);
    setSaveError(null);
  };

  const handleFotoClick = () => {
    if (isEditing) fileInputRef.current?.click();
  };

  const handleFotoChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFotoFile(file);
    const reader = new FileReader();
    reader.onload = (ev) => setFotoPreview(ev.target?.result as string);
    reader.readAsDataURL(file);
  };

  const handleSaveProfile = async () => {
    if (!user) return;
    setIsSaving(true);
    setSaveError(null);
    setSaveSuccess(false);

    try {
      let fotoUrl: string | null = user.fotoUrl;
      const emailChanged =
        email.trim().toLowerCase() !== user.email.toLowerCase();

      if (fotoFile) {
        fotoUrl = await funcionarioService.uploadFoto(fotoFile);
      }

      await funcionarioService.atualizarPerfil({ nome, email, fotoUrl });
      if (emailChanged) {
        clearUser();
        authService.logout();
        return;
      }

      await refreshUser();

      setSaveSuccess(true);
      setIsEditing(false);
    } catch (err: unknown) {
      const message =
        err instanceof Error ? err.message : "Erro ao salvar. Tente novamente.";
      setSaveError(message);
    } finally {
      setIsSaving(false);
    }
  };

  const handleAlterarSenha = async () => {
    setSenhaError(null);
    setSenhaSuccess(false);

    if (novaSenha !== confirmacaoSenha) {
      setSenhaError("Nova senha e confirmacao nao conferem.");
      return;
    }
    if (novaSenha.length < 6) {
      setSenhaError("A nova senha deve ter pelo menos 6 caracteres.");
      return;
    }

    setIsSavingSenha(true);
    try {
      const dto: AlterarSenhaDto = { senhaAtual, novaSenha, confirmacaoSenha };
      await funcionarioService.alterarSenha(dto);
      setSenhaSuccess(true);
      setSenhaAtual("");
      setNovaSenha("");
      setConfirmacaoSenha("");
    } catch {
      setSenhaError("Senha atual incorreta ou erro no servidor.");
    } finally {
      setIsSavingSenha(false);
    }
  };

  const handleExcluirConta = async () => {
    if (confirmacaoExclusao !== "EXCLUIR") {
      setDeleteError('Digite "EXCLUIR" para confirmar.');
      return;
    }
    setIsDeleting(true);
    setDeleteError(null);
    try {
      await funcionarioService.excluirConta();
      clearUser();
      authService.logout();
    } catch {
      setDeleteError("Erro ao excluir a conta. Tente novamente.");
    } finally {
      setIsDeleting(false);
    }
  };

  const avatarSrc = fotoPreview ?? user?.fotoUrl ?? DEFAULT_AVATAR_URL;

  if (isLoading) {
    return (
      <div className={styles.loadingWrapper}>
        <div className={styles.spinner} />
        <span>Carregando perfil...</span>
      </div>
    );
  }

  if (!user) {
    return (
      <div className={styles.loadingWrapper}>
        <div className={styles.alertError}>
          {error ?? "Nao foi possivel carregar o perfil."}
        </div>
        <button className={styles.saveBtn} onClick={refreshUser}>
          Tentar novamente
        </button>
      </div>
    );
  }

  return (
    <div className={styles.pageWrapper}>
    
      <h1 className={styles.pageTitle}>Configuracoes da Conta</h1>
      <p className={styles.pageSubtitle}>
        Gerencie suas informacoes pessoais e seguranca da conta.
      </p>

      <div className={styles.layout}>
        <aside className={styles.sideCard}>
          <div className={styles.avatarSection}>
            <div
              className={`${styles.avatarWrapper} ${
                isEditing ? styles.avatarEditable : ""
              }`}
              onClick={handleFotoClick}
              role={isEditing ? "button" : undefined}
              tabIndex={isEditing ? 0 : undefined}
              onKeyDown={
                isEditing
                  ? (e) => e.key === "Enter" && handleFotoClick()
                  : undefined
              }
              aria-label={isEditing ? "Alterar foto de perfil" : undefined}
            >
              {avatarSrc ? (
                <img
                  src={avatarSrc}
                  alt={user.nome}
                  className={styles.avatarImg}
                />
              ) : (
                <div className={styles.avatarInitials}>
                  {user.nome.charAt(0).toUpperCase()}
                </div>
              )}
              {isEditing && (
                <div className={styles.avatarEditOverlay}>
                  <IconEdit />
                </div>
              )}
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className={styles.hiddenInput}
              onChange={handleFotoChange}
            />
            <h2 className={styles.userName}>{user.nome}</h2>
            <p className={styles.userCargo}>{CargoLabel[user.cargo]}</p>
          </div>

          <div className={styles.divider} />

          <nav className={styles.tabNav}>
            <button
              className={`${styles.tabBtn} ${
                activeTab === "info" ? styles.tabBtnActive : ""
              }`}
              onClick={() => {
                setActiveTab("info");
                setIsEditing(false);
              }}
            >
              <IconUser />
              Informacoes pessoais
            </button>
            <button
              className={`${styles.tabBtn} ${
                activeTab === "seguranca" ? styles.tabBtnActive : ""
              }`}
              onClick={() => {
                setActiveTab("seguranca");
                setIsEditing(false);
              }}
            >
              <IconLock />
              Seguranca
            </button>
          </nav>

          <div className={styles.divider} />

          <button
            className={styles.deleteAccountBtn}
            onClick={() => {
              setActiveTab("excluir");
              setIsEditing(false);
            }}
          >
            <IconTrash />
            Excluir conta
          </button>
        </aside>

        <main className={styles.mainCard}>
          {activeTab === "info" && (
            <>
              <div className={styles.cardHeader}>
                <h3 className={styles.cardTitle}>Dados Pessoais</h3>
                {!isEditing ? (
                  <button className={styles.editBtn} onClick={handleStartEdit}>
                    <IconEdit />
                    Editar dados
                  </button>
                ) : (
                  <div className={styles.editActions}>
                    <button
                      className={styles.cancelBtn}
                      onClick={handleCancelEdit}
                      disabled={isSaving}
                    >
                      Cancelar
                    </button>
                    <button
                      className={styles.saveBtn}
                      onClick={handleSaveProfile}
                      disabled={isSaving}
                    >
                      {isSaving ? "Salvando..." : "Salvar"}
                    </button>
                  </div>
                )}
              </div>

              {saveError && (
                <div className={styles.alertError}>{saveError}</div>
              )}
              {saveSuccess && (
                <div className={styles.alertSuccess}>
                  Perfil atualizado com sucesso!
                </div>
              )}

              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>NOME</label>
                {isEditing ? (
                  <input
                    type="text"
                    className={styles.fieldInput}
                    value={nome}
                    onChange={(e) => setNome(e.target.value)}
                    autoFocus
                  />
                ) : (
                  <div className={styles.fieldReadonly}>{user.nome}</div>
                )}
              </div>

              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>EMAIL</label>
                {isEditing ? (
                  <input
                    type="email"
                    className={styles.fieldInput}
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                  />
                ) : (
                  <div className={styles.fieldReadonly}>
                    <IconMail />
                    {user.email}
                  </div>
                )}
              </div>

              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>CARGO</label>
                <div
                  className={`${styles.fieldReadonly} ${styles.fieldDisabled}`}
                >
                  <IconBriefcase />
                  {CargoLabel[user.cargo]}
                </div>
              </div>
            </>
          )}

          {activeTab === "seguranca" && (
            <>
              <div className={styles.cardHeader}>
                <h3 className={styles.cardTitle}>Alterar Senha</h3>
              </div>

              {senhaError && (
                <div className={styles.alertError}>{senhaError}</div>
              )}
              {senhaSuccess && (
                <div className={styles.alertSuccess}>
                  Senha alterada com sucesso!
                </div>
              )}

              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>SENHA ATUAL</label>
                <input
                  type="password"
                  className={styles.fieldInput}
                  value={senhaAtual}
                  onChange={(e) => setSenhaAtual(e.target.value)}
                  placeholder="••••••••"
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>NOVA SENHA</label>
                <input
                  type="password"
                  className={styles.fieldInput}
                  value={novaSenha}
                  onChange={(e) => setNovaSenha(e.target.value)}
                  placeholder="Minimo 6 caracteres"
                />
              </div>
              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>
                  CONFIRMAR NOVA SENHA
                </label>
                <input
                  type="password"
                  className={styles.fieldInput}
                  value={confirmacaoSenha}
                  onChange={(e) => setConfirmacaoSenha(e.target.value)}
                  placeholder="Repita a nova senha"
                />
              </div>

              <button
                className={styles.saveBtn}
                onClick={handleAlterarSenha}
                disabled={isSavingSenha}
                style={{ marginTop: "8px" }}
              >
                {isSavingSenha ? "Salvando..." : "Alterar senha"}
              </button>
            </>
          )}

          {activeTab === "excluir" && (
            <>
              <div className={styles.cardHeader}>
                <h3 className={`${styles.cardTitle} ${styles.dangerTitle}`}>
                  Excluir Conta
                </h3>
              </div>

              <div className={styles.dangerBox}>
                <p className={styles.dangerText}>
                  Esta acao e <strong>irreversivel</strong>. Todos os seus dados
                  serao permanentemente removidos. Para confirmar, digite
                  <strong> EXCLUIR</strong> no campo abaixo.
                </p>
              </div>

              {deleteError && (
                <div className={styles.alertError}>{deleteError}</div>
              )}

              <div className={styles.formGroup}>
                <label className={styles.fieldLabel}>CONFIRMACAO</label>
                <input
                  type="text"
                  className={`${styles.fieldInput} ${styles.dangerInput}`}
                  value={confirmacaoExclusao}
                  onChange={(e) => setConfirmacaoExclusao(e.target.value)}
                  placeholder='Digite "EXCLUIR" para confirmar'
                />
              </div>

              <button
                className={styles.deleteFinalBtn}
                onClick={handleExcluirConta}
                disabled={isDeleting || confirmacaoExclusao !== "EXCLUIR"}
              >
                {isDeleting ? "Excluindo..." : "Confirmar exclusao"}
              </button>
            </>
          )}
        </main>
      </div>

      <div className={styles.privacyBanner}>
        <span className={styles.privacyIcon}>
          <IconShield />
        </span>
        <div>
          <p className={styles.privacyTitle}>
            Sua privacidade e nossa prioridade
          </p>
          <p className={styles.privacyText}>
            Os dados acima sao utilizados exclusivamente para gestao interna e
            conformidade com as politicas de seguranca da JAF Construtora.
          </p>
        </div>
      </div>
    </div>
  );
}
