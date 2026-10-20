package com.severus.finance.loan;

import com.severus.finance.loan.enums.PaymentState;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LoanInstallmentRepository implements PanacheRepositoryBase<LoanInstallment, UUID> {

    public List<LoanInstallment> findByLoanId(UUID loanId) {
        return list("loan.id = ?1 order by installmentNumber asc", loanId);
    }

    public List<LoanInstallment> findUnpaidByLoanId(UUID loanId) {
        return list("loan.id = ?1 and paymentState = ?2 order by installmentNumber asc", loanId, PaymentState.UNPAID);
    }

    public Optional<LoanInstallment> findByLoanIdAndNumber(UUID loanId, int installmentNumber) {
        return find("loan.id = ?1 and installmentNumber = ?2", loanId, installmentNumber).firstResultOptional();
    }

    public List<LoanInstallment> findInstallmentsDueInDateRange(UUID userId, LocalDate startDate, LocalDate endDate) {
        return list("loan.user.id = ?1 and loan.status = 'ACTIVE' and dueDate >= ?2 and dueDate <= ?3 order by dueDate asc", userId, startDate, endDate);
    }

    public long deleteUnpaidFutureInstallments(UUID loanId) {
        return delete("loan.id = ?1 and paymentState = 'UNPAID'", loanId);
    }

    public long deleteByLoanId(UUID loanId) {
        return delete("loan.id = ?1", loanId);
    }
}
