import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { LedgerEntry, LedgerSummary } from '@/types';
import { StatCard } from '@/components/common/StatCard';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { formatINR, formatDate } from '@/lib/utils';
import { Users, Plus, ArrowUpRight, ArrowDownLeft, DollarSign, CheckCircle2, RotateCcw, Trash2 } from 'lucide-react';
import { toast } from 'sonner';

export const LedgerPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [filterDirection, setFilterDirection] = useState<string>('ALL');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isPayOpen, setIsPayOpen] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState<LedgerEntry | null>(null);

  // Add form state
  const [personName, setPersonName] = useState('');
  const [direction, setDirection] = useState<'LENT' | 'BORROWED'>('LENT');
  const [origAmount, setOrigAmount] = useState('');
  const [entryDate, setEntryDate] = useState(new Date().toISOString().split('T')[0]);
  const [dueDate, setDueDate] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(false);

  // Payment form state
  const [payAmount, setPayAmount] = useState('');
  const [payDate, setPayDate] = useState(new Date().toISOString().split('T')[0]);

  const { data: entries, isLoading } = useQuery<LedgerEntry[]>({
    queryKey: ['ledger', filterDirection],
    queryFn: async () => {
      const param = filterDirection !== 'ALL' ? `?direction=${filterDirection}` : '';
      const res = await api.get(`/api/v1/ledger${param}`);
      return res.data;
    },
  });

  const { data: summary } = useQuery<LedgerSummary>({
    queryKey: ['ledger-summary'],
    queryFn: async () => {
      const res = await api.get('/api/v1/ledger/summary');
      return res.data;
    },
  });

  const handleCreateEntry = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await api.post('/api/v1/ledger', {
        personName,
        direction,
        originalAmount: parseFloat(origAmount),
        entryDate,
        dueDate: dueDate || null,
        description,
      });

      toast.success('Ledger entry added!');
      setIsAddOpen(false);
      setPersonName('');
      setOrigAmount('');
      setDescription('');
      queryClient.invalidateQueries({ queryKey: ['ledger'] });
      queryClient.invalidateQueries({ queryKey: ['ledger-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to create ledger entry');
    } finally {
      setLoading(false);
    }
  };

  const handleOpenPayment = (entry: LedgerEntry) => {
    setSelectedEntry(entry);
    setPayAmount(entry.remainingBalance.toString());
    setIsPayOpen(true);
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEntry) return;
    setLoading(true);

    try {
      await api.post(`/api/v1/ledger/${selectedEntry.id}/payments`, {
        amount: parseFloat(payAmount),
        paymentDate: payDate,
        allowOverpayment: true,
      });

      toast.success('Repayment recorded!');
      setIsPayOpen(false);
      queryClient.invalidateQueries({ queryKey: ['ledger'] });
      queryClient.invalidateQueries({ queryKey: ['ledger-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to record payment');
    } finally {
      setLoading(false);
    }
  };

  const handleReopen = async (id: string) => {
    try {
      await api.post(`/api/v1/ledger/${id}/reopen`);
      toast.success('Ledger entry reopened');
      queryClient.invalidateQueries({ queryKey: ['ledger'] });
      queryClient.invalidateQueries({ queryKey: ['ledger-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to reopen entry');
    }
  };

  const handleDelete = async (id: string) => {
    try {
      await api.delete(`/api/v1/ledger/${id}`);
      toast.success('Ledger entry deleted');
      queryClient.invalidateQueries({ queryKey: ['ledger'] });
      queryClient.invalidateQueries({ queryKey: ['ledger-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to delete entry');
    }
  };

  return (
    <div className="space-y-8 animate-in fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
            Lend & Borrow Ledger
          </h1>
          <p className="text-xs text-slate-400">
            Track money lent to others and borrowed funds with repayment histories
          </p>
        </div>

        <Button variant="primary" onClick={() => setIsAddOpen(true)}>
          <Plus className="w-4 h-4" />
          Add Ledger Entry
        </Button>
      </div>

      {/* Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <StatCard
            title="Total Money Lent (Receivable)"
            amount={summary.totalLentReceivable}
            subtitle={`${summary.openLentCount} Open Accounts`}
            icon={ArrowUpRight}
            color="emerald"
          />
          <StatCard
            title="Total Money Borrowed (Payable)"
            amount={summary.totalBorrowedPayable}
            subtitle={`${summary.openBorrowedCount} Open Accounts`}
            icon={ArrowDownLeft}
            color="rose"
          />
        </div>
      )}

      {/* Filters */}
      <div className="flex items-center gap-2 border-b border-purple-900/20 pb-3">
        {['ALL', 'LENT', 'BORROWED'].map((dir) => (
          <button
            key={dir}
            onClick={() => setFilterDirection(dir)}
            className={`px-4 py-2 text-xs font-semibold rounded-xl transition-all ${
              filterDirection === dir
                ? 'bg-purple-600 text-white shadow-md'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
            }`}
          >
            {dir === 'ALL' ? 'All Entries' : dir === 'LENT' ? 'Money Lent (Receivable)' : 'Money Borrowed (Payable)'}
          </button>
        ))}
      </div>

      {/* Ledger Cards / List */}
      {isLoading ? (
        <Skeleton className="h-80 w-full" />
      ) : entries && entries.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {entries.map((entry) => (
            <Card key={entry.id} className="p-6 relative">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <Badge variant={entry.direction === 'LENT' ? 'emerald' : 'rose'}>
                      {entry.direction}
                    </Badge>
                    <Badge variant={entry.status === 'SETTLED' ? 'slate' : 'purple'}>
                      {entry.status}
                    </Badge>
                  </div>
                  <h3 className="text-xl font-bold text-slate-100 font-heading">{entry.personName}</h3>
                  {entry.description && <p className="text-xs text-slate-400 mt-0.5">{entry.description}</p>}
                </div>
                <div className="text-right">
                  <span className="text-xs text-slate-400 block">Remaining</span>
                  <span className="text-xl font-extrabold text-purple-300">
                    {formatINR(entry.remainingBalance)}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 py-3 border-y border-purple-900/20 my-3 text-xs">
                <div>
                  <span className="text-slate-400 block">Original Amount</span>
                  <span className="font-semibold text-slate-200">{formatINR(entry.originalAmount)}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Entry Date</span>
                  <span className="font-semibold text-slate-200">{formatDate(entry.entryDate)}</span>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2">
                <span className="text-xs text-slate-400">
                  {entry.dueDate ? `Due: ${formatDate(entry.dueDate)}` : 'No due date set'}
                </span>
                <div className="flex items-center gap-2">
                  {entry.status !== 'SETTLED' ? (
                    <Button variant="outline" size="sm" onClick={() => handleOpenPayment(entry)}>
                      <DollarSign className="w-3.5 h-3.5 mr-1" /> Record Repayment
                    </Button>
                  ) : (
                    <Button variant="ghost" size="sm" onClick={() => handleReopen(entry.id)}>
                      <RotateCcw className="w-3.5 h-3.5 mr-1" /> Reopen
                    </Button>
                  )}
                  <button
                    onClick={() => handleDelete(entry.id)}
                    className="p-2 text-slate-400 hover:text-rose-400 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={Users}
          title="No ledger entries found"
          description="Keep track of personal loans to friends, family, or borrowed money."
          actionText="Add Ledger Entry"
          onAction={() => setIsAddOpen(true)}
        />
      )}

      {/* Add Ledger Dialog */}
      <Dialog isOpen={isAddOpen} onClose={() => setIsAddOpen(false)} title="Create Ledger Entry">
        <form onSubmit={handleCreateEntry} className="space-y-4">
          <Input label="Person Name" value={personName} onChange={(e) => setPersonName(e.target.value)} required placeholder="e.g. Vikram Verma" />
          <Select
            label="Direction"
            value={direction}
            onChange={(e) => setDirection(e.target.value as any)}
            options={[
              { value: 'LENT', label: 'Money Lent (I gave money - Receivable)' },
              { value: 'BORROWED', label: 'Money Borrowed (I took money - Payable)' },
            ]}
          />
          <Input label="Original Amount (₹)" type="number" step="0.01" value={origAmount} onChange={(e) => setOrigAmount(e.target.value)} required placeholder="10000" />
          <div className="grid grid-cols-2 gap-4">
            <Input label="Entry Date" type="date" value={entryDate} onChange={(e) => setEntryDate(e.target.value)} required />
            <Input label="Due Date (Optional)" type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} />
          </div>
          <Input label="Description / Purpose" value={description} onChange={(e) => setDescription(e.target.value)} placeholder="e.g. Travel tickets, Emergency loan" />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" isLoading={loading}>
              Save Ledger Entry
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Record Repayment Dialog */}
      <Dialog isOpen={isPayOpen} onClose={() => setIsPayOpen(false)} title="Record Repayment">
        {selectedEntry && (
          <form onSubmit={handleRecordPayment} className="space-y-4">
            <div className="p-4 rounded-xl bg-purple-950/40 border border-purple-800/40 text-xs">
              <p className="font-bold text-slate-200">{selectedEntry.personName}</p>
              <p className="text-purple-300">Remaining Balance: {formatINR(selectedEntry.remainingBalance)}</p>
            </div>

            <Input label="Repayment Amount (₹)" type="number" step="0.01" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} required />
            <Input label="Payment Date" type="date" value={payDate} onChange={(e) => setPayDate(e.target.value)} required />

            <div className="flex items-center justify-end gap-3 pt-4 border-t border-purple-900/20">
              <Button variant="ghost" type="button" onClick={() => setIsPayOpen(false)}>
                Cancel
              </Button>
              <Button variant="primary" type="submit" isLoading={loading}>
                Confirm Repayment
              </Button>
            </div>
          </form>
        )}
      </Dialog>
    </div>
  );
};
