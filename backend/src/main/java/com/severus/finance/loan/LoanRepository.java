package com.severus.finance.loan;

import com.severus.finance.loan.enums.LoanStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LoanRepository implements PanacheRepositoryBase<Loan, UUID> {

    public List<Loan> findByUserId(UUID userId) {
        return list("user.id = ?1 order by createdAt desc", userId);
    }

    public List<Loan> findByUserIdAndStatus(UUID userId, LoanStatus status) {
        return list("user.id = ?1 and status = ?2 order by createdAt desc", userId, status);
    }

    public Optional<Loan> findByIdAndUserId(UUID id, UUID userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }
}
