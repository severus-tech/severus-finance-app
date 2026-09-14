package com.severus.finance.dashboard;

import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.common.util.DateUtils;
import com.severus.finance.dashboard.dto.*;
import com.severus.finance.expense.Expense;
import com.severus.finance.expense.ExpenseRepository;
import com.severus.finance.expense.enums.ExpenseCategory;
import com.severus.finance.expense.enums.ExpenseType;
import com.severus.finance.ledger.LedgerEntry;
import com.severus.finance.ledger.LedgerEntryRepository;
import com.severus.finance.ledger.enums.LedgerDirection;
import com.severus.finance.ledger.enums.LedgerStatus;
import com.severus.finance.loan.Loan;
import com.severus.finance.loan.LoanInstallment;
import com.severus.finance.loan.LoanInstallmentRepository;
import com.severus.finance.loan.LoanRepository;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.PaymentState;
import com.severus.finance.settings.AppSettings;
import com.severus.finance.settings.AppSettingsService;
import com.severus.finance.todo.TodoItemRepository;
import com.severus.finance.todo.TodoItemService;
import com.severus.finance.todo.dto.TodoResponse;
import com.severus.finance.todo.enums.TodoStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class DashboardService {

    private final AppSettingsService settingsService;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final ExpenseRepository expenseRepository;
    private final LedgerEntryRepository ledgerRepository;
    private final TodoItemRepository todoRepository;
    private final TodoItemService todoService;

    @Inject
    public DashboardService(
            AppSettingsService settingsService,
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository,
            ExpenseRepository expenseRepository,
            LedgerEntryRepository ledgerRepository,
            TodoItemRepository todoRepository,
            TodoItemService todoService
    ) {
        this.settingsService = settingsService;
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.expenseRepository = expenseRepository;
        this.ledgerRepository = ledgerRepository;
        this.todoRepository = todoRepository;
        this.todoService = todoService;
    }

    @jakarta.transaction.Transactional
    public DashboardSummaryResponse getDashboardSummary(UUID userId, LocalDate asOfDateParam) {
        LocalDate asOfDate = asOfDateParam != null ? asOfDateParam : DateUtils.todayInKolkata();

        var settingsResponse = settingsService.getSettings(userId);
        BigDecimal monthlySalary = settingsResponse.monthlySalary();
        int salaryCreditDay = settingsResponse.salaryCreditDay();

        DateUtils.SalaryCycle currentCycle = DateUtils.calculateSalaryCycle(asOfDate, salaryCreditDay);
        LocalDate cycleStart = currentCycle.startDate();
        LocalDate cycleEnd = currentCycle.endDate();

        // 1. Loan Installment Calculations
        List<LoanInstallment> cycleInstallments = installmentRepository.findInstallmentsDueInDateRange(userId, cycleStart, cycleEnd);

        BigDecimal installmentsDueSoFar = BigDecimal.ZERO;
        BigDecimal totalCycleInstallments = BigDecimal.ZERO;

        for (LoanInstallment inst : cycleInstallments) {
            if (inst.getPaymentState() != PaymentState.SKIPPED) {
                totalCycleInstallments = totalCycleInstallments.add(inst.getScheduledAmount());
                if (!inst.getDueDate().isAfter(asOfDate)) {
                    installmentsDueSoFar = installmentsDueSoFar.add(inst.getScheduledAmount());
                }
            }
        }

        // Active Loan Aggregates
        List<Loan> activeLoans = loanRepository.findByUserIdAndStatus(userId, LoanStatus.ACTIVE);
        BigDecimal totalOriginalPrincipal = BigDecimal.ZERO;
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;
        BigDecimal totalRemainingRepayment = BigDecimal.ZERO;
        BigDecimal totalMonthlyEmi = BigDecimal.ZERO;
        int paidEmiCount = 0;
        int remainingEmiCount = 0;

        for (Loan loan : activeLoans) {
            totalOriginalPrincipal = totalOriginalPrincipal.add(loan.getOriginalPrincipal());
            totalOutstandingPrincipal = totalOutstandingPrincipal.add(loan.getCurrentOutstandingPrincipal());
            totalMonthlyEmi = totalMonthlyEmi.add(loan.getEmiAmount());

            List<LoanInstallment> insts = installmentRepository.findByLoanId(loan.getId());
            for (LoanInstallment i : insts) {
                if (i.getPaymentState() == PaymentState.PAID) {
                    paidEmiCount++;
                } else if (i.getPaymentState() == PaymentState.UNPAID) {
                    remainingEmiCount++;
                    totalRemainingRepayment = totalRemainingRepayment.add(i.getScheduledAmount());
                }
            }
        }

        BigDecimal estimatedRemainingInterest = CurrencyUtils.subtract(totalRemainingRepayment, totalOutstandingPrincipal);
        if (estimatedRemainingInterest.compareTo(BigDecimal.ZERO) < 0) {
            estimatedRemainingInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        // 2. Expense Calculations
        List<Expense> activeFixed = expenseRepository.findActiveFixedExpenses(userId);
        BigDecimal fixedExpensesDueSoFar = BigDecimal.ZERO;
        BigDecimal fixedExpensesCurrentCycle = BigDecimal.ZERO;

        for (Expense exp : activeFixed) {
            int dueDay = exp.getMonthlyDueDay() != null ? exp.getMonthlyDueDay() : 1;

            // Determine occurrence date for current cycle month(s)
            LocalDate occurrence = DateUtils.resolveDueDate(cycleStart.getYear(), cycleStart.getMonthValue(), dueDay);
            if (occurrence.isBefore(cycleStart)) {
                occurrence = DateUtils.resolveDueDate(cycleEnd.getYear(), cycleEnd.getMonthValue(), dueDay);
            }

            if (!occurrence.isBefore(cycleStart) && !occurrence.isAfter(cycleEnd)) {
                if (exp.getPaymentState() != PaymentState.SKIPPED) {
                    fixedExpensesCurrentCycle = fixedExpensesCurrentCycle.add(exp.getAmount());
                    if (!occurrence.isAfter(asOfDate)) {
                        fixedExpensesDueSoFar = fixedExpensesDueSoFar.add(exp.getAmount());
                    }
                }
            }
        }

        List<Expense> variableInCycle = expenseRepository.findVariableExpensesInDateRange(userId, cycleStart, cycleEnd);
        BigDecimal variableExpensesDueSoFar = BigDecimal.ZERO;
        BigDecimal variableExpensesCurrentCycle = BigDecimal.ZERO;

        for (Expense exp : variableInCycle) {
            variableExpensesCurrentCycle = variableExpensesCurrentCycle.add(exp.getAmount());
            if (!exp.getExpenseDate().isAfter(asOfDate)) {
                variableExpensesDueSoFar = variableExpensesDueSoFar.add(exp.getAmount());
            }
        }

        BigDecimal totalExpensesCurrentCycle = CurrencyUtils.add(fixedExpensesCurrentCycle, variableExpensesCurrentCycle);

        // 3. Salary Remaining Calculations
        BigDecimal remainingSalaryToday = monthlySalary
                .subtract(installmentsDueSoFar)
                .subtract(fixedExpensesDueSoFar)
                .subtract(variableExpensesDueSoFar)
                .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal salaryRemainingAfterAllScheduled = monthlySalary
                .subtract(totalCycleInstallments)
                .subtract(fixedExpensesCurrentCycle)
                .subtract(variableExpensesCurrentCycle)
                .setScale(2, RoundingMode.HALF_EVEN);

        // 4. Ledger Summaries
        List<LedgerEntry> ledgerEntries = ledgerRepository.findByUserId(userId);
        BigDecimal moneyLent = BigDecimal.ZERO;
        BigDecimal moneyBorrowed = BigDecimal.ZERO;

        for (LedgerEntry e : ledgerEntries) {
            if (e.getStatus() != LedgerStatus.SETTLED) {
                if (e.getDirection() == LedgerDirection.LENT) {
                    moneyLent = moneyLent.add(e.getRemainingBalance());
                } else {
                    moneyBorrowed = moneyBorrowed.add(e.getRemainingBalance());
                }
            }
        }

        // 5. Upcoming Payments Timeline (Next 30 days)
        LocalDate thirtyDaysLater = asOfDate.plusDays(30);
        List<UpcomingPaymentResponse> upcoming = new ArrayList<>();

        for (Loan loan : activeLoans) {
            List<LoanInstallment> insts = installmentRepository.findByLoanId(loan.getId());
            for (LoanInstallment i : insts) {
                if (i.getPaymentState() == PaymentState.UNPAID && !i.getDueDate().isBefore(asOfDate) && !i.getDueDate().isAfter(thirtyDaysLater)) {
                    upcoming.add(new UpcomingPaymentResponse(
                            i.getId(),
                            loan.getName() + " (EMI #" + i.getInstallmentNumber() + ")",
                            "LOAN_EMI",
                            i.getScheduledAmount(),
                            i.getDueDate(),
                            false,
                            loan.getLenderOrBank()
                    ));
                }
            }
        }

        for (Expense exp : activeFixed) {
            int dueDay = exp.getMonthlyDueDay() != null ? exp.getMonthlyDueDay() : 1;
            LocalDate occ = DateUtils.resolveDueDate(asOfDate.getYear(), asOfDate.getMonthValue(), dueDay);
            if (occ.isBefore(asOfDate)) {
                LocalDate nextM = asOfDate.plusMonths(1);
                occ = DateUtils.resolveDueDate(nextM.getYear(), nextM.getMonthValue(), dueDay);
            }

            if (!occ.isBefore(asOfDate) && !occ.isAfter(thirtyDaysLater)) {
                upcoming.add(new UpcomingPaymentResponse(
                        exp.getId(),
                        exp.getName(),
                        "FIXED_EXPENSE",
                        exp.getAmount(),
                        occ,
                        false,
                        exp.getCategory().name()
                ));
            }
        }

        for (LedgerEntry e : ledgerEntries) {
            if (e.getStatus() != LedgerStatus.SETTLED && e.getDueDate() != null) {
                if (!e.getDueDate().isBefore(asOfDate) && !e.getDueDate().isAfter(thirtyDaysLater)) {
                    upcoming.add(new UpcomingPaymentResponse(
                            e.getId(),
                            (e.getDirection() == LedgerDirection.LENT ? "Collect from " : "Pay to ") + e.getPersonName(),
                            "LEDGER_DUE",
                            e.getRemainingBalance(),
                            e.getDueDate(),
                            false,
                            e.getDirection().name()
                    ));
                }
            }
        }

        upcoming.sort(Comparator.comparing(UpcomingPaymentResponse::dueDate));

        // 6. Overdue Items
        List<UpcomingPaymentResponse> overdue = new ArrayList<>();
        for (Loan loan : activeLoans) {
            List<LoanInstallment> insts = installmentRepository.findByLoanId(loan.getId());
            for (LoanInstallment i : insts) {
                if (i.getPaymentState() == PaymentState.UNPAID && i.getDueDate().isBefore(asOfDate)) {
                    overdue.add(new UpcomingPaymentResponse(
                            i.getId(),
                            loan.getName() + " (EMI #" + i.getInstallmentNumber() + ")",
                            "LOAN_EMI",
                            i.getScheduledAmount(),
                            i.getDueDate(),
                            true,
                            loan.getLenderOrBank()
                    ));
                }
            }
        }

        for (LedgerEntry e : ledgerEntries) {
            if (e.getStatus() != LedgerStatus.SETTLED && e.getDueDate() != null && e.getDueDate().isBefore(asOfDate)) {
                overdue.add(new UpcomingPaymentResponse(
                        e.getId(),
                        (e.getDirection() == LedgerDirection.LENT ? "Overdue collect from " : "Overdue pay to ") + e.getPersonName(),
                        "LEDGER_DUE",
                        e.getRemainingBalance(),
                        e.getDueDate(),
                        true,
                        e.getDirection().name()
                ));
            }
        }
        overdue.sort(Comparator.comparing(UpcomingPaymentResponse::dueDate));

        // 7. Category Breakdown for current cycle
        Map<ExpenseCategory, BigDecimal> categoryMap = new HashMap<>();
        for (Expense exp : activeFixed) {
            categoryMap.merge(exp.getCategory(), exp.getAmount(), CurrencyUtils::add);
        }
        for (Expense exp : variableInCycle) {
            categoryMap.merge(exp.getCategory(), exp.getAmount(), CurrencyUtils::add);
        }

        BigDecimal grandCategoryTotal = categoryMap.values().stream().reduce(BigDecimal.ZERO, CurrencyUtils::add);
        List<ExpenseCategoryBreakdownResponse> categoryBreakdown = new ArrayList<>();

        categoryMap.forEach((cat, amt) -> {
            double pct = grandCategoryTotal.compareTo(BigDecimal.ZERO) > 0 ?
                    amt.divide(grandCategoryTotal, 4, RoundingMode.HALF_EVEN).doubleValue() * 100 : 0.0;
            categoryBreakdown.add(new ExpenseCategoryBreakdownResponse(cat.name(), CurrencyUtils.format(amt), Math.round(pct * 10.0) / 10.0));
        });
        categoryBreakdown.sort(Comparator.comparing(ExpenseCategoryBreakdownResponse::totalAmount).reversed());

        // 8. Six Cycle Trend
        List<CycleExpenseTrendResponse> sixCycleTrend = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (int c = 5; c >= 0; c--) {
            LocalDate refDate = asOfDate.minusMonths(c);
            DateUtils.SalaryCycle sc = DateUtils.calculateSalaryCycle(refDate, salaryCreditDay);

            List<Expense> vars = expenseRepository.findVariableExpensesInDateRange(userId, sc.startDate(), sc.endDate());
            BigDecimal varSum = vars.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, CurrencyUtils::add);

            BigDecimal fixedSum = activeFixed.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, CurrencyUtils::add);
            List<LoanInstallment> instList = installmentRepository.findInstallmentsDueInDateRange(userId, sc.startDate(), sc.endDate());
            BigDecimal emiSum = instList.stream().map(LoanInstallment::getScheduledAmount).reduce(BigDecimal.ZERO, CurrencyUtils::add);

            BigDecimal tot = CurrencyUtils.add(CurrencyUtils.add(varSum, fixedSum), emiSum);

            sixCycleTrend.add(new CycleExpenseTrendResponse(
                    sc.startDate().format(fmt),
                    sc.startDate(),
                    sc.endDate(),
                    CurrencyUtils.format(tot),
                    CurrencyUtils.format(emiSum),
                    CurrencyUtils.format(fixedSum),
                    CurrencyUtils.format(varSum)
            ));
        }

        // 9. Pending Todos
        List<TodoResponse> pendingTodosPreview = todoService.getTodos(userId, TodoStatus.PENDING).stream().limit(5).toList();
        long pendingTodoCount = todoRepository.countPendingByUserId(userId);

        return new DashboardSummaryResponse(
                asOfDate,
                cycleStart,
                cycleEnd,
                CurrencyUtils.format(monthlySalary),
                CurrencyUtils.format(remainingSalaryToday),
                CurrencyUtils.format(salaryRemainingAfterAllScheduled),
                CurrencyUtils.format(totalOriginalPrincipal),
                CurrencyUtils.format(totalOutstandingPrincipal),
                CurrencyUtils.format(totalRemainingRepayment),
                CurrencyUtils.format(estimatedRemainingInterest),
                activeLoans.size(),
                CurrencyUtils.format(totalMonthlyEmi),
                paidEmiCount,
                remainingEmiCount,
                CurrencyUtils.format(fixedExpensesCurrentCycle),
                CurrencyUtils.format(fixedExpensesDueSoFar),
                CurrencyUtils.format(variableExpensesCurrentCycle),
                CurrencyUtils.format(totalExpensesCurrentCycle),
                CurrencyUtils.format(moneyLent),
                CurrencyUtils.format(moneyBorrowed),
                upcoming,
                overdue,
                categoryBreakdown,
                sixCycleTrend,
                pendingTodosPreview,
                pendingTodoCount
        );
    }
}
