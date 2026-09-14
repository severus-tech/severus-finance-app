import React, { useState } from 'react';
import { Dialog } from '@/components/ui/Dialog';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { api } from '@/lib/api';
import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

interface QuickAddDialogProps {
  isOpen: boolean;
  onClose: () => void;
}

export const QuickAddDialog: React.FC<QuickAddDialogProps> = ({ isOpen, onClose }) => {
  const [type, setType] = useState<'EXPENSE' | 'LEDGER' | 'TODO'>('EXPENSE');
  const [loading, setLoading] = useState(false);
  const queryClient = useQueryClient();

  // Expense fields
  const [expName, setExpName] = useState('');
  const [expAmount, setExpAmount] = useState('');
  const [expCategory, setExpCategory] = useState('FOOD');
  const [expType, setExpType] = useState<'VARIABLE' | 'FIXED_RECURRING'>('VARIABLE');
  const [expDate, setExpDate] = useState(new Date().toISOString().split('T')[0]);

  // Ledger fields
  const [personName, setPersonName] = useState('');
  const [ledgerAmount, setLedgerAmount] = useState('');
  const [direction, setDirection] = useState<'LENT' | 'BORROWED'>('LENT');

  // Todo fields
  const [todoTitle, setTodoTitle] = useState('');
  const [todoPriority, setTodoPriority] = useState<'LOW' | 'MEDIUM' | 'HIGH'>('MEDIUM');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (type === 'EXPENSE') {
        await api.post('/api/v1/expenses', {
          name: expName,
          category: expCategory,
          type: expType,
          amount: parseFloat(expAmount),
          expenseDate: expDate,
          paymentState: 'UNPAID',
        });
        toast.success('Expense recorded successfully');
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['expenses'] });
      } else if (type === 'LEDGER') {
        await api.post('/api/v1/ledger', {
          personName,
          direction,
          originalAmount: parseFloat(ledgerAmount),
          entryDate: new Date().toISOString().split('T')[0],
        });
        toast.success('Ledger entry recorded');
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['ledger'] });
      } else if (type === 'TODO') {
        await api.post('/api/v1/todos', {
          title: todoTitle,
          priority: todoPriority,
        });
        toast.success('To-do item created');
        queryClient.invalidateQueries({ queryKey: ['dashboard'] });
        queryClient.invalidateQueries({ queryKey: ['todos'] });
      }

      onClose();
    } catch (err: unknown) {
      toast.error('Failed to create entry');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog isOpen={isOpen} onClose={onClose} title="Quick Action">
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="flex gap-2 p-1 bg-slate-950 rounded-xl border border-slate-800 mb-4">
          <button
            type="button"
            onClick={() => setType('EXPENSE')}
            className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-colors ${
              type === 'EXPENSE' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Expense
          </button>
          <button
            type="button"
            onClick={() => setType('LEDGER')}
            className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-colors ${
              type === 'LEDGER' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            Lend / Borrow
          </button>
          <button
            type="button"
            onClick={() => setType('TODO')}
            className={`flex-1 py-2 text-xs font-semibold rounded-lg transition-colors ${
              type === 'TODO' ? 'bg-purple-600 text-white' : 'text-slate-400 hover:text-white'
            }`}
          >
            To-Do Item
          </button>
        </div>

        {type === 'EXPENSE' && (
          <>
            <Input label="Title" value={expName} onChange={(e) => setExpName(e.target.value)} required placeholder="e.g. Swiggy Order, Rent" />
            <Input label="Amount (₹)" type="number" step="0.01" value={expAmount} onChange={(e) => setExpAmount(e.target.value)} required placeholder="0.00" />
            <div className="grid grid-cols-2 gap-3">
              <Select
                label="Type"
                value={expType}
                onChange={(e) => setExpType(e.target.value as any)}
                options={[
                  { value: 'VARIABLE', label: 'Variable' },
                  { value: 'FIXED_RECURRING', label: 'Fixed Monthly' },
                ]}
              />
              <Select
                label="Category"
                value={expCategory}
                onChange={(e) => setExpCategory(e.target.value)}
                options={[
                  { value: 'FOOD', label: 'Food & Groceries' },
                  { value: 'RENT', label: 'Rent' },
                  { value: 'ELECTRICITY', label: 'Electricity' },
                  { value: 'INTERNET', label: 'Internet' },
                  { value: 'TRAVEL', label: 'Travel / Commute' },
                  { value: 'SHOPPING', label: 'Shopping' },
                  { value: 'ENTERTAINMENT', label: 'Entertainment' },
                  { value: 'OTHER', label: 'Other' },
                ]}
              />
            </div>
            <Input label="Date" type="date" value={expDate} onChange={(e) => setExpDate(e.target.value)} required />
          </>
        )}

        {type === 'LEDGER' && (
          <>
            <Input label="Person Name" value={personName} onChange={(e) => setPersonName(e.target.value)} required placeholder="e.g. Rahul Sharma" />
            <Select
              label="Direction"
              value={direction}
              onChange={(e) => setDirection(e.target.value as any)}
              options={[
                { value: 'LENT', label: 'Lent (I gave money - Receivable)' },
                { value: 'BORROWED', label: 'Borrowed (I took money - Payable)' },
              ]}
            />
            <Input label="Amount (₹)" type="number" step="0.01" value={ledgerAmount} onChange={(e) => setLedgerAmount(e.target.value)} required placeholder="0.00" />
          </>
        )}

        {type === 'TODO' && (
          <>
            <Input label="Task Title" value={todoTitle} onChange={(e) => setTodoTitle(e.target.value)} required placeholder="e.g. Pay Credit Card Bill" />
            <Select
              label="Priority"
              value={todoPriority}
              onChange={(e) => setTodoPriority(e.target.value as any)}
              options={[
                { value: 'LOW', label: 'Low Priority' },
                { value: 'MEDIUM', label: 'Medium Priority' },
                { value: 'HIGH', label: 'High Priority' },
              ]}
            />
          </>
        )}

        <div className="flex items-center justify-end gap-3 pt-4 border-t border-purple-900/20">
          <Button variant="ghost" type="button" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="primary" type="submit" isLoading={loading}>
            Save Entry
          </Button>
        </div>
      </form>
    </Dialog>
  );
};
