import api from '@/services/api/client';

export type TableStatus = 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'OUT_OF_SERVICE';

export type TableDto = {
  id: string;
  code: string;
  seats: number;
  currentOccupancy: number;
  status: TableStatus;
  pinCode?: string;
  tableNumber?: number | null;
  occupiedUntil?: string | null;
};

export const listTables = async (): Promise<TableDto[]> => {
  const res = await api.get('/tables');
  console.debug('tables.listTables res.data=', res.data);
  return res.data || [];
};

export const getTable = async (id: string): Promise<TableDto> => {
  const res = await api.get(`/tables/${id}`);
  return res.data;
};

export const createTable = async (payload: Partial<TableDto>): Promise<TableDto> => {
  const res = await api.post('/tables', payload);
  return res.data;
};

export const updateTable = async (id: string, payload: Partial<TableDto>): Promise<TableDto> => {
  const res = await api.put(`/tables/${id}`, payload);
  return res.data;
};

export const deleteTable = async (id: string): Promise<void> => {
  await api.delete(`/tables/${id}`);
};