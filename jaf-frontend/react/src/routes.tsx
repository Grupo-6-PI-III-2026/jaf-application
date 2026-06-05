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

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rota pública - Login */}
        <Route path="/" element={<Login />} />
        <Route
          path="/obras/gerenciamentofuncionarios"
          element={<DetalhamentoObras />}
        />

        <Route
          element={
            <PrivateRoute>
              <Layout />
            </PrivateRoute>
          }
        >
          <Route path="/home" element={<Home />} />
          <Route path="/perfil" element={<Profile />} />
          <Route path="/funcionarios/novo" element={<NovoFuncionario />} />
          <Route path="/obras/detalhamento" element={<DetalhamentoObras />} />
          <Route path="/obras/criar" element={<NovaObra />} />
          <Route path="/permissoes" element={<Permissoes />} />
        </Route>

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};
