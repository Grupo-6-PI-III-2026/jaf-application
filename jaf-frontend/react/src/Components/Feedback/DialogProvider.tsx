import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { DialogContext } from "./DialogContext";
import styles from "./DialogProvider.module.css";

type DialogRequest = {
  id: number;
  type: "confirm" | "alert";
  title: string;
  message: string;
  resolve: (confirmed: boolean) => void;
};

export function DialogProvider({ children }: { children: ReactNode }) {
  const [requests, setRequests] = useState<DialogRequest[]>([]);
  const nextId = useRef(0);
  const active = requests[0] ?? null;
  const dialog = useRef<HTMLDialogElement>(null);

  const enqueue = useCallback((type: DialogRequest["type"], message: string, title: string) =>
    new Promise<boolean>((resolve) => {
      const request: DialogRequest = { id: ++nextId.current, type, title, message, resolve };
      setRequests((current) => [...current, request]);
    }), []);

  const confirm = useCallback((message: string, title = "Confirmação") =>
    enqueue("confirm", message, title), [enqueue]);

  const alert = useCallback(async (message: string, title = "Aviso") => {
    await enqueue("alert", message, title);
  }, [enqueue]);

  const settle = useCallback((request: DialogRequest, confirmed: boolean) => {
    request.resolve(confirmed);
    setRequests((current) => current.filter(({ id }) => id !== request.id));
  }, []);

  useEffect(() => {
    const element = dialog.current;
    if (!element) return;
    if (active) {
      if (element.open) return;
      if (typeof element.showModal === "function") element.showModal();
      else element.setAttribute("open", "");
      return;
    }
    if (element.open) {
      if (typeof element.close === "function") element.close();
      else element.removeAttribute("open");
    }
  }, [active]);

  return (
    <DialogContext.Provider value={{ confirm, alert }}>
      {children}
      <dialog
        ref={dialog}
        className={styles.dialog}
        aria-labelledby={active ? `dialog-title-${active.id}` : undefined}
        aria-describedby={active ? `dialog-message-${active.id}` : undefined}
        onCancel={(event) => {
          event.preventDefault();
          if (active) settle(active, false);
        }}
      >
        {active && <>
          <h2 id={`dialog-title-${active.id}`} className={styles.title}>{active.title}</h2>
          <p id={`dialog-message-${active.id}`} className={styles.message}>{active.message}</p>
          <div className={styles.actions}>
            {active.type === "confirm" && (
              <button type="button" className={styles.cancel} onClick={() => settle(active, false)}>
                Cancelar
              </button>
            )}
            <button type="button" className={styles.accept} autoFocus onClick={() => settle(active, true)}>
              {active.type === "confirm" ? "Confirmar" : "OK"}
            </button>
          </div>
        </>}
      </dialog>
    </DialogContext.Provider>
  );
}
