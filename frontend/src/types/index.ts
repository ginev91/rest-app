

export type RoleName = "ROLE_USER" | "ROLE_EMPLOYEE" | "ROLE_ADMIN" | "ROLE_BLOCKED";

/* ---------- Auth / User ---------- */

export interface User {
  id?: string;
  userId?: string; 
  username?: string;
  email?: string;
  name?: string;
  fullName?: string;
  role?: string;
  roles?: RoleName[];
  tableNumber?: number;
  tableId?: string | null;
}

export interface AuthState {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
  tableNumber?: number;
  tablePin?: string;
}

export interface LoginResponse {
  token?: string;
  accessToken?: string;
  user?: User;
  username?: string;
  userId?: string;
  role?: RoleName;
  tableNumber?: number;
  tableId?: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  fullName?: string;
  email?: string;
}

export interface AuthResponseDto {
  token?: string;
  
}

/* ---------- Roles / Admin ---------- */

export interface RoleDto {
  id: string;
  name: string;
  description?: string;
}

export interface RoleChangeRequestDto {
  roleName: string;
}

/* ---------- Menu / Items ---------- */

export interface MenuItem {
  id: string;
  name: string;
  description?: string;
  price: number;
  category?: string;
  calories?: number;
  protein?: number;
  image?: string;
  available?: boolean;
  tags?: string[];
}

/* ---------- Orders ---------- */

export interface OrderItem {
  id?: string;
  menuItemId: string;
  menuItemName?: string;
  quantity: number;
  price?: number;
  status?: string;
  specialInstructions?: string;
}

export interface Order {
  id: string;
  userId?: string;
  username?: string;
  tableNumber?: number;
  items: OrderItem[];
  status: string;
  totalAmount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateOrderRequest {
  tableId: string;
  waiterId?: string;
  items: { menuItemId: string; quantity: number; note?: string }[];
}

/* ---------- Tables & Reservations ---------- */

export interface RestaurantTable {
  id: string;
  number: number;
  seats?: number;
  capacity?: number;
  occupied?: boolean;
  metadata?: Record<string, any>;
}

export interface ReservationRequestDto {
  from: string; 
  to: string; 
  requestedBy?: string; 
  userId?: string;
}

export interface TableReservationResponseDto {
  id: string;
  tableId: string;
  userId?: string | null;
  from: string;
  to: string;
  status?: string;
  requestedBy?: string;
}

/* ---------- Recommendations / AI ---------- */

export interface RecommendationRequest {
  prompt: string;
}

export interface RecommendationResponse {
  menuItemName?: string;
  recipe?: string;
  description?: string;
  calories?: number;
  protein?: number;
  fats?: number;
  carbs?: number;
  ingredients?: string[] | null;
  score?: number;
}

/* ---------- Kitchen / Internal DTOs ---------- */

export interface KitchenStatusDto {
  status: string;
  note?: string;
}

/* ---------- Profile / User update DTOs ---------- */

export interface UpdateProfileRequestDto {
  username?: string;
  fullName?: string;
}

export interface ChangePasswordRequestDto {
  oldPassword: string;
  newPassword: string;
}

export type AnyObject = Record<string, any>;