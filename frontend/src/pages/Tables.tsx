import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Layout from '@/components/Layout';
import { Users, Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { toast } from 'sonner';
import api from '@/services/api/client';

interface Table {
  id: string;
  code: string;
  seats: number;
  currentOccupancy: number;
  status: 'AVAILABLE' | 'OCCUPIED' | 'RESERVED' | 'OUT_OF_SERVICE';
}

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
  const [tables, setTables] = useState<Table[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadTables();
  }, []);

  async function loadTables() {
    setLoading(true);
    try {
      const res = await api.get('/api/tables');
      console.log('Tables loaded:', res.data);
      setTables(res.data);
    } catch (err: any) {
      console.error('Failed to load tables:', err);
      toast.error('Failed to load tables');
    } finally {
      setLoading(false);
    }
  }

  const availableCount = tables.filter(t => t.status === 'AVAILABLE').length;
  const occupiedCount = tables.filter(t => t.status === 'OCCUPIED').length;
  const reservedCount = tables.filter(t => t.status === 'RESERVED').length;

  if (loading) {
    return (
      <Layout>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </Layout>
    );
  }

  return (
    <Layout>
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold">Table Management</h2>
          <p className="text-muted-foreground">Monitor and manage restaurant tables</p>
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
                onClick={() => navigate(`/tables/${table.id}`)}
              >
                <CardContent className="p-6 text-center space-y-3">
                  <div className="text-3xl font-bold">
                    {table.code}
                  </div>
                  <div className="flex items-center justify-center gap-1 text-sm text-muted-foreground">
                    <Users className="h-4 w-4" />
                    <span>{table.currentOccupancy}/{table.seats}</span>
                  </div>
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
      </div>
    </Layout>
  );
};

export default Tables;