export const NOME_REGEX = /^[\p{L}][\p{L}\s'.-]+$/u;
export const TITULO_OBRA_REGEX = /^[\p{L}\p{N}\s\-.,()/'&]+$/u;
export const SENHA_REGEX =
  /^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
export const EMAIL_REGEX = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;

export function validarNome(nome: string): string | undefined {
  const valor = nome.trim();
  if (!valor) return "O nome do usuário é obrigatório.";
  if (valor.length < 3 || valor.length > 100) {
    return "O nome do usuário deve ter entre 3 e 100 caracteres.";
  }
  if (!NOME_REGEX.test(valor)) return "O nome do usuário contém caracteres inválidos.";
  return undefined;
}

export function validarTituloObra(titulo: string): string | undefined {
  const valor = titulo.trim();
  if (!valor) return "O título da obra é obrigatório.";
  if (valor.length < 5 || valor.length > 150) {
    return "O título da obra deve ter entre 5 e 150 caracteres.";
  }
  if (!TITULO_OBRA_REGEX.test(valor)) return "O título da obra contém caracteres inválidos.";
  return undefined;
}

export function validarSenha(senha: string): string | undefined {
  if (!SENHA_REGEX.test(senha)) {
    return "A senha deve ter no mínimo 8 caracteres, incluindo 1 maiúscula, 1 minúscula, 1 número e 1 caractere especial (@$!%*?&).";
  }
  return undefined;
}

export function validarEmail(email: string): string | undefined {
  if (!EMAIL_REGEX.test(email.trim())) return "Informe um e-mail válido.";
  return undefined;
}
