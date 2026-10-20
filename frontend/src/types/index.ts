export interface User {
  id: string;
  email: string;
  displayName: string;
}

export interface AppSettings {
  monthlySalary: number;
  salaryCreditDay: number;
  currency: string;
  timezone: string;
  displayName: string;
  themePreference: 'LIGHT' | 'DARK';
  createdAt: string;
  updatedAt: string;
}

export type LoanType = 'HOME_LOAN' | 'PERSONAL_LOAN' | 'CAR_LOAN' | 'EDUCATION_LOAN' | 'CREDIT_CARD' | 'OTHER';
export type CalculationMode = 'REDUCING_BALANCE' | 'MANUAL';
export type LoanStatus = 'ACTIVE' | 'PAUSED' | 'CLOSED';
export type PaymentState = 'UNPAID' | 'PAID' | 'SKIPPED';

export interface LoanInstallment {
  id: string;
  loanId: string;
  installmentNumber: number;
  dueDate: string;
  scheduledAmount: number;
  principalComponent: number;
  interestComponent: number;
  paymentState: PaymentState;
  overdue?: boolean;
  paidDate?: string;
  actualPaidAmount?: number;
  notes?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface Loan {
  id: string;
  name: string;
  lenderOrBank: string;
  loanType: LoanType;
  maskedAccountNumber?: string;
  originalPrincipal: number;
  currentOutstandingPrincipal: number;
  remainingPrincipal?: number;
  remainingRepayment: number;
  remainingLoanAmount?: number;
  estimatedRemainingInterest: number;
  annualInterestRate: number;
  startDate: string;
  firstEmiDate: string;
  endDate?: string;
  nextEmiDate?: string | null;
  emiDueDay: number;
  emiAmount: number;
  originalTenureMonths: number;
  totalEmiCount: number;
  paidEmiCount: number;
  elapsedEmiCount?: number;
  remainingEmiCount: number;
  overdueEmiCount?: number;
  calculationMode: CalculationMode;
  outstandingPrincipalManualOverride: boolean;
  status: LoanStatus;
  notes?: string;
  version: number;
  installments?: LoanInstallment[];
  createdAt: string;
  updatedAt: string;
}

export interface LoanSummary {
  totalOriginalPrincipal: number;
  totalOutstandingPrincipal: number;
  totalRemainingRepayment: number;
  totalEstimatedRemainingInterest: number;
  totalMonthlyEmi: number;
  activeLoanCount: number;
  totalPaidEmiCount: number;
  totalRemainingEmiCount: number;
}

export type ExpenseType = 'FIXED_RECURRING' | 'VARIABLE';
export type ExpenseCategory =
  | 'RENT'
  | 'ELECTRICITY'
  | 'INTERNET'
  | 'MOBILE_BILL'
  | 'INSURANCE'
  | 'OTT_SUBSCRIPTION'
  | 'SOFTWARE_SUBSCRIPTION'
  | 'MAINTENANCE'
  | 'FOOD'
  | 'TRAVEL'
  | 'SHOPPING'
  | 'MEDICAL'
  | 'ENTERTAINMENT'
  | 'FUEL'
  | 'OTHER';

export interface Expense {
  id: string;
  name: string;
  category: ExpenseCategory;
  type: ExpenseType;
  amount: number;
  expenseDate: string;
  monthlyDueDay?: number;
  startDate?: string;
  endDate?: string;
  active: boolean;
  paymentState: PaymentState;
  paidDate?: string;
  notes?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export type LedgerDirection = 'LENT' | 'BORROWED';
export type LedgerStatus = 'OPEN' | 'PARTIALLY_SETTLED' | 'SETTLED';

export interface LedgerPayment {
  id: string;
  ledgerEntryId: string;
  amount: number;
  paymentDate: string;
  notes?: string;
  createdAt: string;
}

export interface LedgerEntry {
  id: string;
  personName: string;
  direction: LedgerDirection;
  originalAmount: number;
  remainingBalance: number;
  entryDate: string;
  dueDate?: string;
  description?: string;
  status: LedgerStatus;
  notes?: string;
  version: number;
  payments?: LedgerPayment[];
  createdAt: string;
  updatedAt: string;
}

export interface LedgerSummary {
  totalLentReceivable: number;
  totalBorrowedPayable: number;
  openLentCount: number;
  openBorrowedCount: number;
}

export type TodoPriority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TodoStatus = 'PENDING' | 'COMPLETED';

export interface TodoItem {
  id: string;
  title: string;
  description?: string;
  dueDate?: string;
  priority: TodoPriority;
  status: TodoStatus;
  completedAt?: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface UpcomingPayment {
  id: string;
  title: string;
  type: 'LOAN_EMI' | 'FIXED_EXPENSE' | 'LEDGER_DUE';
  amount: number;
  dueDate: string;
  overdue: boolean;
  categoryOrLender: string;
}

export interface ExpenseCategoryBreakdown {
  category: string;
  totalAmount: number;
  percentage: number;
}

export interface CycleExpenseTrend {
  cycleLabel: string;
  cycleStart: string;
  cycleEnd: string;
  totalExpenses: number;
  totalEmis: number;
  totalFixed: number;
  totalVariable: number;
}

export interface DashboardSummary {
  asOfDate: string;
  cycleStartDate: string;
  cycleEndDate: string;
  monthlySalary: number;
  remainingSalaryToday: number;
  salaryRemainingAfterAllScheduledCommitments: number;
  totalOriginalLoanPrincipal: number;
  totalOutstandingLoanPrincipal: number;
  totalRemainingRepayment: number;
  totalEstimatedRemainingInterest: number;
  activeLoanCount: number;
  totalMonthlyEmi: number;
  totalPaidEmiCount: number;
  totalRemainingEmiCount: number;
  fixedExpensesCurrentCycle: number;
  fixedExpensesDueSoFar: number;
  variableExpensesCurrentCycle: number;
  totalExpensesCurrentCycle: number;
  moneyLentReceivable: number;
  moneyBorrowedPayable: number;
  upcomingPayments: UpcomingPayment[];
  overdueItems: UpcomingPayment[];
  categoryBreakdown: ExpenseCategoryBreakdown[];
  sixCycleTrend: CycleExpenseTrend[];
  pendingTodosPreview: TodoItem[];
  pendingTodoCount: number;
}

export interface ApiError {
  timestamp: string;
  status: number;
  errorCode: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
  correlationId: string;
}
