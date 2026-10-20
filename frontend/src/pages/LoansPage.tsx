import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { Loan, LoanSummary } from '@/types';
import { StatCard } from '@/components/common/StatCard';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';
import { Dialog } from '@/components/ui/Dialog';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Skeleton } from '@/components/ui/Skeleton';
import { EmptyState } from '@/components/common/EmptyState';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { formatINR, formatDate, cn } from '@/lib/utils';
import { computeLoanMetrics, calculateExpectedEndDate, calculateTenureMonths, downloadLoanPortfolioPdf } from '@/lib/loanUtils';
import {
  Landmark,
  Plus,
  ArrowRight,
  Percent,
  Calendar,
  FileText,
  TrendingUp,
  Trash2,
  FileDown,
} from 'lucide-react';
import { toast } from 'sonner';

export const LoansPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [filterStatus, setFilterStatus] = useState<string>('ACTIVE');
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isDownloadingPdf, setIsDownloadingPdf] = useState(false);
  const [deleteLoanId, setDeleteLoanId] = useState<string | null>(null);

  // Form State
  const [name, setName] = useState('');
  const [lender, setLender] = useState('');
  const [loanType, setLoanType] = useState('HOME_LOAN');
  const [maskedAccount, setMaskedAccount] = useState('');
  const [origPrincipal, setOrigPrincipal] = useState('');
  const [currentPrincipal, setCurrentPrincipal] = useState('');
  const [interestRate, setInterestRate] = useState('9.5');
  const initialStartDate = new Date().toISOString().split('T')[0];
  const [startDate, setStartDate] = useState(initialStartDate);
  const [emiDueDay, setEmiDueDay] = useState(new Date().getDate().toString());
  const [emiAmount, setEmiAmount] = useState('');
  const [tenureMonths, setTenureMonths] = useState('60');
  const [endDate, setEndDate] = useState(
    calculateExpectedEndDate(initialStartDate, 60, new Date().getDate())
  );
  const [autoMarkPastPaid, setAutoMarkPastPaid] = useState(true);

  const { data: loans, isLoading } = useQuery<Loan[]>({
    queryKey: ['loans', filterStatus],
    queryFn: async () => {
      const param = filterStatus !== 'ALL' ? `?status=${filterStatus}` : '';
      const res = await api.get(`/api/v1/loans${param}`);
      return res.data;
    },
  });

  const { data: summary } = useQuery<LoanSummary>({
    queryKey: ['loans-summary'],
    queryFn: async () => {
      const res = await api.get('/api/v1/loans/summary');
      return res.data;
    },
  });

  const handleCreateLoan = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      await api.post('/api/v1/loans', {
        name,
        lenderOrBank: lender,
        loanType,
        maskedAccountNumber: maskedAccount || undefined,
        originalPrincipal: parseFloat(origPrincipal),
        currentOutstandingPrincipal: currentPrincipal ? parseFloat(currentPrincipal) : undefined,
        annualInterestRate: parseFloat(interestRate),
        startDate,
        firstEmiDate: startDate,
        endDate: endDate || undefined,
        emiDueDay: parseInt(emiDueDay, 10),
        emiAmount: parseFloat(emiAmount),
        originalTenureMonths: parseInt(tenureMonths, 10),
        calculationMode: 'REDUCING_BALANCE',
        autoMarkPastEmisPaid: autoMarkPastPaid,
      });

      toast.success('Loan created and amortization schedule generated!');
      setIsAddOpen(false);
      // Reset form
      setName('');
      setLender('');
      setOrigPrincipal('');
      setCurrentPrincipal('');
      setEmiAmount('');
      queryClient.invalidateQueries({ queryKey: ['loans'] });
      queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      queryClient.invalidateQueries({ queryKey: ['dashboard-active-loans'] });
    } catch {
      toast.error('Failed to create loan');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadPdf = async () => {
    if (isDownloadingPdf) return;
    setIsDownloadingPdf(true);
    try {
      toast.info('Generating complete loan portfolio PDF...');
      await downloadLoanPortfolioPdf();
      toast.success('Loan summary PDF downloaded successfully!');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to download loan summary PDF');
    } finally {
      setIsDownloadingPdf(false);
    }
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-300">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
            Loans & Debt Management
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Track principal remaining, monthly EMI freedom, and loan closure timelines
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5 self-start sm:self-auto">
          <Button
            variant="outline"
            onClick={handleDownloadPdf}
            disabled={isDownloadingPdf}
            className="rounded-xl border-purple-500/40 hover:bg-purple-950/40 text-purple-200 min-h-[42px]"
          >
            <FileDown className="w-4 h-4 mr-1.5" />
            {isDownloadingPdf ? 'Generating PDF...' : 'Download Loan Summary PDF'}
          </Button>

          <Button
            variant="primary"
            onClick={() => setIsAddOpen(true)}
            className="rounded-xl shadow-lg shadow-purple-950/40 min-h-[42px]"
          >
            <Plus className="w-4 h-4 mr-1.5" />
            Add New Loan
          </Button>
        </div>
      </div>

      {/* Summary KPI Cards */}
      {summary && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-6">
          <StatCard
            title="Principal Remaining"
            amount={summary.totalOutstandingPrincipal}
            subtitle={`${summary.activeLoanCount} Active Loans`}
            icon={Landmark}
            color="purple"
          />
          <StatCard
            title="Total Monthly EMIs"
            amount={summary.totalMonthlyEmi}
            subtitle="Monthly debt commitment"
            icon={Calendar}
            color="indigo"
          />
          <StatCard
            title="Total Loan Balance"
            amount={summary.totalRemainingRepayment}
            subtitle="Principal + remaining interest"
            icon={FileText}
            color="amber"
          />
          <StatCard
            title="Est. Remaining Interest"
            amount={summary.totalEstimatedRemainingInterest}
            subtitle="Future interest component"
            icon={Percent}
            color="emerald"
          />
        </div>
      )}

      {/* Filter Tabs */}
      <div className="flex items-center gap-2 border-b border-purple-900/20 pb-3 overflow-x-auto scrollbar-none">
        {['ACTIVE', 'CLOSED', 'ALL'].map((st) => (
          <button
            key={st}
            onClick={() => setFilterStatus(st)}
            className={cn(
              'px-4 py-2 text-xs font-semibold rounded-xl transition-all min-h-[38px] whitespace-nowrap',
              filterStatus === st
                ? 'bg-purple-600 text-white shadow-md shadow-purple-900/40 font-bold'
                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900 bg-slate-950 border border-slate-800'
            )}
          >
            {st === 'ALL' ? 'All Loans' : st.charAt(0) + st.slice(1).toLowerCase() + ' Loans'}
          </button>
        ))}
      </div>

      {/* Loan List */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
          <Skeleton className="h-64 rounded-2xl" />
          <Skeleton className="h-64 rounded-2xl" />
        </div>
      ) : loans && loans.length > 0 ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
          {loans.map((loan) => {
            const metrics = computeLoanMetrics(loan);

            return (
              <Card
                key={loan.id}
                className="p-5 sm:p-6 relative group hover:border-purple-500/60 active:scale-[0.99] transition-all duration-300 cursor-pointer bg-slate-900/90 shadow-xl flex flex-col justify-between"
                onClick={() => navigate(`/loans/${loan.id}`)}
              >
                <div className="space-y-4">
                  {/* Top Header */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant={loan.status === 'ACTIVE' ? 'purple' : 'slate'} className="text-[10px]">
                          {loan.status}
                        </Badge>
                        <span className="text-xs text-purple-400 font-semibold truncate">
                          {loan.loanType.replace('_', ' ')}
                        </span>
                      </div>
                      <h3 className="text-lg sm:text-xl font-bold text-slate-100 font-heading truncate">
                        {loan.name}
                      </h3>
                      <p className="text-xs text-slate-400 truncate">
                        {loan.lenderOrBank} {loan.maskedAccountNumber && `• Acc ${loan.maskedAccountNumber}`}
                      </p>
                    </div>

                    <div className="flex items-center gap-1.5 shrink-0">
                      <button
                        type="button"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteLoanId(loan.id);
                        }}
                        className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-950/40 transition-colors"
                        title="Delete loan"
                        aria-label="Delete loan"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                      <div className="p-3 rounded-2xl bg-purple-950/70 border border-purple-800/40 text-purple-300">
                        <Landmark className="w-5 h-5 sm:w-6 sm:h-6" />
                      </div>
                    </div>
                  </div>

                  {/* Monthly EMI Freed Highlight Banner */}
                  <div className="p-2.5 sm:p-3 rounded-xl bg-gradient-to-r from-purple-950/80 to-indigo-950/60 border border-purple-800/40 flex items-center justify-between gap-2">
                    <div className="flex items-center gap-2 min-w-0">
                      <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0" />
                      <span className="text-xs font-bold text-purple-200 truncate">
                        Frees <strong className="text-white font-extrabold">{formatINR(metrics.monthlyEmiFreed)}/mo</strong> upon closure
                      </span>
                    </div>
                    <Badge variant="purple" className="text-[10px] shrink-0 font-bold">
                      {metrics.remainingEmis} EMIs left
                    </Badge>
                  </div>

                  {/* Key Loan Metrics Grid */}
                  <div className="grid grid-cols-2 gap-3.5 py-3 border-y border-purple-900/20 text-xs">
                    <div>
                      <span className="text-slate-400 block mb-0.5">Principal Remaining</span>
                      <span className="text-base font-black text-purple-300">
                        {formatINR(metrics.principalRemaining)}
                      </span>
                      <span className="text-[10px] text-slate-400 block mt-0.5">
                        Original: {formatINR(metrics.originalPrincipal)}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Remaining Loan Amount</span>
                      <span className="text-base font-black text-amber-300">
                        {formatINR(metrics.totalLoanBalance)}
                      </span>
                      <span className="text-[10px] text-slate-400 block mt-0.5">
                        {metrics.remainingEmis} EMIs × {formatINR(metrics.emiAmount)}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Monthly EMI</span>
                      <span className="text-sm font-extrabold text-slate-100">
                        {formatINR(metrics.emiAmount)}
                      </span>
                      <span className="text-[10px] text-slate-400 block mt-0.5">
                        Due {loan.emiDueDay}th • Next: {metrics.nextEmiDate ? formatDate(metrics.nextEmiDate) : 'Completed'}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Expected End Date</span>
                      <span className="text-sm font-bold text-slate-200">
                        {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'TBD'}
                      </span>
                      <span className="text-[10px] text-slate-400 block mt-0.5">
                        {metrics.paidEmis} of {metrics.totalEmis} paid
                      </span>
                    </div>
                  </div>

                  {/* Repayment Progress Bar */}
                  <div>
                    <div className="flex items-center justify-between text-[11px] font-semibold text-slate-400 mb-1">
                      <span>Principal Repaid ({metrics.progressPercent}%)</span>
                      <span className="text-purple-300 font-bold">
                        {formatINR(metrics.repaidPrincipal)} / {formatINR(metrics.originalPrincipal)}
                      </span>
                    </div>
                    <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-500"
                        style={{ width: `${metrics.progressPercent}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* Footer Action */}
                <div className="pt-4 flex items-center justify-between text-xs text-purple-400 font-semibold group-hover:text-purple-300">
                  <span>View Full Schedule & Prepayments</span>
                  <ArrowRight className="w-4 h-4 group-hover:translate-x-1.5 transition-transform" />
                </div>
              </Card>
            );
          })}
        </div>
      ) : (
        <EmptyState
          icon={Landmark}
          title="No loans found"
          description="Track home loans, car loans, or personal debts with automatic amortization and closure planning."
          actionText="Add First Loan"
          onAction={() => setIsAddOpen(true)}
        />
      )}

      {/* Add Loan Dialog Modal */}
      <Dialog isOpen={isAddOpen} onClose={() => setIsAddOpen(false)} title="Create New Loan" maxWidth="lg">
        <form onSubmit={handleCreateLoan} className="space-y-4">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Loan Name" value={name} onChange={(e) => setName(e.target.value)} required placeholder="e.g. HDFC Home Loan" />
            <Input label="Lender / Bank" value={lender} onChange={(e) => setLender(e.target.value)} required placeholder="e.g. HDFC Bank" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Select
              label="Loan Type"
              value={loanType}
              onChange={(e) => setLoanType(e.target.value)}
              options={[
                { value: 'HOME_LOAN', label: 'Home Loan' },
                { value: 'PERSONAL_LOAN', label: 'Personal Loan' },
                { value: 'CAR_LOAN', label: 'Car Loan' },
                { value: 'EDUCATION_LOAN', label: 'Education Loan' },
                { value: 'OTHER', label: 'Other Loan' },
              ]}
            />
            <Input label="Account Number (Last 4 digits)" value={maskedAccount} onChange={(e) => setMaskedAccount(e.target.value)} placeholder="e.g. 4321" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Original Principal Amount (₹)" type="number" step="0.01" value={origPrincipal} onChange={(e) => setOrigPrincipal(e.target.value)} required placeholder="500000" />
            <Input label="Current Outstanding Principal (₹)" type="number" step="0.01" value={currentPrincipal} onChange={(e) => setCurrentPrincipal(e.target.value)} placeholder="Leave blank if new loan" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input label="Interest Rate (% p.a.)" type="number" step="0.01" value={interestRate} onChange={(e) => setInterestRate(e.target.value)} required />
            <Input label="Monthly EMI (₹)" type="number" step="0.01" value={emiAmount} onChange={(e) => setEmiAmount(e.target.value)} required placeholder="10500" />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <Input
              label="Loan Start Date"
              type="date"
              value={startDate}
              onChange={(e) => {
                const val = e.target.value;
                setStartDate(val);
                if (val) {
                  const day = parseInt(val.split('-')[2], 10);
                  if (!isNaN(day)) {
                    setEmiDueDay(day.toString());
                  }
                  const t = parseInt(tenureMonths, 10);
                  if (t > 0) {
                    setEndDate(calculateExpectedEndDate(val, t, isNaN(day) ? 5 : day));
                  }
                }
              }}
              required
            />
            <Input
              label="Tenure (Months)"
              type="number"
              min="1"
              value={tenureMonths}
              onChange={(e) => {
                const val = e.target.value;
                setTenureMonths(val);
                const t = parseInt(val, 10);
                const day = parseInt(emiDueDay, 10) || 5;
                if (startDate && t > 0) {
                  setEndDate(calculateExpectedEndDate(startDate, t, day));
                }
              }}
              required
            />
            <Input
              label="Expected End Date"
              type="date"
              value={endDate}
              onChange={(e) => {
                const val = e.target.value;
                setEndDate(val);
                if (startDate && val) {
                  const implied = calculateTenureMonths(startDate, val);
                  if (implied > 0) {
                    setTenureMonths(implied.toString());
                  }
                }
              }}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 items-center">
            <Input
              label="EMI Due Day of Month"
              type="number"
              min="1"
              max="31"
              value={emiDueDay}
              onChange={(e) => {
                const val = e.target.value;
                setEmiDueDay(val);
                const day = parseInt(val, 10);
                const t = parseInt(tenureMonths, 10);
                if (startDate && t > 0 && day > 0) {
                  setEndDate(calculateExpectedEndDate(startDate, t, day));
                }
              }}
              required
            />
            <div className="pt-2 sm:pt-6">
              <label className="flex items-center gap-2 cursor-pointer text-xs text-slate-300">
                <input
                  type="checkbox"
                  checked={autoMarkPastPaid}
                  onChange={(e) => setAutoMarkPastPaid(e.target.checked)}
                  className="w-4 h-4 rounded border-slate-700 bg-slate-950 text-purple-600 focus:ring-purple-500"
                />
                <span>Automatically mark past EMIs before today as paid</span>
              </label>
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-4 border-t border-purple-900/20">
            <Button variant="ghost" type="button" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button variant="primary" type="submit" disabled={loading}>
              {loading ? 'Creating Loan...' : 'Create Loan'}
            </Button>
          </div>
        </form>
      </Dialog>

      <ConfirmModal
        isOpen={!!deleteLoanId}
        onClose={() => setDeleteLoanId(null)}
        onConfirm={async () => {
          if (!deleteLoanId) return;
          try {
            await api.delete(`/api/v1/loans/${deleteLoanId}`);
            toast.success('Loan deleted');
            queryClient.invalidateQueries({ queryKey: ['loans'] });
            queryClient.invalidateQueries({ queryKey: ['loans-summary'] });
            queryClient.invalidateQueries({ queryKey: ['dashboard'] });
            queryClient.invalidateQueries({ queryKey: ['dashboard-active-loans'] });
          } catch (err: any) {
            toast.error(err?.response?.data?.message || 'Failed to delete loan');
          } finally {
            setDeleteLoanId(null);
          }
        }}
        title="Delete Loan"
        message="Are you sure you want to delete this loan? This will delete all its amortization installments permanently."
        confirmText="Delete Loan"
        variant="danger"
      />
    </div>
  );
};
