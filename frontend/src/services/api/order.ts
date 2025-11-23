import api from "./client";

export interface OrderItemReq { menuItemId: string; quantity: number; note?: string; }
export interface CreateOrderDTO { tableId: string; waiterId?: string; items: OrderItemReq[]; }

export async function createOrder(dto: CreateOrderDTO) {
  const r = await api.post("/api/orders", dto);
  return r.data;
}

export async function getOrders(params?: Record<string, any>) {
  const r = await api.get("/api/orders", { params });
  return r.data;
}

export async function sendOrderToKitchen(orderId: string) {
  const r = await api.post(`/api/orders/${orderId}/send`);
  return r.data;
}