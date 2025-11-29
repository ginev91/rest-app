import api from './client';

export interface RecommendationRequest {
  prompt: string;
}

export interface RecommendationResponse {
  recipe?: string;
  description?: string;
  matchedMenuItemId?: string;
  menuItemId?: string;
  menuItemName?: string;
  score?: number;
  matchPercentage?: number;
  calories?: number;
  protein?: number;
}

export const recommend = async (request: RecommendationRequest): Promise<RecommendationResponse> => {
  console.log('Sending recommendation request:', request);
  const response = await api.post('/api/recommendations', request);
  console.log('Recommendation response:', response.data);
  return response.data;
};

export const getRecommendationHistory = async () => {
  const response = await api.get('/api/recommendations/history');
  return response.data;
};