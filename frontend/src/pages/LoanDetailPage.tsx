import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { Loan, LoanInstallment, LoanType, LoanStatus } from '@/types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatINR, formatDate, cn } from '@/lib/utils';
import { computeLoanMetrics, calculateExpectedEndDate, calculateTenureMonths } from '@/lib/loanUtils';
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
  Pencil,
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

  // Edit Loan Modal State
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [editName, setEditName] = useState('');
  const [editLender, setEditLender] = useState('');
  const [editLoanType, setEditLoanType] = useState<LoanType>('HOME_LOAN');
  const [editMaskedAccount, setEditMaskedAccount] = useState('');
  const [editOriginalPrincipal, setEditOriginalPrincipal] = useState('');
  const [editCurrentPrincipal, setEditCurrentPrincipal] = useState('');
  const [editInterestRate, setEditInterestRate] = useState('');
  const [editStartDate, setEditStartDate] = useState('');
  const [editFirstEmiDate, setEditFirstEmiDate] = useState('');
  const [editEndDate, setEditEndDate] = useState('');
  const [editTenureMonths, setEditTenureMonths] = useState('');
  const [editEmiAmount, setEditEmiAmount] = useState('');
  const [editEmiDueDay, setEditEmiDueDay] = useState('5');
  const [editStatus, setEditStatus] = useState<LoanStatus>('ACTIVE');
  const [editNotes, setEditNotes] = useState('');
  const [editRecalculateSchedule, setEditRecalculateSchedule] = useState(false);
  const [editLoading, setEditLoading] = useState(false);

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

      toast.success('Prepayment recorded! Remaining principal reduced.');
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

  const handleOpenEditModal = () => {
    if (!loan) return;
    setEditName(loan.name);
    setEditLender(loan.lenderOrBank);
    setEditLoanType(loan.loanType);
    setEditMaskedAccount(loan.maskedAccountNumber || '');
    setEditOriginalPrincipal(loan.originalPrincipal ? loan.originalPrincipal.toString() : '');
    setEditCurrentPrincipal(
      loan.currentOutstandingPrincipal !== undefined
        ? loan.currentOutstandingPrincipal.toString()
        : ''
    );
    setEditInterestRate(loan.annualInterestRate ? loan.annualInterestRate.toString() : '');
    const sDate = loan.startDate || '';
    const fDate = loan.firstEmiDate || sDate;
    const tenure = loan.totalEmiCount || loan.originalTenureMonths || 1;
    setEditStartDate(sDate);
    setEditFirstEmiDate(fDate);
    setEditTenureMonths(tenure.toString());
    const expectedEnd =
      loan.endDate || calculateExpectedEndDate(fDate, tenure, loan.emiDueDay);
    setEditEndDate(expectedEnd);
    setEditEmiAmount(loan.emiAmount ? loan.emiAmount.toString() : '');
    setEditEmiDueDay(loan.emiDueDay ? loan.emiDueDay.toString() : '5');
    setEditStatus(loan.status);
    setEditNotes(loan.notes || '');
    setEditRecalculateSchedule(false);
    setEditModalOpen(true);
  };

  const handleUpdateLoan = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id) return;
    setEditLoading(true);

    try {
      await api.put(`/api/v1/loans/${id}`, {
        name: editName,
        lenderOrBank: editLender,
        loanType: editLoanType,
        maskedAccountNumber: editMaskedAccount || undefined,
        originalPrincipal: editOriginalPrincipal ? parseFloat(editOriginalPrincipal) : undefined,
        currentOutstandingPrincipal: editCurrentPrincipal ? parseFloat(editCurrentPrincipal) : undefined,
        annualInterestRate: parseFloat(editInterestRate),
        startDate: editStartDate || undefined,
        firstEmiDate: editFirstEmiDate || editStartDate || undefined,
        endDate: editEndDate || undefined,
        originalTenureMonths: editTenureMonths ? parseInt(editTenureMonths, 10) : undefined,
        emiDueDay: parseInt(editEmiDueDay),
        emiAmount: parseFloat(editEmiAmount),
        status: editStatus,
        manualOverride: true,
        recalculateFutureSchedule: editRecalculateSchedule,
        notes: editNotes || undefined,
      });

      toast.success('Loan updated successfully!');
      setEditModalOpen(false);
      queryClient.invalidateQueries({ queryKey: ['loan-detail', id] });
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-active-loans'] });
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to update loan');
    } finally {
      setEditLoading(false);
    }
  };

  const handleDeleteLoan = async () => {
    if (!id) return;
    try {
      await api.delete(`/api/v1/loans/${id}`);
      toast.success('Loan deleted');
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-active-loans'] });
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
            onClick={handleOpenEditModal}
            className="rounded-xl min-h-[42px] border-slate-700 hover:bg-slate-800 text-slate-200"
          >
            <Pencil className="w-4 h-4 mr-1.5" />
            Edit Loan
          </Button>
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
            title="Delete Loan"
            aria-label="Delete Loan"
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

      {/* 12 Key Loan Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-4">
        {/* 1. Original Principal Amount */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Original Principal
          </span>
          <p className="text-lg sm:text-xl font-black text-slate-100">
            {formatINR(metrics.originalPrincipal)}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Initial sanctioned amount</p>
        </Card>

        {/* 2. Remaining Principal Amount */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Remaining Principal
          </span>
          <p className="text-lg sm:text-xl font-black text-purple-300">
            {formatINR(metrics.principalRemaining)}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Current principal balance</p>
        </Card>

        {/* 3. Monthly EMI Amount */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Monthly EMI
          </span>
          <p className="text-lg sm:text-xl font-black text-slate-100">
            {formatINR(metrics.emiAmount)}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Due {loan.emiDueDay}th of month</p>
        </Card>

        {/* 4. Original Total Tenure */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Original Total Tenure
          </span>
          <p className="text-lg sm:text-xl font-black text-emerald-300">
            {metrics.totalEmis} <span className="text-xs font-normal text-slate-400">Months</span>
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Fixed schedule commitment</p>
        </Card>

        {/* 5. Elapsed EMI Count */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Elapsed EMIs
          </span>
          <p className="text-lg sm:text-xl font-black text-indigo-300">
            {metrics.elapsedEmis} <span className="text-xs font-normal text-slate-400">/ {metrics.totalEmis}</span>
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Due dates reached so far</p>
        </Card>

        {/* 6. Actually Paid EMI Count */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Actually Paid EMIs
          </span>
          <p className="text-lg sm:text-xl font-black text-emerald-400">
            {metrics.paidEmis} <span className="text-xs font-normal text-slate-400">EMIs</span>
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Explicitly recorded as paid</p>
        </Card>

        {/* 7. Remaining EMI Count */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Remaining EMIs
          </span>
          <p className="text-lg sm:text-xl font-black text-amber-300">
            {metrics.remainingEmis} <span className="text-xs font-normal text-slate-400">EMIs</span>
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Total tenure - elapsed EMIs</p>
        </Card>

        {/* 8. Remaining Loan Amount */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Remaining Loan Amount
          </span>
          <p className="text-lg sm:text-xl font-black text-amber-300">
            {formatINR(metrics.totalLoanBalance)}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">{metrics.remainingEmis} EMIs × {formatINR(metrics.emiAmount)}</p>
        </Card>

        {/* 9. Loan Start Date */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Loan Start Date
          </span>
          <p className="text-base sm:text-lg font-bold text-slate-200">
            {loan.startDate ? formatDate(loan.startDate) : 'N/A'}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">First EMI date</p>
        </Card>

        {/* 10. Expected Loan End Date */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Expected End Date
          </span>
          <p className="text-base sm:text-lg font-bold text-purple-300">
            {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'N/A'}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Final installment due</p>
        </Card>

        {/* 11. Next EMI Date */}
        <Card className="p-3.5 sm:p-4 border border-slate-800">
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Next EMI Date
          </span>
          <p className="text-base sm:text-lg font-bold text-slate-100">
            {metrics.nextEmiDate ? formatDate(metrics.nextEmiDate) : (metrics.remainingEmis === 0 ? 'Fully Elapsed' : 'None upcoming')}
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">Next from current day</p>
        </Card>

        {/* 12. Overdue EMI Count */}
        <Card className={cn(
          "p-3.5 sm:p-4 border",
          metrics.overdueEmis > 0 ? "border-rose-800/60 bg-rose-950/20" : "border-slate-800"
        )}>
          <span className="text-[11px] text-slate-400 font-semibold uppercase block mb-1">
            Overdue EMIs
          </span>
          <p className={cn(
            "text-lg sm:text-xl font-black",
            metrics.overdueEmis > 0 ? "text-rose-400" : "text-emerald-400"
          )}>
            {metrics.overdueEmis} <span className="text-xs font-normal text-slate-400">EMIs</span>
          </p>
          <p className="text-[10px] text-slate-500 mt-0.5">
            {metrics.overdueEmis > 0 ? 'Due date passed, unpaid' : 'Zero overdue payments'}
          </p>
        </Card>
      </div>

      {/* Full Amortization Schedule */}
      <Card>
        <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <CardTitle>EMI Schedule</CardTitle>
            <CardDescription>
              Fixed schedule • {metrics.paidEmis} of {metrics.totalEmis} EMIs paid ({metrics.remainingEmis} remaining)
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
                <th className="px-6 py-4">EMI #</th>
                <th className="px-6 py-4">Due Date</th>
                <th className="px-6 py-4">Fixed EMI Amount</th>
                <th className="px-6 py-4">Status</th>
                <th className="px-6 py-4">Actual Payment Date</th>
                <th className="px-6 py-4 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-purple-900/10">
              {loan.installments?.map((inst) => {
                const todayStr = new Date().toISOString().split('T')[0];
                const isPaid = inst.paymentState === 'PAID';
                const isOverdue = !isPaid && (inst.overdue || inst.dueDate < todayStr);

                return (
                  <tr key={inst.id} className="hover:bg-slate-900/50 transition-colors">
                    <td className="px-6 py-4 font-bold text-slate-300">#{inst.installmentNumber}</td>
                    <td className="px-6 py-4 text-slate-200 font-medium">{formatDate(inst.dueDate)}</td>
                    <td className="px-6 py-4 font-extrabold text-slate-100">{formatINR(inst.scheduledAmount)}</td>
                    <td className="px-6 py-4">
                      {isPaid ? (
                        <Badge variant="emerald">Paid</Badge>
                      ) : isOverdue ? (
                        <Badge variant="rose">Overdue</Badge>
                      ) : (
                        <Badge variant="amber">Unpaid</Badge>
                      )}
                    </td>
                    <td className="px-6 py-4 text-slate-300">
                      {inst.paidDate ? formatDate(inst.paidDate) : '-'}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {isPaid ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleUndoPayment(inst.id)}
                          className="rounded-xl min-h-[36px] text-slate-400 hover:text-slate-200"
                        >
                          <Undo className="w-3.5 h-3.5 mr-1" />
                          Undo
                        </Button>
                      ) : (
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenPayModal(inst)}
                          className="rounded-xl min-h-[36px]"
                        >
                          <CheckCircle2 className="w-3.5 h-3.5 mr-1 text-emerald-400" />
                          Mark Paid
                        </Button>
                      )}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </CardContent>

        {/* Mobile Responsive List View (sm:hidden - Guaranteed ZERO horizontal scroll on 360px-430px) */}
        <CardContent className="p-3 sm:hidden divide-y divide-purple-900/20 space-y-3">
          {loan.installments?.map((inst) => {
            const todayStr = new Date().toISOString().split('T')[0];
            const isPaid = inst.paymentState === 'PAID';
            const isOverdue = !isPaid && (inst.overdue || inst.dueDate < todayStr);

            return (
              <div key={inst.id} className="pt-3 first:pt-0 space-y-2">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="w-6 h-6 rounded-full bg-slate-800 text-[11px] font-extrabold text-purple-300 flex items-center justify-center shrink-0">
                      {inst.installmentNumber}
                    </span>
                    <span className="text-xs font-bold text-slate-200">{formatDate(inst.dueDate)}</span>
                  </div>
                  {isPaid ? (
                    <Badge variant="emerald" className="text-[10px]">Paid</Badge>
                  ) : isOverdue ? (
                    <Badge variant="rose" className="text-[10px]">Overdue</Badge>
                  ) : (
                    <Badge variant="amber" className="text-[10px]">Unpaid</Badge>
                  )}
                </div>

                <div className="grid grid-cols-2 gap-2 p-2.5 rounded-xl bg-slate-950/60 border border-slate-800 text-[11px]">
                  <div>
                    <span className="text-slate-400 block">Fixed EMI Amount</span>
                    <span className="font-extrabold text-slate-100">{formatINR(inst.scheduledAmount)}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block">Payment Date</span>
                    <span className="font-semibold text-slate-300">
                      {inst.paidDate ? formatDate(inst.paidDate) : '-'}
                    </span>
                  </div>
                </div>

                <div className="flex justify-end pt-1">
                  {isPaid ? (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => handleUndoPayment(inst.id)}
                      className="w-full rounded-xl min-h-[44px] text-xs text-slate-400 hover:text-slate-200"
                    >
                      <Undo className="w-4 h-4 mr-1" />
                      Undo Payment
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleOpenPayModal(inst)}
                      className="w-full rounded-xl min-h-[44px] text-xs font-bold"
                    >
                      <CheckCircle2 className="w-4 h-4 mr-1 text-emerald-400" />
                      Mark EMI #{inst.installmentNumber} Paid
                    </Button>
                  )}
                </div>
              </div>
            );
          })}
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
        description="Prepayments reduce your remaining principal balance without shortening your fixed schedule tenure."
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
            <p className="font-semibold">Principal Balance Reduction:</p>
            <p className="text-[11px] text-slate-300 mt-0.5">
              Prepayments directly pay down your outstanding principal. Your scheduled EMI installments and tenure remain fixed, reducing total interest liability.
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

      {/* Edit Loan Dialog Modal */}
      <Dialog
        isOpen={editModalOpen}
        onClose={() => setEditModalOpen(false)}
        title="Edit Loan Details"
        description="Update loan parameters, tenure, or outstanding balance."
        maxWidth="lg"
      >
        <form onSubmit={handleUpdateLoan} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Loan Name"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              required
            />
            <Input
              label="Lender / Bank"
              value={editLender}
              onChange={(e) => setEditLender(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Loan Type"
              value={editLoanType}
              onChange={(e) => setEditLoanType(e.target.value as LoanType)}
              options={[
                { value: 'HOME_LOAN', label: 'Home Loan' },
                { value: 'PERSONAL_LOAN', label: 'Personal Loan' },
                { value: 'CAR_LOAN', label: 'Car Loan' },
                { value: 'EDUCATION_LOAN', label: 'Education Loan' },
                { value: 'OTHER', label: 'Other Loan' },
              ]}
            />
            <Input
              label="Account Number (Last 4 digits)"
              value={editMaskedAccount}
              onChange={(e) => setEditMaskedAccount(e.target.value)}
              placeholder="e.g. 4321"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              label="Original Principal (₹)"
              type="number"
              step="0.01"
              value={editOriginalPrincipal}
              onChange={(e) => setEditOriginalPrincipal(e.target.value)}
              required
            />
            <Input
              label="Current Outstanding Principal (₹)"
              type="number"
              step="0.01"
              value={editCurrentPrincipal}
              onChange={(e) => setEditCurrentPrincipal(e.target.value)}
              required
            />
            <Input
              label="Annual Interest Rate (% p.a.)"
              type="number"
              step="0.01"
              value={editInterestRate}
              onChange={(e) => setEditInterestRate(e.target.value)}
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              label="Loan Start Date"
              type="date"
              value={editStartDate}
              onChange={(e) => {
                const val = e.target.value;
                setEditStartDate(val);
                setEditFirstEmiDate(val);
                const tMonths = parseInt(editTenureMonths, 10);
                const day = parseInt(editEmiDueDay, 10) || 5;
                if (val && tMonths > 0) {
                  setEditEndDate(calculateExpectedEndDate(val, tMonths, day));
                }
              }}
              required
            />
            <Input
              label="Tenure (Months)"
              type="number"
              min="1"
              value={editTenureMonths}
              onChange={(e) => {
                const val = e.target.value;
                setEditTenureMonths(val);
                const tMonths = parseInt(val, 10);
                const day = parseInt(editEmiDueDay, 10) || 5;
                if (editStartDate && tMonths > 0) {
                  setEditEndDate(calculateExpectedEndDate(editStartDate, tMonths, day));
                }
              }}
              required
            />
            <Input
              label="Expected End Date"
              type="date"
              value={editEndDate}
              onChange={(e) => {
                const val = e.target.value;
                setEditEndDate(val);
                if (editStartDate && val) {
                  const impliedTenure = calculateTenureMonths(editStartDate, val);
                  if (impliedTenure > 0) {
                    setEditTenureMonths(impliedTenure.toString());
                  }
                }
              }}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              label="Monthly EMI (₹)"
              type="number"
              step="0.01"
              value={editEmiAmount}
              onChange={(e) => setEditEmiAmount(e.target.value)}
              required
            />
            <Input
              label="EMI Due Day of Month"
              type="number"
              min="1"
              max="31"
              value={editEmiDueDay}
              onChange={(e) => {
                const val = e.target.value;
                setEditEmiDueDay(val);
                const day = parseInt(val, 10);
                const tMonths = parseInt(editTenureMonths, 10);
                if (editStartDate && tMonths > 0 && day > 0) {
                  setEditEndDate(calculateExpectedEndDate(editStartDate, tMonths, day));
                }
              }}
              required
            />
            <Select
              label="Loan Status"
              value={editStatus}
              onChange={(e) => setEditStatus(e.target.value as LoanStatus)}
              options={[
                { value: 'ACTIVE', label: 'Active' },
                { value: 'CLOSED', label: 'Closed' },
                { value: 'PAUSED', label: 'Paused' },
              ]}
            />
          </div>

          <div className="p-3 rounded-xl bg-slate-900 border border-purple-900/40 text-xs">
            <label className="flex items-center gap-2.5 cursor-pointer">
              <input
                type="checkbox"
                checked={editRecalculateSchedule}
                onChange={(e) => setEditRecalculateSchedule(e.target.checked)}
                className="w-4 h-4 rounded border-slate-700 bg-slate-950 text-purple-600 focus:ring-purple-500"
              />
              <div>
                <span className="font-semibold text-slate-200">Reconcile / recalculate future schedule</span>
                <p className="text-[11px] text-slate-400">
                  Safely aligns installments with updated dates or tenure, preserving all past paid records.
                </p>
              </div>
            </label>
          </div>

          <Input
            label="Notes (Optional)"
            value={editNotes}
            onChange={(e) => setEditNotes(e.target.value)}
            placeholder="Add any reminders or loan notes..."
          />

          <div className="flex justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setEditModalOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={editLoading}>
              {editLoading ? 'Saving Changes...' : 'Save Changes'}
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
        message="Are you sure you want to delete this loan? This will permanently remove the loan and its entire amortization schedule."
        confirmText="Delete Loan"
        variant="danger"
      />
    </div>
  );
};
