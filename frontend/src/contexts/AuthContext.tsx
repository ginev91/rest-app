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
  login: (email: string, password: string, tableNumber: number, tablePin: string) => Promise<User | null>;
  register: (email: string, password: string, name: string) => Promise<User | null>;
  logout: () => Promise<void>;
  setUser: (user: User | null) => void;
  refreshUser: () => Promise<User | null>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

const cleanRole = (role: string | undefined): string => {
  if (!role) return 'CUSTOMER';
  return role.startsWith('.') ? role.substring(1) : role;
};

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
        const storedUser = localStorage.getItem('user');
        
        if (storedToken) {
          setAccessToken(storedToken);
        }

        if (storedUser) {
          try {
            const parsed = JSON.parse(storedUser) as User;
            parsed.role = cleanRole(parsed.role);
            console.log('Loaded user from localStorage:', parsed);
            if (mounted) {
              setAuthState({ user: parsed, token: storedToken ?? null, isAuthenticated: true });
            }
          } catch {
            localStorage.removeItem('user');
          }
        }

        if (storedToken) {
          const user = await fetchCurrentUser();
          if (!mounted) return;
          if (user) {
            if ((user as any).role) {
              (user as any).role = cleanRole((user as any).role);
            }
            console.log('Fetched user from server:', user);
            localStorage.setItem('user', JSON.stringify(user));
            setAuthState({ user, token: storedToken, isAuthenticated: true });
          }
        }
      } catch (err) {
        console.error('Auth initialization error:', err);
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
        if ((user as any).role) {
          (user as any).role = cleanRole((user as any).role);
        }
        console.log('Refreshed user:', user);
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


const login = async (email: string, password: string, tableNumber: number, tablePin: string): Promise<User | null> => {
  setIsLoading(true);
  try {
    console.log('AuthContext: Starting login with table auth...', { tableNumber, tablePin });
    
    const resp = await apiLogin(email, password, { tableNumber, tablePin });
    console.log('AuthContext: Login response:', resp);
    
    const token = (resp as any)?.token ?? (resp as any)?.accessToken;
    if (token) {
      setAccessToken(token);
      localStorage.setItem('token', token);
    }

    const userId = (resp as any)?.userId;
    const username = (resp as any)?.username || email;
    const role = cleanRole((resp as any)?.role);
    const userTableNumber = (resp as any)?.tableNumber;
    const tableId = (resp as any)?.tableId; // ✅ Get tableId from response

    if (userId) {
      const userData: User = {
        id: userId,
        username: username,
        role: role,
        tableNumber: userTableNumber || tableNumber,
        tableId: tableId 
      };
      
      if (tableId) {
        localStorage.setItem('tableId', tableId);
      }
      
      console.log('AuthContext: Created user data:', userData);
      localStorage.setItem('user', JSON.stringify(userData));
      setAuthState({ user: userData, token: token ?? authState.token, isAuthenticated: true });
      return userData;
    }

    console.log('AuthContext: No userId in response, fetching from /me...');
    const fetched = await fetchCurrentUser();
    if (fetched) {
      if ((fetched as any).role) {
        (fetched as any).role = cleanRole((fetched as any).role);
      }
      (fetched as any).tableNumber = tableNumber;
      (fetched as any).tableId = tableId; // ✅ Add tableId to fetched user
      
      console.log('AuthContext: Fetched user:', fetched);
      localStorage.setItem('user', JSON.stringify(fetched));
      setAuthState({ user: fetched, token: token ?? authState.token, isAuthenticated: true });
      return fetched;
    }

    setAuthState({ user: null, token: null, isAuthenticated: false });
    return null;
  } catch (err) {
    console.error('AuthContext: Login failed:', err);
    throw err;
  } finally {
    setIsLoading(false);
  }
};

  const register = async (email: string, password: string, name: string): Promise<User | null> => {
    setIsLoading(true);
    try {
      console.log('AuthContext: Starting registration...');
      const resp = await apiRegister(name, email, password);
      console.log('AuthContext: Registration response:', resp);
      
      // Don't auto-login after registration
      // User needs to login with table PIN
      return { id: '', username: name, role: 'CUSTOMER' } as User;
    } catch (err) {
      console.error('AuthContext: Registration failed:', err);
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
    if (user && user.role) {
      user.role = cleanRole(user.role);
    }
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