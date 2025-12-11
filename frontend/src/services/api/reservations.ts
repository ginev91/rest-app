import api from "./client";

export interface TableReservationResponseDto {
  id: string;
  tableId: string;
  userId?: string | null;
  from: string;
  to: string;
  status?: string;
  requestedBy?: string;
}

export async function createReservation(payload: any): Promise<TableReservationResponseDto> {
  const r = await api.post<TableReservationResponseDto>("/reservations", payload);
  return r.data;
}

export async function cancelReservation(id: string, cancelledBy: string) {
  const r = await api.post<TableReservationResponseDto>(`/reservations/${id}/cancel`, { cancelledBy });
  return r.data;
}

export async function getActiveForTable(tableId: string): Promise<TableReservationResponseDto[]> {
  const r = await api.get<TableReservationResponseDto[]>(`/reservations/table/${tableId}`);
  return r.data ?? [];
}

export async function getHistoryForTable(tableId: string): Promise<TableReservationResponseDto[]> {
  const r = await api.get<TableReservationResponseDto[]>(`/reservations/table/${tableId}/history`);
  return r.data ?? [];
}