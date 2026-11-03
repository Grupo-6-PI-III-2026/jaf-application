import { describe, expect, it } from "vitest";
import { validarEmail, validarNome, validarSenha, validarTituloObra } from "./validacao";

describe("validações compartilhadas", () => {
  it("espelha as regras de nome e título do backend", () => {
    expect(validarNome("Edifício Aurora")).toBeUndefined();
    expect(validarNome("AB")).toBeDefined();
    expect(validarTituloObra("Edifício Aurora (Bloco A)")).toBeUndefined();
    expect(validarTituloObra("obra@inválida")).toBeDefined();
  });

  it("exige senha com número e caractere especial permitido", () => {
    expect(validarSenha("Abcdefgh!")).toBeDefined();
    expect(validarSenha("Senha#2026")).toBeDefined();
    expect(validarSenha("Senha@2026")).toBeUndefined();
  });

  it("valida e-mail com a mesma regra básica da API", () => {
    expect(validarEmail("pessoa@exemplo.com")).toBeUndefined();
    expect(validarEmail("invalido")).toBeDefined();
  });
});
