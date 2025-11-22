export interface User {
  id: string;
  email: string;
  name: string;
  role: 'customer' | 'waiter' | 'admin';
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}
