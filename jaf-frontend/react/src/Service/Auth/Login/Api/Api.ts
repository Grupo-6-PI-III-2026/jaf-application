import axios from "axios";
import { authorizationSession } from "../authorizationSession";

// const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

export const EVENTO_SESSAO_EXPIRADA = "auth:expirou";
export const EVENTO_SESSAO_ALTERADA = "auth:alterada";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
    (config) => {
    config.withCredentials = true
    
        return config;
    },
    (error) => {
        return Promise.reject(error)
    }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const config = error.config;
    // Falha de CSRF: o servidor já reemitiu o cookie XSRF-TOKEN; repete a requisição uma única vez.
    if (error.response?.status === 403 && error.response.headers?.["x-csrf-retry"] === "true"
        && config && !config.csrfRetried) {
      config.csrfRetried = true;
      return api(config);
    }
    if (error.response?.status === 401) {
      // Login e validação inicial tratam a própria falha (mensagem ou retry no Provider).
      if (["/funcionarios/login", "/funcionarios/me", "/funcionarios/me/permissoes"].includes(error.config?.url)) {
        return Promise.reject(error);
      }
      authorizationSession.clear();
      // Token é gerenciado via cookie HttpOnly pelo backend
      // O roteador trata o evento: avisa o usuário e volta para a tela de login sem recarregar a página.
      window.dispatchEvent(new Event(EVENTO_SESSAO_EXPIRADA));
    }
    return Promise.reject(error);
  }
);


export default api
