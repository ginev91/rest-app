import { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Badge } from '@/components/ui/badge';
import Layout from '@/components/Layout';
import { toast } from 'sonner';
import { Sparkles, Loader2, ChefHat, Flame, Beef, Wheat, AlertCircle } from 'lucide-react';
import { recommend, RecommendationRequest, RecommendationResponse } from '@/services/api/recommendations';

const Recommendations = () => {
  const [prompt, setPrompt] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);

  const getRecommendation = async () => {
    if (!prompt.trim()) {
      toast.error("Please describe what you're looking for");
      return;
    }

    setIsLoading(true);
    setRecommendation(null);

    try {
      const request: RecommendationRequest = { prompt };
      const response = await recommend(request);
      
      console.log('Recommendation received:', response);
      setRecommendation(response);
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
              Example: "I want a high protein meal under 800 calories with chicken and vegetables"
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
              disabled={isLoading || !prompt.trim()}
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
          <Card className="border-primary/50 bg-gradient-to-br from-primary/5 to-background">
            <CardHeader>
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <ChefHat className="h-6 w-6 text-primary" />
                  <CardTitle className="text-2xl">{recommendation.name}</CardTitle>
                </div>
              </div>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Nutritional Information */}
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <div className="p-4 rounded-lg bg-background/50 border">
                  <div className="flex items-center gap-2 mb-1">
                    <Flame className="h-4 w-4 text-orange-500" />
                    <p className="text-sm text-muted-foreground">Calories</p>
                  </div>
                  <p className="text-2xl font-bold">{recommendation.calories}</p>
                </div>
                <div className="p-4 rounded-lg bg-background/50 border">
                  <div className="flex items-center gap-2 mb-1">
                    <Beef className="h-4 w-4 text-red-500" />
                    <p className="text-sm text-muted-foreground">Protein</p>
                  </div>
                  <p className="text-2xl font-bold">{recommendation.protein}g</p>
                </div>
                <div className="p-4 rounded-lg bg-background/50 border">
                  <p className="text-sm text-muted-foreground mb-1">Fats</p>
                  <p className="text-2xl font-bold">{recommendation.fats}g</p>
                </div>
                <div className="p-4 rounded-lg bg-background/50 border">
                  <div className="flex items-center gap-2 mb-1">
                    <Wheat className="h-4 w-4 text-amber-600" />
                    <p className="text-sm text-muted-foreground">Carbs</p>
                  </div>
                  <p className="text-2xl font-bold">{recommendation.carbs}g</p>
                </div>
              </div>

              {/* Products/Ingredients - only show if not empty */}
              {recommendation.ingredients && recommendation.ingredients.length > 0 && (
                <div>
                  <h3 className="font-semibold mb-2">Ingredients</h3>
                  <div className="flex flex-wrap gap-2">
                    {recommendation.ingredients.map((ingredient, index) => (
                      <Badge key={index} variant="secondary">
                        {ingredient}
                      </Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* Description */}
              <div>
                <h3 className="font-semibold mb-2">Description</h3>
                <p className="text-muted-foreground leading-relaxed whitespace-pre-line">
                  {recommendation.description}
                </p>
              </div>

              {/* Allergy Warning */}
              {recommendation.description && !recommendation.description.includes('allerg') && (
                <div className="flex items-start gap-2 p-3 rounded-lg bg-amber-500/10 border border-amber-500/20">
                  <AlertCircle className="h-5 w-5 text-amber-600 mt-0.5 flex-shrink-0" />
                  <p className="text-sm text-amber-900 dark:text-amber-100">
                    Please be aware of potential allergies and check with the restaurant for further information.
                  </p>
                </div>
              )}

              <Button className="w-full" size="lg" disabled>
                Add to Order (Coming Soon)
              </Button>
            </CardContent>
          </Card>
        )}

        {!recommendation && !isLoading && (
          <Card className="border-dashed">
            <CardContent className="flex flex-col items-center justify-center py-12 text-center">
              <Sparkles className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-semibold mb-2">No recommendations yet</p>
              <p className="text-muted-foreground">
                Describe what you're looking for and get personalized meal suggestions
              </p>
            </CardContent>
          </Card>
        )}
      </div>
    </Layout>
  );
};

export default Recommendations;