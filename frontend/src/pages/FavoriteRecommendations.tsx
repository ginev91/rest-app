import React, { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { toast } from 'sonner';
import { listFavorites, FavoriteDto, removeFavorite } from '@/services/api/favorites';
import { Loader2 } from 'lucide-react';

const FavoriteRecommendations: React.FC = () => {
  const [favorites, setFavorites] = useState<FavoriteDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);

  useEffect(() => {
    loadFavorites();
  }, []);

  const loadFavorites = async () => {
    setLoading(true);
    try {
      const favs = await listFavorites();
      setFavorites(favs);
    } catch (err: any) {
      console.error('Failed to load favorites', err);
      toast.error('Failed to load favourites');
    } finally {
      setLoading(false);
    }
  };

  const handleRemove = async (id?: string) => {
    if (!id) return;
    setRemoving(id);
    try {
      await removeFavorite(id);
      setFavorites(prev => prev.filter(f => f.id !== id));
      toast.success('Removed from favourites');
    } catch (err: any) {
      console.error('Failed to remove favourite', err);
      toast.error('Failed to remove favourite');
    } finally {
      setRemoving(null);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[200px]">
        <Loader2 className="h-6 w-6 animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-4">
      <h2 className="text-2xl font-semibold">Favourite Recommendations</h2>

      {favorites.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center text-muted-foreground">No favourites yet</CardContent>
        </Card>
      ) : (
        <div className="space-y-3">
          {favorites.map(f => (
            <Card key={f.id}>
              <CardHeader className="flex items-center justify-between">
                <div>
                  <CardTitle>{f.menuItemName ?? f.menuItemId}</CardTitle>
                  <div className="text-sm text-muted-foreground">{f.description}</div>
                </div>
                <div className="flex items-center gap-2">
                  <Button size="sm" variant="ghost" onClick={() => handleRemove(f.id)} disabled={removing === f.id}>
                    Remove
                  </Button>
                </div>
              </CardHeader>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};

export default FavoriteRecommendations;