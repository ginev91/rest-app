import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import Layout from '@/components/Layout';
import { Order } from '@/types/order';
import { ArrowLeft, Users, Clock, DollarSign } from 'lucide-react';
import { toast } from 'sonner';
import api from '@/services/api/client';

const statusConfig = {
  pending: { label: 'Pending', color: 'bg-warning/10 text-warning' },
  confirmed: { label: 'Confirmed', color: 'bg-accent/10 text-accent' },
  preparing: { label: 'Preparing', color: 'bg-primary/10 text-primary' },
  ready: { label: 'Ready', color: 'bg-success/10 text-success' },
  served: { label: 'Served', color: 'bg-success/10 text-success' },
  completed: { label: 'Completed', color: 'bg-muted text-muted-foreground' },
};

const TableDetails = () => {
  const { tableId } = useParams<{ tableId: string }>();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [tableNumber, setTableNumber] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (tableId) {
      fetchTableOrders();
    }
  }, [tableId]);

  const fetchTableOrders = async () => {
    try {
      setIsLoading(true);
      // Fetch all orders for this table
      const response = await api.get(`/api/tables/${tableId}/orders`);
      const data = response.data;
      
      setOrders(data);
      if (data.length > 0) {
        setTableNumber(data[0].tableNumber);
      }
      
      console.log('Table orders fetched:', data);
    } catch (error: any) {
      console.error('Failed to fetch table orders:', error);
      toast.error('Failed to load table orders');
    } finally {
      setIsLoading(false);
    }
  };

  const totalAmount = orders.reduce((sum, order) => sum + order.totalAmount, 0);
  const activeOrders = orders.filter(o => o.status !== 'completed');
  const completedOrders = orders.filter(o => o.status === 'completed');

  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[400px]">
          <div className="text-muted-foreground">Loading table details...</div>
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate('/tables')}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h2 className="text-3xl font-bold">Table {tableNumber || tableId}</h2>
            <p className="text-muted-foreground">View all orders for this table</p>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Orders</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{orders.length}</div>
              <p className="text-xs text-muted-foreground">
                {activeOrders.length} active, {completedOrders.length} completed
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Active Orders</CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{activeOrders.length}</div>
              <p className="text-xs text-muted-foreground">Currently being prepared</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Bill</CardTitle>
              <DollarSign className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">${totalAmount.toFixed(2)}</div>
              <p className="text-xs text-muted-foreground">Combined from all orders</p>
            </CardContent>
          </Card>
        </div>

        {/* Active Orders */}
        {activeOrders.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">Active Orders</h3>
            {activeOrders.map(order => (
              <Card key={order.id}>
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>Order #{order.id}</CardTitle>
                      <CardDescription>
                        {new Date(order.createdAt).toLocaleString()}
                        {order.userId && ` • Customer ID: ${order.userId}`}
                      </CardDescription>
                    </div>
                    <Badge className={statusConfig[order.status].color} variant="secondary">
                      {statusConfig[order.status].label}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-3">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="flex justify-between items-start">
                        <div className="flex-1">
                          <p className="font-medium">{item.menuItemName}</p>
                          <div className="flex items-center gap-4 text-sm text-muted-foreground">
                            <span>Qty: {item.quantity}</span>
                            <span>${item.price.toFixed(2)} each</span>
                          </div>
                        </div>
                        <p className="font-semibold">${(item.price * item.quantity).toFixed(2)}</p>
                      </div>
                    ))}
                  </div>
                  <Separator />
                  <div className="flex justify-between items-center">
                    <p className="font-semibold">Order Total</p>
                    <p className="text-xl font-bold text-primary">${order.totalAmount.toFixed(2)}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {/* Completed Orders */}
        {completedOrders.length > 0 && (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">Completed Orders</h3>
            {completedOrders.map(order => (
              <Card key={order.id} className="opacity-75">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>Order #{order.id}</CardTitle>
                      <CardDescription>
                        {new Date(order.createdAt).toLocaleString()}
                      </CardDescription>
                    </div>
                    <Badge className={statusConfig[order.status].color} variant="secondary">
                      {statusConfig[order.status].label}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="space-y-2">
                    {order.items.map((item, idx) => (
                      <div key={idx} className="flex justify-between items-center text-sm">
                        <div>
                          <p className="font-medium">{item.menuItemName}</p>
                          <p className="text-muted-foreground">Qty: {item.quantity}</p>
                        </div>
                        <p className="font-semibold">${(item.price * item.quantity).toFixed(2)}</p>
                      </div>
                    ))}
                  </div>
                  <Separator />
                  <div className="flex justify-between items-center">
                    <p className="font-semibold">Order Total</p>
                    <p className="text-lg font-bold">${order.totalAmount.toFixed(2)}</p>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
        )}

        {orders.length === 0 && (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Users className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-semibold mb-2">No orders for this table</p>
              <p className="text-muted-foreground">Orders will appear here once customers place them</p>
            </CardContent>
          </Card>
        )}
      </div>
    </Layout>
  );
};

export default TableDetails;