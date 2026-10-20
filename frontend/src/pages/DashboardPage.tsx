import React, { useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '@/lib/api';
import { DashboardSummary, Loan } from '@/types';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { Button } from '@/components/ui/Button';
import { Skeleton } from '@/components/ui/Skeleton';
import { BottomSheet } from '@/components/ui/BottomSheet';
import { formatINR, formatDate, cn } from '@/lib/utils';
import {
  computeLoanMetrics,
  sortLoansByFocus,
  LoanSortOption,
} from '@/lib/loanUtils';
import {
  Wallet,
  Calendar,
  AlertCircle,
  Clock,
  Landmark,
  Receipt,
  Circle,
  ArrowRight,
  TrendingUp,
  Percent,
  Sparkles,
  ChevronRight,
  ShieldCheck,
  CheckCircle2,
} from 'lucide-react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  Tooltip,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Legend,
} from 'recharts';
import { toast } from 'sonner';

const CATEGORY_COLORS = [
  '#8B5CF6', '#EC4899', '#3B82F6', '#10B981', '#F59E0B',
  '#6366F1', '#14B8A6', '#F43F5E', '#84CC16', '#64748B',
];

export const DashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [asOfDate, setAsOfDate] = useState<string>('');
  const [emiSheetOpen, setEmiSheetOpen] = useState(false);
  const [loanSortOption, setLoanSortOption] = useState<LoanSortOption>('SMALLEST_BALANCE');

  // Query Dashboard Summary
  const { data: dashboard, isLoading: isDashboardLoading, error: dashboardError } = useQuery<DashboardSummary>({
    queryKey: ['dashboard', asOfDate],
    queryFn: async () => {
      const param = asOfDate ? `?asOfDate=${asOfDate}` : '';
      const res = await api.get(`/api/v1/dashboard${param}`);
      return res.data;
    },
  });

  // Query Active Loans for EMI list and Loan Closure Focus
  const { data: activeLoans, isLoading: isLoansLoading } = useQuery<Loan[]>({
    queryKey: ['dashboard-active-loans'],
    queryFn: async () => {
      const res = await api.get('/api/v1/loans?status=ACTIVE');
      return res.data;
    },
  });

  const handleToggleTodo = async (id: string) => {
    try {
      await api.patch(`/api/v1/todos/${id}/toggle`);
      toast.success('Task status updated');
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
    } catch {
      toast.error('Failed to update task');
    }
  };

  if (isDashboardLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-24 w-full rounded-2xl" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-44 rounded-2xl" />
          <Skeleton className="h-44 rounded-2xl" />
        </div>
        <Skeleton className="h-64 w-full rounded-2xl" />
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
          <Skeleton className="h-36 rounded-2xl" />
          <Skeleton className="h-36 rounded-2xl" />
        </div>
      </div>
    );
  }

  if (dashboardError || !dashboard) {
    return (
      <div className="p-8 text-center bg-rose-950/40 border border-rose-800/40 rounded-2xl">
        <AlertCircle className="w-10 h-10 text-rose-400 mx-auto mb-3" />
        <h3 className="text-lg font-bold text-rose-200">Failed to load financial dashboard</h3>
        <p className="text-xs text-rose-300 mt-1">Check backend connection or database status</p>
      </div>
    );
  }

  // Active loans calculations for sorting and display
  const computedActiveLoans = (activeLoans || []).map((loan) => ({
    loan,
    metrics: computeLoanMetrics(loan),
  }));

  // Sort for EMI BottomSheet (sorted by nearest EMI due date)
  const loansSortedByNearestEmi = [...computedActiveLoans].sort((a, b) => {
    const dateA = a.metrics.nextEmiDate;
    const dateB = b.metrics.nextEmiDate;
    if (!dateA) return 1;
    if (!dateB) return -1;
    return new Date(dateA).getTime() - new Date(dateB).getTime();
  });

  // Sort for Loan Closure Focus
  const focusLoans = sortLoansByFocus(activeLoans || [], loanSortOption)
    .slice(0, 3)
    .map((loan) => ({
      loan,
      metrics: computeLoanMetrics(loan),
    }));

  // Overall Principal Repaid progress
  const totalOriginal = dashboard.totalOriginalLoanPrincipal || 0;
  const totalOutstanding = dashboard.totalOutstandingLoanPrincipal || 0;
  const totalRepaidPrincipal = Math.max(0, totalOriginal - totalOutstanding);
  const overallPrincipalProgress =
    totalOriginal > 0 ? Math.min(100, Math.round((totalRepaidPrincipal / totalOriginal) * 100)) : 0;

  return (
    <div className="space-y-6 sm:space-y-8 animate-in fade-in duration-300">
      {/* Top Banner & Salary Cycle Controls */}
      <div className="glass-card p-5 sm:p-6 rounded-3xl border border-purple-500/30 bg-gradient-to-r from-slate-900/90 via-purple-950/20 to-slate-900/90 flex flex-col md:flex-row md:items-center justify-between gap-4 sm:gap-6 shadow-xl shadow-purple-950/20">
        <div className="space-y-1.5">
          <div className="flex items-center gap-2">
            <Badge variant="purple">Active Salary Cycle</Badge>
            <span className="text-xs text-slate-400 font-medium">
              {formatDate(dashboard.cycleStartDate)} — {formatDate(dashboard.cycleEndDate)}
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-100 font-heading tracking-tight">
            Financial Health Overview
          </h1>
          <p className="text-xs text-slate-400">
            Calculated as of <span className="text-purple-300 font-bold">{formatDate(dashboard.asOfDate)}</span>
          </p>
        </div>

        <div className="flex items-center gap-2.5 bg-slate-950/70 p-2.5 rounded-2xl border border-slate-800 self-start md:self-auto">
          <Calendar className="w-4 h-4 text-purple-400 shrink-0" />
          <label className="text-xs font-semibold text-slate-400 uppercase tracking-wider whitespace-nowrap">
            As Of Date:
          </label>
          <input
            type="date"
            value={asOfDate || dashboard.asOfDate}
            onChange={(e) => setAsOfDate(e.target.value)}
            className="bg-slate-900 border border-slate-700 text-slate-200 text-xs rounded-xl px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-purple-500"
          />
          {asOfDate && (
            <Button variant="ghost" size="sm" onClick={() => setAsOfDate('')}>
              Reset
            </Button>
          )}
        </div>
      </div>

      {/* Primary Loan KPI Cards (Grid of 2 prominent cards) */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
        {/* CARD A: Total Monthly EMIs (First & Most Prominent - Clickable) */}
        <Card
          className="p-6 relative overflow-hidden group hover:border-purple-400/60 active:scale-[0.99] transition-all duration-300 cursor-pointer border-purple-500/30 bg-gradient-to-br from-slate-900 via-purple-950/20 to-slate-900 shadow-xl shadow-purple-950/30"
          onClick={() => setEmiSheetOpen(true)}
          role="button"
          tabIndex={0}
          onKeyDown={(e) => e.key === 'Enter' && setEmiSheetOpen(true)}
        >
          <div className="flex items-start justify-between">
            <div className="space-y-1">
              <span className="text-xs font-bold text-purple-400 uppercase tracking-wider block">
                Total Monthly EMIs
              </span>
              <h2 className="text-3xl sm:text-4xl font-black text-slate-100 font-heading tracking-tight">
                {formatINR(dashboard.totalMonthlyEmi)}
                <span className="text-sm sm:text-base font-semibold text-slate-400 font-sans ml-1">/mo</span>
              </h2>
              <div className="flex items-center gap-2 pt-1">
                <Badge variant="purple" className="font-bold">
                  {dashboard.activeLoanCount} Active Loan{dashboard.activeLoanCount === 1 ? '' : 's'}
                </Badge>
                <span className="text-xs text-purple-300 group-hover:text-purple-200 flex items-center gap-1 font-semibold">
                  Tap to view EMI breakdown <ChevronRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition-transform" />
                </span>
              </div>
            </div>

            <div className="p-3.5 rounded-2xl bg-purple-600 text-white shadow-lg shadow-purple-900/50 group-hover:scale-110 transition-transform">
              <Landmark className="w-6 h-6" />
            </div>
          </div>
        </Card>

        {/* CARD B: Total Loan Principal Remaining (Second) */}
        <Card className="p-6 relative overflow-hidden border-purple-500/30 bg-gradient-to-br from-slate-900 via-indigo-950/20 to-slate-900 shadow-xl shadow-indigo-950/20">
          <div className="flex items-start justify-between">
            <div className="space-y-1 flex-1 pr-2">
              <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider block">
                Principal Remaining
              </span>
              <h2 className="text-3xl sm:text-4xl font-black text-indigo-300 font-heading tracking-tight">
                {formatINR(dashboard.totalOutstandingLoanPrincipal)}
              </h2>

              <div className="pt-2">
                <p className="text-xs font-semibold text-slate-300">
                  Remaining Loan Amount:{' '}
                  <span className="font-extrabold text-amber-300">
                    {formatINR(dashboard.totalRemainingRepayment)}
                  </span>
                </p>
                <p className="text-[11px] text-slate-400 mt-0.5">
                  {dashboard.totalRemainingEmiCount} scheduled EMIs commitment
                </p>
              </div>

              {/* Repayment Progress Bar */}
              <div className="pt-3">
                <div className="flex items-center justify-between text-[11px] font-semibold text-slate-400 mb-1">
                  <span>Repaid: {formatINR(totalRepaidPrincipal)}</span>
                  <span className="text-indigo-300 font-bold">{overallPrincipalProgress}%</span>
                </div>
                <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                  <div
                    className="h-full rounded-full bg-gradient-to-r from-purple-500 to-indigo-500 transition-all duration-500"
                    style={{ width: `${overallPrincipalProgress}%` }}
                  />
                </div>
              </div>
            </div>

            <div className="p-3.5 rounded-2xl bg-indigo-900/60 border border-indigo-700/50 text-indigo-300 shadow-md">
              <Percent className="w-6 h-6" />
            </div>
          </div>
        </Card>
      </div>

      {/* SECTION C: Loan Closure Focus (Third — The most important section) */}
      <section className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <Sparkles className="w-5 h-5 text-purple-400" />
              <h2 className="text-xl sm:text-2xl font-extrabold text-slate-100 font-heading tracking-tight">
                Close These Loans Faster
              </h2>
            </div>
            <p className="text-xs text-slate-400">
              Target high-impact loans to eliminate monthly EMI commitments and save on interest.
            </p>
          </div>

          {/* Transparent Sorting Controls */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0 scrollbar-none">
            {[
              { id: 'SMALLEST_BALANCE', label: 'Smallest Balance' },
              { id: 'HIGHEST_INTEREST', label: 'Highest Interest' },
              { id: 'NEAREST_CLOSURE', label: 'Nearest Closure' },
              { id: 'HIGHEST_EMI_FREED', label: 'Highest EMI Freed' },
            ].map((opt) => (
              <button
                key={opt.id}
                onClick={() => setLoanSortOption(opt.id as LoanSortOption)}
                className={cn(
                  'px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all min-h-[36px]',
                  loanSortOption === opt.id
                    ? 'bg-purple-600 text-white shadow-md shadow-purple-900/40 font-bold'
                    : 'bg-slate-900/90 text-slate-400 hover:text-slate-200 border border-slate-800'
                )}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </div>

        {/* Loan Focus Cards */}
        {focusLoans.length > 0 ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 sm:gap-6">
            {focusLoans.map(({ loan, metrics }) => (
              <Card
                key={loan.id}
                className="p-5 sm:p-6 border border-purple-500/30 hover:border-purple-400/60 transition-all duration-300 flex flex-col justify-between group bg-slate-900/90 shadow-xl"
              >
                <div className="space-y-4">
                  {/* Card Header with Name & EMI Freed Highlight */}
                  <div className="flex items-start justify-between gap-3">
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <Badge variant="purple" className="text-[10px]">
                          {loan.loanType.replace('_', ' ')}
                        </Badge>
                        <span className="text-xs text-slate-400 truncate">{loan.lenderOrBank}</span>
                      </div>
                      <h3 className="text-lg font-bold text-slate-100 font-heading truncate">
                        {loan.name}
                      </h3>
                    </div>

                    <div className="p-2.5 rounded-xl bg-purple-950/80 border border-purple-800/40 text-purple-300 shrink-0">
                      <Landmark className="w-5 h-5" />
                    </div>
                  </div>

                  {/* Highlight Banner: EMI Freed */}
                  <div className="p-3 rounded-xl bg-gradient-to-r from-purple-950/80 to-indigo-950/60 border border-purple-700/40">
                    <p className="text-xs font-bold text-purple-300 flex items-center gap-1.5">
                      <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0" />
                      Close to free <span className="text-white font-extrabold">{formatINR(metrics.monthlyEmiFreed)}/mo</span> EMI
                    </p>
                    <p className="text-[10px] text-purple-200/70 mt-0.5">{metrics.insight}</p>
                  </div>

                  {/* Key Metrics Grid */}
                  <div className="grid grid-cols-2 gap-3 py-1 text-xs border-y border-purple-900/20">
                    <div>
                      <span className="text-slate-400 block mb-0.5">Principal Remaining</span>
                      <span className="text-sm font-extrabold text-purple-300">
                        {formatINR(metrics.principalRemaining)}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Total Loan Balance</span>
                      <span className="text-sm font-extrabold text-amber-300">
                        {formatINR(metrics.totalLoanBalance)}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Remaining EMIs</span>
                      <span className="font-bold text-slate-200">
                        {metrics.remainingEmis} of {metrics.totalEmis}
                      </span>
                    </div>

                    <div>
                      <span className="text-slate-400 block mb-0.5">Interest Rate</span>
                      <span className="font-bold text-slate-200">
                        {metrics.annualInterestRate}% p.a.
                      </span>
                    </div>

                    <div className="col-span-2">
                      <span className="text-slate-400 block mb-0.5">Last EMI Date</span>
                      <span className="font-semibold text-slate-200">
                        {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'Not scheduled'}
                      </span>
                    </div>
                  </div>

                  {/* Progress Bar */}
                  <div>
                    <div className="flex items-center justify-between text-[11px] font-semibold text-slate-400 mb-1">
                      <span>Repayment Progress</span>
                      <span className="text-purple-300 font-bold">{metrics.progressPercent}%</span>
                    </div>
                    <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-purple-500"
                        style={{ width: `${metrics.progressPercent}%` }}
                      />
                    </div>
                  </div>
                </div>

                {/* View Loan Action Button */}
                <div className="pt-4 mt-2">
                  <Button
                    variant="outline"
                    size="sm"
                    className="w-full justify-between rounded-xl hover:border-purple-500/50"
                    onClick={() => navigate(`/loans/${loan.id}`)}
                  >
                    <span>View & Manage Loan</span>
                    <ArrowRight className="w-4 h-4 text-purple-400" />
                  </Button>
                </div>
              </Card>
            ))}
          </div>
        ) : (
          <Card className="p-8 text-center bg-slate-900/60 border border-slate-800">
            <ShieldCheck className="w-10 h-10 text-emerald-400 mx-auto mb-2" />
            <h4 className="text-base font-bold text-slate-100">No active loans!</h4>
            <p className="text-xs text-slate-400 mt-1">
              You are currently loan-free, or no active loans match the filter.
            </p>
            <Button
              variant="primary"
              size="sm"
              className="mt-4"
              onClick={() => navigate('/loans')}
            >
              Go to Loans Page
            </Button>
          </Card>
        )}
      </section>

      {/* SECTION D: Cycle Status & Other Financial Metrics */}
      <section className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 sm:gap-6">
          <Card className="p-5 sm:p-6 border border-slate-800">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Remaining Salary Today
                </span>
                <h3
                  className={cn(
                    'text-2xl sm:text-3xl font-extrabold tracking-tight',
                    dashboard.remainingSalaryToday >= 0 ? 'text-emerald-300' : 'text-rose-300'
                  )}
                >
                  {formatINR(dashboard.remainingSalaryToday)}
                </h3>
                <p className="text-xs text-slate-400 mt-1 font-medium">
                  Monthly salary minus obligations due on or before today
                </p>
              </div>
              <div className="p-3 rounded-2xl bg-emerald-950/60 border border-emerald-800/40 text-emerald-400">
                <Wallet className="w-5 h-5" />
              </div>
            </div>
          </Card>

          <Card className="p-5 sm:p-6 border border-slate-800">
            <div className="flex items-start justify-between">
              <div>
                <span className="text-xs font-bold text-slate-400 uppercase tracking-wider block mb-1">
                  Current Cycle Expenses
                </span>
                <h3 className="text-2xl sm:text-3xl font-extrabold text-amber-300 tracking-tight">
                  {formatINR(dashboard.totalExpensesCurrentCycle)}
                </h3>
                <p className="text-xs text-slate-400 mt-1 font-medium">
                  Fixed: {formatINR(dashboard.fixedExpensesCurrentCycle)} • Variable: {formatINR(dashboard.variableExpensesCurrentCycle)}
                </p>
              </div>
              <div className="p-3 rounded-2xl bg-amber-950/60 border border-amber-800/40 text-amber-400">
                <Receipt className="w-5 h-5" />
              </div>
            </div>
          </Card>
        </div>

        {/* Secondary Lists Grid: Upcoming 30-Day Timeline & Pending Tasks */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Upcoming 30-Day Timeline */}
          <Card className="lg:col-span-2">
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Clock className="w-5 h-5 text-purple-400" />
                Upcoming 30-Day Timeline
              </CardTitle>
              <CardDescription>Scheduled EMIs, fixed bills, and ledger collections</CardDescription>
            </CardHeader>
            <CardContent>
              {dashboard.upcomingPayments.length > 0 ? (
                <div className="space-y-3">
                  {dashboard.upcomingPayments.map((item) => (
                    <div
                      key={item.id}
                      className="p-3.5 sm:p-4 rounded-xl bg-slate-900/80 border border-slate-800 hover:border-purple-500/30 transition-all flex items-center justify-between gap-3"
                    >
                      <div className="flex items-center gap-3 min-w-0">
                        <div className="p-2 sm:p-2.5 rounded-xl bg-purple-950/80 border border-purple-800/50 text-purple-300 shrink-0">
                          {item.type === 'LOAN_EMI' ? (
                            <Landmark className="w-4 h-4" />
                          ) : item.type === 'FIXED_EXPENSE' ? (
                            <Receipt className="w-4 h-4" />
                          ) : (
                            <Circle className="w-4 h-4" />
                          )}
                        </div>
                        <div className="min-w-0">
                          <h4 className="text-sm font-bold text-slate-100 truncate">{item.title}</h4>
                          <p className="text-xs text-slate-400 truncate">
                            {item.categoryOrLender} • Due {formatDate(item.dueDate)}
                          </p>
                        </div>
                      </div>
                      <div className="text-right shrink-0">
                        <span className="text-sm font-extrabold text-purple-300 block">
                          {formatINR(item.amount)}
                        </span>
                        <Badge variant="purple" className="mt-1 text-[10px]">
                          {item.type.replace('_', ' ')}
                        </Badge>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-400 text-center py-8">
                  No upcoming payments in the next 30 days
                </p>
              )}
            </CardContent>
          </Card>

          {/* Pending Tasks Preview */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center justify-between">
                <span>Pending Tasks</span>
                <Badge variant="amber">{dashboard.pendingTodoCount} Pending</Badge>
              </CardTitle>
              <CardDescription>Financial reminders and to-do items</CardDescription>
            </CardHeader>
            <CardContent>
              {dashboard.pendingTodosPreview.length > 0 ? (
                <div className="space-y-2.5">
                  {dashboard.pendingTodosPreview.map((todo) => (
                    <div
                      key={todo.id}
                      className="p-3 rounded-xl bg-slate-900/80 border border-slate-800 flex items-center justify-between gap-2 group"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        <button
                          onClick={() => handleToggleTodo(todo.id)}
                          className="text-slate-500 hover:text-emerald-400 transition-colors shrink-0 min-h-[36px] min-w-[36px] flex items-center justify-center"
                          aria-label="Toggle task"
                        >
                          <Circle className="w-4 h-4" />
                        </button>
                        <div className="min-w-0">
                          <p className="text-xs font-bold text-slate-200 truncate">{todo.title}</p>
                          {todo.dueDate && (
                            <p className="text-[10px] text-slate-400">Due {formatDate(todo.dueDate)}</p>
                          )}
                        </div>
                      </div>
                      <Badge
                        variant={
                          todo.priority === 'HIGH'
                            ? 'rose'
                            : todo.priority === 'MEDIUM'
                            ? 'amber'
                            : 'slate'
                        }
                        className="text-[10px] shrink-0"
                      >
                        {todo.priority}
                      </Badge>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-slate-400 text-center py-8">No pending task reminders</p>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Visual Analytics Grid (Charts placed below loan & closure focus) */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 pt-2">
          {/* Expense Category Breakdown Chart */}
          <Card>
            <CardHeader>
              <CardTitle>Expense Distribution</CardTitle>
              <CardDescription>Category breakdown for current salary cycle</CardDescription>
            </CardHeader>
            <CardContent>
              {dashboard.categoryBreakdown.length > 0 ? (
                <div className="h-64 flex items-center justify-center">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={dashboard.categoryBreakdown}
                        dataKey="totalAmount"
                        nameKey="category"
                        cx="50%"
                        cy="50%"
                        innerRadius={55}
                        outerRadius={80}
                        paddingAngle={4}
                      >
                        {dashboard.categoryBreakdown.map((_, index) => (
                          <Cell
                            key={`cell-${index}`}
                            fill={CATEGORY_COLORS[index % CATEGORY_COLORS.length]}
                          />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(val: number) => [formatINR(val), 'Amount']}
                        contentStyle={{
                          backgroundColor: '#0f172a',
                          borderColor: '#334155',
                          borderRadius: '12px',
                          color: '#f8fafc',
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <p className="text-xs text-slate-400 text-center py-12">
                  No expense entries recorded in current cycle
                </p>
              )}
            </CardContent>
          </Card>

          {/* 6-Cycle Outflow Trend Chart */}
          <Card>
            <CardHeader>
              <CardTitle>6-Cycle Outflow Trend</CardTitle>
              <CardDescription>EMIs, Fixed, and Variable commitments comparison</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={dashboard.sixCycleTrend}
                    margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                    <XAxis dataKey="cycleLabel" stroke="#94a3b8" fontSize={11} />
                    <YAxis
                      stroke="#94a3b8"
                      fontSize={11}
                      tickFormatter={(v) => `₹${v / 1000}k`}
                    />
                    <Tooltip
                      formatter={(val: number) => formatINR(val)}
                      contentStyle={{
                        backgroundColor: '#0f172a',
                        borderColor: '#334155',
                        borderRadius: '12px',
                        color: '#f8fafc',
                      }}
                    />
                    <Legend />
                    <Bar dataKey="totalEmis" name="Loan EMIs" fill="#6366f1" radius={[4, 4, 0, 0]} />
                    <Bar dataKey="totalFixed" name="Fixed Expenses" fill="#8b5cf6" radius={[4, 4, 0, 0]} />
                    <Bar
                      dataKey="totalVariable"
                      name="Variable Expenses"
                      fill="#ec4899"
                      radius={[4, 4, 0, 0]}
                    />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>
        </div>
      </section>

      {/* EMI BREAKDOWN BOTTOM SHEET / DIALOG */}
      <BottomSheet
        isOpen={emiSheetOpen}
        onClose={() => setEmiSheetOpen(false)}
        title="Active Loan EMIs Breakdown"
        description={`${dashboard.activeLoanCount} active loans totaling ${formatINR(dashboard.totalMonthlyEmi)}/month, sorted by nearest due date`}
        maxWidth="lg"
      >
        <div className="space-y-3 pt-2">
          {loansSortedByNearestEmi.length > 0 ? (
            loansSortedByNearestEmi.map(({ loan, metrics }) => (
              <div
                key={loan.id}
                className="p-4 rounded-2xl bg-slate-950/70 border border-purple-900/30 hover:border-purple-500/50 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 group"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-bold text-slate-100 font-heading">
                      {loan.name}
                    </span>
                    <Badge variant="purple" className="text-[10px]">
                      {loan.lenderOrBank}
                    </Badge>
                  </div>

                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-slate-400">
                    <span>
                      Next Due:{' '}
                      <strong className="text-purple-300">
                        {metrics.nextEmiDate ? formatDate(metrics.nextEmiDate) : 'Due day ' + loan.emiDueDay}
                      </strong>
                    </span>
                    <span>•</span>
                    <span>
                      Remaining: <strong className="text-slate-200">{metrics.remainingEmis} EMIs</strong>
                    </span>
                    <span>•</span>
                    <span>
                      Last EMI:{' '}
                      <strong className="text-slate-200">
                        {metrics.lastEmiDate ? formatDate(metrics.lastEmiDate) : 'TBD'}
                      </strong>
                    </span>
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-3 pt-2 sm:pt-0 border-t sm:border-t-0 border-slate-800">
                  <div className="sm:text-right">
                    <span className="text-base font-extrabold text-slate-100 block">
                      {formatINR(metrics.emiAmount)}
                      <span className="text-xs font-normal text-slate-400">/mo</span>
                    </span>
                    <span className="text-[11px] text-amber-300 font-semibold block">
                      Balance: {formatINR(metrics.totalLoanBalance)}
                    </span>
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    className="rounded-xl shrink-0"
                    onClick={() => {
                      setEmiSheetOpen(false);
                      navigate(`/loans/${loan.id}`);
                    }}
                  >
                    <span>View Loan</span>
                    <ArrowRight className="w-3.5 h-3.5 ml-1 text-purple-400" />
                  </Button>
                </div>
              </div>
            ))
          ) : (
            <p className="text-xs text-slate-400 text-center py-6">No active loans found</p>
          )}
        </div>
      </BottomSheet>
    </div>
  );
};
