import { authService } from "../Service/Auth/Login/authService";
import type { Cargo, FuncionarioResponseDto } from "../Types/user";

// Testes declaram explicitamente as permissões recebidas; sem padrões por cargo.
export function validarSessao(cargo: Cargo, permissions: string[] = [], id = 1) {
  authService.clearAuthorization();
  const user: FuncionarioResponseDto = {
    id, nome: "Pessoa autenticada", email: `pessoa${id}@exemplo.com`, cargo, fotoUrl: null,
  };
  localStorage.setItem("userId", String(id));
  localStorage.setItem("userEmail", user.email);
  localStorage.setItem("userCargo", cargo);
  authService.setValidatedSession(user, permissions);
  return user;
}