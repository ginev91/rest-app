import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import Layout from '@/components/Layout';
import { Order } from '@/types/order';
import { Clock, CheckCircle2, ChefHat, Utensils } from 'lucide-react';

const mockOrders: Order[] = [
  {
    id: '1',
    userId: '1',
    userName: 'John Doe',
    tableNumber: 5,
    items: [
      { menuItemId: '1', menuItemName: 'Grilled Chicken Breast', quantity: 1, price: 18.99 },
      { menuItemId: '2', menuItemName: 'Caesar Salad', quantity: 1, price: 12.99 },
    ],
    status: 'preparing',
    totalAmount: 31.98,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: '2',
    userId: '1',
    userName: 'John Doe',
    items: [
      { menuItemId: '3', menuItemName: 'Salmon Fillet', quantity: 1, price: 24.99 },
    ],
    status: 'completed',
    totalAmount: 24.99,
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 86400000).toISOString(),
  },
];

const statusConfig = {
  pending: { label: 'Pending', icon: Clock, color: 'bg-warning/10 text-warning' },
  confirmed: { label: 'Confirmed', icon: CheckCircle2, color: 'bg-accent/10 text-accent' },
  preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-primary/10 text-primary' },
  ready: { label: 'Ready', icon: Utensils, color: 'bg-success/10 text-success' },
  served: { label: 'Served', icon: CheckCircle2, color: 'bg-success/10 text-success' },
  completed: { label: 'Completed', icon: CheckCircle2, color: 'bg-muted text-muted-foreground' },
};

const Orders = () => {
  const activeOrders = mockOrders.filter(o => o.status !== 'completed');
  const completedOrders = mockOrders.filter(o => o.status === 'completed');

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

        {mockOrders.length === 0 && (
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
