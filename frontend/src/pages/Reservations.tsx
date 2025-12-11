import React, { useEffect, useMemo, useState } from 'react';
import { DayPicker } from 'react-day-picker';
import 'react-day-picker/dist/style.css';
import { useAuth } from '@/contexts/AuthContext';
import { Button } from '@/components/ui/button';
import api from '@/services/api/client';
import { toast } from 'sonner';

type TableItem = {
  id: string;
  code?: string;
  seats?: number;
  status?: string;
};

type ReservationDto = {
  id: string;
  tableId: string;
  startTime: string;
  endTime?: string;
  requestedBy?: string;
  userId?: string;
  deleted?: boolean;
  partySize?: number;
};

type Reservation = {
  id: string;
  date: string;
  time?: string | null;
  tableId: string;
  customerName?: string; 
  requestedById?: string;
  startIso?: string;
  endIso?: string;
  deleted?: boolean;
  partySize?: number;
};

const Reservations: React.FC = () => {
  const { user } = useAuth() as any;
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
  const [time, setTime] = useState<string>(() => {
    const now = new Date();
    const hh = now.getHours().toString().padStart(2, '0');
    const mm = Math.ceil(now.getMinutes() / 15) * 15;
    const mmStr = (mm === 60 ? '00' : mm.toString().padStart(2, '0'));
    return `${mm === 60 ? (now.getHours() + 1).toString().padStart(2, '0') : hh}:${mmStr}`;
  });
  const [durationMinutes, setDurationMinutes] = useState<number>(90);
  const [partySize, setPartySize] = useState<number>(2);

  const [tables, setTables] = useState<TableItem[]>([]);
  const [reservations, setReservations] = useState<Reservation[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingTables, setLoadingTables] = useState(false);
  const [selectedTable, setSelectedTable] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  
  const [editingReservationId, setEditingReservationId] = useState<string | null>(null);

  
  const [showEditModal, setShowEditModal] = useState(false);
  const [modalDate, setModalDate] = useState<string>(''); 
  const [modalTime, setModalTime] = useState<string>(''); 
  const [modalDuration, setModalDuration] = useState<number>(90);
  const [modalTableId, setModalTableId] = useState<string | null>(null);
  const [modalPartySize, setModalPartySize] = useState<number>(2);

  
  const [historyOpenForTable, setHistoryOpenForTable] = useState<string | null>(null);
  const [openTableCode, setOpenTableCode] = useState<string | null>(null);
  const [historyReservations, setHistoryReservations] = useState<Reservation[]>([]);
  const [loadingHistory, setLoadingHistory] = useState<boolean>(false);

  
  const [userCache, setUserCache] = useState<Record<string, { username?: string; fullName?: string }>>({});

  const displayNameFromData = (data: any, fallbackId: string) => {
    if (!data) return fallbackId;
    if (data.fullName) return data.fullName;
    if (data.full_name) return data.full_name;
    if (data.username) return data.username;
    if (data.email) return data.email;
    return fallbackId;
  };

  const fetchUserDisplayName = async (userId: string): Promise<string> => {
    if (!userId) return userId;
    if (userCache[userId]) {
      const cached = userCache[userId];
      return cached.fullName || cached.username || userId;
    }
    try {
      const res = await api.get(`/users/${userId}`);
      const data = res.data;
      const name = displayNameFromData(data, userId);
      setUserCache((prev) => ({ ...prev, [userId]: { username: data?.username, fullName: data?.fullName ?? data?.full_name } }));
      return name;
    } catch {
      if (user?.userId === userId || user?.id === userId) {
        const name = user?.fullName || user?.full_name || user?.username || userId;
        setUserCache((prev) => ({ ...prev, [userId]: { username: user?.username, fullName: user?.fullName } }));
        return name;
      }
      return userId;
    }
  };

  const formatDateYMD = (d: Date) => {
    const y = d.getFullYear();
    const m = (d.getMonth() + 1).toString().padStart(2, '0');
    const day = d.getDate().toString().padStart(2, '0');
    return `${y}-${m}-${day}`;
  };

  const formatTimeRange = (startIso?: string, endIso?: string) => {
    if (!startIso && !endIso) return '—';
    try {
      if (startIso && endIso) {
        const s = new Date(startIso);
        const e = new Date(endIso);
        const sf = s.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        const ef = e.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
        return `${sf} — ${ef}`;
      }
      if (startIso) {
        const s = new Date(startIso);
        return s.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      }
      return '—';
    } catch {
      return startIso ? startIso.slice(11, 16) : '—';
    }
  };

  const intervalsOverlap = (aStart: number, aEnd: number, bStart: number, bEnd: number) => {
    return aStart < bEnd && bStart < aEnd;
  };

  
  const roleValue = (user?.role || user?.roles || '').toString().toUpperCase();
  const isStaff =
    roleValue.includes('ADMIN') ||
    roleValue.includes('EMPLOYEE') ||
    (Array.isArray(user?.roles) && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_EMPLOYEE')));

  useEffect(() => {
    const loadTables = async () => {
      setLoadingTables(true);
      try {
        const res = await api.get<TableItem[]>('tables');
        setTables(res.data || []);
        if ((res.data || []).length > 0 && !selectedTable) {
          setSelectedTable((res.data as TableItem[])[0].id);
        }
      } catch (e: any) {
        console.error(e);
        setError('Unable to load tables');
      } finally {
        setLoadingTables(false);
      }
    };
    loadTables();
  }, []);

  
  useEffect(() => {
    const loadReservations = async () => {
      if (!selectedDate) {
        setReservations([]);
        return;
      }
      setLoading(true);
      try {
        const iso = formatDateYMD(selectedDate);
        const res = await api.get<ReservationDto[]>('reservations', { params: { date: iso } });
        const list = res.data || [];
        const initial: Reservation[] = list.map((d) => {
          const datePart = d.startTime ? d.startTime.slice(0, 10) : iso;
          const timePart = d.startTime ? d.startTime.slice(11, 16) : undefined;
          return {
            id: d.id,
            date: datePart,
            time: timePart,
            tableId: d.tableId,
            customerName: d.requestedBy ?? d.userId ?? undefined,
            requestedById: d.requestedBy ?? d.userId ?? undefined,
            startIso: d.startTime,
            endIso: d.endTime,
            deleted: d.deleted ?? false,
            partySize: (d as any).partySize ?? (d as any).party_size ?? undefined,
          } as Reservation;
        });
        setReservations(initial);

        const uniqueIds = Array.from(new Set(initial.map((r) => r.requestedById).filter(Boolean))) as string[];
        await Promise.all(
          uniqueIds.map(async (uid) => {
            const name = await fetchUserDisplayName(uid);
            setReservations((prev) => prev.map((r) => (r.requestedById === uid ? { ...r, customerName: name } : r)));
          })
        );
      } catch (e) {
        console.error(e);
        setReservations([]);
      } finally {
        setLoading(false);
      }
    };
    loadReservations();
  }, [selectedDate]);

  const reservedByTable = useMemo(() => {
    const map = new Map<string, Reservation[]>();
    for (const r of reservations) {
      const arr = map.get(r.tableId) || [];
      arr.push(r);
      map.set(r.tableId, arr);
    }
    return map;
  }, [reservations]);

  
  const availableTables = useMemo(() => {
    if (!selectedDate || !time) return tables;
    const dateStr = formatDateYMD(selectedDate);
    const fromLocal = new Date(`${dateStr}T${time}:00`);
    const fromTs = fromLocal.getTime();
    const toLocal = new Date(fromLocal.getTime() + durationMinutes * 60000);
    const toTs = toLocal.getTime();

    const defaultResDuration = 2 * 60 * 60000;

    return tables.filter((t) => {
      const reserved = (reservedByTable.get(t.id) || []).filter((r) => !r.deleted && r.id !== editingReservationId);
      if (reserved.length === 0) return true;
      return !reserved.some((r) => {
        const rStart = r.startIso ? new Date(r.startIso).getTime() : NaN;
        if (isNaN(rStart)) return false; 
        const rEnd = r.endIso ? new Date(r.endIso).getTime() : (rStart + defaultResDuration);
        return intervalsOverlap(fromTs, toTs, rStart, rEnd);
      });
    });
  }, [tables, reservedByTable, selectedDate, time, durationMinutes, editingReservationId]);

  const handleSelectTable = (tableId: string) => {
    setSelectedTable(tableId);
    setError(null);
  };

  const clearMessages = () => {
    setError(null);
    setMessage(null);
  };

  const resolveCurrentUserUuid = async (): Promise<string | null> => {
    if (user?.userId) return user.userId;
    if (user?.id) return user.id;
    try {
      const me = await api.get('/auth/me');
      const data: any = me.data;
      if (data?.id) return data.id;
      if (data?.user?.id) return data.user.id;
    } catch (err) {
      console.debug('Could not resolve /auth/me', err);
    }
    return null;
  };

  
  const handleCreateReservation = async () => {
    clearMessages();
    if (!selectedTable) {
      setError('Please select a table.');
      return;
    }
    if (!selectedDate) {
      setError('Please pick a date.');
      return;
    }
    if (!time) {
      setError('Please pick a time.');
      return;
    }
    if (!partySize || partySize < 1) {
      setError('Please enter a valid number of people.');
      return;
    }

    setLoading(true);
    try {
      const resolvedUserId = await resolveCurrentUserUuid();
      if (!resolvedUserId) {
        setError('Unable to determine current user id (required by backend). Please login again.');
        setLoading(false);
        return;
      }

      const dateStr = formatDateYMD(selectedDate);
      const fromLocal = new Date(`${dateStr}T${time}:00`);
      const toLocal = new Date(fromLocal.getTime() + durationMinutes * 60000);

      const existing = (reservedByTable.get(selectedTable) || []).filter((r) => !r.deleted);
      const desiredStart = fromLocal.getTime();
      const desiredEnd = toLocal.getTime();
      const defaultResDuration = 2 * 60 * 60000;

      const hasOverlap = existing.some((r) => {
        const rStart = r.startIso ? new Date(r.startIso).getTime() : NaN;
        if (isNaN(rStart)) return false;
        const rEnd = r.endIso ? new Date(r.endIso).getTime() : (rStart + defaultResDuration);
        return intervalsOverlap(desiredStart, desiredEnd, rStart, rEnd);
      });

      if (hasOverlap) {
        setError('Selected time overlaps an existing reservation for this table. Please pick a different time or table.');
        setLoading(false);
        return;
      }

      const payload = {
        tableId: selectedTable,
        from: fromLocal.toISOString(),
        to: toLocal.toISOString(),
        requestedBy: resolvedUserId,
        userId: resolvedUserId,
        partySize,
      };

      await api.post('reservations', payload);
      toast.success('Reservation created');

      
      const iso = formatDateYMD(selectedDate);
      const rres = await api.get<ReservationDto[]>('reservations', { params: { date: iso } });
      const list = rres.data || [];
      const mapped = list.map((d) => ({
        id: d.id,
        date: d.startTime ? d.startTime.slice(0, 10) : iso,
        time: d.startTime ? d.startTime.slice(11, 16) : undefined,
        tableId: d.tableId,
        customerName: d.requestedBy ?? d.userId,
        requestedById: d.requestedBy ?? d.userId,
        startIso: d.startTime,
        endIso: d.endTime,
        deleted: d.deleted ?? false,
        partySize: (d as any).partySize ?? (d as any).party_size ?? undefined,
      } as Reservation));
      setReservations(mapped);

      const uniqueIds = Array.from(new Set(mapped.map((r) => r.requestedById).filter(Boolean))) as string[];
      await Promise.all(uniqueIds.map(async (uid) => {
        const name = await fetchUserDisplayName(uid);
        setReservations((prev) => prev.map((r) => (r.requestedById === uid ? { ...r, customerName: name } : r)));
      }));

      setMessage('Reservation created');
      setSelectedTable(null);
      setPartySize(2);
    } catch (e: any) {
      console.error(e);
      const msg = e?.response?.data?.message || e.message || 'Failed to create reservation';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  
  const cancelReservation = async (reservationId: string) => {
    const confirmed = window.confirm('Cancel this reservation?');
    if (!confirmed) return false;
    try {
      const cancelledBy = await resolveCurrentUserUuid();
      if (!cancelledBy) throw new Error('Cannot determine cancelling user');
      await api.post(`reservations/${reservationId}/cancel`, { cancelledBy });
      toast.success('Reservation cancelled');

      
      const iso = formatDateYMD(selectedDate!);
      const rres = await api.get<ReservationDto[]>('reservations', { params: { date: iso } });
      const list = rres.data || [];
      const mapped = list.map((d) => ({
        id: d.id,
        date: d.startTime ? d.startTime.slice(0, 10) : iso,
        time: d.startTime ? d.startTime.slice(11, 16) : undefined,
        tableId: d.tableId,
        customerName: d.requestedBy ?? d.userId,
        requestedById: d.requestedBy ?? d.userId,
        startIso: d.startTime,
        endIso: d.endTime,
        deleted: d.deleted ?? false,
        partySize: (d as any).partySize ?? (d as any).party_size ?? undefined,
      } as Reservation));
      setReservations(mapped);
      return true;
    } catch (err: any) {
      console.error('Cancel failed', err);
      toast.error(err?.response?.data?.message || 'Failed to cancel reservation');
      return false;
    }
  };

  
  const beginEditReservation = (res: Reservation) => {
    if (res.deleted) {
      toast.error('This reservation was cancelled and cannot be edited.');
      return;
    }

    setEditingReservationId(res.id);
    setModalTableId(res.tableId);
    setModalDate(res.date);
    setModalTime(res.time ?? '12:00');
    if (res.startIso && res.endIso) {
      const start = new Date(res.startIso).getTime();
      const end = new Date(res.endIso).getTime();
      const dur = Math.max(15, Math.round((end - start) / 60000));
      setModalDuration(dur);
    } else {
      setModalDuration(durationMinutes);
    }
    setModalPartySize(res.partySize ?? partySize);
    setShowEditModal(true);
    setMessage('Editing reservation — change fields in the modal and save');
  };

  
  const saveEditedReservationFromModal = async () => {
    if (!editingReservationId) return;
    if (!modalTableId || !modalDate || !modalTime) {
      toast.error('Please select table, date and time in the edit form.');
      return;
    }
    if (!modalPartySize || modalPartySize < 1) {
      toast.error('Please enter a valid number of people.');
      return;
    }

    setLoading(true);
    try {
      const resolvedUserId = await resolveCurrentUserUuid();
      if (!resolvedUserId) {
        toast.error('Unable to determine current user id; please login again.');
        setLoading(false);
        return;
      }

      const fromLocal = new Date(`${modalDate}T${modalTime}:00`);
      const toLocal = new Date(fromLocal.getTime() + modalDuration * 60000);

      
      const existing = (reservedByTable.get(modalTableId) || []).filter((r) => !r.deleted && r.id !== editingReservationId);
      const desiredStart = fromLocal.getTime();
      const desiredEnd = toLocal.getTime();
      const defaultResDuration = 2 * 60 * 60000;

      const hasOverlap = existing.some((r) => {
        const rStart = r.startIso ? new Date(r.startIso).getTime() : NaN;
        if (isNaN(rStart)) return false;
        const rEnd = r.endIso ? new Date(r.endIso).getTime() : (rStart + defaultResDuration);
        return intervalsOverlap(desiredStart, desiredEnd, rStart, rEnd);
      });

      if (hasOverlap) {
        toast.error('Selected time overlaps another reservation for this table.');
        setLoading(false);
        return;
      }

      const payload = {
        tableId: modalTableId,
        from: fromLocal.toISOString(),
        to: toLocal.toISOString(),
        requestedBy: resolvedUserId,
        userId: resolvedUserId,
        partySize: modalPartySize,
      };

      try {
        await api.put(`reservations/${editingReservationId}`, payload);
      } catch (err: any) {
        const status = err?.response?.status;
        const serverMsg = err?.response?.data?.message || err?.message;
        if (status === 409 || (serverMsg && serverMsg.toLowerCase().includes('overlap'))) {
          toast.error(serverMsg || 'Reservation overlaps another reservation (server)');
          setLoading(false);
          return;
        }
        console.error('Update failed:', err);
        toast.error(serverMsg || 'Failed to update reservation.');
        setLoading(false);
        return;
      }

      toast.success('Reservation updated');

      
      const iso = formatDateYMD(new Date(modalDate));
      const rres = await api.get<ReservationDto[]>('reservations', { params: { date: iso } });
      const list = rres.data || [];
      const mapped = list.map((d) => ({
        id: d.id,
        date: d.startTime ? d.startTime.slice(0, 10) : iso,
        time: d.startTime ? d.startTime.slice(11, 16) : undefined,
        tableId: d.tableId,
        customerName: d.requestedBy ?? d.userId,
        requestedById: d.requestedBy ?? d.userId,
        startIso: d.startTime,
        endIso: d.endTime,
        deleted: d.deleted ?? false,
        partySize: (d as any).partySize ?? (d as any).party_size ?? undefined,
      } as Reservation));
      setReservations(mapped);

      const uniqueIds = Array.from(new Set(mapped.map((r) => r.requestedById).filter(Boolean))) as string[];
      await Promise.all(uniqueIds.map(async (uid) => {
        const name = await fetchUserDisplayName(uid);
        setReservations((prev) => prev.map((r) => (r.requestedById === uid ? { ...r, customerName: name } : r)));
      }));

      setShowEditModal(false);
      setEditingReservationId(null);
      setMessage(null);
      setSelectedTable(null);
    } catch (err: any) {
      console.error('Save edit failed', err);
      toast.error(err?.response?.data?.message || 'Failed to update reservation');
    } finally {
      setLoading(false);
    }
  };

  const openHistoryForTable = async (tableId: string, tableCode: string) => {
    if (!isStaff) return;
    setHistoryOpenForTable(tableId);
    setOpenTableCode(tableCode)
    setLoadingHistory(true);
    try {
      const res = await api.get<ReservationDto[]>(`reservations/table/${tableId}/history`);
      const list = res.data || [];
      
      const mapped: Reservation[] = list
        .map((d) => ({
          id: d.id,
          date: d.startTime ? d.startTime.slice(0, 10) : (d as any).date ?? '',
          time: d.startTime ? d.startTime.slice(11, 16) : undefined,
          tableId: d.tableId,
          customerName: d.requestedBy ?? d.userId,
          requestedById: d.requestedBy ?? d.userId,
          startIso: d.startTime,
          endIso: d.endTime,
          deleted: d.deleted ?? false,
          partySize: (d as any).partySize ?? (d as any).party_size ?? undefined,
        } as Reservation))
        .filter((r) => {
          
          
          
          const now = Date.now();
          if (r.endIso) {
            const endTs = Number(new Date(r.endIso).getTime());
            return !isNaN(endTs) && endTs < now;
          }
          if (r.startIso) {
            const startTs = Number(new Date(r.startIso).getTime());
            return !isNaN(startTs) && startTs < now;
          }
          
          return false;
        });

      setHistoryReservations(mapped);

      const uniqueIds = Array.from(new Set(mapped.map((r) => r.requestedById).filter(Boolean))) as string[];
      await Promise.all(uniqueIds.map(async (uid) => {
        const name = await fetchUserDisplayName(uid);
        setHistoryReservations((prev) => prev.map((r) => (r.requestedById === uid ? { ...r, customerName: name } : r)));
      }));
    } catch (err: any) {
      console.error('Failed to load table history', err);
      setHistoryReservations([]);
      toast.error(err?.response?.data?.message || 'Failed to load table history');
    } finally {
      setLoadingHistory(false);
    }
  };

  const closeHistoryModal = () => {
    setHistoryOpenForTable(null);
    setHistoryReservations([]);
    setLoadingHistory(false);
  };

  
  const canManageReservation = (res: Reservation) => {
    if (!user) return false;
    const role = (user.role || user.roles || '').toString().toUpperCase();
    const isAdminOrEmployee =
      role?.includes('ADMIN') || role?.includes('EMPLOYEE') || (Array.isArray(user.roles) && (user.roles.includes('ROLE_ADMIN') || user.roles.includes('ROLE_EMPLOYEE')));
    const isOwner = user.userId === res.requestedById || user.id === res.requestedById;
    return isAdminOrEmployee || isOwner;
  };

  
  const renderHistoryModalContent = () => {
    if (loadingHistory) {
      return <div className="text-sm text-muted-foreground">Loading history…</div>;
    }
    if (historyReservations.length === 0) {
      return <div className="text-sm text-muted-foreground">No past/finished reservations for this table.</div>;
    }

    const rows = [...historyReservations].sort((a, b) => {
      const ta = a.startIso ? new Date(a.startIso).getTime() : 0;
      const tb = b.startIso ? new Date(b.startIso).getTime() : 0;
      return ta - tb;
    });

    return (
      <div className="overflow-auto max-h-[60vh]">
        <table className="w-full text-sm">
          <thead>
            <tr className="text-left">
              <th className="p-2">Date</th>
              <th className="p-2">Time</th>
              <th className="p-2">Party</th>
              <th className="p-2">Customer</th>
              <th className="p-2">Status</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const status = r.deleted ? 'Cancelled' : 'Finished';
              return (
                <tr key={r.id} className="border-t">
                  <td className="p-2 align-top">{r.date}</td>
                  <td className="p-2 align-top">{formatTimeRange(r.startIso, r.endIso)}</td>
                  <td className="p-2 align-top">{r.partySize ?? '-'}</td>
                  <td className="p-2 align-top">{r.customerName ?? (r.requestedById ?? '-')}</td>
                  <td className="p-2 align-top">{status}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  };

  
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-semibold">Reservations</h2>
          <div className="text-sm text-muted-foreground">{user ? `Logged in as ${user.username || user.email || user.userId}` : 'Not logged in'}</div>
        </div>

        <div className="flex items-center gap-2">
          {/* no global history tab; history is per-table */}
        </div>
      </div>

      <div className="grid md:grid-cols-3 gap-6">
        <section className="p-4 border rounded-lg md:col-span-1">
          <h3 className="font-medium mb-3">Choose date & time</h3>

          <div className="w-full max-w-full mb-3">
            <div className="w-full md:w-96 lg:w-[34rem]">
              <DayPicker
                mode="single"
                selected={selectedDate}
                onSelect={(d) => setSelectedDate(d ?? undefined)}
                fromDate={new Date()}
                modifiersClassNames={{
                  today: 'ring ring-primary/60',
                  selected: 'bg-primary text-primary-foreground',
                }}
              />
            </div>
          </div>

          <div className="space-y-2">
            <label className="block text-sm font-medium">Time</label>
            <input type="time" className="input input-sm" value={time} onChange={(e) => setTime(e.target.value)} />

            <div>
              <label className="block text-sm font-medium mt-2">Duration (minutes)</label>
              <input
                type="number"
                min={15}
                step={15}
                className="input input-sm w-32"
                value={durationMinutes}
                onChange={(e) => setDurationMinutes(Number(e.target.value || 0))}
              />
            </div>

            <div className="mt-3">
              <label className="block text-sm font-medium">People</label>
              <input
                type="number"
                min={1}
                step={1}
                className="input input-sm w-24"
                value={partySize}
                onChange={(e) => setPartySize(Number(e.target.value || 1))}
              />
            </div>
          </div>
        </section>

        <section className="p-4 border rounded-lg md:col-span-2">
          <div className="flex items-center justify-between">
            <h3 className="font-medium mb-3">Tables availability for {selectedDate ? formatDateYMD(selectedDate) : '—'}</h3>
            <div className="text-sm text-muted-foreground">Selected time: {time} • duration: {durationMinutes}m</div>
          </div>

          {loadingTables ? (
            <div>Loading tables…</div>
          ) : tables.length === 0 ? (
            <div className="text-sm text-muted-foreground">No tables configured.</div>
          ) : (
            <>
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {tables.map((t) => {
                  const reservedAll = reservations.filter((r) => r.tableId === t.id) || [];
                  const reserved = reservedAll.filter((r) => !r.deleted);
                  const isAvailable = availableTables.some((at) => at.id === t.id);

                  return (
                    <label
                      key={t.id}
                      className={`p-3 border rounded-lg flex flex-col justify-between cursor-pointer ${
                        isAvailable ? 'hover:shadow-md' : 'opacity-70 bg-gray-50'
                      } ${selectedTable === t.id ? 'ring ring-primary' : ''}`}
                    >
                      <div>
                        <div className="font-medium">
                          {t.code ?? t.id} {t.seats ? <span className="text-sm text-muted-foreground">— {t.seats} seats</span> : null}
                        </div>
                        <div className="text-sm text-muted-foreground mt-1">
                          {isAvailable ? <span className="text-green-600 font-medium">Available</span> : <span className="text-red-600 font-medium">Reserved</span>}
                        </div>

                        {reserved.length > 0 && (
                          <div className="text-xs mt-2">
                            {reserved.map((r) => {
                              const isOwner = user && (user.userId === r.requestedById || user.id === r.requestedById);
                              const timeRange = formatTimeRange(r.startIso, r.endIso);
                              return (
                                <div
                                  key={r.id}
                                  className={`mb-1 flex items-center justify-between ${r.deleted ? 'opacity-60 text-gray-500' : ''}`}
                                >
                                  <div>
                                    <strong>{timeRange}</strong>
                                    {(isStaff || isOwner) && r.customerName ? (
                                      <span className="ml-2">— {isOwner ? 'You' : r.customerName}</span>
                                    ) : null}
                                    {r.partySize ? <span className="ml-2 text-muted-foreground">• {r.partySize}p</span> : null}
                                    {r.deleted ? <span className="ml-2 italic text-xs"> (cancelled)</span> : null}
                                  </div>

                                  <div className="ml-2 flex items-center gap-2">
                                    {canManageReservation(r) && !r.deleted && (
                                      <>
                                        <button
                                          type="button"
                                          className="text-xs text-primary underline"
                                          onClick={(ev) => {
                                            ev.stopPropagation();
                                            beginEditReservation(r);
                                          }}
                                        >
                                          Edit
                                        </button>
                                        <button
                                          type="button"
                                          className="text-xs text-red-600 underline"
                                          onClick={async (ev) => {
                                            ev.stopPropagation();
                                            await cancelReservation(r.id);
                                          }}
                                        >
                                          Cancel
                                        </button>
                                      </>
                                    )}
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        )}
                      </div>

                      <div className="mt-3 flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <input
                            type="radio"
                            name="table"
                            value={t.id}
                            checked={selectedTable === t.id}
                            onChange={() => handleSelectTable(t.id)}
                            disabled={!isAvailable}
                          />
                          <div className="text-sm">{!isAvailable ? <span className="text-sm text-muted-foreground">Choose other</span> : <span className="text-sm text-primary">Select</span>}</div>
                        </div>

                        {isStaff && (
                          <Button size="sm" variant="ghost" onClick={() => openHistoryForTable(t.id, t.code)}>
                            History
                          </Button>
                        )}
                      </div>
                    </label>
                  );
                })}
              </div>

              <div className="mt-4 flex items-center gap-3">
                <Button onClick={handleCreateReservation} disabled={loading}>
                  {loading ? 'Saving…' : 'Reserve selected table'}
                </Button>
                <Button variant="ghost" onClick={() => { setSelectedTable(null); clearMessages(); }}>
                  Clear selection
                </Button>

                {message && <div className="text-sm text-green-600 ml-2">{message}</div>}
                {error && <div className="text-sm text-red-600 ml-2">{error}</div>}
              </div>

              {loading ? <div className="mt-3 text-sm">Refreshing reservations…</div> : null}
            </>
          )}
        </section>
      </div>

      {/* Edit Modal */}
      {showEditModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => { setShowEditModal(false); setEditingReservationId(null); }} />
          <div className="relative z-10 w-full max-w-xl bg-white dark:bg-slate-800 rounded-lg p-6 shadow-lg">
            <h3 className="text-lg font-medium mb-4">Edit reservation</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium">Date</label>
                <input type="date" className="input input-sm w-full" value={modalDate} onChange={(e) => setModalDate(e.target.value)} />
              </div>

              <div>
                <label className="block text-sm font-medium">Time</label>
                <input type="time" className="input input-sm w-full" value={modalTime} onChange={(e) => setModalTime(e.target.value)} />
              </div>

              <div>
                <label className="block text-sm font-medium">Duration (minutes)</label>
                <input type="number" min={15} step={15} className="input input-sm w-full" value={modalDuration} onChange={(e) => setModalDuration(Number(e.target.value || 0))} />
              </div>

              <div>
                <label className="block text-sm font-medium">Party size</label>
                <input type="number" min={1} step={1} className="input input-sm w-full" value={modalPartySize} onChange={(e) => setModalPartySize(Number(e.target.value || 1))} />
              </div>

              <div>
                <label className="block text-sm font-medium">Table</label>
                <select className="input input-sm w-full" value={modalTableId ?? ''} onChange={(e) => setModalTableId(e.target.value || null)}>
                  <option value="">Select table</option>
                  {tables.map((t) => <option key={t.id} value={t.id}>{t.code ?? t.id} {t.seats ? `— ${t.seats} seats` : ''}</option>)}
                </select>
              </div>
            </div>

            <div className="mt-4 flex items-center justify-end gap-2">
              <Button variant="ghost" onClick={() => { setShowEditModal(false); setEditingReservationId(null); }}>
                Cancel
              </Button>
              <Button onClick={saveEditedReservationFromModal} disabled={loading}>
                {loading ? 'Saving…' : 'Save changes'}
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* History modal (staff-only) - shows only past/finished reservations, no edit/cancel) */}
      {historyOpenForTable && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={closeHistoryModal} />
          <div className="relative z-10 w-full max-w-2xl bg-white dark:bg-slate-800 rounded-lg p-6 shadow-lg">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-medium">Table history — {openTableCode}</h3>
              <Button variant="ghost" size="sm" onClick={closeHistoryModal}>Close</Button>
            </div>

            {renderHistoryModalContent()}
          </div>
        </div>
      )}
    </div>
  );
};

export default Reservations;