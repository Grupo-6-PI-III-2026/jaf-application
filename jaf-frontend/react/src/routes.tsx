import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./Pages/Auth/Login/Login";
import Layout from "./Components/Layout/Layout";
import NovoFuncionario from "./Pages/Funcionarios/NovoFuncionario/NovoFuncionario";
import DetalhamentoObras from "./Pages/Obras/DetalhamentoObras";
import NovaObra from "./Pages/Obras/NovaObra/NovaObra";
import Home from "./Pages/Home/Home";
import Permissoes from "./Pages/Permissoes/Permissoes";
import PrivateRoute from "./Components/PrivateRoute/PrivateRoute";
import Profile from "./Pages/Profile/Profile";
import AlocacaoFuncionario from "./Pages/Alocacoes/AlocacaoFuncionario";
import Dashboard from "./Pages/Dashboard/Dashboard";

export const AppRoutes = () => {
  return (
    <BrowserRouter>
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
          <Route path="/obras/detalhamento" element={<DetalhamentoObras />} />
          <Route path="/obras/detalhamento/:id" element={<DetalhamentoObras />} />
          <Route path="/obras/detalhamento/:id/financeiro" element={<Dashboard />} />
          <Route path="/financeiro" element={<Navigate to="/home" replace />} />
          <Route path="/relatorios" element={<Navigate to="/home" replace />} />
          <Route
            path="/obras/criar"
            element={
              <PrivateRoute requiredPermission="CRIAR_OBRA">
                <NovaObra />
              </PrivateRoute>
            }
          />
          <Route
            path="/permissoes"
            element={
              <PrivateRoute requiredPermission="EDITAR_FUNCIONARIO">
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

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};
