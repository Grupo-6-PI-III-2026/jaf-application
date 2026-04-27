import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./Pages/Auth/Login/Login";
import Layout from "./Components/Layout/Layout";
import NovoFuncionario from "./Pages/Funcionarios/NovoFuncionario/NovoFuncionario";
import DetalhamentoObras from "./Pages/Obras/DetalhamentoObras";
import Home from "./Pages/Home/Home";
import PrivateRoute from "./Components/PrivateRoute/PrivateRoute";

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        {/* Rota pública - Login */}
        <Route path="/" element={<Login />} />

    
        <Route element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }>
          <Route path="/home" element={<Home />} />
          <Route path="/funcionarios/novo" element={<NovoFuncionario />} />
          <Route path="/obras/detalhamento" element={<DetalhamentoObras />} />
          <Route path="/obras/criar" element={<NovaObra />} />
        </Route>

     
        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};