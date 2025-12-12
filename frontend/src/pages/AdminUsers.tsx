import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectTrigger, SelectContent, SelectItem, SelectValue } from '@/components/ui/select';
import { Input } from '@/components/ui/input';
import { toast } from 'sonner';
import { Loader2, Trash, UserPlus, Slash } from 'lucide-react';
import {
  listAdminUsers,
  AdminUser,
  blockAdminUser,
  deleteAdminUser,
  changeUserRole,
  createAdminUser,
} from '@/services/api/adminUsers';

const ROLE_OPTIONS = ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'];

type RoleFilter = 'ALL' | 'ROLE_USER' | 'ROLE_EMPLOYEE';

const AdminUsers: React.FC = () => {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [loading, setLoading] = useState(false);
  const [opLoadingIds, setOpLoadingIds] = useState<Record<string, boolean>>({});
  const [showCreate, setShowCreate] = useState(false);
  const [newUsername, setNewUsername] = useState('');
  const [newFullName, setNewFullName] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [newRole, setNewRole] = useState<string>('ROLE_USER');

  // filter: All / only USERS / only EMPLOYEES
  const [roleFilter, setRoleFilter] = useState<RoleFilter>('ALL');

  useEffect(() => {
    loadUsers();
  }, []);

  const loadUsers = async () => {
    setLoading(true);
    try {
      const res = await listAdminUsers();
      console.debug('listAdminUsers response:', res);

      let arr: AdminUser[] = [];
      if (Array.isArray(res)) {
        arr = res;
      } else if (res && Array.isArray((res as any).data)) {
        arr = (res as any).data;
      } else if (res && Array.isArray((res as any).users)) {
        arr = (res as any).users;
      } else if (res && typeof res === 'object' && (res as any).id) {
        arr = [res as AdminUser];
      } else {
        arr = [];
      }

      setUsers(arr || []);
    } catch (err: any) {
      console.error('Failed to load users', err);
      toast.error(err?.response?.data?.message || 'Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const setLoadingFor = (id: string, v: boolean) => setOpLoadingIds((prev) => ({ ...prev, [id]: v }));

  const handleBlockToggle = async (u: AdminUser) => {
    const id = u.id;
    setLoadingFor(id, true);
    try {
      const updated = await blockAdminUser(id, !Boolean(u.blocked));
      setUsers((prev) => prev.map(p => p.id === id ? updated : p));
      toast.success(updated.blocked ? 'User blocked' : 'User unblocked');
    } catch (err: any) {
      console.error('Block/unblock failed', err);
      toast.error(err?.response?.data?.message || 'Failed to update user');
    } finally {
      setLoadingFor(id, false);
    }
  };

  const handleDelete = async (u: AdminUser) => {
    if (!window.confirm(`Delete user ${u.username}? This is irreversible.`)) return;
    setLoadingFor(u.id, true);
    try {
      await deleteAdminUser(u.id);
      setUsers((prev) => prev.filter(p => p.id !== u.id));
      toast.success('User deleted');
    } catch (err: any) {
      console.error('Delete failed', err);
      toast.error(err?.response?.data?.message || 'Failed to delete user');
    } finally {
      setLoadingFor(u.id, false);
    }
  };

  const handleRoleChange = async (u: AdminUser, roleName: string) => {
    setLoadingFor(u.id, true);
    try {
      const updated = await changeUserRole(u.id, roleName);
      setUsers((prev) => prev.map(p => p.id === u.id ? updated : p));
      toast.success('Role updated');
    } catch (err: any) {
      console.error('Change role failed', err);
      toast.error(err?.response?.data?.message || 'Failed to change role');
    } finally {
      setLoadingFor(u.id, false);
    }
  };

  const handleCreate = async () => {
    if (!newUsername || !newPassword || !newFullName) {
      toast.error('Please fill username, password and full name');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        username: newUsername,
        password: newPassword,
        fullName: newFullName,
        roleName: newRole,
      };
      await createAdminUser(payload);
      toast.success('User created');
      setShowCreate(false);
      setNewUsername('');
      setNewFullName('');
      setNewPassword('');
      setNewRole('ROLE_USER');
      await loadUsers();
    } catch (err: any) {
      console.error('Create user failed', err);
      toast.error(err?.response?.data?.message || 'Failed to create user');
    } finally {
      setLoading(false);
    }
  };

  // Defensive rendering: ensure users is an array
  const displayedUsers = Array.isArray(users) ? users : [];

  // Apply role filter
  const filteredUsers = displayedUsers.filter(u => {
    if (roleFilter === 'ALL') return true;
    return (u.role?.name ?? '').toUpperCase() === roleFilter;
  });

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Admin — Users</h2>
          <div className="text-sm text-muted-foreground">Manage user accounts: block/unblock, roles and delete.</div>
        </div>

        <div className="flex items-center gap-2">
          <Select value={roleFilter} onValueChange={(v: RoleFilter) => setRoleFilter(v)}>
            <SelectTrigger className="w-[180px] h-8">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="ALL">All roles</SelectItem>
              <SelectItem value="ROLE_USER">Users</SelectItem>
              <SelectItem value="ROLE_EMPLOYEE">Employees</SelectItem>
            </SelectContent>
          </Select>

          <Button onClick={() => { setShowCreate(true); setNewRole('ROLE_EMPLOYEE'); }} variant="secondary" size="sm" className="gap-2">
            <UserPlus className="h-4 w-4" /> Create employee
          </Button>
        </div>
      </div>

      {showCreate && (
        <Card>
          <CardHeader>
            <CardTitle>Create new user</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-3 gap-2">
              <Input placeholder="Username" value={newUsername} onChange={(e) => setNewUsername(e.target.value)} />
              <Input placeholder="Full name" value={newFullName} onChange={(e) => setNewFullName(e.target.value)} />
              <Input type="password" placeholder="Password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} />
            </div>

            <div className="flex items-center gap-2">
              <Select value={newRole} onValueChange={(v) => setNewRole(v)}>
                <SelectTrigger className="w-[220px] h-8">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {ROLE_OPTIONS.map(r => <SelectItem key={r} value={r}>{r}</SelectItem>)}
                </SelectContent>
              </Select>
              <Button onClick={handleCreate} disabled={loading}>{loading ? 'Creating…' : 'Create'}</Button>
            </div>
          </CardContent>
        </Card>
      )}

      <div className="space-y-3">
        {loading ? (
          <div className="flex items-center gap-2"><Loader2 className="animate-spin" /> Loading users…</div>
        ) : filteredUsers.length === 0 ? (
          <Card>
            <CardContent className="py-8 text-center text-muted-foreground">No users</CardContent>
          </Card>
        ) : (
          filteredUsers.map(user => {
            const loadingOp = Boolean(opLoadingIds[user.id]);
            return (
              <Card key={user.id}>
                <CardHeader>
                  <div className="flex items-center justify-between w-full">
                    <div>
                      <div className="flex items-center gap-2">
                        <CardTitle className="text-lg">{user.username}</CardTitle>
                        <div className="text-sm text-muted-foreground">{user.fullName}</div>
                        {user.blocked && <Badge variant="destructive" className="ml-2">Blocked</Badge>}
                      </div>
                      <div className="text-xs text-muted-foreground">Role: {user.role?.name ?? '—'}</div>
                    </div>

                    <div className="flex items-center gap-2">
                      <Select
                        value={user.role?.name ?? 'ROLE_USER'}
                        onValueChange={(v) => handleRoleChange(user, v)}
                        disabled={loadingOp}
                      >
                        <SelectTrigger className="w-[180px] h-8">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {ROLE_OPTIONS.map(r => <SelectItem key={r} value={r}>{r}</SelectItem>)}
                        </SelectContent>
                      </Select>

                      <Button size="sm" variant="outline" onClick={() => handleBlockToggle(user)} disabled={loadingOp} className="gap-2">
                        <Slash className="h-4 w-4" />
                        {user.blocked ? 'Unblock' : 'Block'}
                      </Button>

                      <Button size="sm" variant="ghost" onClick={() => handleDelete(user)} disabled={loadingOp} className="text-red-600">
                        <Trash className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                </CardHeader>
              </Card>
            );
          })
        )}
      </div>
    </div>
  );
};

export default AdminUsers;