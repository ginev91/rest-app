import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Separator } from '@/components/ui/separator';
import { ArrowLeft, Loader2, Edit, Trash, Users } from 'lucide-react';
import { toast } from 'sonner';
import { getTable, updateTable, deleteTable, listTables } from '@/services/api/tables';

const TableDetails = () => {
  const { tableId } = useParams<{ tableId: string }>();
  const navigate = useNavigate();

  const [table, setTable] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // form state
  const [code, setCode] = useState('');
  const [seats, setSeats] = useState<number | ''>('');
  const [tableNumber, setTableNumber] = useState<number | ''>('');
  const [pinCode, setPinCode] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!tableId) {
      setLoading(false);
      return;
    }
    fetchTableMetadata(tableId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tableId]);

  const fetchTableMetadata = async (idOrCode: string) => {
    setLoading(true);
    try {
      const t = await getTable(idOrCode).catch(() => null);
      if (t && t.id) {
        setTable(t);
        return;
      }

      const all = await listTables();
      if (Array.isArray(all)) {
        const found = all.find((x: any) => {
          if (!x) return false;
          if (String(x.id) === idOrCode) return true;
          if (String(x.code).toUpperCase() === idOrCode.toUpperCase()) return true;
          if (x.tableNumber != null && String(x.tableNumber) === idOrCode) return true;
          return false;
        });
        if (found) {
          setTable(found);
          return;
        }
      }

      toast.error('Table not found');
    } catch (err: any) {
      console.error('Failed to fetch table metadata', err);
      toast.error('Failed to load table');
    } finally {
      setLoading(false);
    }
  };

  const startEdit = () => {
    if (!table) return;
    setFormError(null);
    setCode(table.code ?? '');
    setSeats(table.seats ?? '');
    setTableNumber(table.tableNumber ?? '');
    setPinCode(table.pinCode ?? '');
    setEditing(true);
  };

  const cancelEdit = () => {
    setEditing(false);
    setFormError(null);
  };

  const handleSave = async () => {
    setFormError(null);

    const newCode = (code || '').trim().toUpperCase();
    const seatsVal = typeof seats === 'number' ? seats : parseInt(String(seats || ''), 10);
    const tableNumberVal = tableNumber === '' ? null : Number(tableNumber);

    if (!newCode) {
      setFormError('Code is required');
      return;
    }
    if (!/^T\d+$/i.test(newCode)) {
      setFormError('Code must start with "T" followed by digits, e.g. T6');
      return;
    }
    if (!seatsVal || seatsVal <= 0) {
      setFormError('Seats must be a positive number');
      return;
    }

    setSaving(true);
    try {
      // check duplicate code
      const all = await listTables();
      if (Array.isArray(all)) {
        const dup = all.find((t: any) => t.code && t.code.toUpperCase() === newCode && String(t.id) !== String(table.id));
        if (dup) {
          setFormError('A table with that code already exists');
          setSaving(false);
          return;
        }
      }

      const payload: any = {
        code: newCode,
        seats: seatsVal,
        tableNumber: tableNumberVal,
      };
      if (pinCode !== undefined) payload.pinCode = pinCode || null;

      const updated = await updateTable(table.id, payload);
      setTable(updated);
      setEditing(false);
      toast.success('Table updated');
    } catch (err: any) {
      console.error('Failed to update table', err);
      setFormError(err?.response?.data?.message || 'Failed to update table');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!table) return;
    if (!window.confirm('Delete this table? This cannot be undone.')) return;

    setDeleting(true);
    try {
      await deleteTable(table.id);
      toast.success('Table deleted');
      navigate('/tables');
    } catch (err: any) {
      console.error('Failed to delete table', err);
      toast.error(err?.response?.data?.message || 'Failed to delete table');
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[300px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!table) {
    return (
      <Card>
        <CardContent className="py-12 flex flex-col items-center">
          <Users className="h-12 w-12 text-muted-foreground mb-4" />
          <p className="text-lg font-semibold">Table not found</p>
          <p className="text-muted-foreground">It may have been removed or the id/code is invalid.</p>
          <div className="mt-4">
            <Button onClick={() => navigate('/tables')}>Back to list</Button>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => navigate('/tables')}>
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h2 className="text-3xl font-bold">Table {table.code}</h2>
            <p className="text-muted-foreground">Manage table metadata</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {!editing ? (
            <>
              <Button variant="outline" size="sm" onClick={startEdit}>
                <Edit className="h-4 w-4" /> Edit
              </Button>
              <Button variant="destructive" size="sm" onClick={handleDelete} disabled={deleting}>
                <Trash className="h-4 w-4" /> Delete
              </Button>
            </>
          ) : (
            <>
              <Button variant="ghost" size="sm" onClick={cancelEdit} disabled={saving}>
                Cancel
              </Button>
              <Button size="sm" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving…' : 'Save'}
              </Button>
            </>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Code</CardTitle>
          </CardHeader>
          <CardContent>
            {!editing ? (
              <>
                <div className="text-lg font-medium">{table.code}</div>
                <CardDescription className="mt-2">Unique table code used in routes and UI</CardDescription>
              </>
            ) : (
              <div className="space-y-2">
                <Input value={code} onChange={(e) => setCode(e.target.value.toUpperCase())} />
                <CardDescription className="mt-1">Must start with "T" followed by digits (e.g. T6)</CardDescription>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Seats</CardTitle>
          </CardHeader>
          <CardContent>
            {!editing ? (
              <>
                <div className="text-lg font-medium">{table.seats ?? '-'}</div>
                <CardDescription className="mt-2">Number of seats at this table</CardDescription>
              </>
            ) : (
              <div className="space-y-2">
                <Input
                  type="number"
                  min={1}
                  value={seats}
                  onChange={(e) => setSeats(e.target.value === '' ? '' : Number(e.target.value))}
                />
                <CardDescription className="mt-1">Enter seats (positive integer)</CardDescription>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Occupancy & Status</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-1">
              <div>Current occupancy: <strong>{table.currentOccupancy ?? 0}</strong></div>
              <div>Status: <strong>{table.status ?? 'UNKNOWN'}</strong></div>
              <div>Occupied until: <strong>{table.occupiedUntil ? new Date(table.occupiedUntil).toLocaleString() : '—'}</strong></div>
            </div>
          </CardContent>
        </Card>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Table number</CardTitle>
          </CardHeader>
          <CardContent>
            {!editing ? (
              <>
                <div className="text-lg font-medium">{table.tableNumber ?? '—'}</div>
                <CardDescription className="mt-2">Numeric table number (optional)</CardDescription>
              </>
            ) : (
              <div className="space-y-2">
                <Input
                  type="number"
                  value={tableNumber}
                  onChange={(e) => setTableNumber(e.target.value === '' ? '' : Number(e.target.value))}
                />
                <CardDescription className="mt-1">Optional numeric table number</CardDescription>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Pin code</CardTitle>
          </CardHeader>
          <CardContent>
            {!editing ? (
              <>
                <div className="text-lg font-medium">{table.pinCode ?? '—'}</div>
                <CardDescription className="mt-2">Pin code used by the table device (if any)</CardDescription>
              </>
            ) : (
              <div className="space-y-2">
                <Input value={pinCode} onChange={(e) => setPinCode(e.target.value)} />
                <CardDescription className="mt-1">Optional pin code</CardDescription>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {formError && <div className="text-sm text-red-600">{formError}</div>}

      <Separator />

      <div className="flex items-center gap-2">
        <Button onClick={() => navigate('/tables')}>Back to tables</Button>
      </div>
    </div>
  );
};

export default TableDetails;