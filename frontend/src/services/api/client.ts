import axios, { AxiosInstance, AxiosRequestHeaders } from "axios";

const baseURL =
  (import.meta.env.VITE_API_BASE_URL as string) ||
  (import.meta.env.VITE_API_BASE as string) ||
  "http://localhost:8080/api/";

const api: AxiosInstance = axios.create({
  baseURL,
  withCredentials: true, 
  headers: {
    "Content-Type": "application/json",
  },
});


let accessToken: string | null = null; 

const storedToken =
  typeof window !== "undefined" ? localStorage.getItem("auth_token") : null;
if (storedToken) {
  setAccessToken(storedToken, false);
}

export function setAccessToken(token: string | null, persist = true) {
  accessToken = token;
  if (token) {
    api.defaults.headers.common["Authorization"] = `Bearer ${token}`;
    if (persist) localStorage.setItem("auth_token", token);
  } else {
    delete api.defaults.headers.common["Authorization"];
    localStorage.removeItem("auth_token");
  }
}

export function getAccessToken() {
  return accessToken;
}


api.interceptors.request.use((cfg) => {
  const strategy = (import.meta.env.VITE_AUTH_STRATEGY as string) ?? "cookie";
  if (strategy === "bearer") {
    const t = accessToken;
    if (t) {
      
      cfg.headers = (cfg.headers as AxiosRequestHeaders) ?? ({} as AxiosRequestHeaders);
      (cfg.headers as AxiosRequestHeaders)["Authorization"] = `Bearer ${t}`;
    }
  }
  return cfg;
});


api.interceptors.response.use(
  (r) => r,
  async (error) => {
    const originalRequest = error?.config as any;
    if (!originalRequest) return Promise.reject(error);

    const status = error.response?.status;
    const url = originalRequest.url ?? "";

    
    const reqHeaders = originalRequest.headers as Record<string, any> | undefined;
    const skipRefresh = reqHeaders?.["x-skip-refresh"] || reqHeaders?.["X-Skip-Refresh"];

    
    const isAuthEndpoint =
      url.includes("/auth/login") || url.includes("/auth/logout") || url.includes("/auth/me");
    if (isAuthEndpoint || skipRefresh) {
      return Promise.reject(error);
    }

    
    if (status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        
        await api.get("auth/me", { headers: { "x-skip-refresh": "1" } });
        
        return api(originalRequest);
      } catch (refreshErr) {
        
        setAccessToken(null);
        localStorage.removeItem("auth_token");
        localStorage.removeItem("user");
        localStorage.removeItem("tableId");
        localStorage.removeItem("tableNumber");
        if (!window.location.pathname.includes("/login")) {
          window.location.href = "/login";
        }
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(error);
  }
);

export default api;