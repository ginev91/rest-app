import api from '@/services/api/client';

export type RoleShort = {
  id?: string;
  name?: string;
};

export type AdminUser = {
  id: string;
  username: string;
  fullName?: string;
  role?: RoleShort;
  blocked?: boolean;
  sessionTableNumber?: number | null;
};

export type RegisterPayload = {
  username: string;
  password: string;
  fullName: string;
  roleName?: string;
};

export const listAdminUsers = async (): Promise<AdminUser[]> => {
  const res = await api.get('/admin/users');
  console.debug('adminUsers.listAdminUsers: response.data =', res.data);
  return res.data as AdminUser[]; // ensure we return the body (array)
};

export const deleteAdminUser = async (userId: string): Promise<void> => {
  await api.delete(`/admin/users/${userId}`);
};

export const blockAdminUser = async (userId: string, blocked: boolean): Promise<AdminUser> => {
  const res = await api.put(`/admin/users/${userId}/block`, { blocked });
  return res.data as AdminUser;
};

export const changeUserRole = async (userId: string, roleName: string): Promise<AdminUser> => {
  const res = await api.put(`/admin/users/${userId}/role`, { roleName });
  return res.data as AdminUser;
};

export const createAdminUser = async (payload: RegisterPayload): Promise<AdminUser> => {
  const res = await api.post('/admin/users', payload);
  return res.data as AdminUser;
};