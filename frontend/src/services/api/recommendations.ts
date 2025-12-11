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

export const recommend = async (request: RecommendationRequest): Promise<RecommendationResponse[]> => {
  console.log('Sending recommendation request:', request);
  const response = await api.post('ai/recommendations', request);
  console.log('Recommendation response:', response.data);

  const payload = Array.isArray(response.data) ? response.data : [response.data];

  const normalized: RecommendationResponse[] = payload.map((item: any) => ({
    name: item?.name ?? item?.menuItemName ?? 'Custom Meal Recommendation',
    description: item?.text ?? item?.description ?? item?.recipe ?? 'No description available',
    calories: item?.calories ?? 0,
    protein: item?.protein ?? 0,
    fats: item?.fats ?? 0,
    carbs: item?.carbs ?? 0,
    ingredients: Array.isArray(item?.products) 
      ? item.products 
      : Array.isArray(item?.ingredients) 
      ? item.ingredients 
      : [],
  }));

  console.log('Normalized response:', normalized);
  return normalized;
};

export const getRecommendationHistory = async () => {
  const response = await api.get('recommendations/history');
  return response.data;
};