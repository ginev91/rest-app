import axios, { AxiosInstance } from "axios";

const baseURL = (import.meta.env.VITE_API_BASE_URL as string) || "http://localhost:8080";

const api: AxiosInstance = axios.create({
  baseURL,
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

let accessToken: string | null = null;

const storedToken = localStorage.getItem('token');
if (storedToken) {
  setAccessToken(storedToken);
}

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (token) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
  } else {
    delete api.defaults.headers.common["Authorization"];
  }
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (!originalRequest) return Promise.reject(error);

    // Don't retry /me endpoint itself
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url?.includes('/api/auth/me')) {
      originalRequest._retry = true;
      try {
        // Try to verify session with /me
        const r = await api.get('/api/auth/me');
        
        if (r.status === 200) {
          return api(originalRequest);
        }
      } catch (refreshErr) {
        // /me failed (401 or 403), logout and clear
        setAccessToken(null);
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        localStorage.removeItem('tableId');
        localStorage.removeItem('tableNumber');
        
        // Redirect to login
        if (!window.location.pathname.includes('/login')) {
          window.location.href = '/login';
        }
        
        return Promise.reject(refreshErr);
      }
    }
    return Promise.reject(error);
  }
);

export default api;