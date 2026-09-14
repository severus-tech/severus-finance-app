package com.severus.finance.ledger;

import com.severus.finance.ledger.enums.LedgerDirection;
import com.severus.finance.ledger.enums.LedgerStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class LedgerEntryRepository implements PanacheRepositoryBase<LedgerEntry, UUID> {

    public List<LedgerEntry> findByUserId(UUID userId) {
        return list("user.id = ?1 order by entryDate desc, createdAt desc", userId);
    }

    public List<LedgerEntry> findByUserIdAndDirection(UUID userId, LedgerDirection direction) {
        return list("user.id = ?1 and direction = ?2 order by entryDate desc, createdAt desc", userId, direction);
    }

    public List<LedgerEntry> findByUserIdAndStatus(UUID userId, LedgerStatus status) {
        return list("user.id = ?1 and status = ?2 order by entryDate desc, createdAt desc", userId, status);
    }

    public Optional<LedgerEntry> findByIdAndUserId(UUID id, UUID userId) {
        return find("id = ?1 and user.id = ?2", id, userId).firstResultOptional();
    }
}
