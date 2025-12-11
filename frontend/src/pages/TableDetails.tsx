import { useEffect, useState } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import Layout from '@/components/Layout';
import { Order, OrderItem } from '@/types';
import { ArrowLeft, Users, Clock, DollarSign, Loader2, ChefHat, User, List } from 'lucide-react';
import { toast } from 'sonner';
import api from '@/services/api/client';

const orderStatusConfig: Record<string, { label: string; color: string }> = {
  'New': { label: 'New', color: 'bg-purple-500/10 text-purple-700 dark:text-purple-500' },
  'Pending': { label: 'Pending', color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  'In Progress': { label: 'In Progress', color: 'bg-blue-500/10 text-blue-700 dark:text-blue-500' },
  'Ready': { label: 'Ready', color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  'Completed': { label: 'Completed', color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },
  'Cancelled': { label: 'Cancelled', color: 'bg-red-500/10 text-red-700 dark:text-red-500' },
};

const itemStatusConfig: Record<string, { label: string; color: string }> = {
  'Pending': { label: 'Pending', color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  'Preparing': { label: 'Preparing', color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  'Ready': { label: 'Ready', color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  'Served': { label: 'Served', color: 'bg-blue-500/10 text-blue-700 dark:text-blue-500' },
  'Cancelled': { label: 'Cancelled', color: 'bg-red-500/10 text-red-700 dark:text-red-500' },
};

const orderStatuses = ['New', 'Pending', 'In Progress', 'Ready', 'Completed', 'Cancelled'];
const itemStatuses = ['Pending', 'Preparing', 'Ready', 'Served', 'Cancelled'];

interface GroupedOrder {
  userId: string;
  userName: string;
  items: (OrderItem & { orderId: string; orderStatus: string })[];
  total: number;
}

const TableDetails = () => {
  const { tableId } = useParams<{ tableId: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [orders, setOrders] = useState<Order[]>([]);
  const [tableNumber, setTableNumber] = useState<number | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [updatingStatus, setUpdatingStatus] = useState<string | null>(null);
  const [viewMode, setViewMode] = useState<'orders' | 'users'>('orders');

  useEffect(() => {
    let extractedTableNumber: number | null = null;

    if (tableId) {
      const match = tableId.match(/\d+/);
      if (match) {
        extractedTableNumber = parseInt(match[0], 10);
      }
    }

    if (!extractedTableNumber) {
      const tableParam = searchParams.get('table');
      if (tableParam) {
        extractedTableNumber = parseInt(tableParam, 10);
      }
    }

    console.log('TableDetails - Extracted table number:', {
      tableId,
      extractedTableNumber,
    });

    if (extractedTableNumber && !isNaN(extractedTableNumber)) {
      setTableNumber(extractedTableNumber);
      fetchTableOrders(extractedTableNumber);
    } else {
      toast.error('Invalid table number');
      setIsLoading(false);
    }
  }, [tableId, searchParams]);

  const fetchTableOrders = async (tableNum: number) => {
    try {
      setIsLoading(true);
      console.log('Fetching orders for table:', tableNum);
      
      const response = await api.get(`orders?tableNumber=${tableNum}`);
      const data = response.data;
      
      console.log('Table orders fetched:', data);
      setOrders(data);
    } catch (error: any) {
      console.error('Failed to fetch table orders:', error);
      toast.error('Failed to load table orders');
      setOrders([]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleOrderStatusChange = async (orderId: string, newStatus: string) => {
    const statusKey = `order-${orderId}`;
    setUpdatingStatus(statusKey);
    
    try {
      console.log('Updating order status:', { orderId, newStatus });
      
      await api.patch(`orders/${orderId}/status`, { status: newStatus });
      
      
      setOrders(prevOrders =>
        prevOrders.map(order =>
          (order.id || order.id) === orderId
            ? { ...order, status: newStatus }
            : order
        )
      );
      
      toast.success('Order status updated');
    } catch (error: any) {
      console.error('Failed to update order status:', error);
      toast.error(error.response?.data?.message || 'Failed to update order status');
    } finally {
      setUpdatingStatus(null);
    }
  };

  const handleItemStatusChange = async (orderId: string, itemId: string, newStatus: string) => {
    const statusKey = `item-${orderId}-${itemId}`;
    setUpdatingStatus(statusKey);
    
    try {
      console.log('Updating item status:', { orderId, itemId, newStatus });
      
      await api.patch(`orders/${orderId}/items/${itemId}/status`, { status: newStatus });
      
      
      setOrders(prevOrders =>
        prevOrders.map(order => {
          if ((order.id || order.id) === orderId) {
            return {
              ...order,
              items: order.items.map(item =>
                (item.id || item.menuItemId) === itemId
                  ? { ...item, status: newStatus }
                  : item
              ),
            };
          }
          return order;
        })
      );
      
      toast.success('Item status updated');
    } catch (error: any) {
      console.error('Failed to update item status:', error);
      toast.error(error.response?.data?.message || 'Failed to update item status');
    } finally {
      setUpdatingStatus(null);
    }
  };

  const groupOrdersByUser = (): GroupedOrder[] => {
    const grouped: Record<string, GroupedOrder> = {};

    orders.forEach(order => {
      const userId = order.id || 'unknown';
      const userName = order.username || 'Unknown User';
      const orderId = order.id || order.id || '';
      const orderStatus = order.status;

      order.items.forEach(item => {
        if (!grouped[userId]) {
          grouped[userId] = {
            userId,
            userName,
            items: [],
            total: 0,
          };
        }

        grouped[userId].items.push({
          ...item,
          orderId,
          orderStatus,
        });
        grouped[userId].total += item.price * item.quantity;
      });
    });

    return Object.values(grouped);
  };

  const totalAmount = orders.reduce((sum, order) => sum + order.totalAmount, 0);
  const activeOrders = orders.filter(o => 
    !['Completed', 'Cancelled'].includes(o.status)
  );
  const completedOrders = orders.filter(o => 
    ['Completed', 'Cancelled'].includes(o.status)
  );

  if (isLoading) {
    return (
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading table details...</p>
        </div>
    );
  }

  if (tableNumber === null) {
    return (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <p className="text-lg font-semibold mb-2">Invalid Table</p>
            <p className="text-muted-foreground mb-4">Could not determine table number</p>
            <Button onClick={() => navigate('/tables')}>Back to Tables</Button>
          </CardContent>
        </Card>
    );
  }

  const groupedOrders = groupOrdersByUser();

  return (
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Button variant="ghost" size="icon" onClick={() => navigate('/tables')}>
              <ArrowLeft className="h-5 w-5" />
            </Button>
            <div>
              <h2 className="text-3xl font-bold">Table {tableNumber}</h2>
              <p className="text-muted-foreground">Manage orders and status</p>
            </div>
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

        {/* View Toggle */}
        <Tabs value={viewMode} onValueChange={(v) => setViewMode(v as 'orders' | 'users')} className="space-y-6">
          <TabsList className="grid w-full max-w-md grid-cols-2">
            <TabsTrigger value="orders" className="gap-2">
              <List className="h-4 w-4" />
              By Order
            </TabsTrigger>
            <TabsTrigger value="users" className="gap-2">
              <User className="h-4 w-4" />
              By User
            </TabsTrigger>
          </TabsList>

          {/* Orders View */}
          <TabsContent value="orders" className="space-y-6">
            {/* Active Orders */}
            {activeOrders.length > 0 && (
              <div className="space-y-4">
                <h3 className="text-xl font-semibold">Active Orders</h3>
                {activeOrders.map(order => {
                  const orderId = order.id || order.id || '';
                  const config = orderStatusConfig[order.status] || orderStatusConfig['Pending'];
                  
                  return (
                    <Card key={orderId}>
                      <CardHeader>
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex-1">
                            <CardTitle className="flex items-center gap-2">
                              Order #{orderId}
                              {order.username && (
                                <Badge variant="outline" className="font-normal">
                                  <User className="h-3 w-3 mr-1" />
                                  {order.username}
                                </Badge>
                              )}
                            </CardTitle>
                            <CardDescription>
                              {new Date(order.createdAt).toLocaleString()}
                            </CardDescription>
                          </div>
                          <div className="flex items-center gap-2">
                            <Badge className={config.color} variant="secondary">
                              {config.label}
                            </Badge>
                            <Select
                              value={order.status}
                              onValueChange={(value) => handleOrderStatusChange(orderId, value)}
                              disabled={updatingStatus === `order-${orderId}`}
                            >
                              <SelectTrigger className="w-[140px]">
                                <SelectValue />
                              </SelectTrigger>
                              <SelectContent>
                                {orderStatuses.map(status => (
                                  <SelectItem key={status} value={status}>
                                    {status}
                                  </SelectItem>
                                ))}
                              </SelectContent>
                            </Select>
                          </div>
                        </div>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        <div className="space-y-3">
                          {order.items.map((item, idx) => {
                            const itemId = item.id || item.menuItemId || `${idx}`;
                            const itemConfig = item.status ? itemStatusConfig[item.status] : itemStatusConfig['Pending'];
                            
                            return (
                              <div key={itemId} className="flex items-start gap-4 p-3 rounded-lg bg-muted/50">
                                <ChefHat className="h-5 w-5 text-muted-foreground mt-0.5 flex-shrink-0" />
                                <div className="flex-1 min-w-0">
                                  <div className="flex items-start justify-between gap-2 mb-2">
                                    <p className="font-medium">{item.menuItemName}</p>
                                    <p className="font-semibold whitespace-nowrap">
                                      ${(item.price * item.quantity).toFixed(2)}
                                    </p>
                                  </div>
                                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                                    <span>Qty: {item.quantity}</span>
                                    <span>${item.price.toFixed(2)} each</span>
                                  </div>
                                  {item.specialInstructions && (
                                    <p className="text-xs text-muted-foreground mt-2 italic">
                                      Note: {item.specialInstructions}
                                    </p>
                                  )}
                                  <div className="flex items-center gap-2 mt-3">
                                    {item.status && (
                                      <Badge className={itemConfig.color} variant="secondary">
                                        {itemConfig.label}
                                      </Badge>
                                    )}
                                    <Select
                                      value={item.status || 'Pending'}
                                      onValueChange={(value) => handleItemStatusChange(orderId, itemId, value)}
                                      disabled={updatingStatus === `item-${orderId}-${itemId}`}
                                    >
                                      <SelectTrigger className="w-[130px] h-8">
                                        <SelectValue />
                                      </SelectTrigger>
                                      <SelectContent>
                                        {itemStatuses.map(status => (
                                          <SelectItem key={status} value={status}>
                                            {status}
                                          </SelectItem>
                                        ))}
                                      </SelectContent>
                                    </Select>
                                  </div>
                                </div>
                              </div>
                            );
                          })}
                        </div>
                        <Separator />
                        <div className="flex justify-between items-center">
                          <p className="font-semibold">Order Total</p>
                          <p className="text-xl font-bold text-primary">
                            ${order.totalAmount.toFixed(2)}
                          </p>
                        </div>
                      </CardContent>
                    </Card>
                  );
                })}
              </div>
            )}

            {/* Completed Orders */}
            {completedOrders.length > 0 && (
              <div className="space-y-4">
                <h3 className="text-xl font-semibold">Completed Orders</h3>
                {completedOrders.map(order => {
                  const orderId = order.id || order.id || '';
                  const config = orderStatusConfig[order.status] || orderStatusConfig['Completed'];
                  
                  return (
                    <Card key={orderId} className="opacity-75">
                      <CardHeader>
                        <div className="flex items-start justify-between">
                          <div>
                            <CardTitle>Order #{orderId}</CardTitle>
                            <CardDescription>
                              {new Date(order.createdAt).toLocaleString()}
                              {order.username && ` • ${order.username}`}
                            </CardDescription>
                          </div>
                          <Badge className={config.color} variant="secondary">
                            {config.label}
                          </Badge>
                        </div>
                      </CardHeader>
                      <CardContent className="space-y-4">
                        <div className="space-y-2">
                          {order.items.map((item, idx) => (
                            <div key={item.id || idx} className="flex justify-between items-center text-sm">
                              <div>
                                <p className="font-medium">{item.menuItemName}</p>
                                <p className="text-muted-foreground">Qty: {item.quantity}</p>
                              </div>
                              <p className="font-semibold">
                                ${(item.price * item.quantity).toFixed(2)}
                              </p>
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
                  );
                })}
              </div>
            )}
          </TabsContent>

          {/* Users View */}
          <TabsContent value="users" className="space-y-6">
            {groupedOrders.length > 0 ? (
              <div className="space-y-4">
                <h3 className="text-xl font-semibold">Orders by User</h3>
                {groupedOrders.map(group => (
                  <Card key={group.userId}>
                    <CardHeader>
                      <div className="flex items-start justify-between">
                        <div>
                          <CardTitle className="flex items-center gap-2">
                            <User className="h-5 w-5" />
                            {group.userName}
                          </CardTitle>
                          <CardDescription>
                            {group.items.length} items • ${group.total.toFixed(2)} total
                          </CardDescription>
                        </div>
                      </div>
                    </CardHeader>
                    <CardContent className="space-y-3">
                      {group.items.map((item, idx) => {
                        const itemId = item.id || item.menuItemId || `${idx}`;
                        const itemConfig = item.status ? itemStatusConfig[item.status] : itemStatusConfig['Pending'];
                        const orderConfig = orderStatusConfig[item.orderStatus] || orderStatusConfig['Pending'];
                        
                        return (
                          <div key={`${item.orderId}-${itemId}`} className="flex items-start gap-4 p-3 rounded-lg bg-muted/50">
                            <ChefHat className="h-5 w-5 text-muted-foreground mt-0.5 flex-shrink-0" />
                            <div className="flex-1 min-w-0">
                              <div className="flex items-start justify-between gap-2 mb-2">
                                <div className="flex-1">
                                  <p className="font-medium">{item.menuItemName}</p>
                                  <p className="text-xs text-muted-foreground">
                                    Order #{item.orderId}
                                  </p>
                                </div>
                                <p className="font-semibold whitespace-nowrap">
                                  ${(item.price * item.quantity).toFixed(2)}
                                </p>
                              </div>
                              <div className="flex items-center gap-4 text-sm text-muted-foreground mb-2">
                                <span>Qty: {item.quantity}</span>
                                <span>${item.price.toFixed(2)} each</span>
                              </div>
                              {item.specialInstructions && (
                                <p className="text-xs text-muted-foreground mb-2 italic">
                                  Note: {item.specialInstructions}
                                </p>
                              )}
                              <div className="flex items-center gap-2 flex-wrap">
                                <Badge className={`${orderConfig.color} text-xs`} variant="outline">
                                  Order: {orderConfig.label}
                                </Badge>
                                {item.status && (
                                  <Badge className={`${itemConfig.color} text-xs`} variant="secondary">
                                    {itemConfig.label}
                                  </Badge>
                                )}
                                <Select
                                  value={item.status || 'Pending'}
                                  onValueChange={(value) => handleItemStatusChange(item.orderId, itemId, value)}
                                  disabled={updatingStatus === `item-${item.orderId}-${itemId}`}
                                >
                                  <SelectTrigger className="w-[130px] h-7 text-xs">
                                    <SelectValue />
                                  </SelectTrigger>
                                  <SelectContent>
                                    {itemStatuses.map(status => (
                                      <SelectItem key={status} value={status}>
                                        {status}
                                      </SelectItem>
                                    ))}
                                  </SelectContent>
                                </Select>
                              </div>
                            </div>
                          </div>
                        );
                      })}
                      <Separator />
                      <div className="flex justify-between items-center pt-2">
                        <p className="font-semibold">User Total</p>
                        <p className="text-xl font-bold text-primary">
                          ${group.total.toFixed(2)}
                        </p>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            ) : (
              <Card>
                <CardContent className="flex flex-col items-center justify-center py-12">
                  <Users className="h-12 w-12 text-muted-foreground mb-4" />
                  <p className="text-lg font-semibold mb-2">No orders yet</p>
                  <p className="text-muted-foreground">Orders will appear here once customers place them</p>
                </CardContent>
              </Card>
            )}
          </TabsContent>
        </Tabs>

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
  );
};

export default TableDetails;