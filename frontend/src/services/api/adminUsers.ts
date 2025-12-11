import api from "./client";
import type { User } from "@/types";


export async function listAdminUsers(): Promise<User[]> {
  const r = await api.get<User[]>("/admin/users");
  return r.data ?? [];
}

export async function changeRole(userId: string, roleName: string): Promise<User> {
  const r = await api.put<User>(`/admin/users/${userId}/role`, { roleName });
  return r.data;
}


export async function blockUser(userId: string): Promise<User> {
  const r = await api.put<User>(`/admin/users/${userId}/block`);
  return r.data;
}

export async function unblockUser(userId: string, roleName: string): Promise<User> {
  const r = await api.put<User>(`/admin/users/${userId}/unblock`, { roleName });
  return r.data;
}