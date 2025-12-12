import { useEffect, useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Separator } from '@/components/ui/separator';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Checkbox } from '@/components/ui/checkbox';
import { ArrowLeft, Loader2, Plus, Edit, Trash } from 'lucide-react';
import { toast } from 'sonner';
import { useNavigate } from 'react-router-dom';
import Layout from '@/components/Layout';
import { useAuth } from '@/contexts/AuthContext';
import api from '@/services/api/client';

type AdminMenuItemForm = {
  id?: string;
  name: string;
  description?: string;
  price: string;
  category: string;
  calories: string;
  protein: string;
  fat: string;
  carbs: string;
  available: boolean;
  itemType: string;
};


const NO_CATEGORY = '__none';

const defaultForm = (): AdminMenuItemForm => ({
  name: '',
  description: '',
  price: '0.00',
  category: NO_CATEGORY,
  calories: '0',
  protein: '0',
  fat: '0',
  carbs: '0',
  available: true,
  itemType: 'KITCHEN',
});

const AdminMenu = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const [openEditor, setOpenEditor] = useState(false);
  const [form, setForm] = useState<AdminMenuItemForm>(defaultForm());
  const [saving, setSaving] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [categories, setCategories] = useState<any[]>([]);

  useEffect(() => {
    if (!user) return;
    fetchItems();
    fetchCategories();
  }, [user]);

  const fetchItems = async () => {
    setLoading(true);
    try {
      const res = await api.get('menu');
      setItems(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to fetch menu', err);
      toast.error('Failed to load menu items');
    } finally {
      setLoading(false);
    }
  };

  const fetchCategories = async () => {
    try {
      const res = await api.get('categories');
      setCategories(Array.isArray(res.data) ? res.data : []);
    } catch (err) {
      console.error('Failed to fetch categories', err);
      toast.error('Failed to load categories');
    }
  };

  const openCreate = () => {
    setForm(defaultForm());
    setOpenEditor(true);
  };

  const openEdit = (item: any) => {
    setForm({
      id: item.id,
      name: item.name ?? '',
      description: item.description ?? '',
      price: String(item.price ?? '0.00'),
      category: item.category ?? NO_CATEGORY,
      calories: String(item.calories ?? '0'),
      protein: String(item.macros?.protein ?? 0),
      fat: String(item.macros?.fat ?? 0),
      carbs: String(item.macros?.carbs ?? 0),
      available: !!item.available,
      itemType: item.itemType ?? 'KITCHEN',
    });
    setOpenEditor(true);
  };

  const handleChange = (k: keyof AdminMenuItemForm, v: any) => {
    setForm(prev => ({ ...prev, [k]: v }));
  };

  const validateForm = (): string | null => {
    if (!form.name.trim()) return 'Name is required';
    if (!form.category || form.category === NO_CATEGORY) return 'Category is required';
    if (isNaN(Number(form.price))) return 'Price must be a number';
    return null;
  };

  const handleSave = async () => {
    const err = validateForm();
    if (err) {
      toast.error(err);
      return;
    }

    setSaving(true);
    try {
      const payload: any = {
        name: form.name.trim(),
        description: form.description?.trim(),
        price: parseFloat(form.price),
        category: form.category === NO_CATEGORY ? null : form.category.trim(),
        calories: parseInt(form.calories || '0', 10) || 0,
        macros: {
          protein: parseInt(form.protein || '0', 10) || 0,
          fat: parseInt(form.fat || '0', 10) || 0,
          carbs: parseInt(form.carbs || '0', 10) || 0,
        },
        available: !!form.available,
        itemType: form.itemType,
      };

      if (form.id) {
        await api.put(`menu/${form.id}`, payload);
        toast.success('Menu item updated');
      } else {
        await api.post('menu', payload);
        toast.success('Menu item created');
      }

      setOpenEditor(false);
      await fetchItems();
      await fetchCategories();
    } catch (error: any) {
      console.error('Failed to save', error);
      toast.error(error?.response?.data?.message || 'Failed to save menu item');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Delete this menu item?')) return;
    setDeletingId(id);
    try {
      await api.delete(`menu/${id}`);
      toast.success('Deleted');
      await fetchItems();
    } catch (error: any) {
      console.error('Delete failed', error);
      toast.error(error?.response?.data?.message || 'Failed to delete');
    } finally {
      setDeletingId(null);
    }
  };

  if (!user || !user.role || !user.role.startsWith('ROLE_ADMIN')) {
    return (
      <div className="p-6">
        <Card>
          <CardHeader>
            <CardTitle>Admin menu</CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-muted-foreground">You are not authorized to view this page.</p>
            <div className="mt-4">
              <Button onClick={() => navigate('/menu')}>Back to public menu</Button>
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-bold">Menu Administration</h2>
        <div className="flex items-center gap-2">
          <Button onClick={openCreate}><Plus className="h-4 w-4 mr-2" /> New</Button>
          <Button variant="ghost" onClick={() => navigate('/menu')}>View public</Button>
        </div>
      </div>

      {loading ? (
        <div className="flex items-center justify-center min-h-[200px]">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {items.map(item => (
            <Card key={item.id} className="overflow-hidden">
              <CardHeader>
                <div className="flex items-start justify-between">
                  <div>
                    <CardTitle>{item.name}</CardTitle>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="mb-2 text-sm text-muted-foreground">{item.description}</div>
                <div className="flex justify-between items-center mb-2">
                  <div className="text-lg font-bold">${Number(item.price).toFixed(2)}</div>
                  <div className="text-sm">
                    <strong>{item.category}</strong>
                  </div>
                </div>
                <div className="text-sm text-muted-foreground mb-2">
                  Calories: {item.calories} • Macros P:{item.macros?.protein ?? 0} F:{item.macros?.fat ?? 0} C:{item.macros?.carbs ?? 0}
                </div>
                <div className="flex gap-2">
                  <Button size="sm" variant="outline" onClick={() => openEdit(item)}><Edit className="h-4 w-4" /> Edit</Button>
                  <Button size="sm" variant="destructive" onClick={() => handleDelete(item.id)} disabled={deletingId === item.id}>
                    <Trash className="h-4 w-4" /> {deletingId === item.id ? 'Deleting…' : 'Delete'}
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <Dialog open={openEditor} onOpenChange={setOpenEditor}>
        <DialogContent className="max-w-2xl" aria-describedby="menu-item-dialog-desc">
          <DialogHeader>
            <DialogTitle>{form.id ? 'Edit Menu Item' : 'Create Menu Item'}</DialogTitle>
            <DialogDescription id="menu-item-dialog-desc">
              Fill in the menu item details below (name, category, price, macros). Categories come from the server.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-3 py-2">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
              <div>
                <label className="text-sm font-medium">Name</label>
                <Input value={form.name} onChange={(e) => handleChange('name', e.target.value)} />
              </div>
              <div>
                <label className="text-sm font-medium">Category</label>
                <Select value={form.category} onValueChange={(v) => handleChange('category', v)}>
                  <SelectTrigger className="w-full"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value={NO_CATEGORY} disabled>-- choose category --</SelectItem>
                    {categories.map((c: any) => (
                      <SelectItem key={c.id} value={c.name}>{c.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div>
                <label className="text-sm font-medium">Price</label>
                <Input value={form.price} onChange={(e) => handleChange('price', e.target.value)} />
              </div>
              <div>
                <label className="text-sm font-medium">Calories</label>
                <Input value={form.calories} onChange={(e) => handleChange('calories', e.target.value)} />
              </div>
            </div>

            <div>
              <label className="text-sm font-medium">Description</label>
              <Textarea value={form.description} onChange={(e) => handleChange('description', e.target.value)} />
            </div>

            <div className="grid grid-cols-3 gap-3">
              <div>
                <label className="text-sm font-medium">Protein</label>
                <Input value={form.protein} onChange={(e) => handleChange('protein', e.target.value)} />
              </div>
              <div>
                <label className="text-sm font-medium">Fat</label>
                <Input value={form.fat} onChange={(e) => handleChange('fat', e.target.value)} />
              </div>
              <div>
                <label className="text-sm font-medium">Carbs</label>
                <Input value={form.carbs} onChange={(e) => handleChange('carbs', e.target.value)} />
              </div>
            </div>

            <div className="flex items-center gap-4">
              <Checkbox checked={form.available} onCheckedChange={(v) => handleChange('available', !!v)} />
              <span className="text-sm">Available</span>

              <div className="ml-auto">
                <label className="text-sm font-medium mr-2">Type</label>
                <Select value={form.itemType} onValueChange={(v) => handleChange('itemType', v)}>
                  <SelectTrigger className="w-[140px]"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="KITCHEN">KITCHEN</SelectItem>
                    <SelectItem value="BAR">BAR</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setOpenEditor(false)} disabled={saving}>Cancel</Button>
            <Button onClick={handleSave} disabled={saving}>{saving ? 'Saving…' : 'Save'}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default AdminMenu;