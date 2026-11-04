export const TIPOS_NOTA_FISCAL = ["image/jpeg", "image/png", "image/webp", "application/pdf"];
export const LIMITE_NOTA_FISCAL_BYTES = 10 * 1024 * 1024;
export const LIMITE_FOTO_PERFIL_BYTES = 5 * 1024 * 1024;
const LIMITE_COMPRESSAO_BYTES = 2 * 1024 * 1024;
const LADO_MAXIMO_PX = 2000;
const QUALIDADE_JPEG = 0.8;

export function validarNotaFiscal(arquivo: File): string | null {
  if (arquivo.size > LIMITE_NOTA_FISCAL_BYTES) return "A nota fiscal deve ter no máximo 10 MB.";
  if (!TIPOS_NOTA_FISCAL.includes(arquivo.type)) return "Envie uma nota fiscal em PDF, JPG, PNG ou WEBP.";
  return null;
}

export function validarFotoPerfil(arquivo: File): string | null {
  if (!arquivo.type.startsWith("image/")) return "Envie um arquivo de imagem.";
  if (arquivo.size > LIMITE_FOTO_PERFIL_BYTES) return "A foto deve ter no máximo 5 MB.";
  return null;
}

/**
 * Reduz fotos grandes (comuns em celular) antes do envio: lado maior de 2000 px, JPEG 0,8.
 * Se algo falhar ou o resultado não ficar menor, devolve o arquivo original.
 */
export async function comprimirImagem(arquivo: File): Promise<File> {
  if (!arquivo.type.startsWith("image/") || arquivo.size <= LIMITE_COMPRESSAO_BYTES) return arquivo;
  try {
    const imagem = await createImageBitmap(arquivo);
    const escala = Math.min(1, LADO_MAXIMO_PX / Math.max(imagem.width, imagem.height));
    const canvas = document.createElement("canvas");
    canvas.width = Math.max(1, Math.round(imagem.width * escala));
    canvas.height = Math.max(1, Math.round(imagem.height * escala));
    const contexto = canvas.getContext("2d");
    if (!contexto) return arquivo;
    // JPEG não tem transparência: sem fundo branco, PNG transparente ficaria preto.
    contexto.fillStyle = "#ffffff";
    contexto.fillRect(0, 0, canvas.width, canvas.height);
    contexto.drawImage(imagem, 0, 0, canvas.width, canvas.height);
    imagem.close?.();
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", QUALIDADE_JPEG));
    if (!blob || blob.size >= arquivo.size) return arquivo;
    return new File([blob], `${arquivo.name.replace(/\.[^.]+$/, "")}.jpg`, { type: "image/jpeg", lastModified: Date.now() });
  } catch {
    return arquivo;
  }
}