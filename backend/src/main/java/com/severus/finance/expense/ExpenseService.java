package com.severus.finance.expense;

import com.severus.finance.common.exception.ApiException;
import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.expense.dto.*;
import com.severus.finance.expense.enums.ExpenseType;
import com.severus.finance.loan.enums.PaymentState;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    @Inject
    public ExpenseService(ExpenseRepository expenseRepository, UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<ExpenseResponse> getExpenses(UUID userId, ExpenseType typeFilter) {
        List<Expense> list = typeFilter != null ?
                expenseRepository.findByUserIdAndType(userId, typeFilter) :
                expenseRepository.findByUserId(userId);
        return list.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ExpenseResponse getExpense(UUID id, UUID userId) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "EXPENSE_NOT_FOUND", "Expense not found"));
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse createExpense(UUID userId, CreateExpenseRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Expense expense = new Expense();
        expense.setId(UUID.randomUUID());
        expense.setUser(user);
        expense.setName(request.name());
        expense.setCategory(request.category());
        expense.setType(request.type());
        expense.setAmount(CurrencyUtils.format(request.amount()));
        expense.setExpenseDate(request.expenseDate());
        expense.setMonthlyDueDay(request.monthlyDueDay());
        expense.setStartDate(request.startDate());
        expense.setEndDate(request.endDate());
        expense.setActive(request.active() != null ? request.active() : true);
        expense.setPaymentState(request.paymentState() != null ? request.paymentState() : PaymentState.UNPAID);
        expense.setPaidDate(request.paidDate());
        expense.setNotes(request.notes());
        expense.setCreatedAt(Instant.now());
        expense.setUpdatedAt(Instant.now());

        expenseRepository.persist(expense);
        return toResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID id, UUID userId, UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "EXPENSE_NOT_FOUND", "Expense not found"));

        expense.setName(request.name());
        expense.setCategory(request.category());
        expense.setAmount(CurrencyUtils.format(request.amount()));
        expense.setExpenseDate(request.expenseDate());
        expense.setMonthlyDueDay(request.monthlyDueDay());
        expense.setStartDate(request.startDate());
        expense.setEndDate(request.endDate());
        if (request.active() != null) {
            expense.setActive(request.active());
        }
        if (request.paymentState() != null) {
            expense.setPaymentState(request.paymentState());
        }
        expense.setPaidDate(request.paidDate());
        expense.setNotes(request.notes());
        expense.setUpdatedAt(Instant.now());

        return toResponse(expense);
    }

    @Transactional
    public void deleteExpense(UUID id, UUID userId) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "EXPENSE_NOT_FOUND", "Expense not found"));
        expenseRepository.delete(expense);
    }

    @Transactional
    public ExpenseResponse toggleExpensePaymentState(UUID id, UUID userId, PaymentState state) {
        Expense expense = expenseRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "EXPENSE_NOT_FOUND", "Expense not found"));

        expense.setPaymentState(state);
        if (state == PaymentState.PAID) {
            expense.setPaidDate(java.time.LocalDate.now());
        } else {
            expense.setPaidDate(null);
        }
        expense.setUpdatedAt(Instant.now());
        return toResponse(expense);
    }

    public ExpenseResponse toResponse(Expense e) {
        return new ExpenseResponse(
                e.getId(),
                e.getName(),
                e.getCategory(),
                e.getType(),
                e.getAmount(),
                e.getExpenseDate(),
                e.getMonthlyDueDay(),
                e.getStartDate(),
                e.getEndDate(),
                e.isActive(),
                e.getPaymentState(),
                e.getPaidDate(),
                e.getNotes(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
