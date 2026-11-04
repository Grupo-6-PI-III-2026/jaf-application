import { useEffect, useState } from "react";
import { hojeSaoPaulo } from "../Utils/data";

/** "Hoje" em São Paulo, atualizado quando a aba volta ao foco ou a data muda com a tela aberta. */
export function useHoje(): string {
  const [hoje, setHoje] = useState(hojeSaoPaulo);

  useEffect(() => {
    const atualizar = () => setHoje(hojeSaoPaulo());
    const aoVoltar = () => {
      if (document.visibilityState === "visible") atualizar();
    };
    document.addEventListener("visibilitychange", aoVoltar);
    window.addEventListener("focus", atualizar);
    const intervalo = setInterval(atualizar, 60_000);
    return () => {
      document.removeEventListener("visibilitychange", aoVoltar);
      window.removeEventListener("focus", atualizar);
      clearInterval(intervalo);
    };
  }, []);

  return hoje;
}