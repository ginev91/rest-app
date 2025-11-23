import React, { createContext, useContext, useEffect, useState } from 'react';
import { User, AuthState } from '@/types/auth';
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  fetchCurrentUser,
} from '@/services/api/auth';
import { setAccessToken } from '@/services/api/client';

interface AuthContextType extends AuthState {
  isLoading: boolean;
  login: (email: string, password: string) => Promise<User | null>;
  register: (email: string, password: string, name: string) => Promise<User | null>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
  refreshUser: () => Promise<User | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    token: null,
    isAuthenticated: false,
  });
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const storedToken = localStorage.getItem('token');
        if (storedToken) {
          setAccessToken(storedToken);
        }

        const user = await fetchCurrentUser();
        if (!mounted) return;
        if (user) {
          localStorage.setItem('user', JSON.stringify(user));
          setAuthState({ user, token: storedToken ?? null, isAuthenticated: true });
        } else {
          const storedUser = localStorage.getItem('user');
          if (storedUser) {
            try {
              const parsed = JSON.parse(storedUser) as User;
              setAuthState({ user: parsed, token: storedToken ?? null, isAuthenticated: !!parsed });
            } catch {
              localStorage.removeItem('user');
            }
          }
        }
      } catch (err) {
        // ignore
      } finally {
        if (mounted) setIsLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  const refreshUser = async (): Promise<User | null> => {
    setIsLoading(true);
    try {
      const user = await fetchCurrentUser();
      if (user) {
        localStorage.setItem('user', JSON.stringify(user));
        setAuthState(prev => ({ ...prev, user, isAuthenticated: true }));
        return user;
      } else {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        setAccessToken(null);
        setAuthState({ user: null, token: null, isAuthenticated: false });
        return null;
      }
    } finally {
      setIsLoading(false);
    }
  };

  // Login: frontend uses email as username
  const login = async (email: string, password: string): Promise<User | null> => {
    setIsLoading(true);
    try {
      const resp = await apiLogin(email, password); // sends { username, password }
      const token = (resp as any)?.token ?? (resp as any)?.accessToken;
      if (token) {
        setAccessToken(token);
        localStorage.setItem('token', token);
      }

      // If backend returned a user object use it; if only username returned, try to fetch /me
      const userFromResp = (resp as any)?.user ?? ((resp as any)?.username ? { username: (resp as any).username } : null);
      if (userFromResp && Object.keys(userFromResp).length > 0) {
        localStorage.setItem('user', JSON.stringify(userFromResp));
        setAuthState({ user: userFromResp, token: token ?? authState.token, isAuthenticated: true });
        return userFromResp;
      }

      const fetched = await fetchCurrentUser();
      if (fetched) {
        localStorage.setItem('user', JSON.stringify(fetched));
        setAuthState({ user: fetched, token: token ?? authState.token, isAuthenticated: true });
        return fetched;
      }

      setAuthState({ user: null, token: null, isAuthenticated: false });
      return null;
    } catch (err) {
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  // Register: your Login UI calls register(email,password,name) - we map to backend payload username=email, fullName=name
  const register = async (email: string, password: string, name: string): Promise<User | null> => {
    setIsLoading(true);
    try {
      const resp = await apiRegister(name, email, password); // apiRegister maps to { username: email, password, fullName: name }
      const token = (resp as any)?.token ?? (resp as any)?.accessToken;
      if (token) {
        setAccessToken(token);
        localStorage.setItem('token', token);
      }

      const userFromResp = (resp as any)?.user ?? ((resp as any)?.username ? { username: (resp as any).username } : null);
      if (userFromResp && Object.keys(userFromResp).length > 0) {
        localStorage.setItem('user', JSON.stringify(userFromResp));
        setAuthState({ user: userFromResp, token: token ?? authState.token, isAuthenticated: true });
        return userFromResp;
      }

      const fetched = await fetchCurrentUser();
      if (fetched) {
        localStorage.setItem('user', JSON.stringify(fetched));
        setAuthState({ user: fetched, token: token ?? authState.token, isAuthenticated: true });
        return fetched;
      }

      setAuthState({ user: null, token: null, isAuthenticated: false });
      return null;
    } catch (err) {
      throw err;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = async (): Promise<void> => {
    setIsLoading(true);
    try {
      await apiLogout();
    } catch {
      // ignore
    } finally {
      setAccessToken(null);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      setAuthState({ user: null, token: null, isAuthenticated: false });
      setIsLoading(false);
    }
  };

  const setUser = (user: User | null) => {
    setAuthState(prev => ({
      ...prev,
      user,
      isAuthenticated: !!user,
    }));
    if (user) {
      localStorage.setItem('user', JSON.stringify(user));
    } else {
      localStorage.removeItem('user');
    }
  };

  return (
    <AuthContext.Provider
      value={{
        ...authState,
        isLoading,
        login,
        register,
        logout,
        setUser,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};