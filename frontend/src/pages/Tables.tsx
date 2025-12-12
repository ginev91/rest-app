import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Layout from '@/components/Layout';
import { Users, Loader2, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import { listTables, createTable, TableDto } from '@/services/api/tables';
import api from '@/services/api/client';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';

const statusConfig = {
  AVAILABLE: {
    label: 'Available',
    bgColor: 'bg-success/10 hover:bg-success/20 border-success/30',
    badgeColor: 'bg-success text-success-foreground'
  },
  OCCUPIED: {
    label: 'Occupied',
    bgColor: 'bg-destructive/10 hover:bg-destructive/20 border-destructive/30',
    badgeColor: 'bg-destructive text-destructive-foreground'
  },
  RESERVED: {
    label: 'Reserved',
    bgColor: 'bg-warning/10 hover:bg-warning/20 border-warning/30',
    badgeColor: 'bg-warning text-warning-foreground'
  },
  OUT_OF_SERVICE: {
    label: 'Out of Service',
    bgColor: 'bg-muted/10 hover:bg-muted/20 border-muted/30',
    badgeColor: 'bg-muted text-muted-foreground'
  }
};

const Tables = () => {
  const [tables, setTables] = useState<TableDto[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();

  // Create modal state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newCode, setNewCode] = useState('');
  const [newSeats, setNewSeats] = useState<number | ''>(4);
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    loadTables();
  }, []);

  async function loadTables() {
    setLoading(true);
    try {
      const res = await listTables();
      console.log('Tables loaded:', res);
      setTables(res || []);
    } catch (err: any) {
      console.error('Failed to load tables:', err);
      toast.error('Failed to load tables');
    } finally {
      setLoading(false);
    }
  }

  const openCreateModal = () => {
    setFormError(null);
    setNewCode('');
    setNewSeats(4);
    setShowCreateModal(true);
  };

  const validateCode = (code: string) => {
    if (!code) return 'Code is required';
    // must start with 'T' followed by one or more digits (e.g. T1, T10)
    if (!/^T\d+$/i.test(code)) return 'Code must start with "T" followed by digits (e.g. T6)';
    return null;
  };

  const isCodeDuplicate = (code: string) => {
    return tables.some(t => t.code?.toUpperCase() === code.toUpperCase());
  };

  const handleCreateTable = async () => {
    setFormError(null);
    const code = (newCode || '').trim();
    const seats = typeof newSeats === 'number' ? newSeats : parseInt(String(newSeats || '0'), 10);

    const codeErr = validateCode(code);
    if (codeErr) {
      setFormError(codeErr);
      return;
    }

    if (isCodeDuplicate(code)) {
      setFormError('A table with that code already exists');
      return;
    }

    if (!seats || seats <= 0) {
      setFormError('Seats must be a positive number');
      return;
    }

    setCreating(true);
    try {
      const payload: Partial<TableDto> = {
        code,
        seats,
        tableNumber: undefined,
        pinCode: Math.floor(Math.random() * 10000).toString().padStart(4, '0')
      };

      const created = await createTable(payload);
      toast.success('Table created');
      setShowCreateModal(false);
      await loadTables();
      navigate(`/tables/${created.code}`);
    } catch (err: any) {
      console.error('Failed to create table:', err);
      setFormError(err?.response?.data?.message || 'Failed to create table');
    } finally {
      setCreating(false);
    }
  };

  const availableCount = tables.filter(t => t.status === 'AVAILABLE').length;
  const occupiedCount = tables.filter(t => t.status === 'OCCUPIED').length;
  const reservedCount = tables.filter(t => t.status === 'RESERVED').length;

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold">Table Management</h2>
          <p className="text-muted-foreground">Monitor and manage restaurant tables</p>
        </div>
        <div>
          {/* TODO: Gate this button on client-side admin check if you have one; backend will enforce role. */}
          <Button onClick={openCreateModal} className="inline-flex items-center gap-2">
            <Plus className="h-4 w-4" /> Add table
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="text-4xl text-success">{availableCount}</CardTitle>
            <p className="text-sm text-muted-foreground">Available Tables</p>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-4xl text-destructive">{occupiedCount}</CardTitle>
            <p className="text-sm text-muted-foreground">Occupied Tables</p>
          </CardHeader>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-4xl text-warning">{reservedCount}</CardTitle>
            <p className="text-sm text-muted-foreground">Reserved Tables</p>
          </CardHeader>
        </Card>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-4">
        {tables.map(table => {
          const config = statusConfig[table.status];
          return (
            <Card
              key={table.id}
              className={cn(
                'cursor-pointer transition-all border-2',
                config.bgColor
              )}
              onClick={() => navigate(`/tables/${table.code}`)}
            >
              <CardContent className="p-6 text-center space-y-3">
                <div className="text-3xl font-bold">
                  {table.code}
                </div>
                {/* <div className="flex items-center justify-center gap-1 text-sm text-muted-foreground">
                  <Users className="h-4 w-4" />
                  <span>{table.currentOccupancy}/{table.seats}</span>
                </div> */}
                <Badge className={cn('text-xs', config.badgeColor)}>
                  {config.label}
                </Badge>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {tables.length === 0 && (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Users className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-semibold mb-2">No tables found</p>
            <p className="text-muted-foreground">Tables will appear here once they are added</p>
          </CardContent>
        </Card>
      )}

      {/* Create table modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/40" onClick={() => { if (!creating) setShowCreateModal(false); }} />
          <div className="relative w-full max-w-md mx-4 bg-white dark:bg-slate-900 rounded-lg shadow-lg overflow-hidden">
            <div className="p-4 border-b dark:border-slate-700 flex items-center justify-between">
              <h3 className="text-lg font-semibold">Create table</h3>
              <button
                className="text-muted-foreground"
                onClick={() => { if (!creating) setShowCreateModal(false); }}
                aria-label="Close"
              >
                ✕
              </button>
            </div>

            <div className="p-4 space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">Table code</label>
                <Input
                  placeholder="T6"
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value.toUpperCase())}
                  disabled={creating}
                />
                <p className="text-xs text-muted-foreground mt-1">Must start with "T" followed by digits, e.g. T6</p>
              </div>

              <div>
                <label className="block text-sm font-medium mb-1">Seats</label>
                <Input
                  type="number"
                  min={1}
                  value={newSeats}
                  onChange={(e) => setNewSeats(e.target.value === '' ? '' : Number(e.target.value))}
                  disabled={creating}
                />
              </div>

              {formError && <div className="text-sm text-red-600">{formError}</div>}

              <div className="flex items-center justify-end gap-2 pt-2">
                <Button variant="ghost" onClick={() => { if (!creating) setShowCreateModal(false); }}>
                  Cancel
                </Button>
                <Button onClick={handleCreateTable} disabled={creating}>
                  {creating ? 'Creating…' : 'Create table'}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Tables;