import api from "./client";

export interface RecommendationRequest {
  maxKcal?: number;
  preferences?: string; 
  likes?: string[];
  dislikes?: string[];
}

export interface RecommendationDTO {
  recipe: string;
  matchedMenuItemId?: string;
  score?: number;
}

export async function recommend(req: RecommendationRequest): Promise<RecommendationDTO> {
  const r = await api.post("/api/recommendations", req);
  return r.data as RecommendationDTO;
}