import { useContext } from "react";
import { UserContext } from "./UserContextDefinition";
import type { UserContextValue } from "./UserContextDefinition";

export function useUser(): UserContextValue {
  const ctx = useContext(UserContext);
  if (!ctx) {
    throw new Error("useUser deve ser usado dentro de <UserProvider>");
  }
  return ctx;
}