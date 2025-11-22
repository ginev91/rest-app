export interface MenuItem {
  id: string;
  name: string;
  description: string;
  price: number;
  category: string;
  calories?: number;
  protein?: number;
  image?: string;
  available: boolean;
}

export interface MenuCategory {
  id: string;
  name: string;
  items: MenuItem[];
}
