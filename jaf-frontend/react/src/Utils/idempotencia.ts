import { useRef } from "react";

export function novaChave(): string {
  if (typeof globalThis.crypto?.randomUUID === "function") return globalThis.crypto.randomUUID();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = Math.floor(Math.random() * 16);
    return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
  });
}

export const opcoesIdempotencia = (chave?: string) =>
  chave ? { headers: { "Idempotency-Key": chave } } : undefined;

// A mesma chave é reaproveitada em novas tentativas e só é trocada após um sucesso.
export function useChaveIdempotencia() {
  const ref = useRef(novaChave());
  return {
    atual: () => ref.current,
    renovar: () => { ref.current = novaChave(); },
  };
}
