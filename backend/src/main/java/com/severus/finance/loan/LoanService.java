package com.severus.finance.loan;

import com.severus.finance.common.exception.ApiException;
import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.common.util.DateUtils;
import com.severus.finance.loan.dto.*;
import com.severus.finance.loan.enums.CalculationMode;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.PaymentState;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@ApplicationScoped
public class LoanService {

    private static final Logger LOG = Logger.getLogger(LoanService.class);

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final UserRepository userRepository;

    @Inject
    public LoanService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<LoanResponse> getLoans(UUID userId, LoanStatus statusFilter) {
        List<Loan> loans = (statusFilter != null) ?
                loanRepository.findByUserIdAndStatus(userId, statusFilter) :
                loanRepository.findByUserId(userId);

        return loans.stream().map(this::toLoanResponseWithSchedule).toList();
    }

    @Transactional
    public LoanResponse getLoanDetails(UUID id, UUID userId) {
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        // Safe auto-repair: if existing stored installments are incomplete, reconcile with authoritative tenure
        List<LoanInstallment> existing = installmentRepository.findByLoanId(loan.getId());
        if (existing.size() < loan.getTotalEmiCount()) {
            LOG.infof("Loan %s has incomplete schedule (%d of %d). Reconciling to full tenure...",
                    loan.getId(), existing.size(), loan.getTotalEmiCount());
            reconcileSchedule(loan);
        }

        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public LoanResponse createLoan(UUID userId, CreateLoanRequest request) {
        User user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "USER_NOT_FOUND", "User not found"));

        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setUser(user);
        loan.setName(request.name());
        loan.setLenderOrBank(request.lenderOrBank());
        loan.setLoanType(request.loanType());
        loan.setMaskedAccountNumber(request.maskedAccountNumber());

        BigDecimal origPrincipal = CurrencyUtils.format(request.originalPrincipal());
        loan.setOriginalPrincipal(origPrincipal);

        boolean manualOverride = request.currentOutstandingPrincipal() != null || Boolean.TRUE.equals(request.manualOverride());
        BigDecimal currentPrincipal = request.currentOutstandingPrincipal() != null ?
                CurrencyUtils.format(request.currentOutstandingPrincipal()) : origPrincipal;
        loan.setCurrentOutstandingPrincipal(currentPrincipal);
        loan.setOutstandingPrincipalManualOverride(manualOverride);

        loan.setAnnualInterestRate(request.annualInterestRate());
        loan.setStartDate(request.startDate());
        LocalDate firstEmiDate = request.firstEmiDate() != null ? request.firstEmiDate() : request.startDate();
        loan.setFirstEmiDate(firstEmiDate);
        loan.setEmiDueDay(request.emiDueDay());

        BigDecimal emi = CurrencyUtils.format(request.emiAmount());
        loan.setEmiAmount(emi);

        int tenure = request.originalTenureMonths() > 0 ? request.originalTenureMonths() : 1;
        loan.setOriginalTenureMonths(tenure);
        loan.setTotalEmiCount(tenure);

        LocalDate expectedEndDate = request.endDate() != null ? request.endDate() :
                DateUtils.calculateExpectedEndDate(firstEmiDate, request.emiDueDay(), tenure);
        loan.setEndDate(expectedEndDate);

        loan.setCalculationMode(request.calculationMode() != null ? request.calculationMode() : CalculationMode.REDUCING_BALANCE);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes(request.notes());
        loan.setCreatedAt(Instant.now());
        loan.setUpdatedAt(Instant.now());

        loanRepository.persist(loan);

        boolean autoMarkPastPaid = request.autoMarkPastEmisPaid() == null || Boolean.TRUE.equals(request.autoMarkPastEmisPaid());
        // Generate full fixed schedule of exactly `tenure` installments. Never stops early.
        generateSchedule(loan, origPrincipal, 1, firstEmiDate, tenure, autoMarkPastPaid);

        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public LoanResponse updateLoan(UUID id, UUID userId, UpdateLoanRequest request) {
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        loan.setName(request.name());
        loan.setLenderOrBank(request.lenderOrBank());
        if (request.loanType() != null) {
            loan.setLoanType(request.loanType());
        }
        loan.setMaskedAccountNumber(request.maskedAccountNumber());
        loan.setAnnualInterestRate(request.annualInterestRate());
        loan.setNotes(request.notes());

        if (request.originalPrincipal() != null) {
            loan.setOriginalPrincipal(CurrencyUtils.format(request.originalPrincipal()));
        }

        if (request.status() != null) {
            loan.setStatus(request.status());
        }

        if (Boolean.TRUE.equals(request.manualOverride()) && request.currentOutstandingPrincipal() != null) {
            loan.setOutstandingPrincipalManualOverride(true);
            loan.setCurrentOutstandingPrincipal(CurrencyUtils.format(request.currentOutstandingPrincipal()));
        } else if (request.currentOutstandingPrincipal() != null) {
            loan.setCurrentOutstandingPrincipal(CurrencyUtils.format(request.currentOutstandingPrincipal()));
        }

        boolean scheduleAttributesChanged = false;

        if (request.emiDueDay() > 0 && request.emiDueDay() != loan.getEmiDueDay()) {
            loan.setEmiDueDay(request.emiDueDay());
            scheduleAttributesChanged = true;
        }

        if (request.emiAmount() != null) {
            BigDecimal newEmi = CurrencyUtils.format(request.emiAmount());
            if (newEmi.compareTo(loan.getEmiAmount()) != 0) {
                loan.setEmiAmount(newEmi);
                scheduleAttributesChanged = true;
            }
        }

        if (request.startDate() != null) {
            loan.setStartDate(request.startDate());
        }

        if (request.firstEmiDate() != null && !request.firstEmiDate().isEqual(loan.getFirstEmiDate())) {
            loan.setFirstEmiDate(request.firstEmiDate());
            scheduleAttributesChanged = true;
        }

        if (request.originalTenureMonths() != null && request.originalTenureMonths() > 0 && request.originalTenureMonths() != loan.getTotalEmiCount()) {
            loan.setOriginalTenureMonths(request.originalTenureMonths());
            loan.setTotalEmiCount(request.originalTenureMonths());
            scheduleAttributesChanged = true;
        }

        LocalDate expectedEndDate = DateUtils.calculateExpectedEndDate(loan.getFirstEmiDate(), loan.getEmiDueDay(), loan.getTotalEmiCount());
        loan.setEndDate(expectedEndDate);

        // Reconcile schedule if dates, tenure, or due day changed, or explicit recalculation requested
        if (scheduleAttributesChanged || Boolean.TRUE.equals(request.recalculateFutureSchedule())) {
            reconcileSchedule(loan);
        }

        loan.setUpdatedAt(Instant.now());
        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public LoanResponse markInstallmentPaid(UUID loanId, UUID installmentId, UUID userId, RecordPaymentRequest request) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        LoanInstallment installment = installmentRepository.findByIdOptional(installmentId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "INSTALLMENT_NOT_FOUND", "Installment not found"));

        if (!installment.getLoan().getId().equals(loanId)) {
            throw new ApiException(Response.Status.BAD_REQUEST, "INVALID_INSTALLMENT", "Installment does not belong to this loan");
        }

        if (installment.getPaymentState() == PaymentState.PAID) {
            throw new ApiException(Response.Status.BAD_REQUEST, "ALREADY_PAID", "Installment is already marked paid");
        }

        installment.setPaymentState(PaymentState.PAID);
        installment.setPaidDate(request.paidDate());
        BigDecimal actualPaid = CurrencyUtils.format(request.actualPaidAmount());
        installment.setActualPaidAmount(actualPaid);

        if (request.notes() != null) {
            installment.setNotes(request.notes());
        }
        installment.setUpdatedAt(Instant.now());

        // Update loan current outstanding principal if reducing balance
        if (!loan.isOutstandingPrincipalManualOverride()) {
            BigDecimal principalPaid = installment.getPrincipalComponent();
            BigDecimal updatedPrincipal = CurrencyUtils.subtract(loan.getCurrentOutstandingPrincipal(), principalPaid);
            if (updatedPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                updatedPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }
            loan.setCurrentOutstandingPrincipal(updatedPrincipal);
        }
        loan.setUpdatedAt(Instant.now());

        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public LoanResponse undoInstallmentPayment(UUID loanId, UUID installmentId, UUID userId) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        LoanInstallment installment = installmentRepository.findByIdOptional(installmentId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "INSTALLMENT_NOT_FOUND", "Installment not found"));

        if (installment.getPaymentState() != PaymentState.PAID) {
            throw new ApiException(Response.Status.BAD_REQUEST, "NOT_PAID", "Installment is not in paid state");
        }

        installment.setPaymentState(PaymentState.UNPAID);
        installment.setPaidDate(null);
        installment.setActualPaidAmount(null);
        installment.setUpdatedAt(Instant.now());

        if (!loan.isOutstandingPrincipalManualOverride()) {
            BigDecimal principalReinstated = installment.getPrincipalComponent();
            BigDecimal updatedPrincipal = CurrencyUtils.add(loan.getCurrentOutstandingPrincipal(), principalReinstated);
            loan.setCurrentOutstandingPrincipal(updatedPrincipal);
        }

        loan.setUpdatedAt(Instant.now());
        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public LoanResponse recordPrepayment(UUID loanId, UUID userId, PrepaymentRequest request) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        BigDecimal prepayAmount = CurrencyUtils.format(request.amount());
        if (prepayAmount.compareTo(loan.getCurrentOutstandingPrincipal()) > 0) {
            throw new ApiException(Response.Status.BAD_REQUEST, "OVERPAYMENT", "Prepayment amount exceeds current outstanding principal");
        }

        BigDecimal newOutstanding = CurrencyUtils.subtract(loan.getCurrentOutstandingPrincipal(), prepayAmount);
        loan.setCurrentOutstandingPrincipal(newOutstanding);

        // In compliance with Rule A and Scenario 9:
        // A principal prepayment does NOT reduce tenure, does NOT delete future installments,
        // and does NOT alter the original fixed schedule.
        loan.setUpdatedAt(Instant.now());
        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public void deleteLoan(UUID id, UUID userId) {
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        // Delete all associated installments first to maintain database referential integrity
        installmentRepository.delete("loan.id = ?1", id);
        loanRepository.delete(loan);
    }

    @Transactional
    public LoanSummaryResponse getLoanSummary(UUID userId) {
        LocalDate today = DateUtils.todayInKolkata();
        List<Loan> activeLoans = loanRepository.findByUserIdAndStatus(userId, LoanStatus.ACTIVE);

        BigDecimal totalOriginalPrincipal = BigDecimal.ZERO;
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;
        BigDecimal totalRemainingLoanAmount = BigDecimal.ZERO;
        BigDecimal totalMonthlyEmi = BigDecimal.ZERO;
        int totalPaidEmiCount = 0;
        int totalRemainingEmiCount = 0;

        for (Loan loan : activeLoans) {
            totalOriginalPrincipal = totalOriginalPrincipal.add(loan.getOriginalPrincipal());
            totalOutstandingPrincipal = totalOutstandingPrincipal.add(loan.getCurrentOutstandingPrincipal());

            List<LoanInstallment> installments = installmentRepository.findByLoanId(loan.getId());
            int elapsedCount = 0;
            for (LoanInstallment inst : installments) {
                if (!inst.getDueDate().isAfter(today)) {
                    elapsedCount++;
                }
                if (inst.getPaymentState() == PaymentState.PAID) {
                    totalPaidEmiCount++;
                }
            }

            int totalEmis = loan.getTotalEmiCount();
            int remainingEmis = Math.max(0, totalEmis - Math.min(totalEmis, elapsedCount));

            if (remainingEmis > 0) {
                totalMonthlyEmi = totalMonthlyEmi.add(loan.getEmiAmount());
                BigDecimal loanRemainingCommitment = loan.getEmiAmount().multiply(BigDecimal.valueOf(remainingEmis));
                totalRemainingLoanAmount = totalRemainingLoanAmount.add(loanRemainingCommitment);
                totalRemainingEmiCount += remainingEmis;
            }
        }

        return new LoanSummaryResponse(
                CurrencyUtils.format(totalOriginalPrincipal),
                CurrencyUtils.format(totalOutstandingPrincipal),
                CurrencyUtils.format(totalRemainingLoanAmount),
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN),
                CurrencyUtils.format(totalMonthlyEmi),
                activeLoans.size(),
                totalPaidEmiCount,
                totalRemainingEmiCount
        );
    }

    /**
     * Generates a fixed amortization schedule containing exactly `count` installments.
     * Guaranteed to never stop early when calculated principal balance reaches zero.
     */
    private void generateSchedule(Loan loan, BigDecimal principal, int startNumber, LocalDate firstDueDate, int count, boolean markPastPaid) {
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        BigDecimal currentPrincipal = principal;
        LocalDate today = DateUtils.todayInKolkata();
        BigDecimal lastRemainingPrincipal = principal;

        for (int i = 0; i < count; i++) {
            int installmentNumber = startNumber + i;
            LocalDate currentDueDate = DateUtils.calculateInstallmentDueDate(firstDueDate, loan.getEmiDueDay(), installmentNumber);

            BigDecimal interestComponent = currentPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal principalComponent = CurrencyUtils.subtract(loan.getEmiAmount(), interestComponent);

            if (principalComponent.compareTo(BigDecimal.ZERO) < 0) {
                principalComponent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }

            if (principalComponent.compareTo(currentPrincipal) >= 0) {
                principalComponent = currentPrincipal;
            }

            // Fixed EMI amount is always loan.getEmiAmount()
            BigDecimal scheduledAmount = loan.getEmiAmount();
            currentPrincipal = CurrencyUtils.subtract(currentPrincipal, principalComponent);
            if (currentPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                currentPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }

            LoanInstallment installment = new LoanInstallment();
            installment.setId(UUID.randomUUID());
            installment.setLoan(loan);
            installment.setInstallmentNumber(installmentNumber);
            installment.setDueDate(currentDueDate);
            installment.setScheduledAmount(scheduledAmount);
            installment.setPrincipalComponent(principalComponent);
            installment.setInterestComponent(interestComponent);

            if (markPastPaid && currentDueDate.isBefore(today)) {
                installment.setPaymentState(PaymentState.PAID);
                installment.setPaidDate(currentDueDate);
                installment.setActualPaidAmount(scheduledAmount);
                lastRemainingPrincipal = currentPrincipal;
            } else {
                installment.setPaymentState(PaymentState.UNPAID);
            }

            installment.setCreatedAt(Instant.now());
            installment.setUpdatedAt(Instant.now());

            installmentRepository.persist(installment);
            // Notice: NEVER breaks early, guarantees all `count` installments are stored.
        }

        if (markPastPaid && !loan.isOutstandingPrincipalManualOverride()) {
            loan.setCurrentOutstandingPrincipal(lastRemainingPrincipal);
            loanRepository.persist(loan);
        }
    }

    /**
     * Reconciles a loan's installments against its authoritative tenure and dates.
     * Preserves payment history, actual paid amounts, paid dates, and notes.
     * Restores missing future or overdue installments without creating duplicates.
     */
    @Transactional
    public void reconcileSchedule(Loan loan) {
        int targetCount = loan.getTotalEmiCount() > 0 ? loan.getTotalEmiCount() : 1;
        List<LoanInstallment> existing = installmentRepository.findByLoanId(loan.getId());

        Map<Integer, LoanInstallment> existingMap = new HashMap<>();
        for (LoanInstallment inst : existing) {
            existingMap.put(inst.getInstallmentNumber(), inst);
        }

        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);
        BigDecimal currentPrincipal = loan.getOriginalPrincipal();

        for (int num = 1; num <= targetCount; num++) {
            LocalDate expectedDueDate = DateUtils.calculateInstallmentDueDate(loan.getFirstEmiDate(), loan.getEmiDueDay(), num);

            BigDecimal interestComponent = currentPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal principalComponent = CurrencyUtils.subtract(loan.getEmiAmount(), interestComponent);
            if (principalComponent.compareTo(BigDecimal.ZERO) < 0) {
                principalComponent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }
            if (principalComponent.compareTo(currentPrincipal) >= 0) {
                principalComponent = currentPrincipal;
            }
            currentPrincipal = CurrencyUtils.subtract(currentPrincipal, principalComponent);
            if (currentPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                currentPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }

            if (existingMap.containsKey(num)) {
                LoanInstallment inst = existingMap.get(num);
                inst.setDueDate(expectedDueDate);
                inst.setScheduledAmount(loan.getEmiAmount());
                inst.setPrincipalComponent(principalComponent);
                inst.setInterestComponent(interestComponent);
                inst.setUpdatedAt(Instant.now());
                installmentRepository.persist(inst);
            } else {
                LoanInstallment newInst = new LoanInstallment();
                newInst.setId(UUID.randomUUID());
                newInst.setLoan(loan);
                newInst.setInstallmentNumber(num);
                newInst.setDueDate(expectedDueDate);
                newInst.setScheduledAmount(loan.getEmiAmount());
                newInst.setPrincipalComponent(principalComponent);
                newInst.setInterestComponent(interestComponent);
                newInst.setPaymentState(PaymentState.UNPAID);
                newInst.setCreatedAt(Instant.now());
                newInst.setUpdatedAt(Instant.now());
                installmentRepository.persist(newInst);
            }
        }

        // Delete excess UNPAID installments if tenure was reduced
        for (LoanInstallment inst : existing) {
            if (inst.getInstallmentNumber() > targetCount && inst.getPaymentState() != PaymentState.PAID) {
                installmentRepository.delete(inst);
            }
        }

        LocalDate expectedEndDate = DateUtils.calculateExpectedEndDate(loan.getFirstEmiDate(), loan.getEmiDueDay(), targetCount);
        loan.setEndDate(expectedEndDate);
        loanRepository.persist(loan);
    }

    /**
     * Bulk reconciliation utility for system maintenance / boot reconciliation.
     */
    @Transactional
    public void reconcileAllLoans() {
        List<Loan> allLoans = loanRepository.listAll();
        for (Loan loan : allLoans) {
            try {
                reconcileSchedule(loan);
            } catch (Exception e) {
                LOG.errorf(e, "Error reconciling loan %s", loan.getId());
            }
        }
    }

    public LoanResponse toLoanResponseWithSchedule(Loan loan) {
        LocalDate today = DateUtils.todayInKolkata();
        List<LoanInstallment> installments = installmentRepository.findByLoanId(loan.getId());

        int totalEmiCount = loan.getTotalEmiCount() > 0 ? loan.getTotalEmiCount() :
                (loan.getOriginalTenureMonths() > 0 ? loan.getOriginalTenureMonths() : Math.max(1, installments.size()));

        int elapsedCount = 0;
        int actuallyPaidCount = 0;
        int overdueCount = 0;
        LocalDate nextEmiDate = null;
        LocalDate lastScheduledEmiDate = loan.getEndDate() != null ? loan.getEndDate() :
                DateUtils.calculateExpectedEndDate(loan.getFirstEmiDate(), loan.getEmiDueDay(), totalEmiCount);

        List<LoanInstallmentResponse> instResponses = new ArrayList<>();

        for (LoanInstallment i : installments) {
            boolean isDueOnOrBeforeToday = !i.getDueDate().isAfter(today);
            boolean isPaid = i.getPaymentState() == PaymentState.PAID;
            boolean isOverdue = !isPaid && i.getDueDate().isBefore(today);

            if (isDueOnOrBeforeToday) {
                elapsedCount++;
                if (!isPaid) {
                    overdueCount++;
                }
            } else {
                if (nextEmiDate == null) {
                    nextEmiDate = i.getDueDate();
                }
            }

            if (isPaid) {
                actuallyPaidCount++;
            }

            instResponses.add(new LoanInstallmentResponse(
                    i.getId(),
                    i.getLoan().getId(),
                    i.getInstallmentNumber(),
                    i.getDueDate(),
                    i.getScheduledAmount(),
                    i.getPrincipalComponent(),
                    i.getInterestComponent(),
                    i.getPaymentState(),
                    i.getPaidDate(),
                    i.getActualPaidAmount(),
                    i.getNotes(),
                    i.getVersion(),
                    isOverdue,
                    i.getCreatedAt(),
                    i.getUpdatedAt()
            ));
        }

        // If next upcoming installment wasn't in future, check if today is an EMI due date and unpaid
        if (nextEmiDate == null) {
            for (LoanInstallment i : installments) {
                if (i.getDueDate().isEqual(today) && i.getPaymentState() != PaymentState.PAID) {
                    nextEmiDate = i.getDueDate();
                    break;
                }
            }
        }

        // Rule B & C: Remaining EMIs = Total Tenure - Elapsed EMIs
        int remainingEmiCount = Math.max(0, totalEmiCount - Math.min(totalEmiCount, elapsedCount));
        if (loan.getStatus() == LoanStatus.CLOSED) {
            remainingEmiCount = 0;
        }

        // Rule D: Remaining Loan Amount = Fixed Monthly EMI × Remaining EMI Count
        BigDecimal remainingLoanAmount = remainingEmiCount > 0 ?
                loan.getEmiAmount().multiply(BigDecimal.valueOf(remainingEmiCount)) :
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal estimatedInterest = CurrencyUtils.subtract(remainingLoanAmount, loan.getCurrentOutstandingPrincipal());
        if (estimatedInterest.compareTo(BigDecimal.ZERO) < 0) {
            estimatedInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        return new LoanResponse(
                loan.getId(),
                loan.getName(),
                loan.getLenderOrBank(),
                loan.getLoanType(),
                loan.getMaskedAccountNumber(),
                loan.getOriginalPrincipal(),
                loan.getCurrentOutstandingPrincipal(),
                loan.getCurrentOutstandingPrincipal(), // remainingPrincipal
                CurrencyUtils.format(remainingLoanAmount),
                CurrencyUtils.format(remainingLoanAmount), // remainingRepayment backward-compatible
                CurrencyUtils.format(estimatedInterest),
                loan.getAnnualInterestRate(),
                loan.getStartDate(),
                loan.getFirstEmiDate(),
                lastScheduledEmiDate,
                nextEmiDate,
                loan.getEmiDueDay(),
                loan.getEmiAmount(),
                loan.getOriginalTenureMonths(),
                totalEmiCount,
                elapsedCount,
                actuallyPaidCount,
                remainingEmiCount,
                overdueCount,
                loan.getCalculationMode(),
                loan.isOutstandingPrincipalManualOverride(),
                loan.getStatus(),
                loan.getNotes(),
                loan.getVersion(),
                instResponses,
                loan.getCreatedAt(),
                loan.getUpdatedAt()
        );
    }
}
