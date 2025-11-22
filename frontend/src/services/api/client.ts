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

    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const r = await axios.post(
          `${baseURL}/api/auth/refresh`,
          {},
          { withCredentials: true, headers: { "Content-Type": "application/json" } }
        );

        const newToken = r.data?.accessToken;
        if (newToken) {
          setAccessToken(newToken);
        } else {
          if (accessToken) {
            api.defaults.headers.common["Authorization"] = `Bearer ${accessToken}`;
          } else {
            delete api.defaults.headers.common["Authorization"];
          }
        }

        return api(originalRequest);
      } catch (refreshErr) {
        setAccessToken(null);
        return Promise.reject(refreshErr);
      }
    }
    return Promise.reject(error);
  }
);

export default api;