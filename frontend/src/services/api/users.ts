import api from "./client";
import type { User, LoginRequest, LoginResponse, RegisterRequest } from "@/types";


export async function listUsers(): Promise<User[]> {
  const r = await api.get<User[]>("/users");
  return r.data ?? [];
}

export async function getUser(id: string): Promise<User> {
  const r = await api.get<User>(`/users/${id}`);
  return r.data;
}

export async function createUser(payload: RegisterRequest): Promise<User> {
  const r = await api.post<User>("/users", payload);
  return r.data;
}

export async function updateUser(id: string, payload: Partial<RegisterRequest>): Promise<User> {
  const r = await api.put<User>(`/users/${id}`, payload);
  return r.data;
}

export async function deleteUser(id: string): Promise<void> {
  await api.delete(`/users/${id}`);
}

export async function assignRole(userId: string, roleName: string): Promise<User> {
  const r = await api.post<User>(`/users/${userId}/roles`, null, { params: { roleName } });
  return r.data;
}

export async function registerAndLogin(payload: RegisterRequest): Promise<LoginResponse> {
  const r = await api.post<LoginResponse>("/auth/register", payload);
  return r.data;
}