import axios from "axios";

// const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || "/api",
  timeout: 10000,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use(
    (config) => {
    // CORREÇÃO DE SEGURANÇA A07: Removida leitura de token do localStorage
    // Antes: const token = localStorage.getItem('token') ?? localStorage.getItem('auth_token')
    //        if (token) { config.headers.Authorization = `Bearer ${token}` }
    // Agora: Token é enviado automaticamente via cookie HttpOnly pelo browser
    // withCredentials: true deve ser configurado para enviar cookies
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
   
    if (error.response?.status === 401) {
      // CORREÇÃO DE SEGURANÇA A07: Limpa dados do usuário do localStorage
      // Token é gerenciado via cookie HttpOnly pelo backend
      localStorage.removeItem('userEmail');
      localStorage.removeItem('userName');
      localStorage.removeItem('userId');
      localStorage.removeItem('userCargo');
      window.location.href = '/';
    }
    return Promise.reject(error);
  }
);


export default api