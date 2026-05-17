import { useState } from "react";
import "./Permissions.css";

type Permission = {
  id: string;
  label: string;
  description: string;
  badge: "restrito" | "critico" | null;
  defaultChecked: boolean;
};

const permissions: Permission[] = [
  {
    id: "view",
    label: "Visualizar Lista de Obras",
    description:
      "Permite ver o painel geral e o status resumido de todas as obras ativas.",
    badge: null,
    defaultChecked: true,
  },
  {
    id: "create",
    label: "Criar Nova Obra",
    description: "Permite cadastrar novos empreendimentos no sistema.",
    badge: null,
    defaultChecked: true,
  },
  {
    id: "edit",
    label: "Editar Detalhes da Obra",
    description: "Modificar cronograma, responsáveis e escopo do projeto.",
    badge: null,
    defaultChecked: true,
  },
  {
    id: "financial",
    label: "Acessar Dados Financeiros da Obra",
    description:
      "Visualizar orçamentos, custos alocados e notas fiscais da obra específica.",
    badge: "restrito",
    defaultChecked: false,
  },
  {
    id: "delete",
    label: "Excluir Obra",
    description:
      "Remover permanentemente registros de obras do banco de dados.",
    badge: "critico",
    defaultChecked: false,
  },
];

export default function UserPermissions() {
  const [checked, setChecked] = useState<Record<string, boolean>>(() =>
    Object.fromEntries(permissions.map((p) => [p.id, p.defaultChecked])),
  );

  const allChecked = permissions.every((p) => checked[p.id]);

  function toggleAll() {
    const next = !allChecked;
    setChecked(Object.fromEntries(permissions.map((p) => [p.id, next])));
  }

  function toggle(id: string) {
    setChecked((prev) => ({ ...prev, [id]: !prev[id] }));
  }

  return (
    <div className="up-page">
      <h1 className="up-title">Gestão de Permissões do Usuário</h1>
      <p className="up-subtitle">
        Configure as permissões de acesso ao sistema para este colaborador.
        Certifique-se de alinhar os acessos com as responsabilidades do cargo.
      </p>

      <div className="up-layout">
        <aside className="up-sidebar">
          <div className="up-profile-card">
            <p className="up-name">Guilherme Peres</p>
            <p className="up-role">Gerente de Projetos</p>

            <ul className="up-info">
              <li>
                <span className="up-icon"></span>
                Guilherme Peres
              </li>
              <li>
                <span className="up-icon"></span> Matriz – São Paulo
              </li>
              <li>
                <span className="up-icon"></span> ID: JAF-2023-045
              </li>
            </ul>
          </div>

          <div className="up-impact-card">
            <p className="up-impact-title"> Impacto das Permissões</p>
            <p className="up-impact-text">
              Alterações salvas entrarão em vigor no próximo login do
              colaborador. Recomendamos notificar o colaborador sobre novos
              acessos concedidos.
            </p>
          </div>
        </aside>

        <section className="up-access-card">
          <div className="up-access-header">
            <div>
              <p className="up-access-title"> Configurações de Acesso</p>
              <p className="up-access-desc">
                Defina níveis de leitura, escrita e exclusão por módulo.
              </p>
            </div>
          </div>

          <div className="up-module">
            <div className="up-module-header">
              <span className="up-module-icon"></span>
              <span className="up-module-name">Módulo: Obras</span>
              <label className="up-select-all">
                <span>Selecionar Tudo</span>
                <input
                  type="checkbox"
                  checked={allChecked}
                  onChange={toggleAll}
                />
              </label>
            </div>

            <ul className="up-perm-list">
              {permissions.map((p) => (
                <li key={p.id} className="up-perm-item">
                  <label className="up-perm-label">
                    <input
                      type="checkbox"
                      checked={checked[p.id]}
                      onChange={() => toggle(p.id)}
                      className="up-checkbox"
                    />
                    <div className="up-perm-info">
                      <span className="up-perm-name">{p.label}</span>
                      <span className="up-perm-desc">{p.description}</span>
                    </div>
                    {p.badge && (
                      <span className={`up-badge up-badge--${p.badge}`}>
                        {p.badge === "restrito" ? "RESTRITO" : "CRÍTICO"}
                      </span>
                    )}
                  </label>
                </li>
              ))}
            </ul>
          </div>

          <div className="up-actions">
            <button className="up-btn-cancel">Cancelar</button>
            <button className="up-btn-save">Salvar Alterações</button>
          </div>
        </section>
      </div>
    </div>
  );
}
