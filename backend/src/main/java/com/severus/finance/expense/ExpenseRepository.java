package com.severus.finance.expense;

import com.severus.finance.expense.enums.ExpenseType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExpenseRepository implements PanacheRepositoryBase<Expense, UUID> {

    public List<Expense> findByUserId(UUID userId) {
        return list("user.id = ?1 order by expenseDate desc, createdAt desc", userId);
    }

    public List<Expense> findByUserIdAndType(UUID userId, ExpenseType type) {
        return list("user.id = ?1 and type = ?2 order by expenseDate desc, createdAt desc", userId, type);
    }

    public Optional<Expense> findByIdAndUserId(UUID id, UUID userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }

    public List<Expense> findActiveFixedExpenses(UUID userId) {
        return list("user.id = ?1 and type = 'FIXED_RECURRING' and active = true", userId);
    }

    public List<Expense> findVariableExpensesInDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        return list("user.id = ?1 and type = 'VARIABLE' and expenseDate >= ?2 and expenseDate <= ?3", userId, startDate, endDate);
    }
}
