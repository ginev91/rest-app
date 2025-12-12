import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { toast } from 'sonner';
import { UtensilsCrossed, Loader2 } from 'lucide-react';

/**
 * Combined Login component:
 * - Customer / Table login (default): requires tableNumber + tablePin
 * - Employee / Staff login (toggle "Employee mode"): does NOT require tableNumber / tablePin
 *
 * Registration is only available for customer accounts in this UI.
 * If you create employee accounts, those should be created via admin tooling.
 */
const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [tableNumber, setTableNumber] = useState('');
  const [tablePin, setTablePin] = useState('');
  const [isRegister, setIsRegister] = useState(false);
  const [isEmployee, setIsEmployee] = useState(false); 
  const [isLoading, setIsLoading] = useState(false);
  const { login, register, isAuthenticated, token } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isAuthenticated && token) {
      if (isEmployee) navigate('/tables', { replace: true });
      else navigate('/menu', { replace: true });
    }
  }, [isAuthenticated, token, navigate, isEmployee]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);

    try {
      if (isRegister) {
        if (isEmployee) {
          toast.error('Employee accounts must be created by an administrator.');
          setIsLoading(false);
          return;
        }

        const user = await register(email, password, name);
        toast.success('Account created successfully! Please login with your table PIN.');
        setIsRegister(false);
        setPassword('');
        setName('');
        setTableNumber('');
        setTablePin('');
      } else {
        if (isEmployee) {
          // Employee login: no tableNumber / tablePin required
          const user = await login(email, password, undefined, undefined);
          if (user) {
            toast.success('Welcome back!');
            navigate('/menu', { replace: true });
          } else {
            toast.error('Login failed. Please check your credentials.');
          }
        } else {
          // Customer / table login
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

          const user = await login(email, password, parseInt(tableNumber), tablePin);
          if (user) {
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
      }
    } catch (error: any) {
      console.error('Login/Register error:', error);
      toast.error(
        error.response?.data?.message ||
        (isRegister ? 'Registration failed. Please try again.' : 'Login failed. Please check your credentials.')
      );
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
          <CardDescription>
            {isRegister
              ? 'Create a new customer account'
              : isEmployee
              ? 'Employee sign in — no table PIN required'
              : 'Sign in to access your table'}
          </CardDescription>
        </CardHeader>

        <CardContent>
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <input
                id="employeeMode"
                type="checkbox"
                checked={isEmployee}
                onChange={() => {
                  setIsEmployee((v) => !v);
                  setIsRegister(false);
                  setTableNumber('');
                  setTablePin('');
                }}
                className="h-4 w-4"
              />
              <label htmlFor="employeeMode" className="text-sm">
                Employee / Staff login
              </label>
            </div>

            <button
              type="button"
              onClick={() => {
                // toggle register only allowed in customer mode
                if (isEmployee) {
                  toast('Switch to customer mode to register accounts');
                  return;
                }
                setIsRegister((r) => !r);
                setTableNumber('');
                setTablePin('');
                setPassword('');
                setName('');
              }}
              className="text-sm text-primary hover:underline"
            >
              {isRegister ? 'Already have an account? Sign in' : "Don't have an account? Register"}
            </button>
          </div>

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

            {/* Table authentication - only for customer login */}
            {!isRegister && !isEmployee && (
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
                  <p className="text-xs text-muted-foreground">Enter the PIN provided by your waiter</p>
                </div>
              </>
            )}

            <Button type="submit" className="w-full" disabled={isLoading}>
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

          {!isRegister && (
            <div className="mt-6 text-center text-sm text-muted-foreground">
              <p className="mb-2">Demo credentials:</p>
              <div className="space-y-1 text-xs">
                <p>Customer: customer@example.com</p>
                <p>Waiter: waiter@example.com</p>
                <p>Admin: admin@example.com</p>
                <p>Employee: employee@example.com</p>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

export default Login;