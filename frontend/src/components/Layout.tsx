import { ReactNode } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { UtensilsCrossed, LogOut, Menu, ShoppingCart, Users, LayoutDashboard, Sparkles } from 'lucide-react';
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
    { path: '/menu', label: 'Menu', icon: Menu, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/orders', label: 'Orders', icon: ShoppingCart, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, roles: ['ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/tables', label: 'Tables', icon: Users, roles: ['ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
    { path: '/recommendations', label: 'AI Recommendations', icon: Sparkles, roles: ['ROLE_USER', 'ROLE_EMPLOYEE', 'ROLE_ADMIN'] },
  ];

  const visibleNavItems = navItems.filter(item => item.roles.includes(user?.role || 'ROLE_USER'));

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
                  {user.username} • {user.role} • {user.tableNumber}
                </p>
              )}
            </div>
          </div>
          <Button variant="ghost" size="sm" onClick={handleLogout}>
            <LogOut className="h-4 w-4 mr-2" />
            Logout
          </Button>
        </div>
      </header>

      {/* Navigation */}
      <nav className="border-b bg-muted/30">
        <div className="container flex h-12 items-center gap-1">
          {visibleNavItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
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
