import api from './client';

export interface RecommendationRequest {
  prompt: string;
}

export interface RecommendationResponse {
  name: string;
  description: string;
  calories: number;
  fats: number;
  carbs: number;
  protein: number;
  ingredients: string[];
}

export const recommend = async (request: RecommendationRequest): Promise<RecommendationResponse> => {
  console.log('Sending recommendation request:', request);
  const response = await api.post('/api/ai/recommendations', request);
  console.log('Recommendation response:', response.data);

  const payload = Array.isArray(response.data) ? response.data[0] : response.data;

  const normalized: RecommendationResponse = {
    name: payload?.name ?? payload?.menuItemName ?? 'Custom Meal Recommendation',
    description: payload?.text ?? payload?.description ?? payload?.recipe ?? 'No description available',
    calories: payload?.calories ?? 0,
    protein: payload?.protein ?? 0,
    fats: payload?.fats ?? 0,
    carbs: payload?.carbs ?? 0,
    ingredients: Array.isArray(payload?.products) 
      ? payload.products 
      : Array.isArray(payload?.ingredients) 
      ? payload.ingredients 
      : [],
  };

  console.log('Normalized response:', normalized);
  return normalized;
};

export const getRecommendationHistory = async () => {
  const response = await api.get('/api/recommendations/history');
  return response.data;
};