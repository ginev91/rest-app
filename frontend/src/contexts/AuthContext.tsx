import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
  fetchCurrentUser,
} from "@/services/api/auth";
import { setAccessToken } from "@/services/api/client";
import type { User, LoginRequest } from "@/types";


type AuthContextType = {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  loading: boolean;
  isLoading: boolean;
  login: (username: string, password: string, tableNumber?: number, tablePin?: string) => Promise<User | null>;
  register: (email: string, password: string, name?: string) => Promise<User | null>;
  logout: () => Promise<void>;
  hasRole: (role: string) => boolean;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const strategy = (import.meta.env.VITE_AUTH_STRATEGY as string) ?? "cookie";
  const storeStrategy = (import.meta.env.VITE_STORE_TOKEN as string) ?? "local";

  const navigate = useNavigate();
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(() => {
    if (strategy === "bearer" && storeStrategy === "local") {
      return localStorage.getItem("auth_token");
    }
    return null;
  });
  const [loading, setLoading] = useState<boolean>(true);

  // Ensure client has token (bearer) on start
  useEffect(() => {
    if (strategy === "bearer" && token) {
      setAccessToken(token);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // On mount try to load current user (works for both cookie and bearer)
  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const me = await fetchCurrentUser();
        if (mounted && me) {
          setUser(me);
          if (strategy === "cookie") {
            setToken("__cookie__");
          }
        } else {
          if (mounted) {
            setUser(null);
            if (strategy === "cookie") setToken(null);
          }
        }
      } catch (err) {
        if (mounted) {
          setUser(null);
          if (strategy === "cookie") setToken(null);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [strategy]);

  // login wrapper used by UI — CALLS API WITH SINGLE PAYLOAD OBJECT
  const login = async (username: string, password: string, tableNumber?: number, tablePin?: string) => {
    try {
      const payload: LoginRequest = { username, password, tableNumber, tablePin };
      // apiLogin expects one argument (the payload object)
      const resp = await apiLogin(payload);
      // If bearer strategy and server returned token, persist it (apiLogin may have returned it)
      const maybeToken = (resp as any)?.token ?? (resp as any)?.accessToken ?? null;
      if (strategy === "bearer" && maybeToken) {
        setAccessToken(maybeToken);
        setToken(maybeToken);
        if (storeStrategy === "local") localStorage.setItem("auth_token", maybeToken);
      }

      // For cookie strategy, server sets cookie. We must fetch current user via /auth/me
      const me = await fetchCurrentUser();
      if (me) {
        setUser(me);
        if (strategy === "cookie") {
          setToken("__cookie__");
        }
        return me as User;
      } else {
        setUser(null);
        return null;
      }
    } catch (err) {
      throw err;
    }
  };

  // register wrapper — CALLS API WITH SINGLE PAYLOAD OBJECT
  const register = async (email: string, password: string, name?: string) => {
    try {
      // adapt to API which expects a single payload object; common shape: { username, password, fullName }
      const payload = { username: email, password, fullName: name ?? "" };
      const resp = await apiRegister(payload);
      const maybeToken = (resp as any)?.token ?? (resp as any)?.accessToken ?? null;
      if (strategy === "bearer" && maybeToken) {
        setAccessToken(maybeToken);
        setToken(maybeToken);
        if (storeStrategy === "local") localStorage.setItem("auth_token", maybeToken);
      }

      try {
        const me = await fetchCurrentUser();
        if (me) {
          setUser(me);
          if (strategy === "cookie") setToken("__cookie__");
          return me as User;
        }
      } catch {
        // registration succeeded, but not logged in automatically
      }

      return null;
    } catch (err) {
      throw err;
    }
  };

  const logout = async () => {
    try {
      await apiLogout();
    } catch (err) {
      console.warn("logout error", err);
    } finally {
      setUser(null);
      setToken(null);
      if (strategy === "bearer") {
        setAccessToken(null);
        if (storeStrategy === "local") localStorage.removeItem("auth_token");
      }
      try {
        navigate("/login");
      } catch {
        // ignore
      }
    }
  };

  const hasRole = (role: string) => {
    if (!user) return false;
    if (Array.isArray((user as any).roles)) return (user as any).roles.includes(role as any);
    if (typeof (user as any).role === "string") return (user as any).role === role;
    return false;
  };

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: !!user,
      loading,
      isLoading: loading,
      login,
      register,
      logout,
      hasRole,
    }),
    [user, token, loading]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}