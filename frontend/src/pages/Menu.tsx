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
import { MenuItem, Order } from '@/types';
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
  const [isFetchingOrder, setIsFetchingOrder] = useState(false);

  const tableNumber = user?.tableNumber || parseInt(localStorage.getItem('tableNumber') || '0');
  const tableId = user?.tableId || localStorage.getItem('tableId');

  useEffect(() => {
    fetchMenuItems();
    if (user?.userId) {
      fetchActiveOrder();
    }
  }, [user?.userId]);

  const fetchMenuItems = async () => {
    try {
      setIsLoading(true);
      console.log('Fetching menu items...');
      const response = await api.get('menu');
      console.log('Menu items loaded:', response.data.length);
      setMenuItems(response.data);
    } catch (error) {
      console.error('Failed to fetch menu items:', error);
      toast.error('Failed to load menu items');
    } finally {
      setIsLoading(false);
    }
  };

  const fetchActiveOrder = async () => {
    if (!user?.userId) {
      console.warn('⚠️ No user ID - skipping active order fetch');
      return;
    }

    try {
      setIsFetchingOrder(true);
      console.log('🔍 Fetching active order for user:', user.userId);
      
      const response = await api.get(`orders/active?userId=${user.userId}`);
      
      if (response.data) {
        setActiveOrder(response.data);
        console.log('Active order found:', response.data.id);
        toast.info(`You have an active order`);
      }
    } catch (error: any) {
      if (error.response?.status === 404) {
        console.log('No active order (404)');
        setActiveOrder(null);
      } else {
        console.error('Failed to fetch active order:', error);
      }
    } finally {
      setIsFetchingOrder(false);
    }
  };

  const categories = ['All', ...Array.from(new Set(menuItems.map(item => item.category)))];

  const filteredItems = selectedCategory === 'All' 
    ? menuItems 
    : menuItems.filter(item => item.category === selectedCategory);

  const addToCart = (itemId: string) => {
    setCart(prev => ({ ...prev, [itemId]: (prev[itemId] || 0) + 1 }));
    toast.success('Added order');
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
        console.log('Adding items to order:', activeOrder.orderId);
        response = await api.post(`orders/${activeOrder.orderId}/items`, {
          items: orderItems
        });
        toast.success('Items added to order!');
      } else {
        console.log('📝 Creating new order');
        response = await api.post('orders', {
          tableNumber: tableNumber,
          tableId: tableId,
          items: orderItems,
          customerId: user.userId,
          customerName: user.username || user.name || user.email
        });
        toast.success('Order created!');
      }
      
      setCart({});
      setShowOrderDialog(false);
      await fetchActiveOrder();
      
      setTimeout(() => {
        navigate('/orders');
      }, 1000);
    } catch (error: any) {
      console.error(' Order failed:', error);
      toast.error(error.response?.data?.message || 'Failed to create order');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
    );
  }

  return (
      <div className="space-y-6">
        {/* Header */}
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
              <Button size="lg" variant="outline" onClick={() => navigate('/orders')}>
                <Receipt className="h-5 w-5 mr-2" />
                Order #{activeOrder.orderId} - ${activeOrder.totalAmount.toFixed(2)}
              </Button>
            )}
            {cartItemsCount > 0 && (
              <Button size="lg" onClick={() => setShowOrderDialog(true)}>
                <ShoppingCart className="h-5 w-5 mr-2" />
                Add ({cartItemsCount}) - ${getCartTotal().toFixed(2)}
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
              <Card key={item.id} className="overflow-hidden hover:shadow-lg transition-shadow">
                <CardHeader>
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>{item.name}</CardTitle>
                      <CardDescription className="mt-2">{item.description}</CardDescription>
                    </div>
                    <Badge variant="secondary">{item.category}</Badge>
                  </div>
                </CardHeader>
                <CardContent>
                  <p className="text-2xl font-bold text-primary">${item.price.toFixed(2)}</p>
                </CardContent>
                <CardFooter>
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
                    <Button className="w-full" onClick={() => addToCart(item.id)}>
                      <Plus className="h-4 w-4 mr-2" />
                      Add to Order
                    </Button>
                  )}
                </CardFooter>
              </Card>
            ))}
          </div>
        )}

        {/* Order Dialog */}
        <Dialog open={showOrderDialog} onOpenChange={setShowOrderDialog}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>
                {activeOrder ? `Add to Order #${activeOrder.orderId}` : 'Create Order'}
              </DialogTitle>
              <DialogDescription>
                Table {tableNumber} • {user?.username}
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4 max-h-[60vh] overflow-y-auto">
              {getCartItems().map(({ item, quantity, subtotal }) => (
                <div key={item!.id} className="flex items-center justify-between p-3 bg-muted rounded-lg">
                  <div className="flex-1">
                    <p className="font-semibold">{item!.name}</p>
                    <div className="flex items-center gap-2 mt-2">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => removeFromCart(item!.id)}
                      >
                        <Minus className="h-3 w-3" />
                      </Button>
                      <span className="text-sm w-8 text-center">{quantity}</span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-7 w-7"
                        onClick={() => addToCart(item!.id)}
                      >
                        <Plus className="h-3 w-3" />
                      </Button>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="font-bold">${subtotal.toFixed(2)}</span>
                    <Button
                      variant="ghost"
                      size="icon"
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

              <Separator />

              <div className="flex justify-between text-xl font-bold">
                <span>Total</span>
                <span className="text-primary">${getCartTotal().toFixed(2)}</span>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={clearCart} disabled={isSubmitting}>
                Clear Cart
              </Button>
              <Button onClick={handleAddToOrder} disabled={isSubmitting}>
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    Processing...
                  </>
                ) : (
                  <>
                    <ShoppingCart className="h-4 w-4 mr-2" />
                    {activeOrder ? 'Add to Order' : 'Create Order'}
                  </>
                )}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
  );
};

export default Menu;