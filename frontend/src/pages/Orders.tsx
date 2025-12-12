import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import Layout from '@/components/Layout';
import { Order } from '@/types';
import { Clock, CheckCircle2, ChefHat, Utensils, Loader2, User, Table, XCircle } from 'lucide-react';
import {
  getOrders,
  claimOrder as apiClaimOrder,
  assignOrder as apiAssignOrder,
  updateOrderItemStatus as apiUpdateOrderItemStatus,
  updateOrderStatus as apiUpdateOrderStatus,
} from '@/services/api/order';
import { toast } from 'sonner';
import { useAuth } from '@/contexts/AuthContext';

const statusConfig: Record<string, { label: string; icon: any; color: string }> = {
  // legacy / item keys
  pending: { label: 'Pending', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  confirmed: { label: 'Confirmed', icon: CheckCircle2, color: 'bg-blue-500/10 text-blue-700 dark:text-blue-500' },
  preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  ready: { label: 'Ready', icon: Utensils, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  served: { label: 'Served', icon: CheckCircle2, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  completed: { label: 'Completed', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },
  Cancelled: { label: 'Cancelled', icon: XCircle, color: 'bg-red-500/10 text-red-700 dark:text-red-500' },

  // Title-case duplicates (some APIs return "New", "Processing", etc.)
  Pending: { label: 'Pending', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  Preparing: { label: 'Preparing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  Ready: { label: 'Ready', icon: Utensils, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },
  Served: { label: 'Served', icon: CheckCircle2, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },

  // Backend enum names and labels
  NEW: { label: 'New', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },
  New: { label: 'New', icon: Clock, color: 'bg-yellow-500/10 text-yellow-700 dark:text-yellow-500' },

  PROCESSING: { label: 'Processing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },
  Processing: { label: 'Processing', icon: ChefHat, color: 'bg-orange-500/10 text-orange-700 dark:text-orange-500' },

  READY: { label: 'Ready', icon: Utensils, color: 'bg-green-500/10 text-green-700 dark:text-green-500' },

  COMPLETED: { label: 'Completed', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },
  Completed: { label: 'Completed', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },

  PAID: { label: 'Paid', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },
  Paid: { label: 'Paid', icon: CheckCircle2, color: 'bg-gray-500/10 text-gray-700 dark:text-gray-500' },

  CANCELLED: { label: 'Cancelled', icon: XCircle, color: 'bg-red-500/10 text-red-700 dark:text-red-500' },
};

const itemStatusOptions = ['Pending', 'Preparing', 'In Progress', 'Ready', 'Served', 'Cancelled'];
// Use Title-case labels that match backend enum labels (frontend-friendly)
const orderStatusOptions = ['New', 'Processing', 'Ready', 'Completed', 'Paid', 'Cancelled'];

const normalizeOrderStatus = (s: any) => {
  if (!s) return 'New';
  const v = String(s).trim();
  const map: Record<string, string> = {
    NEW: 'New',
    New: 'New',
    new: 'New',
    PROCESSING: 'Processing',
    Processing: 'Processing',
    processing: 'Processing',
    READY: 'Ready',
    Ready: 'Ready',
    ready: 'Ready',
    COMPLETED: 'Completed',
    Completed: 'Completed',
    completed: 'Completed',
    PAID: 'Paid',
    Paid: 'Paid',
    paid: 'Paid',
    CANCELLED: 'Cancelled',
    Cancelled: 'Cancelled',
    cancelled: 'Cancelled',
    PENDING: 'Pending',
    Pending: 'Pending',
    pending: 'Pending',
  };
  return map[v] ?? (v.charAt(0).toUpperCase() + v.slice(1).toLowerCase());
};

const Orders = () => {
  const { user, isLoading: authLoading } = useAuth();
  const [myOrders, setMyOrders] = useState<Order[]>([]);
  const [tableOrders, setTableOrders] = useState<Order[]>([]);
  const [isLoadingMy, setIsLoadingMy] = useState(true);
  const [isLoadingTable, setIsLoadingTable] = useState(false);
  const [activeTab, setActiveTab] = useState<'my' | 'table'>('my');

  const currentUserId = user?.userId ?? user?.id ?? null;

  const roles: string[] = (user?.roles ?? user?.authorities ?? []) as string[];
  const isEmployee = roles.some(r => r === 'ROLE_EMPLOYEE' || r === 'ROLE_WAITER' || r === 'EMPLOYEE');
  const isAdmin = roles.some(r => r === 'ROLE_ADMIN' || r === 'ADMIN');

  const getWaiterId = (o: any): string | null => {
    if (!o) return null;
    const cand =
      o.waiterId ??
      (o.waiter && (o.waiter.id ?? o.waiter.userId ?? (o.waiter.user && (o.waiter.user.id ?? o.waiter.user.userId)))) ??
      null;
    return cand != null ? String(cand) : null;
  };

  const fetchAllAndPartition = async () => {
    setIsLoadingMy(true);
    setIsLoadingTable(true);
    try {
      const data = await getOrders();
      console.log('fetchAllAndPartition got orders:', data);
      const arr = Array.isArray(data) ? data : [];
      const my = arr.filter(o => {
        const waiter = getWaiterId(o);
        return waiter && String(waiter).toLowerCase() === String(currentUserId).toLowerCase();
      });
      const available = arr.filter(o => !getWaiterId(o));
      setMyOrders(my);
      setTableOrders(available);
    } catch (err: any) {
      console.error('fetchAllAndPartition failed', err);
      toast.error('Failed to load orders');
      setMyOrders([]);
      setTableOrders([]);
    } finally {
      setIsLoadingMy(false);
      setIsLoadingTable(false);
    }
  };

  const fetchMyOrders = async () => {
    if (!currentUserId) {
      setIsLoadingMy(false);
      return;
    }
    setIsLoadingMy(true);
    try {
      const data = await getOrders({ userId: currentUserId });
      setMyOrders(Array.isArray(data) ? data : []);
    } catch (err: any) {
      console.error('fetchMyOrders failed', err);
      toast.error('Failed to load your orders');
      setMyOrders([]);
    } finally {
      setIsLoadingMy(false);
    }
  };

  const fetchTableOrders = async () => {
    setIsLoadingTable(true);
    try {
      const tableId = user?.tableId ?? localStorage.getItem('tableId');
      const tableNumber = user?.tableNumber ?? localStorage.getItem('tableNumber');

      if (tableId || tableNumber) {
        const params = tableId ? { tableId } : { tableNumber };
        const data = await getOrders(params);
        const arr = Array.isArray(data) ? data : [];
        setTableOrders(arr.filter(o => !getWaiterId(o)));
      } else {
        const data = await getOrders();
        const arr = Array.isArray(data) ? data : [];
        setTableOrders(arr.filter(o => !getWaiterId(o)));
      }
    } catch (err: any) {
      console.error('fetchTableOrders failed', err);
      toast.error('Failed to load table orders');
      setTableOrders([]);
    } finally {
      setIsLoadingTable(false);
    }
  };

  useEffect(() => {
    if (authLoading) return;

    if (isAdmin || isEmployee) {
      fetchAllAndPartition();
    } else {
      if (activeTab === 'my') {
        fetchMyOrders();
      } else {
        fetchTableOrders();
      }
    }
  }, [authLoading, activeTab, currentUserId, isAdmin, isEmployee]);

  const handleTabChange = (value: string) => {
    setActiveTab(value as 'my' | 'table');
  };

  const handleClaim = async (orderId: string) => {
    try {
      const res = await apiClaimOrder(orderId, currentUserId ?? undefined);
      if (res && (res as any).claimed) {
        toast.success('Order claimed');
      } else {
        toast('Order already claimed');
      }

      if (isAdmin || isEmployee) {
        await fetchAllAndPartition();
      } else {
        await fetchMyOrders();
        await fetchTableOrders();
      }
    } catch (err: any) {
      console.error('Claim failed', err);
      toast.error('Failed to claim order');
    }
  };

  const handleAssign = async (orderId: string, waiterId: string) => {
    try {
      await apiAssignOrder(orderId, waiterId);
      toast.success('Order assigned');
      if (isAdmin || isEmployee) {
        await fetchAllAndPartition();
      } else {
        await fetchMyOrders();
        await fetchTableOrders();
      }
    } catch (err: any) {
      console.error('Assign failed', err);
      toast.error('Failed to assign order');
    }
  };

  const handleUpdateItemStatus = async (orderId: string, itemId: string, statusLabel: string) => {
    try {
      await apiUpdateOrderItemStatus(orderId, itemId, statusLabel);
      toast.success('Item status updated');
      if (isAdmin || isEmployee) {
        await fetchAllAndPartition();
      } else {
        await fetchMyOrders();
        if (activeTab === 'table') await fetchTableOrders();
      }
    } catch (err: any) {
      console.error('Update item status failed', err);
      toast.error('Failed to update item status');
    }
  };

  const handleUpdateOrderStatus = async (orderId: string, statusLabel: string) => {
    try {
      await apiUpdateOrderStatus(orderId, statusLabel);
      toast.success('Order status updated');
      if (isAdmin || isEmployee) {
        await fetchAllAndPartition();
      } else {
        await fetchMyOrders();
        if (activeTab === 'table') await fetchTableOrders();
      }
    } catch (err: any) {
      console.error('Update order status failed', err);
      toast.error('Failed to update order status');
    }
  };

  const OrderCard = ({ order }: { order: Order }) => {
    const config = statusConfig[(order as any).status] || statusConfig['New'];
    const Icon = config?.icon || Clock;

    const userName = (order as any).username ?? (order as any).userName ?? null;
    const waiterAssigned: string | null = getWaiterId(order);

    const canEdit = isAdmin || (isEmployee && waiterAssigned && String(waiterAssigned).toLowerCase() === String(currentUserId).toLowerCase());

    const [selectedOrderStatus, setSelectedOrderStatus] = useState<string>(normalizeOrderStatus((order as any).status));

    const ItemRow = ({ item }: { item: any }) => {
      const itemConfig = item.status ? statusConfig[item.status] : null;
      const ItemIcon = itemConfig?.icon;
      const [localStatus, setLocalStatus] = useState<string>(item.status ?? 'Pending');

      return (
        <div key={item.id ?? item.menuItemId} className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            <div className="flex items-start justify-between gap-2">
              <p className="font-medium">{item.menuItemName}</p>
              {itemConfig && (
                <Badge variant="outline" className={`${itemConfig.color} text-xs flex-shrink-0`}>
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
              <p className="text-xs text-muted-foreground mt-1 italic">Note: {item.specialInstructions}</p>
            )}

            {/* editable controls visible only to admin/employee */}
            {(isAdmin || isEmployee) ? (
              <div className="flex items-center gap-2 mt-3">
                <select className="border px-2 py-1 rounded text-sm" value={localStatus} onChange={(e) => setLocalStatus(e.target.value)} disabled={!canEdit}>
                  {itemStatusOptions.map(opt => (
                    <option key={opt} value={opt}>{opt}</option>
                  ))}
                </select>
                <button className="px-2 py-1 text-sm rounded bg-primary text-white disabled:opacity-50" onClick={() => handleUpdateItemStatus((order.orderId ?? (order as any).id) as string, String(item.id ?? item.menuItemId), localStatus)} disabled={!canEdit}>
                  Save
                </button>
                {!waiterAssigned && <span className="text-xs text-muted-foreground ml-2">Unassigned — editing disabled</span>}
                {waiterAssigned && !canEdit && <span className="text-xs text-muted-foreground ml-2">Assigned to {String(waiterAssigned).slice(0, 8)} — editing disabled</span>}
              </div>
            ) : null}
          </div>
        </div>
      );
    };

    return (
      <Card className="overflow-hidden">
        <CardHeader>
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <CardTitle className="flex items-center gap-2 flex-wrap">
                Order #{(order.orderId ?? (order as any).id) as any}
                {userName && (
                  <Badge variant="outline" className="font-normal">
                    <User className="h-3 w-3 mr-1" />
                    {userName}
                  </Badge>
                )}
                {waiterAssigned && (
                  <Badge variant="outline" className="ml-2 text-xs">
                    Waiter: {String(waiterAssigned).slice(0, 8)}
                  </Badge>
                )}
              </CardTitle>
              <CardDescription>
                {order.createdAt ? new Date(order.createdAt).toLocaleString() : '—'}
                {order.tableNumber && ` • Table ${order.tableNumber}`}
              </CardDescription>
            </div>
            <div className="flex flex-col items-end gap-2">
              <Badge className={config.color} variant="secondary">
                <Icon className="h-3 w-3 mr-1" />
                {config.label}
              </Badge>

              {isEmployee && !waiterAssigned && (
                <button className="px-2 py-1 text-sm rounded bg-primary text-white mt-2" onClick={() => handleClaim((order.orderId ?? (order as any).id) as string)}>
                  Claim (Assign to me)
                </button>
              )}

              {isAdmin && (
                <button className="px-2 py-1 text-sm rounded border mt-2" onClick={() => {
                  if (!currentUserId) {
                    toast.error('No current user id available to assign');
                    return;
                  }
                  handleAssign((order.orderId ?? (order as any).id) as string, currentUserId as string);
                }}>
                  Assign to me
                </button>
              )}
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-3">{(order.items ?? []).map((item: any, idx: number) => <ItemRow key={item.id ?? `${item.menuItemId}-${idx}`} item={item} />)}</div>
          <Separator />
          <div className="flex justify-between items-center">
            <p className="text-lg font-bold">Total</p>
            <p className="text-2xl font-bold text-primary">${(order.totalAmount ?? 0).toFixed(2)}</p>
          </div>

          {/* show order status controls only for admin/employees; hide for customers */}
          {(isAdmin || isEmployee) ? (
            <div className="flex items-center gap-2">
              <select className="border px-2 py-1 rounded text-sm" value={selectedOrderStatus} onChange={(e) => setSelectedOrderStatus(e.target.value)} disabled={!canEdit}>
                {orderStatusOptions.map(opt => <option key={opt} value={opt}>{opt}</option>)}
              </select>
              <button className="px-2 py-1 text-sm rounded bg-primary text-white disabled:opacity-50" onClick={() => handleUpdateOrderStatus((order.orderId ?? (order as any).id) as string, selectedOrderStatus)} disabled={!canEdit}>
                Update Order Status
              </button>
              {!waiterAssigned && <span className="text-xs text-muted-foreground ml-2">Unassigned — editing disabled</span>}
              {waiterAssigned && !canEdit && <span className="text-xs text-muted-foreground ml-2">Assigned to {String(waiterAssigned).slice(0, 8)} — editing disabled</span>}
            </div>
          ) : (
            <div className="text-sm text-muted-foreground">Status: {config.label}</div>
          )}
        </CardContent>
      </Card>
    );
  };

  const OrdersList = ({ orders, isLoading }: { orders: Order[]; isLoading: boolean }) => {
    if (isLoading) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[400px] gap-4">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
          <p className="text-muted-foreground">Loading orders...</p>
        </div>
      );
    }

    if (!orders || orders.length === 0) {
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

    const activeOrders = orders.filter(o => (o.status ?? '').toString().toLowerCase() !== 'completed' && (o.status ?? '').toString().toLowerCase() !== 'cancelled');
    const completedOrders = orders.filter(o => (o.status ?? '').toString().toLowerCase() === 'completed' || (o.status ?? '').toString().toLowerCase() === 'cancelled');

    return (
      <div className="space-y-8">
        {activeOrders.length > 0 && (
          <div className="space-y-4">
            <div>
              <h2 className="text-2xl font-bold">Active Orders</h2>
              <p className="text-muted-foreground">Track your current orders</p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {activeOrders.map(order => <OrderCard key={(order.orderId ?? (order as any).id) as any} order={order} />)}
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
              {completedOrders.map(order => <OrderCard key={(order.orderId ?? (order as any).id) as any} order={order} />)}
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
          {isAdmin || isEmployee ? 'Available Orders' : 'Table Orders'}
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