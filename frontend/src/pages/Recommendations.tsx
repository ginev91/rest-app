import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import Layout from '@/components/Layout';
import { toast } from 'sonner';
import { Sparkles, Loader2 } from 'lucide-react';
import { recommend } from '@/services/api/recommendations';
import { fetchMenuItem } from '@/services/api/menu';

interface Recommendation {
  mealName: string;
  description: string;
  menuItem: string;
  calories: number;
  protein: number;
  matchPercentage: number;
}

const Recommendations = () => {
  const [prompt, setPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<Recommendation | null>(null);

  const getRecommendation = async () => {
    if (!prompt.trim()) {
      toast.error("Please describe what you're looking for");
      return;
    }

    setIsLoading(true);
    setRecommendation(null);

    try {
      // Call backend recommendation API with the free-text prompt.
      // The backend is expected to accept a body like { preferences: string, likes?: string[], dislikes?: string[], maxKcal?: number }
      const resp = await recommend({ preferences: prompt });

      // resp may contain recipe text, matchedMenuItemId and score
      // Map response to the local Recommendation shape.
      let mealName = 'Recommended meal';
      let description = (resp as any)?.recipe ?? '';
      let calories = (resp as any)?.calories ?? 0;
      let protein = (resp as any)?.protein ?? 0;
      let matchPercentage = Math.round(((resp as any)?.score ?? (resp as any)?.matchPercentage ?? 0) * 100);

      // If the recommendation includes a matchedMenuItemId, try to fetch its name
      let menuItem = '';
      const matchedMenuItemId = (resp as any)?.matchedMenuItemId ?? (resp as any)?.menuItemId ?? null;
      if (matchedMenuItemId) {
        try {
          const mi = await fetchMenuItem(matchedMenuItemId);
          menuItem = mi?.name ?? '';
          mealName = mi?.name ?? mealName;
        } catch (e) {
          // ignore, use recipe-based name
        }
      } else if (description) {
        // Derive a short meal name from the recipe/description
        mealName = description.split('\n')[0].slice(0, 60);
      }

      const mapped: Recommendation = {
        mealName,
        description,
        menuItem: menuItem || 'Custom suggestion',
        calories,
        protein,
        matchPercentage,
      };

      setRecommendation(mapped);
      toast.success('Recommendation generated!');
    } catch (err: any) {
      console.error('Recommendation error', err);
      const msg = err?.response?.data?.message ?? 'Recommendation failed. Please try again.';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <Layout>
      <div className="max-w-4xl mx-auto space-y-6">
        <div className="text-center space-y-2">
          <div className="flex items-center justify-center gap-2 mb-4">
            <Sparkles className="h-8 w-8 text-primary" />
            <h2 className="text-3xl font-bold">AI Meal Recommendations</h2>
          </div>
          <p className="text-muted-foreground">
            Tell us your preferences, dietary goals, and nutritional requirements. Our AI will recommend the perfect meal for you.
          </p>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>What are you looking for?</CardTitle>
            <CardDescription>
              Example: "I want a high protein meal within 800 kcal, I like chicken and salads mainly with tomatoes"
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Textarea
              placeholder="Describe your meal preferences, dietary requirements, and nutritional goals..."
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              className="min-h-[120px]"
            />
            <Button
              onClick={getRecommendation}
              disabled={isLoading}
              className="w-full gap-2"
              size="lg"
            >
              {isLoading ? (
                <>
                  <Loader2 className="h-5 w-5 animate-spin" />
                  Generating recommendation...
                </>
              ) : (
                <>
                  <Sparkles className="h-5 w-5" />
                  Get AI Recommendation
                </>
              )}
            </Button>
          </CardContent>
        </Card>

        {recommendation && (
          <Card className="border-primary/50 shadow-lg">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-2xl">{recommendation.mealName}</CardTitle>
                  <CardDescription className="mt-2">
                    Recommended menu item: <span className="font-semibold text-foreground">{recommendation.menuItem}</span>
                  </CardDescription>
                </div>
                <Badge className="bg-accent text-accent-foreground">
                  {recommendation.matchPercentage}% Match
                </Badge>
              </div>
            </CardHeader>
            <CardContent className="space-y-4">
              <p className="text-muted-foreground leading-relaxed">
                {recommendation.description}
              </p>

              <div className="flex gap-4 pt-4 border-t">
                <div className="flex-1 text-center p-4 bg-primary/5 rounded-lg">
                  <p className="text-3xl font-bold text-primary">{recommendation.calories}</p>
                  <p className="text-sm text-muted-foreground mt-1">Calories</p>
                </div>
                <div className="flex-1 text-center p-4 bg-accent/5 rounded-lg">
                  <p className="text-3xl font-bold text-accent">{recommendation.protein}g</p>
                  <p className="text-sm text-muted-foreground mt-1">Protein</p>
                </div>
              </div>

              <Button className="w-full" size="lg">
                Add to Order
              </Button>
            </CardContent>
          </Card>
        )}
      </div>
    </Layout>
  );
};

export default Recommendations;