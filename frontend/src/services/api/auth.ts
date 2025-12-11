import api, { setAccessToken, getAccessToken } from "./client";
import type { LoginRequest, LoginResponse, User } from "@/types";




export async function login(payload: LoginRequest): Promise<{ user: User | null; token?: string | null }> {
  const strategy = (import.meta.env.VITE_AUTH_STRATEGY as string) ?? "cookie";
  const resp = await api.post<LoginResponse>("/auth/login", payload);
  const data = resp.data;
  const token = data?.token ?? data?.accessToken ?? null;

  if (strategy === "bearer") {
    if (!token) throw new Error("No token returned from login (bearer strategy)");
    setAccessToken(token);
    
    try {
      const me = await fetchCurrentUser();
      return { user: me, token };
    } catch {
      return { user: null, token };
    }
  } else {
    
    try {
      const me = await fetchCurrentUser();
      return { user: me, token: null };
    } catch (e) {
      return { user: null, token: null };
    }
  }
}

export async function logout(): Promise<void> {
  await api.post("/auth/logout");
  setAccessToken(null);
}

export async function fetchCurrentUser(): Promise<User | null> {
  try {
    const r = await api.get("/auth/me");
    return r.data as User;
  } catch (err) {
    return null;
  }
}

export async function register(payload: any): Promise<LoginResponse> {
  const r = await api.post("/auth/register", payload);
  const data = r.data;
  
  if ((import.meta.env.VITE_AUTH_STRATEGY as string) === "bearer") {
    const token = data?.token ?? data?.accessToken;
    if (token) setAccessToken(token);
  }
  return data;
}