import { lazy, Suspense, useEffect, useRef } from "react";
import { BrowserRouter, Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import { toast } from "sonner";
import { EVENTO_SESSAO_ALTERADA, EVENTO_SESSAO_EXPIRADA } from "./Service/Auth/Login/Api/Api";
import Login from "./Pages/Auth/Login/Login";
import Layout from "./Components/Layout/Layout";
const NovoFuncionario = lazy(() => import("./Pages/Funcionarios/NovoFuncionario/NovoFuncionario"));
const DetalhamentoObras = lazy(() => import("./Pages/Obras/DetalhamentoObras"));
const NovaObra = lazy(() => import("./Pages/Obras/NovaObra/NovaObra"));
import Home from "./Pages/Home/Home";
const Permissoes = lazy(() => import("./Pages/Permissoes/Permissoes"));
import PrivateRoute from "./Components/PrivateRoute/PrivateRoute";
const Profile = lazy(() => import("./Pages/Profile/Profile"));
const AlocacaoFuncionario = lazy(() => import("./Pages/Alocacoes/AlocacaoFuncionario"));
const Dashboard = lazy(() => import("./Pages/Dashboard/Dashboard"));
const GestaoPresenca = lazy(() => import("./Pages/ControlePresenca/GestaoPresenca"));

// Sessão expirada ou alterada em outra aba: avisa e volta ao login sem recarregar a página.
function EventosDeSessao() {
  const navigate = useNavigate();
  const location = useLocation();
  const localAtual = useRef(location);

  useEffect(() => { localAtual.current = location; }, [location]);

  useEffect(() => {
    const aoExpirar = () => {
      if (localAtual.current.pathname === "/") return;
      toast.error("Sua sessão expirou. Entre novamente.", { id: "sessao-expirada" });
      navigate("/", { replace: true, state: { de: localAtual.current, motivo: "sessao-expirada" } });
    };
    const aoAlterar = (evento: Event) => {
      if (localAtual.current.pathname === "/") return;
      const motivo = (evento as CustomEvent<{ motivo?: string }>).detail?.motivo;
      toast.info(motivo === "saiu" ? "Você saiu da conta em outra aba." : "A conta foi trocada em outra aba. Entre novamente.", { id: "sessao-alterada" });
      navigate("/", { replace: true });
    };
    window.addEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
    window.addEventListener(EVENTO_SESSAO_ALTERADA, aoAlterar);
    return () => {
      window.removeEventListener(EVENTO_SESSAO_EXPIRADA, aoExpirar);
      window.removeEventListener(EVENTO_SESSAO_ALTERADA, aoAlterar);
    };
  }, [navigate]);

  return null;
}

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <EventosDeSessao />
      <Suspense fallback={null}>
      <Routes>
        {/* Rota pública - Login */}
        <Route path="/" element={<Login />} />

        <Route
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route path="/home" element={<Home />} />
          <Route path="/perfil" element={<Profile />} />
          <Route
            path="/funcionarios/novo"
            element={
              <PrivateRoute requiredPermission="CRIAR_FUNCIONARIO">
                <NovoFuncionario />
              </PrivateRoute>
            }
          />
          <Route path="/obras/detalhamento" element={<PrivateRoute requiredPermission="VISUALIZAR_OBRA" mostrarSemAcesso><DetalhamentoObras /></PrivateRoute>} />
          <Route path="/obras/detalhamento/:id" element={<PrivateRoute requiredPermission="VISUALIZAR_OBRA" mostrarSemAcesso><DetalhamentoObras /></PrivateRoute>} />
          <Route
            path="/obras/detalhamento/:id/financeiro"
            element={
              <PrivateRoute adminOnly requiredPermission="VISUALIZAR_GASTOS">
                <Dashboard />
              </PrivateRoute>
            }
          />
          <Route path="/financeiro" element={<Navigate to="/home" replace />} />
          <Route path="/relatorios" element={<Navigate to="/home" replace />} />
          <Route
            path="/presencas"
            element={
              <PrivateRoute requiredPermission="VISUALIZAR_PRESENCAS">
                <GestaoPresenca />
              </PrivateRoute>
            }
          />
          <Route
            path="/obras/criar"
            element={
              <PrivateRoute
                requiredPermission="CRIAR_OBRA"
                deniedMessage="Você não tem permissão para criar obras. Solicite ao administrador."
              >
                <NovaObra />
              </PrivateRoute>
            }
          />
          <Route
            path="/permissoes"
            element={
              <PrivateRoute adminOnly>
                <Permissoes />
              </PrivateRoute>
            }
          />
          <Route
            path="/obras/:id/alocacoes"
            element={
              <PrivateRoute requiredPermission="VISUALIZAR_ALOCACOES">
                <AlocacaoFuncionario />
              </PrivateRoute>
            }
          />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
      </Suspense>
    </BrowserRouter>
  );
};
