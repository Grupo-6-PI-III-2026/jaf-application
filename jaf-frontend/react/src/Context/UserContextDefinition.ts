import { createContext } from "react";
import type { FuncionarioResponseDto } from "../Types/user";

export interface UserContextValue {
  user: FuncionarioResponseDto | null;
  isLoading: boolean;
  error: string | null;
  refreshUser: () => Promise<void>;
  clearUser: () => void;
}

export const UserContext = createContext<UserContextValue | undefined>(undefined);