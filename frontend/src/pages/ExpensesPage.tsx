import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Expense } from '@/types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { formatINR, formatDate } from '@/lib/utils';
import { Receipt, Plus, Trash2, CheckCircle2, Circle, Calendar, Filter } from 'lucide-react';
import { toast } from 'sonner';

export const ExpensesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [activeTab, setActiveTab] = useState<'ALL' | 'VARIABLE' | 'FIXED_RECURRING'>('ALL');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState<'VARIABLE' | 'FIXED_RECURRING'>('VARIABLE');
  const [category, setCategory] = useState('FOOD');
  const [expenseDate, setExpenseDate] = useState(new Date().toISOString().split('T')[0]);
  const [monthlyDueDay, setMonthlyDueDay] = useState('1');
  const [notes, setNotes] = useState('');

  const { data: expenses, isLoading } = useQuery<Expense[]>({
    queryKey: ['expenses', activeTab],
    queryFn: async () => {
      const param = activeTab !== 'ALL' ? `?type=${activeTab}` : '';
      const res = await api.get(`/api/v1/expenses${param}`);
      return res.data;
    },
  });

  const handleCreateExpense = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await api.post('/api/v1/expenses', {
        name,
        category,
        type,
        amount: parseFloat(amount),
        expenseDate,
        monthlyDueDay: type === 'FIXED_RECURRING' ? parseInt(monthlyDueDay) : null,
        notes,
        paymentState: 'UNPAID',
      });

      toast.success('Expense created!');
      setIsAddOpen(false);
      setName('');
      setAmount('');
      setNotes('');
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to create expense');
    } finally {
      setLoading(false);
    }
  };

  const handleToggleStatus = async (id: string, currentState: string) => {
    const newState = currentState === 'PAID' ? 'UNPAID' : 'PAID';
    try {
      await api.patch(`/api/v1/expenses/${id}/status?state=${newState}`);
      toast.success(`Expense marked as ${newState.toLowerCase()}`);
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to update status');
    }
  };

  const handleDeleteExpense = async (id: string) => {
    try {
      await api.delete(`/api/v1/expenses/${id}`);
      toast.success('Expense deleted');
      queryClient.invalidateQueries({ queryKey: ['expenses'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to delete expense');
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
            Expense Management
          </h1>
          <p className="text-xs text-slate-400">
            Fixed monthly recurring bills and day-to-day variable expenses
          </p>
        </div>

        <Button variant="primary" onClick={() => setIsAddOpen(true)}>
          <Plus className="w-4 h-4" />
          Add Expense
        </Button>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-2 border-b border-purple-900/20 pb-3">
        {[
          { id: 'ALL', label: 'All Expenses' },
          { id: 'VARIABLE', label: 'Variable Expenses' },
          { id: 'FIXED_RECURRING', label: 'Fixed Monthly Bills' },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`px-4 py-2 text-xs font-semibold rounded-xl transition-all ${
              activeTab === tab.id
                ? 'bg-purple-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Expense List Table */}
      {isLoading ? (
        <Skeleton className="h-80 w-full" />
      ) : expenses && expenses.length > 0 ? (
        <Card>
          <CardContent className="p-0 overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-purple-900/20 uppercase tracking-wider">
                <tr>
                  <th className="px-6 py-4">Title</th>
                  <th className="px-6 py-4">Type</th>
                  <th className="px-6 py-4">Category</th>
                  <th className="px-6 py-4">Amount</th>
                  <th className="px-6 py-4">Date / Due Day</th>
                  <th className="px-6 py-4">Status</th>
                  <th className="px-6 py-4 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-purple-900/10">
                {expenses.map((exp) => (
                  <tr key={exp.id} className="hover:bg-slate-900/50 transition-colors">
                    <td className="px-6 py-4">
                      <p className="font-bold text-slate-100">{exp.name}</p>
                      {exp.notes && <p className="text-[11px] text-slate-400">{exp.notes}</p>}
                    </td>
                    <td className="px-6 py-4">
                      <Badge variant={exp.type === 'FIXED_RECURRING' ? 'purple' : 'slate'}>
                        {exp.type === 'FIXED_RECURRING' ? 'Fixed' : 'Variable'}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 font-medium text-slate-300">{exp.category.replace('_', ' ')}</td>
                    <td className="px-6 py-4 font-extrabold text-purple-300">{formatINR(exp.amount)}</td>
                    <td className="px-6 py-4 text-slate-300">
                      {exp.type === 'FIXED_RECURRING'
                        ? `Due ${exp.monthlyDueDay}th of month`
                        : formatDate(exp.expenseDate)}
                    </td>
                    <td className="px-6 py-4">
                      <button
                        onClick={() => handleToggleStatus(exp.id, exp.paymentState)}
                        className="flex items-center gap-1.5 focus:outline-none"
                      >
                        {exp.paymentState === 'PAID' ? (
                          <Badge variant="emerald">
                            <CheckCircle2 className="w-3 h-3" /> Paid
                          </Badge>
                        ) : (
                          <Badge variant="amber">
                            <Circle className="w-3 h-3" /> Unpaid
                          </Badge>
                        )}
                      </button>
                    </td>
                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDeleteExpense(exp.id)}
                        className="p-1.5 text-slate-400 hover:text-rose-400 hover:bg-rose-950/40 rounded-lg transition-colors"
                        title="Delete expense"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      ) : (
        <EmptyState
          icon={Receipt}
          title="No expenses recorded"
          description="Log variable expenses like food and commute, or set up fixed recurring bills."
          actionText="Add Expense"
          onAction={() => setIsAddOpen(true)}
        />
      )}

      {/* Add Expense Dialog */}
      <Dialog isOpen={isAddOpen} onClose={() => setIsAddOpen(false)} title="Create Expense">
        <form onSubmit={handleCreateExpense} className="space-y-4">
          <Input label="Expense Title" value={name} onChange={(e) => setName(e.target.value)} required placeholder="e.g. Electricity Bill, Swiggy" />
          <Input label="Amount (₹)" type="number" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required placeholder="0.00" />

          <div className="grid grid-cols-2 gap-4">
            <Select
              label="Type"
              value={type}
              onChange={(e) => setType(e.target.value as any)}
              options={[
                { value: 'VARIABLE', label: 'Variable Expense' },
                { value: 'FIXED_RECURRING', label: 'Fixed Monthly Bill' },
              ]}
            />
            <Select
              label="Category"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              options={[
                { value: 'RENT', label: 'Rent' },
                { value: 'ELECTRICITY', label: 'Electricity' },
                { value: 'INTERNET', label: 'Internet' },
                { value: 'MOBILE_BILL', label: 'Mobile Bill' },
                { value: 'FOOD', label: 'Food & Groceries' },
                { value: 'TRAVEL', label: 'Travel / Commute' },
                { value: 'SHOPPING', label: 'Shopping' },
                { value: 'MEDICAL', label: 'Medical' },
                { value: 'ENTERTAINMENT', label: 'Entertainment' },
                { value: 'OTHER', label: 'Other' },
              ]}
            />
          </div>

          {type === 'FIXED_RECURRING' ? (
            <Input label="Monthly Due Day (1-31)" type="number" min="1" max="31" value={monthlyDueDay} onChange={(e) => setMonthlyDueDay(e.target.value)} required />
          ) : (
            <Input label="Expense Date" type="date" value={expenseDate} onChange={(e) => setExpenseDate(e.target.value)} required />
          )}

          <Input label="Notes (Optional)" value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Add optional details..." />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={loading}>
              Save Expense
            </Button>
          </div>
        </form>
      </Dialog>
    </div>
  );
};
