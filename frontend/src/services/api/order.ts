import api from "./client";

export interface OrderItemReq { menuItemId: string; quantity: number; note?: string; }
export interface CreateOrderDTO { tableId: string; waiterId?: string; items: OrderItemReq[]; }

export async function createOrder(dto: CreateOrderDTO) {
  const r = await api.post("orders", dto);
  return r.data;
}

export async function getOrders(params?: Record<string, any>) {
  const r = await api.get("orders", { params });
  return r.data;
}

export async function sendOrderToKitchen(orderId: string) {
  const r = await api.post(`orders/${orderId}/send`);
  return r.data;
}

export async function claimOrder(orderId: string, waiterId?: string) {
  
  const payload = waiterId ? { waiterId } : { waiterId: undefined };
  const r = await api.put(`orders/${orderId}/claim`, payload);
  return r.data; 
}

export async function assignOrder(orderId: string, waiterId: string) {
  
  const r = await api.put(`orders/${orderId}/assign`, { waiterId });
  return r.data;
}

export async function updateOrderItemStatus(orderId: string, itemId: string, statusLabel: string) {
  
  const r = await api.put(`orders/${orderId}/items/${itemId}/status`, { status: statusLabel });
  return r.data;
}

export async function updateOrderStatus(orderId: string, statusLabel: string) {
  const r = await api.put(`orders/${orderId}/status`, { status: statusLabel });
  return r.data;
}