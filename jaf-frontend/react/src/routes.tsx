import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./Pages/Auth/Login/Login";
import Layout from "./Components/Layout/Layout";
import NovoFuncionario from "./Pages/Funcionarios/NovoFuncionario/NovoFuncionario";
import DetalhamentoObras from "./Pages/Obras/DetalhamentoObras";
import PrivateRoute from "./Components/PrivateRoute/PrivateRoute";

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rota pública - Login */}
        <Route path="/" element={<Login />} />
        <Route path="/obras/gerenciamentofuncionarios" element={<DetalhamentoObras />} />

    
        <Route element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }>
          <Route path="/funcionarios/novo" element={<NovoFuncionario />} />
        </Route>

     
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};