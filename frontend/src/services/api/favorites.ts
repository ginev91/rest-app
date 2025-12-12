import api from '@/services/api/client';

export type FavoritePayload = {
  menuItemId?: string;
  menuItemName?: string;
  description?: string;
  ingredients?: string[];
  calories?: number;
  protein?: number;
  fats?: number;
  carbs?: number;
};

export type FavoriteDto = {
  id: string;
  menuItemId?: string;
  menuItemName?: string;
  description?: string;
  ingredients?: string[];
  calories?: number;
  protein?: number;
  fats?: number;
  carbs?: number;
  createdBy?: string;
  createdAt?: string;
};

export const listFavorites = async (): Promise<FavoriteDto[]> => {
  const res = await api.get('recommendations/favorites');
  return res.data || [];
};

export const addFavorite = async (payload: FavoritePayload): Promise<FavoriteDto> => {
  const body = {
    menuItemId: payload.menuItemId,
    menuItemName: payload.menuItemName,
    description: payload.description,
    ingredients: payload.ingredients,
    calories: payload.calories,
    protein: payload.protein,
    fats: payload.fats,
    carbs: payload.carbs,
  };
  const res = await api.post('recommendations/favorites', body);
  return res.data;
};

export const removeFavorite = async (id: string): Promise<void> => {
  await api.delete(`recommendations/favorites/${id}`);
};