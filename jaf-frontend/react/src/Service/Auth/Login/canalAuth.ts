export type MensagemAuth = { tipo: "login" | "logout"; userId: number | null };

let canal: BroadcastChannel | null = null;

function obter(): BroadcastChannel | null {
  if (typeof BroadcastChannel === "undefined") return null;
  canal ??= new BroadcastChannel("jaf-auth");
  return canal;
}

// O canal não entrega a mensagem à própria aba que a enviou, só às demais.
export const canalAuth = {
  publicar(mensagem: MensagemAuth) {
    obter()?.postMessage(mensagem);
  },
  ouvir(ouvinte: (mensagem: MensagemAuth) => void): () => void {
    const aberto = obter();
    if (!aberto) return () => {};
    const aoReceber = (evento: MessageEvent<MensagemAuth>) => ouvinte(evento.data);
    aberto.addEventListener("message", aoReceber);
    return () => aberto.removeEventListener("message", aoReceber);
  },
};
