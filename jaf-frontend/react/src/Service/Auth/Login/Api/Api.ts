import axios from "axios";

const apiBaseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'

const api = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    "Content-Type": "application/json"
  }
})

api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }

        return config;
    },
    (error) => {
        return Promise.reject(error)
    }
)

api.interceptors.response.use(
  (response) => response,
  (error) => {
    // Se receber 401 (não autorizado), limpa token e redireciona
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userEmail');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);


export default api