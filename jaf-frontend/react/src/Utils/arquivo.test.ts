import { describe, expect, it } from "vitest";
import { comprimirImagem, validarFotoPerfil, validarNotaFiscal } from "./arquivo";

const arquivo = (tipo: string, bytes: number, nome = "arquivo") =>
  new File([new Uint8Array(bytes)], nome, { type: tipo });

describe("validação de arquivos", () => {
  it("aceita nota em PDF, JPG, PNG e WEBP até 10 MB", () => {
    for (const tipo of ["application/pdf", "image/jpeg", "image/png", "image/webp"]) {
      expect(validarNotaFiscal(arquivo(tipo, 10))).toBeNull();
    }
  });

  it("recusa nota grande demais ou de outro tipo", () => {
    expect(validarNotaFiscal(arquivo("image/png", 10 * 1024 * 1024 + 1))).toMatch(/10 MB/);
    expect(validarNotaFiscal(arquivo("text/plain", 10))).toMatch(/PDF, JPG, PNG ou WEBP/);
  });

  it("limita a foto de perfil a 5 MB de imagem", () => {
    expect(validarFotoPerfil(arquivo("image/jpeg", 100))).toBeNull();
    expect(validarFotoPerfil(arquivo("image/jpeg", 5 * 1024 * 1024 + 1))).toMatch(/5 MB/);
    expect(validarFotoPerfil(arquivo("application/pdf", 100))).toMatch(/imagem/);
  });
});

describe("comprimirImagem", () => {
  it("não mexe em imagem pequena nem em PDF", async () => {
    const pequena = arquivo("image/jpeg", 1024);
    const pdf = arquivo("application/pdf", 3 * 1024 * 1024);
    expect(await comprimirImagem(pequena)).toBe(pequena);
    expect(await comprimirImagem(pdf)).toBe(pdf);
  });

  it("devolve o original quando o navegador não consegue processar a imagem", async () => {
    const grande = arquivo("image/jpeg", 3 * 1024 * 1024);
    expect(await comprimirImagem(grande)).toBe(grande);
  });
});