import { createContext, useContext } from "react";

export type DialogContextValue = {
  confirm: (message: string, title?: string) => Promise<boolean>;
  alert: (message: string, title?: string) => Promise<void>;
};

export const DialogContext = createContext<DialogContextValue | null>(null);

export function useDialogs() {
  const context = useContext(DialogContext);
  if (!context) throw new Error("useDialogs deve ser usado dentro de DialogProvider");
  return context;
}
