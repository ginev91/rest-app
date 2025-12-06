import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import Layout from '@/components/Layout';
import { Order } from '@/types/order';
import { toast } from 'sonner';
import { ArrowRight, CheckCircle } from 'lucide-react';

const statusFlow = {
  pending: 'confirmed',
  confirmed: 'preparing',
  preparing: 'ready',
  ready: 'served',
  served: 'completed',
};

const Dashboard = () => {
  const [orders, setOrders] = useState<Order[]>([]);

  const updateOrderStatus = (orderId: string, newStatus: Order['status']) => {
    setOrders(prev => 
      prev.map(order => 
        order.id === orderId 
          ? { ...order, status: newStatus, updatedAt: new Date().toISOString() }
          : order
      )
    );
    toast.success(`Order #${orderId} status updated to ${newStatus}`);
  };

  const chnageOrderStatus = (orderId: string, status: Order['status']) => {
    updateOrderStatus(orderId, status);

  };

  const moveToNextStatus = (order: Order) => {
    const nextStatus = statusFlow[order.status as keyof typeof statusFlow];
    if (nextStatus) {
      updateOrderStatus(order.id, nextStatus as Order['status']);
    }
  };

  const activeOrders = orders.filter(o => o.status !== 'completed');

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold">Order Management</h2>
          <p className="text-muted-foreground">Manage and track all restaurant orders</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-4xl">{activeOrders.length}</CardTitle>
              <CardDescription>Active Orders</CardDescription>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle className="text-4xl">
                {activeOrders.filter(o => o.status === 'preparing').length}
              </CardTitle>
              <CardDescription>In Kitchen</CardDescription>
            </CardHeader>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle className="text-4xl">
                {activeOrders.filter(o => o.status === 'ready').length}
              </CardTitle>
              <CardDescription>Ready to Serve</CardDescription>
            </CardHeader>
          </Card>
        </div>

        <div className="space-y-4">
          <h3 className="text-xl font-semibold">Active Orders</h3>
          {activeOrders.length === 0 ? (
            <Card>
              <CardContent className="flex items-center justify-center py-12">
                <p className="text-muted-foreground">No active orders</p>
              </CardContent>
            </Card>
          ) : (
            <div className="space-y-4">
              {activeOrders.map(order => (
                <Card key={order.id}>
                  <CardHeader>
                    <div className="flex items-start justify-between">
                      <div>
                        <CardTitle>Order #{order.id}</CardTitle>
                        <CardDescription>
                          {order.username} • Table {order.tableNumber} • {new Date(order.createdAt).toLocaleTimeString()}
                        </CardDescription>
                      </div>
                      <Badge variant={order.status === 'ready' ? 'default' : 'secondary'}>
                        {order.status}
                      </Badge>
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="space-y-2">
                      {order.items.map((item, idx) => (
                        <div key={idx} className="flex justify-between">
                          <span>{item.quantity}x {item.menuItemName}</span>
                          <span className="font-semibold">${(item.price * item.quantity).toFixed(2)}</span>
                        </div>
                      ))}
                    </div>
                    <div className="flex items-center justify-between pt-4 border-t">
                      <span className="font-bold">Total: ${order.totalAmount.toFixed(2)}</span>
                      <div className="flex gap-2">
                        {order.status === 'confirmed' && (
                          <Button onClick={() => chnageOrderStatus(order.id, 'preparing')} className="gap-2">
                            Send to Kitchen
                            <ArrowRight className="h-4 w-4" />
                          </Button>
                        )}
                        {order.status !== 'confirmed' && statusFlow[order.status as keyof typeof statusFlow] && (
                          <Button onClick={() => moveToNextStatus(order)} className="gap-2">
                            Mark as {statusFlow[order.status as keyof typeof statusFlow]}
                            <CheckCircle className="h-4 w-4" />
                          </Button>
                        )}
                      </div>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          )}
        </div>
      </div>
    </Layout>
  );
};

export default Dashboard;
