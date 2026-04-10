import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./Pages/Auth/Login/Login";
import Layout from "./Components/Layout/Layout";
import NovoFuncionario from "./Pages/Funcionarios/NovoFuncionario/NovoFuncionario";
import DetalhamentoObras from "./Pages/Obras/DetalhamentoObras";

export const AppRoutes = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />

        <Route element={<Layout />}>
          <Route path="/funcionarios/novo" element={<NovoFuncionario />} />
          <Route path="/obras/detalhamento" element={<DetalhamentoObras />} />
        </Route>

        <Route path="*" element={<Navigate to="/" />} />
      </Routes>
    </BrowserRouter>
  );
};