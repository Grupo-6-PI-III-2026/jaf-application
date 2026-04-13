import axios from "axios";

const apiBaseUrl = import.meta.env.VITE_API_URL || "http://localhost:8080"

const api = axios.create({
  baseURL: apiBaseUrl,
    timeout: 10000,
    headers: {
        "Content-Type": "application/json"
    }
})

export default api