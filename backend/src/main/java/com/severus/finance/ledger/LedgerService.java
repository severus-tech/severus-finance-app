package com.severus.finance.ledger;

import com.severus.finance.common.exception.ApiException;
import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.ledger.dto.*;
import com.severus.finance.ledger.enums.LedgerDirection;
import com.severus.finance.ledger.enums.LedgerStatus;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LedgerService {

    private final LedgerEntryRepository entryRepository;
    private final LedgerPaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @Inject
    public LedgerService(
            LedgerEntryRepository entryRepository,
            LedgerPaymentRepository paymentRepository,
            UserRepository userRepository
    ) {
        this.entryRepository = entryRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<LedgerEntryResponse> getLedgerEntries(UUID userId, LedgerDirection direction, LedgerStatus status) {
        List<LedgerEntry> list;
        if (direction != null) {
            list = entryRepository.findByUserIdAndDirection(userId, direction);
        } else if (status != null) {
            list = entryRepository.findByUserIdAndStatus(userId, status);
        } else {
            list = entryRepository.findByUserId(userId);
        }
        return list.stream().map(this::toResponseWithPayments).toList();
    }

    @Transactional
    public LedgerEntryResponse getLedgerEntry(UUID id, UUID userId) {
        LedgerEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LEDGER_ENTRY_NOT_FOUND", "Ledger entry not found"));
        return toResponseWithPayments(entry);
    }

    @Transactional
    public LedgerEntryResponse createLedgerEntry(UUID userId, CreateLedgerEntryRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        LedgerEntry entry = new LedgerEntry();
        entry.setId(UUID.randomUUID());
        entry.setUser(user);
        entry.setPersonName(request.personName());
        entry.setDirection(request.direction());

        BigDecimal amount = CurrencyUtils.format(request.originalAmount());
        entry.setOriginalAmount(amount);
        entry.setRemainingBalance(amount);
        entry.setEntryDate(request.entryDate());
        entry.setDueDate(request.dueDate());
        entry.setDescription(request.description());
        entry.setStatus(LedgerStatus.OPEN);
        entry.setNotes(request.notes());
        entry.setCreatedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());

        entryRepository.persist(entry);
        return toResponseWithPayments(entry);
    }

    @Transactional
    public LedgerEntryResponse updateLedgerEntry(UUID id, UUID userId, UpdateLedgerEntryRequest request) {
        LedgerEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LEDGER_ENTRY_NOT_FOUND", "Ledger entry not found"));

        entry.setPersonName(request.personName());
        entry.setDirection(request.direction());
        entry.setEntryDate(request.entryDate());
        entry.setDueDate(request.dueDate());
        entry.setDescription(request.description());
        entry.setNotes(request.notes());
        entry.setUpdatedAt(Instant.now());

        return toResponseWithPayments(entry);
    }

    @Transactional
    public LedgerEntryResponse recordPayment(UUID entryId, UUID userId, RecordLedgerPaymentRequest request) {
        LedgerEntry entry = entryRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LEDGER_ENTRY_NOT_FOUND", "Ledger entry not found"));

        if (entry.getStatus() == LedgerStatus.SETTLED) {
            throw new ApiException(Response.Status.BAD_REQUEST, "ALREADY_SETTLED", "Ledger entry is already settled. Reopen first if adding payments.");
        }

        BigDecimal paymentAmt = CurrencyUtils.format(request.amount());
        if (paymentAmt.compareTo(entry.getRemainingBalance()) > 0 && !Boolean.TRUE.equals(request.allowOverpayment())) {
            throw new ApiException(Response.Status.BAD_REQUEST, "OVERPAYMENT_NOT_ALLOWED",
                    "Payment amount exceeds remaining balance (" + entry.getRemainingBalance() + "). Pass allowOverpayment=true to confirm adjustment.");
        }

        LedgerPayment payment = new LedgerPayment();
        payment.setId(UUID.randomUUID());
        payment.setLedgerEntry(entry);
        payment.setAmount(paymentAmt);
        payment.setPaymentDate(request.paymentDate());
        payment.setNotes(request.notes());
        payment.setCreatedAt(Instant.now());

        paymentRepository.persist(payment);

        BigDecimal newBalance = CurrencyUtils.subtract(entry.getRemainingBalance(), paymentAmt);
        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            newBalance = BigDecimal.ZERO.setScale(2, CurrencyUtils.DEFAULT_ROUNDING);
            entry.setStatus(LedgerStatus.SETTLED);
        } else {
            entry.setStatus(LedgerStatus.PARTIALLY_SETTLED);
        }

        entry.setRemainingBalance(newBalance);
        entry.setUpdatedAt(Instant.now());

        return toResponseWithPayments(entry);
    }

    @Transactional
    public LedgerEntryResponse reopenEntry(UUID id, UUID userId) {
        LedgerEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LEDGER_ENTRY_NOT_FOUND", "Ledger entry not found"));

        List<LedgerPayment> payments = paymentRepository.findByLedgerEntryId(id);
        BigDecimal totalPaid = payments.stream().map(LedgerPayment::getAmount).reduce(BigDecimal.ZERO, CurrencyUtils::add);
        BigDecimal newBalance = CurrencyUtils.subtract(entry.getOriginalAmount(), totalPaid);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            entry.setStatus(LedgerStatus.SETTLED);
        } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
            entry.setStatus(LedgerStatus.PARTIALLY_SETTLED);
        } else {
            entry.setStatus(LedgerStatus.OPEN);
        }

        entry.setRemainingBalance(newBalance);
        entry.setUpdatedAt(Instant.now());
        return toResponseWithPayments(entry);
    }

    @Transactional
    public void deleteLedgerEntry(UUID id, UUID userId) {
        LedgerEntry entry = entryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LEDGER_ENTRY_NOT_FOUND", "Ledger entry not found"));
        entryRepository.delete(entry);
    }

    @Transactional
    public LedgerSummaryResponse getLedgerSummary(UUID userId) {
        List<LedgerEntry> entries = entryRepository.findByUserId(userId);

        BigDecimal totalLent = BigDecimal.ZERO;
        BigDecimal totalBorrowed = BigDecimal.ZERO;
        long openLent = 0;
        long openBorrowed = 0;

        for (LedgerEntry e : entries) {
            if (e.getStatus() != LedgerStatus.SETTLED) {
                if (e.getDirection() == LedgerDirection.LENT) {
                    totalLent = totalLent.add(e.getRemainingBalance());
                    openLent++;
                } else {
                    totalBorrowed = totalBorrowed.add(e.getRemainingBalance());
                    openBorrowed++;
                }
            }
        }

        return new LedgerSummaryResponse(
                CurrencyUtils.format(totalLent),
                CurrencyUtils.format(totalBorrowed),
                openLent,
                openBorrowed
        );
    }

    public LedgerEntryResponse toResponseWithPayments(LedgerEntry e) {
        List<LedgerPayment> payments = paymentRepository.findByLedgerEntryId(e.getId());
        List<LedgerPaymentResponse> paymentResponses = payments.stream()
                .map(p -> new LedgerPaymentResponse(
                        p.getId(),
                        p.getLedgerEntry().getId(),
                        p.getAmount(),
                        p.getPaymentDate(),
                        p.getNotes(),
                        p.getCreatedAt()
                )).toList();

        return new LedgerEntryResponse(
                e.getId(),
                e.getPersonName(),
                e.getDirection(),
                e.getOriginalAmount(),
                e.getRemainingBalance(),
                e.getEntryDate(),
                e.getDueDate(),
                e.getDescription(),
                e.getStatus(),
                e.getNotes(),
                e.getVersion(),
                paymentResponses,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
