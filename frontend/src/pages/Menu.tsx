import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Separator } from '@/components/ui/separator';
import { Plus, Minus, ShoppingCart, Loader2, X } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import { MenuItem } from '@/types/menu';
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
  const [tableNumber, setTableNumber] = useState<string>('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchMenuItems();
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

  const cartItemsCount = Object.values(cart).reduce((sum, qty) => sum + qty, 0);

  const handlePlaceOrder = async () => {
    if (!user?.id) {
      toast.error('Please log in to place an order');
      return;
    }

    if (cartItemsCount === 0) {
      toast.error('Your cart is empty');
      return;
    }

    if (!tableNumber.trim()) {
      toast.error('Please enter a table number');
      return;
    }

    setIsSubmitting(true);
    try {
      const orderItems = Object.entries(cart).map(([itemId, quantity]) => ({
        menuItemId: itemId,
        quantity
      }));

      const orderData = {
        userId: user.id,
        tableNumber: parseInt(tableNumber),
        items: orderItems
      };

      console.log('Placing order:', orderData);

      const response = await api.post('/api/orders', orderData);
      
      console.log('Order placed successfully:', response.data);
      
      toast.success('Order placed successfully!');
      setCart({});
      setTableNumber('');
      setShowOrderDialog(false);
      
      // Navigate to orders page after a brief delay
      setTimeout(() => {
        navigate('/orders');
      }, 1000);
    } catch (error: any) {
      console.error('Failed to place order:', error);
      toast.error(error.response?.data?.message || 'Failed to place order');
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

  return (
    <Layout>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold">Our Menu</h2>
            <p className="text-muted-foreground">Browse and order your favorite dishes</p>
          </div>
          {cartItemsCount > 0 && (
            <Button size="lg" className="gap-2" onClick={() => setShowOrderDialog(true)}>
              <ShoppingCart className="h-5 w-5" />
              Order ({cartItemsCount}) - ${getCartTotal().toFixed(2)}
            </Button>
          )}
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
                      Add to Order
                    </Button>
                  )}
                </CardFooter>
              </Card>
            ))}
          </div>
        )}

        {/* Order Details Dialog */}
        <Dialog open={showOrderDialog} onOpenChange={setShowOrderDialog}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle className="text-2xl">Order Details</DialogTitle>
              <DialogDescription>
                Review your order and confirm details
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              {/* Order Items */}
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

              <Separator />

              {/* Total */}
              <div className="flex justify-between items-center text-xl font-bold">
                <span>Total</span>
                <span className="text-primary">${getCartTotal().toFixed(2)}</span>
              </div>

              <Separator />

              {/* Table Number Input */}
              <div className="space-y-2">
                <Label htmlFor="tableNumber">Table Number</Label>
                <Input
                  id="tableNumber"
                  type="number"
                  placeholder="Enter your table number"
                  value={tableNumber}
                  onChange={(e) => setTableNumber(e.target.value)}
                  min="1"
                />
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
                onClick={handlePlaceOrder}
                disabled={isSubmitting || cartItemsCount === 0 || !tableNumber.trim()}
                className="gap-2"
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="h-4 w-4 animate-spin" />
                    Placing Order...
                  </>
                ) : (
                  <>
                    <ShoppingCart className="h-4 w-4" />
                    Place Order
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