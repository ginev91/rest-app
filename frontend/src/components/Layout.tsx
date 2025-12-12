import React, { ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import {
  UtensilsCrossed,
  LogOut,
  Menu,
  ShoppingCart,
  Users,
  LayoutDashboard,
  Sparkles,
  Calendar,
  User as UserIcon,
} from 'lucide-react';
import { cn } from '@/lib/utils';

interface LayoutProps {
  children: ReactNode;
}

const Layout = ({ children }: LayoutProps) => {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const navItems = [
    { path: '/menu', label: 'Menu', icon: Menu, roles: ['ROLE_USER', 'ROLE_EMPLOYEE'] },
    { path: '/orders', label: 'Orders', icon: ShoppingCart, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/reservations', label: 'Reservations', icon: Calendar, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/tables', label: 'Tables', icon: Users, roles: ['ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/recommendations', label: 'AI Recommendations', icon: Sparkles, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/favorites', label: 'Favorites', icon: Sparkles, roles: ['ROLE_USER'] },
    { path: '/admin', label: 'Users', icon: Users, roles: ['ROLE_ADMIN'] },
    { path: '/admin/menu', label: 'Admin Menu', icon: Menu, roles: ['ROLE_ADMIN'] },
  ];

  const userRoles: string[] = React.useMemo(() => {
    if (!user) return ['ROLE_USER'];
    const r = (user as any).role;
    if (Array.isArray(r)) return r;
    if (typeof r === 'string') return [r];
    return ['ROLE_USER'];
  }, [user]);

  const visibleNavItems = navItems.filter(item => item.roles.some(role => userRoles.includes(role)));

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="sticky top-0 z-50 w-full border-b bg-card/95 backdrop-blur supports-[backdrop-filter]:bg-card/60">
        <div className="container flex h-16 items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="p-2 bg-primary/10 rounded-lg">
              <UtensilsCrossed className="h-5 w-5 text-primary" />
            </div>
            <div>
              <h1 className="text-lg font-bold">Restaurant Manager</h1>
              {user && (
                <p className="text-xs text-muted-foreground">
                  {user.username} • {Array.isArray(userRoles) ? userRoles.join(', ') : userRoles} • {user.tableNumber ?? ''}
                </p>
              )}
            </div>
          </div>

          <div className="flex items-center gap-2">
            {user && (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => navigate('/profile')}
                className="gap-2"
              >
                <UserIcon className="h-4 w-4 mr-1" />
                Profile
              </Button>
            )}

            <Button variant="ghost" size="sm" onClick={handleLogout}>
              <LogOut className="h-4 w-4 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </header>

      {/* Navigation */}
      <nav className="border-b bg-muted/30">
        <div className="container flex h-12 items-center gap-1">
          {visibleNavItems.map((item) => {
            const Icon = item.icon;
            const isActive =
              location.pathname === item.path || location.pathname.startsWith(item.path + '/');
            return (
              <Button
                key={item.path}
                variant={isActive ? 'default' : 'ghost'}
                size="sm"
                onClick={() => navigate(item.path)}
                className={cn(
                  'gap-2 transition-all',
                  !isActive && 'hover:bg-primary/10'
                )}
                aria-current={isActive ? 'page' : undefined}
              >
                <Icon className="h-4 w-4" />
                {item.label}
              </Button>
            );
          })}
        </div>
      </nav>

      {/* Main Content */}
      <main className="container py-6">
        {children}
      </main>
    </div>
  );
};

export default Layout;