import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import Layout from '@/components/Layout';
import { Table } from '@/types/order';
import { Users } from 'lucide-react';
import { cn } from '@/lib/utils';

// Mock tables data
const mockTables: Table[] = [
  { id: '1', number: 1, capacity: 2, status: 'occupied', currentOrderId: '1' },
  { id: '2', number: 2, capacity: 4, status: 'available' },
  { id: '3', number: 3, capacity: 4, status: 'occupied', currentOrderId: '2' },
  { id: '4', number: 4, capacity: 6, status: 'reserved' },
  { id: '5', number: 5, capacity: 2, status: 'available' },
  { id: '6', number: 6, capacity: 8, status: 'available' },
  { id: '7', number: 7, capacity: 4, status: 'occupied' },
  { id: '8', number: 8, capacity: 4, status: 'available' },
];

const statusConfig = {
  available: { label: 'Available', bgColor: 'bg-success/10 hover:bg-success/20 border-success/30', badgeColor: 'bg-success text-success-foreground' },
  occupied: { label: 'Occupied', bgColor: 'bg-destructive/10 hover:bg-destructive/20 border-destructive/30', badgeColor: 'bg-destructive text-destructive-foreground' },
  reserved: { label: 'Reserved', bgColor: 'bg-warning/10 hover:bg-warning/20 border-warning/30', badgeColor: 'bg-warning text-warning-foreground' },
};

const Tables = () => {
  const [tables] = useState<Table[]>(mockTables);

  const availableCount = tables.filter(t => t.status === 'available').length;
  const occupiedCount = tables.filter(t => t.status === 'occupied').length;
  const reservedCount = tables.filter(t => t.status === 'reserved').length;

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
              >
                <CardContent className="p-6 text-center space-y-3">
                  <div className="text-3xl font-bold">
                    {table.number}
                  </div>
                  <div className="flex items-center justify-center gap-1 text-sm text-muted-foreground">
                    <Users className="h-4 w-4" />
                    {table.capacity}
                  </div>
                  <Badge className={cn('text-xs', config.badgeColor)}>
                    {config.label}
                  </Badge>
                </CardContent>
              </Card>
            );
          })}
        </div>
      </div>
    </Layout>
  );
};

export default Tables;