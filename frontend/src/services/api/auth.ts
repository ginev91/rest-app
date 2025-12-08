import api, { setAccessToken } from "./client";

export interface LoginRequest { username: string; password: string; tableNumber: number; tablePin: string; }
export interface LoginResponse {
  token?: string;
  accessToken?: string;
  user?: any;
  username?: string;
  userId?: string;
  role?: 'ROLE_USER' | 'ROLE_EMPLOYEE' | 'ROLE_ADMIN';
  tableNumber?: number;
  tableId?: string;
}

export async function login(username: string, password: string, tableNumber: number, tablePin: string): Promise<LoginResponse> {
  const resp = await api.post("/api/auth/login", { username, password, tableNumber, tablePin });
  const data = resp.data as LoginResponse;
  const token = data.token ?? data.accessToken;
  if (token) {
    setAccessToken(token);
  }
  return data;
}

export interface RegisterResponse {
  token?: string;
  accessToken?: string;
  user?: any;
  username?: string;
}

export async function register(name: string, email: string, password: string): Promise<RegisterResponse> {
  const payload = {
    username: email,
    password,
    fullName: name,
  };
  const resp = await api.post("/api/auth/register", payload);
  const data = resp.data as RegisterResponse;
  const token = data.token ?? data.accessToken;
  if (token) {
    setAccessToken(token);
  }
  return data;
}

export async function logout(): Promise<void> {
  await api.post("/api/auth/logout");
  setAccessToken(null);
}

export async function fetchCurrentUser(): Promise<any | null> {
  try {
    const r = await api.get("/api/auth/me");
    return r.data;
  } catch (err) {
    console.debug("fetchCurrentUser failed", err?.response?.status);
    return null;
  }
}