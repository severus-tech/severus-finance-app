package com.severus.finance.ledger;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LedgerPaymentRepository implements PanacheRepositoryBase<LedgerPayment, UUID> {

    public List<LedgerPayment> findByLedgerEntryId(UUID entryId) {
        return list("ledgerEntry.id = ?1 order by paymentDate asc, createdAt asc", entryId);
    }
}
