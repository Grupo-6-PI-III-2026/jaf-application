import { useState, type ImgHTMLAttributes } from "react";

type Props = Omit<ImgHTMLAttributes<HTMLImageElement>, "src"> & {
  src?: string | null;
  padrao: string;
};

/** Mostra a imagem padrão quando não há foto ou quando a URL temporária falha (expirada ou removida). */
export default function ImagemComPadrao({ src, padrao, alt = "", ...props }: Props) {
  const [falhou, setFalhou] = useState<string | null>(null);
  const origem = src && falhou !== src ? src : padrao;

  return <img {...props} alt={alt} src={origem} onError={() => { if (src) setFalhou(src); }} />;
}
