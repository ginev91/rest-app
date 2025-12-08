import React, { createContext, useContext, useEffect, useState } from 'react';
import { User, AuthState } from '@/types/auth';
import {
  login as apiLogin,
  register as apiRegister,
  logout as apiLogout,
  fetchCurrentUser,
  LoginResponse,
  RegisterResponse
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
            user.role = cleanRole(user.role);
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
        user.role = cleanRole(user.role);
        console.log('Refreshed user:', user);
        localStorage.setItem('user', JSON.stringify(user));
        setAuthState(prev => ({ ...prev, user, isAuthenticated: true }));
        return user;
      } else {
        localStorage.removeItem('user');
        localStorage.removeItem('token');
        localStorage.removeItem('tableId');
        localStorage.removeItem('tableNumber');
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
      
      const resp: LoginResponse = await apiLogin(email, password, tableNumber, tablePin);
      console.log('AuthContext: Login response:', resp);
      
      const token = resp.token ?? resp.accessToken;
      console.log('AuthContext: Received token:', resp);
      if (token) {
        setAccessToken(token);
        localStorage.setItem('token', token);
      }

      const userId = resp.userId;
      const username = resp.username || email;
      const name = resp.username || username;
      const role = resp.role;
      const userTableNumber = resp.tableNumber;
      const tableId = resp.tableId;

      if (userId) {
        const userData: User = {
          id: userId,
          userId: userId,         
          username: username,
          email: email,           
          name: name,             
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
        fetched.role = cleanRole(fetched.role);
        fetched.tableNumber = tableNumber;
        fetched.tableId = tableId;
        
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
      const resp: RegisterResponse = await apiRegister(name, email, password);
      console.log('AuthContext: Registration response:', resp);
      
      
      
      return null;
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
      
    } finally {
      setAccessToken(null);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      localStorage.removeItem('tableId');
      localStorage.removeItem('tableNumber');
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
      localStorage.removeItem('tableId');
      localStorage.removeItem('tableNumber');
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