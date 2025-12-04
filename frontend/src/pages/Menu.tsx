import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Separator } from '@/components/ui/separator';
import { Plus, Minus, ShoppingCart, Loader2, X, Receipt, User } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import { MenuItem } from '@/types/menu';
import { Order } from '@/types/order';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/services/api/client';

const Menu = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [cart, setCart] = useState<Record<string, number>>({});
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [showOrderDialog, setShowOrderDialog] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [activeOrder, setActiveOrder] = useState<Order | null>(null);

  const tableNumber = user?.tableNumber || parseInt(localStorage.getItem('tableNumber') || '0');
  const tableId = user?.tableId || localStorage.getItem('tableId') || '';

  useEffect(() => {
    fetchMenuItems();
    //fetchActiveOrder();
  }, []);

  const fetchMenuItems = async () => {
    try {
      setIsLoading(true);
      const response = await api.get('/api/menu');
      setMenuItems(response.data);
    } catch (error) {
      console.error('Failed to fetch menu items:', error);
      toast.error('Failed to load menu items');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchActiveOrder = async () => {
    try {
      const response = await api.get('/api/orders/active');
      if (response.data) {
        setActiveOrder(response.data);
        console.log('Active order found:', response.data);
      }
    } catch (error: any) {
      if (error.response?.status !== 404) {
        console.error('Failed to fetch active order:', error);
      }
    }
  };

  const categories = ['All', ...Array.from(new Set(menuItems.map(item => item.category)))];

  const filteredItems = selectedCategory === 'All' 
    ? menuItems 
    : menuItems.filter(item => item.category === selectedCategory);

  const addToCart = (itemId: string) => {
    setCart(prev => ({ ...prev, [itemId]: (prev[itemId] || 0) + 1 }));
    toast.success('Added to cart');
  };

  const removeFromCart = (itemId: string) => {
    setCart(prev => {
      const newCart = { ...prev };
      if (newCart[itemId] > 1) {
        newCart[itemId]--;
      } else {
        delete newCart[itemId];
      }
      return newCart;
    });
  };

  const clearCart = () => {
    setCart({});
    toast.info('Cart cleared');
  };

  const getCartItems = () => {
    return Object.entries(cart).map(([itemId, quantity]) => {
      const item = menuItems.find(i => i.id === itemId);
      return {
        item,
        quantity,
        subtotal: (item?.price || 0) * quantity
      };
    }).filter(cartItem => cartItem.item);
  };

  const getCartTotal = () => {
    return Object.entries(cart).reduce((total, [itemId, quantity]) => {
      const item = menuItems.find(i => i.id === itemId);
      return total + (item?.price || 0) * quantity;
    }, 0);
  };

  const getActiveOrderTotal = () => {
    return activeOrder?.totalAmount || 0;
  };

  const getGrandTotal = () => {
    return getCartTotal() + getActiveOrderTotal();
  };

  const getOrderItemsByUser = () => {
    if (!activeOrder) return {};
    
    const grouped: Record<string, { username: string; items: any[]; total: number }> = {};
    
    activeOrder.items.forEach((item: any) => {
      const userId = item.userId || activeOrder.userId;
      const username = item.username || user?.username || 'Unknown';
      
      if (!grouped[userId]) {
        grouped[userId] = {
          username,
          items: [],
          total: 0
        };
      }
      
      grouped[userId].items.push(item);
      grouped[userId].total += item.price * item.quantity;
    });
    
    return grouped;
  };

  const cartItemsCount = Object.values(cart).reduce((sum, qty) => sum + qty, 0);


const handleAddToOrder = async () => {
  if (!user?.userId) {
    toast.error('Please log in to place an order');
    return;
  }

  if (cartItemsCount === 0) {
    toast.error('Your cart is empty');
    return;
  }

  if (!tableNumber) {
    toast.error('Table number not found. Please login again.');
    return;
  }

  setIsSubmitting(true);
  try {
    const orderItems = Object.entries(cart).map(([itemId, quantity]) => ({
      menuItemId: itemId,
      quantity,
    }));

    let response;
    
    if (activeOrder) {
      console.log('Adding items to existing order:', activeOrder.id);
      console.log('Request payload:', { items: orderItems });
      
      response = await api.post(`/api/orders/${activeOrder.id}/items`, {
        items: orderItems
      });
      
      console.log('Add items response:', response.data);
      toast.success('Items added to your order!');
    } else {
      console.log('Creating new order for table:', tableNumber);
      
      const orderData = {
        tableNumber: tableNumber,
        tableId: tableId,
        items: orderItems,
        customerId: user.userId, 
        customerName: user.username
      };
      
      console.log('Create order request payload:', orderData);
      
      response = await api.post('/api/orders', orderData);
      
      console.log('Create order response:', response.data);
      toast.success('Order created successfully!');
    }
    
    setCart({});
    setShowOrderDialog(false);
    
    //await fetchActiveOrder();
    
    setTimeout(() => {
      navigate('/orders');
    }, 1000);
  } catch (error: any) {
    console.error('Failed to update order:', error);
    console.error('Error response:', error.response?.data);
    toast.error(error.response?.data?.message || 'Failed to update order');
  } finally {
    setIsSubmitting(false);
  }
};


  if (isLoading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </Layout>
    );
  }

  const orderItemsByUser = getOrderItemsByUser();

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold">Our Menu</h2>
            <p className="text-muted-foreground">Browse and order your favorite dishes</p>
            {tableNumber > 0 && (
              <p className="text-sm text-muted-foreground mt-1">Table {tableNumber}</p>
            )}
          </div>
          <div className="flex gap-3">
            {activeOrder && (
              <Button size="lg" variant="outline" className="gap-2" onClick={() => navigate('/orders')}>
                <Receipt className="h-5 w-5" />
                Current Order - ${activeOrder.totalAmount.toFixed(2)}
              </Button>
            )}
            {cartItemsCount > 0 && (
              <Button size="lg" className="gap-2" onClick={() => setShowOrderDialog(true)}>
                <ShoppingCart className="h-5 w-5" />
                Order ({cartItemsCount}) - ${getCartTotal().toFixed(2)}
              </Button>
            )}
          </div>
        </div>

        {/* Category Filter */}
        <div className="flex gap-2 flex-wrap">
          {categories.map(category => (
            <Button
              key={category}
              variant={selectedCategory === category ? 'default' : 'outline'}
              onClick={() => setSelectedCategory(category)}
              className="transition-all"
            >
              {category}
            </Button>
          ))}
        </div>

        {/* Menu Items Grid */}
        {filteredItems.length === 0 ? (
          <Card>
            <CardContent className="flex items-center justify-center py-12">
              <p className="text-muted-foreground">No menu items available</p>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredItems.map(item => (
              <Card key={item.id} className="overflow-hidden transition-all hover:shadow-lg">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle className="text-xl">{item.name}</CardTitle>
                      <CardDescription className="mt-2">{item.description}</CardDescription>
                    </div>
                    <Badge variant="secondary">{item.category}</Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <div className="flex items-center gap-4 text-sm text-muted-foreground">
                    {item.calories && <span>{item.calories} kcal</span>}
                    {item.protein && <span>{item.protein}g protein</span>}
                  </div>
                  <p className="text-2xl font-bold text-primary mt-3">${item.price.toFixed(2)}</p>
                </CardContent>
                <CardFooter className="flex items-center gap-2">
                  {cart[item.id] ? (
                    <div className="flex items-center gap-2 w-full">
                      <Button
                        variant="outline"
                        size="icon"
                        onClick={() => removeFromCart(item.id)}
                      >
                        <Minus className="h-4 w-4" />
                      </Button>
                      <span className="flex-1 text-center font-semibold">
                        {cart[item.id]}
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        onClick={() => addToCart(item.id)}
                      >
                        <Plus className="h-4 w-4" />
                      </Button>
                    </div>
                  ) : (
                    <Button
                      className="w-full gap-2"
                      onClick={() => addToCart(item.id)}
                    >
                      <Plus className="h-4 w-4" />
                      Add to Cart
                    </Button>
                  )}
                </CardFooter>
              </Card>
            ))}
          </div>
        )}

        {/* Order Details Dialog */}
        <Dialog open={showOrderDialog} onOpenChange={setShowOrderDialog}>
          <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="text-2xl">
                {activeOrder ? 'Add to Existing Order' : 'Create New Order'}
              </DialogTitle>
              <DialogDescription>
                {activeOrder 
                  ? `Adding items to Table ${tableNumber}'s order`
                  : `Creating order for Table ${tableNumber}`
                }
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              {/* Table Info Display */}
              <div className="p-4 rounded-lg bg-primary/10 border border-primary/30">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <Receipt className="h-5 w-5 text-primary" />
                    <div>
                      <p className="font-semibold">Table {tableNumber}</p>
                      <p className="text-sm text-muted-foreground">{user?.username}</p>
                    </div>
                  </div>
                </div>
              </div>

              {/* Active Order Summary (if exists) - Grouped by User */}
              {activeOrder && Object.keys(orderItemsByUser).length > 0 && (
                <>
                  <div className="space-y-3">
                    <h3 className="font-semibold flex items-center gap-2">
                      <Receipt className="h-4 w-4" />
                      Current Order Items
                    </h3>
                    
                    {Object.entries(orderItemsByUser).map(([userId, data]) => (
                      <div key={userId} className="p-4 rounded-lg bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800">
                        <div className="flex items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <User className="h-4 w-4 text-blue-700 dark:text-blue-300" />
                            <h4 className="font-semibold text-blue-900 dark:text-blue-100">
                              {data.username}
                            </h4>
                          </div>
                          <Badge variant="outline" className="bg-blue-100 dark:bg-blue-900">
                            {data.items.length} items
                          </Badge>
                        </div>
                        <div className="space-y-1 text-sm text-blue-700 dark:text-blue-300 mb-2">
                          {data.items.map((item: any, idx: number) => (
                            <div key={idx} className="flex justify-between">
                              <span>{item.quantity}x {item.menuItemName}</span>
                              <span>${(item.price * item.quantity).toFixed(2)}</span>
                            </div>
                          ))}
                        </div>
                        <div className="flex justify-between items-center pt-2 border-t border-blue-200 dark:border-blue-800">
                          <span className="text-sm font-medium text-blue-900 dark:text-blue-100">
                            Subtotal
                          </span>
                          <span className="text-lg font-bold text-blue-900 dark:text-blue-100">
                            ${data.total.toFixed(2)}
                          </span>
                        </div>
                      </div>
                    ))}
                    
                    <div className="flex justify-between items-center p-3 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
                      <span className="font-semibold">Current Order Total</span>
                      <span className="text-xl font-bold text-blue-900 dark:text-blue-100">
                        ${activeOrder.totalAmount.toFixed(2)}
                      </span>
                    </div>
                  </div>
                  <Separator />
                </>
              )}

              {/* New Items - Current User */}
              <div>
                <h3 className="font-semibold mb-3 flex items-center gap-2">
                  <User className="h-4 w-4" />
                  Your New Items ({user?.username})
                </h3>
                <div className="space-y-3">
                  {getCartItems().map(({ item, quantity, subtotal }) => (
                    <div key={item!.id} className="flex items-start justify-between p-3 rounded-lg bg-muted/50">
                      <div className="flex-1">
                        <p className="font-semibold">{item!.name}</p>
                        <p className="text-sm text-muted-foreground">{item!.category}</p>
                        <div className="flex items-center gap-2 mt-2">
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-7 w-7"
                            onClick={() => removeFromCart(item!.id)}
                          >
                            <Minus className="h-3 w-3" />
                          </Button>
                          <span className="text-sm font-medium w-8 text-center">{quantity}</span>
                          <Button
                            variant="outline"
                            size="icon"
                            className="h-7 w-7"
                            onClick={() => addToCart(item!.id)}
                          >
                            <Plus className="h-3 w-3" />
                          </Button>
                          <span className="text-sm text-muted-foreground ml-2">
                            ${item!.price.toFixed(2)} each
                          </span>
                        </div>
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <p className="font-bold text-lg">${subtotal.toFixed(2)}</p>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          onClick={() => {
                            const newCart = { ...cart };
                            delete newCart[item!.id];
                            setCart(newCart);
                          }}
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              {/* Totals Breakdown */}
              <div className="space-y-2">
                {activeOrder && (
                  <div className="flex justify-between items-center">
                    <span className="text-muted-foreground">Current Order</span>
                    <span className="font-medium">${getActiveOrderTotal().toFixed(2)}</span>
                  </div>
                )}
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Your New Items</span>
                  <span className="font-medium">${getCartTotal().toFixed(2)}</span>
                </div>
                <Separator />
                <div className="flex justify-between items-center text-xl font-bold">
                  <span>New Total</span>
                  <span className="text-primary">${getGrandTotal().toFixed(2)}</span>
                </div>
              </div>
            </div>

            <DialogFooter className="gap-2">
              <Button
                variant="outline"
                onClick={() => {
                  clearCart();
                  setShowOrderDialog(false);
                }}
                disabled={isSubmitting}
              >
                Clear Cart
              </Button>
              <Button
                onClick={handleAddToOrder}
                disabled={isSubmitting || cartItemsCount === 0}
                className="gap-2"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    {activeOrder ? 'Adding Items...' : 'Creating Order...'}
                  </>
                ) : (
                  <>
                    <ShoppingCart className="h-4 w-4" />
                    {activeOrder ? 'Add Items' : 'Create Order'}
                  </>
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </Layout>
  );
};

export default Menu;