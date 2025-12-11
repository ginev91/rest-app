import api from "./client";

export interface MenuItemDTO {
  id: string;
  name: string;
  description?: string;
  price: number;
  kcal?: number;
  tags?: string[];
}

export async function fetchMenu(): Promise<MenuItemDTO[]> {
  const r = await api.get("menu");
  return r.data as MenuItemDTO[];
}

export async function fetchMenuItem(id: string): Promise<MenuItemDTO> {
  const r = await api.get(`menu/${id}`);
  return r.data as MenuItemDTO;
}