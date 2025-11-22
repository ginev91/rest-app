import api, { setAccessToken } from "./client";

export interface LoginRequest { username: string; password: string; }
export interface LoginResponse {
  token?: string;
  accessToken?: string;
  user?: any;
  username?: string;
}

export async function login(username: string, password: string): Promise<LoginResponse> {
  const resp = await api.post("/api/auth/login", { username, password });
  const data = resp.data as LoginResponse;
  const token = data.token ?? data.accessToken;
  if (token) {
    setAccessToken(token);
  }
  return data;
}

/**
 * Register on backend.
 * Backend expects: { username, password, fullName } (example you showed via curl).
 * Frontend API convenience: call register(name, email, password)
 */
export interface RegisterResponse {
  token?: string;
  accessToken?: string;
  user?: any;
  username?: string;
}

export async function register(name: string, email: string, password: string): Promise<RegisterResponse> {
  // Map frontend fields to backend contract
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

/**
 * refresh: calls /api/auth/refresh, handles token in response
 */
export async function refresh(): Promise<void> {
  const resp = await api.post("/api/auth/refresh", {}, { withCredentials: true });
  const token = resp.data?.token ?? resp.data?.accessToken;
  if (token) setAccessToken(token);
}

/**
 * Fetch the current authenticated user.
 * Backend endpoint: GET /api/auth/me
 * If backend doesn't provide this endpoint, you can map username from login/register response instead.
 */
export async function fetchCurrentUser(): Promise<any | null> {
  try {
    const r = await api.get("/api/auth/me");
    return r.data;
  } catch (err: any) {
    return null;
  }
}