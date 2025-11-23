import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Plus, Minus, ShoppingCart, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import Layout from '@/components/Layout';
import { MenuItem } from '@/types/menu';
import api from '@/services/api/client';

const Menu = () => {
  const [menuItems, setMenuItems] = useState<MenuItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [cart, setCart] = useState<Record<string, number>>({});
  const [selectedCategory, setSelectedCategory] = useState<string>('All');

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

  const getCartTotal = () => {
    return Object.entries(cart).reduce((total, [itemId, quantity]) => {
      const item = menuItems.find(i => i.id === itemId);
      return total + (item?.price || 0) * quantity;
    }, 0);
  };

  const cartItemsCount = Object.values(cart).reduce((sum, qty) => sum + qty, 0);

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
            <Button size="lg" className="gap-2">
              <ShoppingCart className="h-5 w-5" />
              Cart ({cartItemsCount}) - ${getCartTotal().toFixed(2)}
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
                  <p className="text-2xl font-bold text-primary mt-3">${item.price}</p>
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
                      disabled={!item.available}
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
      </div>
    </Layout>
  );
};

export default Menu;