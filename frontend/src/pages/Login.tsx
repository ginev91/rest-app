import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from 'sonner';
import { UtensilsCrossed, Loader2 } from 'lucide-react';

const Login = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [tableNumber, setTableNumber] = useState('');
  const [tablePin, setTablePin] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const { login, register, isAuthenticated, token } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated && token) {
      navigate('/menu', { replace: true });
    }
  }, [isAuthenticated, token, navigate]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (isRegister) {
        // Registration flow
        const user = await register(email, password, name);
        // ✅ Registration successful - switch to login and keep email filled
        toast.success('Account created successfully! Please login with your table PIN.');
        setIsRegister(false);
        setPassword(''); // Clear password for security
        setName(''); // Clear name
        // Keep email filled so user can easily login
      } else {
        // Login flow - requires table authentication for CUSTOMER role
        if (!tableNumber.trim()) {
          toast.error('Please enter your table number');
          setIsLoading(false);
          return;
        }

        if (!tablePin.trim() || tablePin.length !== 4) {
          toast.error('Please enter a valid 4-digit table PIN');
          setIsLoading(false);
          return;
        }

        // Login with table authentication
        const user = await login(email, password, parseInt(tableNumber), tablePin);
        if (user) {
          // Save table info to localStorage
          localStorage.setItem('tableNumber', tableNumber);
          
          if (user.tableId) {
            localStorage.setItem('tableId', user.tableId);
            console.log('Saved tableId:', user.tableId);
          }
          
          toast.success('Welcome back!');
          window.location.href = '/menu';
        } else {
          toast.error('Login failed. Please check your credentials and table PIN.');
        }
      }
    } catch (error: any) {
      console.error('Login/Register error:', error);
      toast.error(error.response?.data?.message || (isRegister ? 'Registration failed. Please try again.' : 'Login failed. Please check your credentials.'));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-background via-secondary/30 to-primary/5 p-4">
      <Card className="w-full max-w-md shadow-lg">
        <CardHeader className="space-y-1 text-center">
          <div className="flex justify-center mb-4">
            <div className="p-3 bg-primary/10 rounded-full">
              <UtensilsCrossed className="h-8 w-8 text-primary" />
            </div>
          </div>
          <CardTitle className="text-2xl font-bold">Restaurant Manager</CardTitle>
          <CardDescription>{isRegister ? 'Create a new account' : 'Sign in to access your table'}</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {isRegister && (
              <div className="space-y-2">
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  type="text"
                  placeholder="Your name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  className="transition-all"
                />
              </div>
            )}
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="transition-all"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="transition-all"
              />
            </div>
            
            {/* Table authentication - only for login */}
            {!isRegister && (
              <>
                <div className="space-y-2">
                  <Label htmlFor="tableNumber">Table Number</Label>
                  <Input
                    id="tableNumber"
                    type="number"
                    placeholder="Your table number"
                    value={tableNumber}
                    onChange={(e) => setTableNumber(e.target.value)}
                    required
                    min="1"
                    className="transition-all"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="tablePin">Table PIN</Label>
                  <Input
                    id="tablePin"
                    type="password"
                    placeholder="4-digit PIN"
                    value={tablePin}
                    onChange={(e) => setTablePin(e.target.value)}
                    required
                    maxLength={4}
                    className="transition-all"
                  />
                  <p className="text-xs text-muted-foreground">
                    Enter the PIN provided by your waiter
                  </p>
                </div>
              </>
            )}

            <Button
              type="submit"
              className="w-full"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {isRegister ? 'Creating account...' : 'Signing in...'}
                </>
              ) : (
                isRegister ? 'Create Account' : 'Sign In'
              )}
            </Button>
          </form>
          <div className="mt-4 text-center">
            <button
              type="button"
              onClick={() => {
                setIsRegister(!isRegister);
                setTableNumber('');
                setTablePin('');
                // ✅ Optionally clear form when switching modes
                if (isRegister) {
                  // Switching from register to login - keep email
                  setPassword('');
                  setName('');
                } else {
                  // Switching from login to register - clear everything
                  setPassword('');
                  setName('');
                }
              }}
              className="text-sm text-primary hover:underline"
            >
              {isRegister ? 'Already have an account? Sign in' : "Don't have an account? Register"}
            </button>
          </div>
          {!isRegister && (
            <div className="mt-6 text-center text-sm text-muted-foreground">
              <p className="mb-2">Demo credentials:</p>
              <div className="space-y-1 text-xs">
                <p>Customer: customer@example.com</p>
                <p>Waiter: waiter@example.com</p>
                <p>Admin: admin@example.com</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;