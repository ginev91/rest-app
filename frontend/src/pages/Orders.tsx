import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import Layout from '@/components/Layout';
import { Order } from '@/types';
import { Clock, CheckCircle2, ChefHat, Utensils, Loader2, User, Table, XCircle } from 'lucide-react';
import { getOrders } from '@/services/api/order';
import { toast } from 'sonner';
import { useAuth } from '@/contexts/AuthContext';

const statusConfig: Record<string, { label: string; icon: any; color: string }> = {
  pending: { label: 'Pending', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  confirmed: { label: 'Confirmed', icon: CheckCircle2, color: 'bg-blue-500/10 text-blue-700 dark:text-blue-500' },
  preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  ready: { label: 'Ready', icon: Utensils, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  served: { label: 'Served', icon: CheckCircle2, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  completed: { label: 'Completed', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },
  Pending: { label: 'Pending', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  Preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  Ready: { label: 'Ready', icon: Utensils, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  Served: { label: 'Served', icon: CheckCircle2, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  Cancelled: { label: 'Cancelled', icon: XCircle, color: 'bg-red-500/10 text-red-700 dark:text-red-500' },
};

const Orders = () => {
  const { user, isLoading: authLoading } = useAuth();
  const [myOrders, setMyOrders] = useState<Order[]>([]);
  const [tableOrders, setTableOrders] = useState<Order[]>([]);
  const [isLoadingMy, setIsLoadingMy] = useState(true);
  const [isLoadingTable, setIsLoadingTable] = useState(false);
  const [activeTab, setActiveTab] = useState<'my' | 'table'>('my');

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
      setIsLoadingMy(false);
      return;
    }

    if (user?.userId) {
      console.log('User ID found, fetching my orders:', user.userId);
      fetchMyOrders();
    } else {
      console.warn('User exists but no ID:', user);
      setIsLoadingMy(false);
    }
  }, [user, authLoading]);

  const fetchMyOrders = async () => {
    if (!user?.userId) {
      console.warn('Cannot fetch orders: No user ID');
      setIsLoadingMy(false);
      return;
    }

    try {
      setIsLoadingMy(true);
      console.log('Fetching my orders for user:', user.userId);
      
      const data = await getOrders({ userId: user.userId });
      console.log('My orders fetched successfully:', data);
      setMyOrders(data);
    } catch (error: any) {
      console.error('Failed to fetch my orders:', error);
      toast.error(`Failed to load your orders: ${error.response?.data?.message || error.message}`);
      setMyOrders([]);
    } finally {
      setIsLoadingMy(false);
    }
  };

  const fetchTableOrders = async () => {
    const tableId = user?.tableId || localStorage.getItem('tableId');
    const tableNumber = user?.tableNumber || localStorage.getItem('tableNumber');

    if (!tableId && !tableNumber) {
      toast.error('No table information available');
      return;
    }

    try {
      setIsLoadingTable(true);
      console.log('Fetching table orders:', { tableId, tableNumber });
      
      const params = tableId ? { tableId } : { tableNumber: tableNumber };
      const data = await getOrders(params);
      
      console.log('Table orders fetched successfully:', data);
      setTableOrders(data);
    } catch (error: any) {
      console.error('Failed to fetch table orders:', error);
      toast.error(`Failed to load table orders: ${error.response?.data?.message || error.message}`);
      setTableOrders([]);
    } finally {
      setIsLoadingTable(false);
    }
  };

  const handleTabChange = (value: string) => {
    setActiveTab(value as 'my' | 'table');
    
    if (value === 'table' && tableOrders.length === 0) {
      fetchTableOrders();
    }
  };

  const OrderCard = ({ order }: { order: Order }) => {
    
    const config = statusConfig[order.status] || statusConfig['Pending'];
    const Icon = config?.icon || Clock;
    const userName = order.username;

    console.log('OrderCard render:', { 
      orderId: order.id || order.id, 
      status: order.status, 
      configFound: !!statusConfig[order.status] 
    });

    return (
      <Card className="overflow-hidden">
        <CardHeader>
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <CardTitle className="flex items-center gap-2 flex-wrap">
                Order #{order.id || order.id}
                {userName && (
                  <Badge variant="outline" className="font-normal">
                    <User className="h-3 w-3 mr-1" />
                    {userName}
                  </Badge>
                )}
              </CardTitle>
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
          <div className="space-y-3">
            {order.items.map((item, idx) => {
              const itemConfig = item.status ? statusConfig[item.status] : null;
              const ItemIcon = itemConfig?.icon;

              return (
                <div key={item.id || `${item.menuItemId}-${idx}`} className="flex items-start justify-between gap-4">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <p className="font-medium">{item.menuItemName}</p>
                      {itemConfig && (
                        <Badge 
                          variant="outline" 
                          className={`${itemConfig.color} text-xs flex-shrink-0`}
                        >
                          {ItemIcon && <ItemIcon className="h-2.5 w-2.5 mr-1" />}
                          {itemConfig.label}
                        </Badge>
                      )}
                    </div>
                    <div className="flex items-center justify-between mt-1">
                      <p className="text-sm text-muted-foreground">Qty: {item.quantity}</p>
                      <p className="font-semibold">${(item.price * item.quantity).toFixed(2)}</p>
                    </div>
                    {item.specialInstructions && (
                      <p className="text-xs text-muted-foreground mt-1 italic">
                        Note: {item.specialInstructions}
                      </p>
                    )}
                  </div>
                </div>
              );
            })}
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

  const OrdersList = ({ orders, isLoading }: { orders: Order[], isLoading: boolean }) => {
    console.log('OrdersList render:', { ordersCount: orders.length, isLoading });

    if (isLoading) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading orders...</p>
        </div>
      );
    }

    if (orders.length === 0) {
      return (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Utensils className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-semibold mb-2">No orders yet</p>
            <p className="text-muted-foreground">Start by browsing our menu</p>
          </CardContent>
        </Card>
      );
    }

    
    const activeOrders = orders.filter(o => 
      o.status.toLowerCase() !== 'completed' && 
      o.status.toLowerCase() !== 'cancelled'
    );
    const completedOrders = orders.filter(o => 
      o.status.toLowerCase() === 'completed' || 
      o.status.toLowerCase() === 'cancelled'
    );

    console.log('Filtered orders:', { active: activeOrders.length, completed: completedOrders.length });

    return (
      <div className="space-y-8">
        {activeOrders.length > 0 && (
          <div className="space-y-4">
            <div>
              <h2 className="text-2xl font-bold">Active Orders</h2>
              <p className="text-muted-foreground">Track your current orders</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {activeOrders.map(order => (
                <OrderCard key={order.id || order.id} order={order} />
              ))}
            </div>
          </div>
        )}

        {completedOrders.length > 0 && (
          <div className="space-y-4">
            <div>
              <h2 className="text-2xl font-bold">Order History</h2>
              <p className="text-muted-foreground">Past orders</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {completedOrders.map(order => (
                <OrderCard key={order.id || order.id} order={order} />
              ))}
            </div>
          </div>
        )}
      </div>
    );
  };

  if (authLoading) {
    return (
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading user information...</p>
        </div>
    );
  }

  return (
      <Tabs value={activeTab} onValueChange={handleTabChange} className="space-y-6">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="my" className="gap-2">
            <User className="h-4 w-4" />
            My Orders
          </TabsTrigger>
          <TabsTrigger value="table" className="gap-2">
            <Table className="h-4 w-4" />
            Table Orders
          </TabsTrigger>
        </TabsList>

        <TabsContent value="my">
          <OrdersList orders={myOrders} isLoading={isLoadingMy} />
        </TabsContent>

        <TabsContent value="table">  
          <OrdersList orders={tableOrders} isLoading={isLoadingTable} />
        </TabsContent>
      </Tabs>
  );
};

export default Orders;