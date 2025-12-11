import api from "./client";

export interface RoleDto {
  id: string;
  name: string;
  description?: string;
}

export async function listRoles(): Promise<RoleDto[]> {
  const r = await api.get<RoleDto[]>("/roles");
  return r.data ?? [];
}

export async function getRole(id: string): Promise<RoleDto> {
  const r = await api.get<RoleDto>(`/roles/${id}`);
  return r.data;
}