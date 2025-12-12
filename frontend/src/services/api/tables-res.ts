import api from "./client";
import type { RestaurantTable, ReservationRequestDto } from "@/types";

export async function fetchTables(): Promise<RestaurantTable[]> {
  const r = await api.get<RestaurantTable[]>("/tables");
  return r.data ?? [];
}

export async function reserveTable(
  tableId: string,
  fromIso: string,
  toIso: string,
  requesterId?: string,
  userId?: string
) {
  const payload: ReservationRequestDto = { from: fromIso, to: toIso, requestedBy: requesterId, userId };
  const r = await api.post(`/tables/${tableId}/reserve`, payload);
  return r.data;
}

export async function getReservationsForTable(tableId: string) {
  const r = await api.get(`/tables/${tableId}/reservations`);
  return r.data;
}

export async function occupyTable(tableNumber: number, minutes: number) {
  const r = await api.post("/tables/occupy", { tableNumber, minutes });
  return r.data;
}