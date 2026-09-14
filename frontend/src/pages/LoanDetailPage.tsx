import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Loan, LoanInstallment } from '@/types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Skeleton } from '@/components/ui/Skeleton';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatINR, formatDate, cn } from '@/lib/utils';
import { computeLoanMetrics } from '@/lib/loanUtils';
import {
  ArrowLeft,
  CheckCircle2,
  Undo,
  TrendingDown,
  Calendar,
  Percent,
  AlertCircle,
  Landmark,
  Sparkles,
  Check,
  TrendingUp,
  FileText,
  Clock,
  Trash2,
} from 'lucide-react';
import { toast } from 'sonner';

export const LoanDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [payModalOpen, setPayModalOpen] = useState(false);
  const [selectedInstallment, setSelectedInstallment] = useState<LoanInstallment | null>(null);
  const [payDate, setPayDate] = useState(new Date().toISOString().split('T')[0]);
  const [payAmount, setPayAmount] = useState('');
  const [payLoading, setPayLoading] = useState(false);

  const [prepayModalOpen, setPrepayModalOpen] = useState(false);
  const [prepayAmount, setPrepayAmount] = useState('');
  const [prepayDate, setPrepayDate] = useState(new Date().toISOString().split('T')[0]);
  const [prepayLoading, setPrepayLoading] = useState(false);

  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  const { data: loan, isLoading, error } = useQuery<Loan>({
    queryKey: ['loan-detail', id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/loans/${id}`);
      return res.data;
    },
    enabled: !!id,
  });

  const handleOpenPayModal = (inst: LoanInstallment) => {
    setSelectedInstallment(inst);
    setPayAmount(inst.scheduledAmount.toString());
    setPayModalOpen(true);
  };

  const handleRecordPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedInstallment || !id) return;
    setPayLoading(true);

    try {
      await api.post(`/api/v1/loans/${id}/installments/${selectedInstallment.id}/pay`, {
        paidDate: payDate,
        actualPaidAmount: parseFloat(payAmount),
      });

      toast.success('Installment marked as paid!');
      setPayModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['loan-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to record payment');
    } finally {
      setPayLoading(false);
    }
  };

  const handleUndoPayment = async (instId: string) => {
    if (!id) return;
    try {
      await api.post(`/api/v1/loans/${id}/installments/${instId}/undo`);
      toast.success('Installment payment undone');
      queryClient.invalidateQueries({ queryKey: ['loan-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to undo payment');
    }
  };

  const handlePrepayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setPrepayLoading(true);

    try {
      await api.post(`/api/v1/loans/${id}/prepayment`, {
        amount: parseFloat(prepayAmount),
        paymentDate: prepayDate,
        keepSameEmiReduceTenure: true,
      });

      toast.success('Prepayment recorded and future schedule recalculated!');
      setPrepayModalOpen(false);
      setPrepayAmount('');
      queryClient.invalidateQueries({ queryKey: ['loan-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to process prepayment');
    } finally {
      setPrepayLoading(false);
    }
  };

  const handleDeleteLoan = async () => {
    if (!id) return;
    try {
      await api.delete(`/api/v1/loans/${id}`);
      toast.success('Loan deleted');
      navigate('/loans');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to delete loan');
    }
  };

  if (isLoading) return <Skeleton className="h-96 w-full rounded-2xl" />;

  if (error || !loan) {
    return (
      <div className="p-8 text-center bg-rose-950/40 border border-rose-800/40 rounded-2xl">
        <AlertCircle className="w-10 h-10 text-rose-400 mx-auto mb-3" />
        <h3 className="text-lg font-bold text-rose-200">Loan Details Not Found</h3>
        <Button variant="ghost" onClick={() => navigate('/loans')} className="mt-4">
          <ArrowLeft className="w-4 h-4 mr-2" /> Back to Loans
        </Button>
      </div>
    );
  }

  const metrics = computeLoanMetrics(loan);

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-300">
      {/* Top Action Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3 sm:gap-4">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => navigate('/loans')}
            className="rounded-xl min-h-[44px] min-w-[44px] p-2"
            aria-label="Back to Loans"
          >
            <ArrowLeft className="w-5 h-5" />
          </Button>
          <div>
            <div className="flex items-center gap-2 mb-0.5">
              <Badge variant={loan.status === 'ACTIVE' ? 'purple' : 'slate'} className="text-[10px]">
                {loan.status}
              </Badge>
              <span className="text-xs text-purple-400 font-semibold">{loan.lenderOrBank}</span>
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading">
              {loan.name}
            </h1>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-start sm:self-auto">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPrepayModalOpen(true)}
            className="rounded-xl min-h-[42px] border-purple-500/40 hover:bg-purple-950/40 text-purple-300"
          >
            <TrendingDown className="w-4 h-4 mr-1.5" />
            Make Prepayment
          </Button>
          <Button
            variant="danger"
            size="sm"
            onClick={() => setDeleteConfirmOpen(true)}
            className="rounded-xl min-h-[42px]"
          >
            <Trash2 className="w-4 h-4" />
          </Button>
        </div>
      </div>

      {/* LOAN CLOSURE IMPACT CARD (Prominent motivation card) */}
      <Card className="p-5 sm:p-6 border border-purple-500/40 bg-gradient-to-r from-slate-900/95 via-purple-950/30 to-slate-900/95 shadow-xl shadow-purple-950/30">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
          <div className="space-y-2 flex-1">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-purple-400" />
              <span className="text-xs font-extrabold text-purple-300 uppercase tracking-wider">
                Loan Closure Impact
              </span>
            </div>

            <h2 className="text-2xl sm:text-3xl font-black text-slate-100 font-heading tracking-tight">
              Frees <span className="text-purple-300">{formatINR(metrics.monthlyEmiFreed)}/mo</span> from your monthly cash flow
            </h2>

            <p className="text-xs text-slate-300">
              Closing this loan completely eliminates {metrics.remainingEmis} scheduled EMI payments and unlocks {formatINR(metrics.monthlyEmiFreed)} every single month.
            </p>

            <div className="flex flex-wrap items-center gap-x-4 gap-y-2 pt-1 text-xs">
              <span className="text-slate-400">
                Balance to close:{' '}
                <strong className="text-amber-300 font-extrabold">
                  {formatINR(metrics.totalLoanBalance)}
                </strong>
              </span>
              <span className="text-slate-500">•</span>
              <span className="text-slate-400">
                Final EMI Date:{' '}
                <strong className="text-slate-200">
                  {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'Not scheduled'}
                </strong>
              </span>
            </div>
          </div>

          <div className="bg-slate-950/60 p-4 rounded-2xl border border-purple-900/30 md:w-64 shrink-0 space-y-2">
            <div className="flex items-center justify-between text-xs font-semibold">
              <span className="text-slate-400">Tenure Progress</span>
              <span className="text-purple-300 font-bold">{metrics.progressPercent}%</span>
            </div>
            <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden">
              <div
                className="h-full rounded-full bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-500"
                style={{ width: `${metrics.progressPercent}%` }}
              />
            </div>
            <p className="text-[11px] text-slate-400 text-center font-medium">
              {metrics.paidEmis} paid • {metrics.remainingEmis} remaining
            </p>
          </div>
        </div>
      </Card>

      {/* Comprehensive Loan Statistics Grid */}
      <div className="grid grid-cols-2 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
        <Card className="p-4 sm:p-5 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase block mb-1">
            Principal Remaining
          </span>
          <p className="text-xl sm:text-2xl font-black text-purple-300">
            {formatINR(metrics.principalRemaining)}
          </p>
          <p className="text-[11px] text-slate-400 mt-1 truncate">
            Original: {formatINR(metrics.originalPrincipal)}
          </p>
        </Card>

        <Card className="p-4 sm:p-5 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase block mb-1">
            Total Loan Balance
          </span>
          <p className="text-xl sm:text-2xl font-black text-amber-300">
            {formatINR(metrics.totalLoanBalance)}
          </p>
          <p className="text-[11px] text-slate-400 mt-1 truncate">
            {metrics.remainingEmis} EMIs × {formatINR(metrics.emiAmount)}
          </p>
        </Card>

        <Card className="p-4 sm:p-5 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase block mb-1">
            Monthly EMI
          </span>
          <p className="text-xl sm:text-2xl font-black text-slate-100">
            {formatINR(metrics.emiAmount)}
          </p>
          <p className="text-[11px] text-slate-400 mt-1 truncate">
            Due {loan.emiDueDay}th of month
          </p>
        </Card>

        <Card className="p-4 sm:p-5 border border-slate-800">
          <span className="text-xs text-slate-400 font-semibold uppercase block mb-1">
            Interest Rate & Tenure
          </span>
          <p className="text-xl sm:text-2xl font-black text-emerald-300">
            {metrics.annualInterestRate}% <span className="text-xs font-normal text-slate-400">p.a.</span>
          </p>
          <p className="text-[11px] text-slate-400 mt-1 truncate">
            {loan.originalTenureMonths} Mo. tenure ({metrics.totalEmis} EMIs)
          </p>
        </Card>
      </div>

      {/* Secondary Metadata Row */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4 p-4 rounded-2xl bg-slate-900/60 border border-purple-900/20 text-xs">
        <div>
          <span className="text-slate-400 block mb-0.5">Next EMI Date</span>
          <span className="font-bold text-slate-200">
            {metrics.nextEmiDate ? formatDate(metrics.nextEmiDate) : 'Not scheduled'}
          </span>
        </div>
        <div>
          <span className="text-slate-400 block mb-0.5">Last EMI Date</span>
          <span className="font-bold text-purple-300">
            {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'Not scheduled'}
          </span>
        </div>
        <div>
          <span className="text-slate-400 block mb-0.5">Paid EMIs</span>
          <span className="font-bold text-emerald-300">
            {metrics.paidEmis} EMIs ({formatINR(metrics.paidEmis * metrics.emiAmount)})
          </span>
        </div>
        <div>
          <span className="text-slate-400 block mb-0.5">Remaining EMIs</span>
          <span className="font-bold text-amber-300">
            {metrics.remainingEmis} EMIs
          </span>
        </div>
      </div>

      {/* Full Amortization Schedule */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <CardTitle>Amortization Schedule</CardTitle>
            <CardDescription>
              Reducing balance schedule • {metrics.paidEmis} of {metrics.totalEmis} EMIs paid
            </CardDescription>
          </div>
          <Badge variant="purple" className="self-start sm:self-auto text-xs font-semibold">
            {metrics.remainingEmis} Payments Left
          </Badge>
        </CardHeader>

        {/* Desktop / Tablet Table View (hidden on mobile to prevent horizontal scroll) */}
        <CardContent className="p-0 hidden sm:block">
          <table className="w-full text-left text-xs">
            <thead className="bg-slate-900/90 text-slate-400 font-semibold border-b border-purple-900/20 uppercase tracking-wider">
              <tr>
                <th className="px-6 py-4">#</th>
                <th className="px-6 py-4">Due Date</th>
                <th className="px-6 py-4">Scheduled EMI</th>
                <th className="px-6 py-4">Principal Component</th>
                <th className="px-6 py-4">Interest Component</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-purple-900/10">
              {loan.installments?.map((inst) => (
                <tr key={inst.id} className="hover:bg-slate-900/50 transition-colors">
                  <td className="px-6 py-4 font-bold text-slate-300">{inst.installmentNumber}</td>
                  <td className="px-6 py-4 text-slate-200 font-medium">{formatDate(inst.dueDate)}</td>
                  <td className="px-6 py-4 font-extrabold text-slate-100">{formatINR(inst.scheduledAmount)}</td>
                  <td className="px-6 py-4 text-purple-300 font-semibold">{formatINR(inst.principalComponent)}</td>
                  <td className="px-6 py-4 text-amber-300 font-semibold">{formatINR(inst.interestComponent)}</td>
                  <td className="px-6 py-4">
                    <Badge variant={inst.paymentState === 'PAID' ? 'emerald' : 'amber'}>
                      {inst.paymentState}
                    </Badge>
                  </td>
                  <td className="px-6 py-4 text-right">
                    {inst.paymentState === 'UNPAID' ? (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenPayModal(inst)}
                        className="rounded-xl min-h-[36px]"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-emerald-400" />
                        Mark Paid
                      </Button>
                    ) : (
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => handleUndoPayment(inst.id)}
                        className="rounded-xl min-h-[36px] text-slate-400 hover:text-slate-200"
                      >
                        <Undo className="w-3.5 h-3.5 mr-1" />
                        Undo
                      </Button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>

        {/* Mobile Responsive List View (sm:hidden - Guaranteed ZERO horizontal scroll on 360px-430px) */}
        <CardContent className="p-3 sm:hidden divide-y divide-purple-900/20 space-y-3">
          {loan.installments?.map((inst) => (
            <div key={inst.id} className="pt-3 first:pt-0 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="w-6 h-6 rounded-full bg-slate-800 text-[11px] font-extrabold text-purple-300 flex items-center justify-center shrink-0">
                    {inst.installmentNumber}
                  </span>
                  <span className="text-xs font-bold text-slate-200">{formatDate(inst.dueDate)}</span>
                </div>
                <Badge variant={inst.paymentState === 'PAID' ? 'emerald' : 'amber'} className="text-[10px]">
                  {inst.paymentState}
                </Badge>
              </div>

              <div className="grid grid-cols-3 gap-2 p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px]">
                <div>
                  <span className="text-slate-400 block">EMI</span>
                  <span className="font-extrabold text-slate-100">{formatINR(inst.scheduledAmount)}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Principal</span>
                  <span className="font-semibold text-purple-300">{formatINR(inst.principalComponent)}</span>
                </div>
                <div>
                  <span className="text-slate-400 block">Interest</span>
                  <span className="font-semibold text-amber-300">{formatINR(inst.interestComponent)}</span>
                </div>
              </div>

              <div className="flex justify-end pt-1">
                {inst.paymentState === 'UNPAID' ? (
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleOpenPayModal(inst)}
                    className="w-full rounded-xl min-h-[44px] text-xs font-bold"
                  >
                    <CheckCircle2 className="w-4 h-4 mr-1 text-emerald-400" />
                    Mark Installment #{inst.installmentNumber} Paid
                  </Button>
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleUndoPayment(inst.id)}
                    className="w-full rounded-xl min-h-[44px] text-xs text-slate-400 hover:text-slate-200"
                  >
                    <Undo className="w-4 h-4 mr-1" />
                    Undo Payment
                  </Button>
                )}
              </div>
            </div>
          ))}
        </CardContent>
      </Card>

      {/* Mark Paid Dialog Modal */}
      <Dialog
        isOpen={payModalOpen}
        onClose={() => setPayModalOpen(false)}
        title={`Record EMI #${selectedInstallment?.installmentNumber} Payment`}
      >
        <form onSubmit={handleRecordPayment} className="space-y-4">
          <Input
            label="Payment Date"
            type="date"
            value={payDate}
            onChange={(e) => setPayDate(e.target.value)}
            required
          />
          <Input
            label="Actual Amount Paid (₹)"
            type="number"
            step="0.01"
            value={payAmount}
            onChange={(e) => setPayAmount(e.target.value)}
            required
          />
          <div className="flex justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setPayModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={payLoading}>
              {payLoading ? 'Saving...' : 'Confirm Paid'}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Prepayment Dialog Modal */}
      <Dialog
        isOpen={prepayModalOpen}
        onClose={() => setPrepayModalOpen(false)}
        title="Make Loan Prepayment"
        description="Prepayments reduce your outstanding principal and recalculate remaining tenure while keeping monthly EMI identical."
      >
        <form onSubmit={handlePrepayment} className="space-y-4">
          <Input
            label="Prepayment Amount (₹)"
            type="number"
            step="0.01"
            value={prepayAmount}
            onChange={(e) => setPrepayAmount(e.target.value)}
            required
            placeholder="e.g. 50000"
          />
          <Input
            label="Prepayment Date"
            type="date"
            value={prepayDate}
            onChange={(e) => setPrepayDate(e.target.value)}
            required
          />
          <div className="p-3 rounded-xl bg-purple-950/40 border border-purple-900/40 text-xs text-purple-300">
            <p className="font-semibold">Reducing Balance Recalculation:</p>
            <p className="text-[11px] text-slate-300 mt-0.5">
              The principal component will be deducted immediately, shortening your loan closure date.
            </p>
          </div>
          <div className="flex justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setPrepayModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={prepayLoading}>
              {prepayLoading ? 'Processing...' : 'Apply Prepayment'}
            </Button>
          </div>
        </form>
      </Dialog>

      {/* Delete Loan Confirmation Modal */}
      <ConfirmModal
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={handleDeleteLoan}
        title="Delete Loan?"
        message="Are you sure you want to delete this loan? Loans with existing payment history cannot be deleted."
        confirmText="Delete Loan"
        variant="danger"
      />
    </div>
  );
};
