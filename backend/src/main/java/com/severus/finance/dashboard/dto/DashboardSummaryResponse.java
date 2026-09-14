package com.severus.finance.dashboard.dto;

import com.severus.finance.loan.dto.LoanSummaryResponse;
import com.severus.finance.todo.dto.TodoResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSummaryResponse(
    LocalDate asOfDate,
    LocalDate cycleStartDate,
    LocalDate cycleEndDate,

    BigDecimal monthlySalary,
    BigDecimal remainingSalaryToday,
    BigDecimal salaryRemainingAfterAllScheduledCommitments,

    // Loan Summaries
    BigDecimal totalOriginalLoanPrincipal,
    BigDecimal totalOutstandingLoanPrincipal,
    BigDecimal totalRemainingRepayment,
    BigDecimal totalEstimatedRemainingInterest,
    long activeLoanCount,
    BigDecimal totalMonthlyEmi,
    int totalPaidEmiCount,
    int totalRemainingEmiCount,

    // Expense Summaries
    BigDecimal fixedExpensesCurrentCycle,
    BigDecimal fixedExpensesDueSoFar,
    BigDecimal variableExpensesCurrentCycle,
    BigDecimal totalExpensesCurrentCycle,

    // Ledger Summaries
    BigDecimal moneyLentReceivable,
    BigDecimal moneyBorrowedPayable,

    // Items and timelines
    List<UpcomingPaymentResponse> upcomingPayments,
    List<UpcomingPaymentResponse> overdueItems,
    List<ExpenseCategoryBreakdownResponse> categoryBreakdown,
    List<CycleExpenseTrendResponse> sixCycleTrend,
    List<TodoResponse> pendingTodosPreview,
    long pendingTodoCount
) {}
