import api from "../Auth/Login/Api/Api";

import { type NovoFuncionarioPayload } from "../../Types/Register";

export const funcionarioService = {
  cadastrar: async (payload: NovoFuncionarioPayload) => {
    const response = await api.post("/funcionarios", payload);
    return response.data;
  },
};
