export interface User {
  id: string;
  email: string;
  name: string;
  role: 'ROLE_USER' | 'ROLE_EMPLOYEE' | 'ROLE_ADMIN';
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}
