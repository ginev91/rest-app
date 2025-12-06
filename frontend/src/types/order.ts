export interface OrderItem {
  id: string;
  menuItemId: string;
  menuItemName: string;
  quantity: number;
  price: number;
  status?: string;
  specialInstructions?: string;
}

export interface Order {
  id: string;
  userId: string;
  username: string;
  tableNumber?: number;
  items: OrderItem[];
  status: string;
  totalAmount: number;
  createdAt: string;
  updatedAt: string;
}

export interface Table {
  id: string;
  number: number;
  capacity: number;
  status: 'available' | 'occupied' | 'reserved';
  currentOrderId?: string;
}
