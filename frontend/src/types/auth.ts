export interface User {
  username: any;
  userId: any;
  id: string;
  email: string;
  name: string;
  role: string;
  tableNumber?: number;
  tableId?: string;
  
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}
