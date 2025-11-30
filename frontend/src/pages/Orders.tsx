import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import Layout from '@/components/Layout';
import { Order } from '@/types/order';
import { Clock, CheckCircle2, ChefHat, Utensils, Loader2 } from 'lucide-react';
import { getOrders } from '@/services/api/order';
import { toast } from 'sonner';
import { useAuth } from '@/contexts/AuthContext';

const statusConfig = {
  pending: { label: 'Pending', icon: Clock, color: 'bg-warning/10 text-warning' },
  confirmed: { label: 'Confirmed', icon: CheckCircle2, color: 'bg-accent/10 text-accent' },
  preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-primary/10 text-primary' },
  ready: { label: 'Ready', icon: Utensils, color: 'bg-success/10 text-success' },
  served: { label: 'Served', icon: CheckCircle2, color: 'bg-success/10 text-success' },
  completed: { label: 'Completed', icon: CheckCircle2, color: 'bg-muted text-muted-foreground' },
};

const Orders = () => {
  const { user, isLoading: authLoading } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    console.log('Orders useEffect triggered');
    console.log('Auth loading:', authLoading);
    console.log('User:', user);
    
    if (authLoading) {
      console.log('Waiting for auth to load...');
      return;
    }

    if (!user) {
      console.warn('No user found after auth loaded');
      setIsLoading(false);
      return;
    }

    if (user?.userId) {
      console.log('User ID found, fetching orders:', user.userId);
      fetchOrders();
    } else {
      console.warn('User exists but no ID:', user);
      setIsLoading(false);
    }
  }, [user, authLoading]);

  const fetchOrders = async () => {
    if (!user?.userId) {
      console.warn('Cannot fetch orders: No user ID');
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      console.log('Starting fetch orders for user:', user.userId);
      
      const data = await getOrders({ userId: user.userId });
      console.log('Orders fetched successfully:', data);
      setOrders(data);
    } catch (error: any) {
      console.error('Failed to fetch orders:', error);
      console.error('Error details:', error.response?.data || error.message);
      toast.error(`Failed to load orders: ${error.response?.data?.message || error.message}`);
      setOrders([]);
    } finally {
      setIsLoading(false);
      console.log('Fetch orders completed');
    }
  };

  const activeOrders = orders.filter(o => o.status !== 'completed');
  const completedOrders = orders.filter(o => o.status === 'completed');

  const OrderCard = ({ order }: { order: Order }) => {
    const config = statusConfig[order.status];
    const Icon = config.icon;

    return (
      <Card className="overflow-hidden">
        <CardHeader>
          <div className="flex items-start justify-between">
            <div>
              <CardTitle>Order #{order.id}</CardTitle>
              <CardDescription>
                {new Date(order.createdAt).toLocaleString()}
                {order.tableNumber && ` • Table ${order.tableNumber}`}
              </CardDescription>
            </div>
            <Badge className={config.color} variant="secondary">
              <Icon className="h-3 w-3 mr-1" />
              {config.label}
            </Badge>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            {order.items.map((item, idx) => (
              <div key={idx} className="flex justify-between items-center">
                <div>
                  <p className="font-medium">{item.menuItemName}</p>
                  <p className="text-sm text-muted-foreground">Qty: {item.quantity}</p>
                </div>
                <p className="font-semibold">${(item.price * item.quantity).toFixed(2)}</p>
              </div>
            ))}
          </div>
          <Separator />
          <div className="flex justify-between items-center">
            <p className="text-lg font-bold">Total</p>
            <p className="text-2xl font-bold text-primary">${order.totalAmount.toFixed(2)}</p>
          </div>
        </CardContent>
      </Card>
    );
  };

  if (authLoading || isLoading) {
    return (
      <Layout>
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">
            {authLoading ? 'Loading user information...' : 'Loading orders...'}
          </p>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-8">
        {activeOrders.length > 0 && (
          <div className="space-y-4">
            <div>
              <h2 className="text-2xl font-bold">Active Orders</h2>
              <p className="text-muted-foreground">Track your current orders</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {activeOrders.map(order => (
                <OrderCard key={order.id} order={order} />
              ))}
            </div>
          </div>
        )}

        {completedOrders.length > 0 && (
          <div className="space-y-4">
            <div>
              <h2 className="text-2xl font-bold">Order History</h2>
              <p className="text-muted-foreground">Your past orders</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {completedOrders.map(order => (
                <OrderCard key={order.id} order={order} />
              ))}
            </div>
          </div>
        )}

        {orders.length === 0 && (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Utensils className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-semibold mb-2">No orders yet</p>
              <p className="text-muted-foreground">Start by browsing our menu</p>
            </CardContent>
          </Card>
        )}
      </div>
    </Layout>
  );
};

export default Orders;