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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LoanService {

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

        loan.setOriginalTenureMonths(request.originalTenureMonths());
        loan.setTotalEmiCount(request.originalTenureMonths());
        loan.setCalculationMode(request.calculationMode() != null ? request.calculationMode() : CalculationMode.REDUCING_BALANCE);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setNotes(request.notes());
        loan.setCreatedAt(Instant.now());
        loan.setUpdatedAt(Instant.now());

        loanRepository.persist(loan);

        generateSchedule(loan, origPrincipal, 1, firstEmiDate, request.originalTenureMonths(), true);

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
        loan.setEmiDueDay(request.emiDueDay());
        loan.setEmiAmount(CurrencyUtils.format(request.emiAmount()));
        loan.setNotes(request.notes());

        if (request.status() != null) {
            loan.setStatus(request.status());
        }

        if (Boolean.TRUE.equals(request.manualOverride()) && request.currentOutstandingPrincipal() != null) {
            loan.setOutstandingPrincipalManualOverride(true);
            loan.setCurrentOutstandingPrincipal(CurrencyUtils.format(request.currentOutstandingPrincipal()));
        } else if (request.manualOverride() != null) {
            loan.setOutstandingPrincipalManualOverride(request.manualOverride());
        }

        if (Boolean.TRUE.equals(request.recalculateFutureSchedule())) {
            recalculateFutureInstallments(loan);
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

            if (updatedPrincipal.compareTo(BigDecimal.ZERO) == 0) {
                loan.setStatus(LoanStatus.CLOSED);
            }
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

        if (!loan.isOutstandingPrincipalManualOverride()) {
            BigDecimal principalReinstated = installment.getPrincipalComponent();
            BigDecimal updatedPrincipal = CurrencyUtils.add(loan.getCurrentOutstandingPrincipal(), principalReinstated);
            loan.setCurrentOutstandingPrincipal(updatedPrincipal);
            if (loan.getStatus() == LoanStatus.CLOSED && updatedPrincipal.compareTo(BigDecimal.ZERO) > 0) {
                loan.setStatus(LoanStatus.ACTIVE);
            }
        }

        installment.setActualPaidAmount(null);
        installment.setUpdatedAt(Instant.now());
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

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            installmentRepository.deleteUnpaidFutureInstallments(loanId);
        } else {
            recalculateFutureInstallments(loan);
        }

        loan.setUpdatedAt(Instant.now());
        return toLoanResponseWithSchedule(loan);
    }

    @Transactional
    public void deleteLoan(UUID id, UUID userId) {
        Loan loan = loanRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(Response.Status.NOT_FOUND, "LOAN_NOT_FOUND", "Loan not found"));

        installmentRepository.delete("loan.id = ?1", id);
        loanRepository.delete(loan);
    }

    @Transactional
    public LoanSummaryResponse getLoanSummary(UUID userId) {
        List<Loan> activeLoans = loanRepository.findByUserIdAndStatus(userId, LoanStatus.ACTIVE);

        BigDecimal totalOriginalPrincipal = BigDecimal.ZERO;
        BigDecimal totalOutstandingPrincipal = BigDecimal.ZERO;
        BigDecimal totalRemainingRepayment = BigDecimal.ZERO;
        BigDecimal totalMonthlyEmi = BigDecimal.ZERO;
        int totalPaidEmiCount = 0;
        int totalRemainingEmiCount = 0;

        for (Loan loan : activeLoans) {
            totalOriginalPrincipal = totalOriginalPrincipal.add(loan.getOriginalPrincipal());
            totalOutstandingPrincipal = totalOutstandingPrincipal.add(loan.getCurrentOutstandingPrincipal());
            totalMonthlyEmi = totalMonthlyEmi.add(loan.getEmiAmount());

            List<LoanInstallment> installments = installmentRepository.findByLoanId(loan.getId());
            for (LoanInstallment inst : installments) {
                if (inst.getPaymentState() == PaymentState.PAID) {
                    totalPaidEmiCount++;
                } else if (inst.getPaymentState() == PaymentState.UNPAID) {
                    totalRemainingEmiCount++;
                    totalRemainingRepayment = totalRemainingRepayment.add(inst.getScheduledAmount());
                }
            }
        }

        BigDecimal totalEstimatedRemainingInterest = CurrencyUtils.subtract(totalRemainingRepayment, totalOutstandingPrincipal);
        if (totalEstimatedRemainingInterest.compareTo(BigDecimal.ZERO) < 0) {
            totalEstimatedRemainingInterest = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
        }

        return new LoanSummaryResponse(
                CurrencyUtils.format(totalOriginalPrincipal),
                CurrencyUtils.format(totalOutstandingPrincipal),
                CurrencyUtils.format(totalRemainingRepayment),
                CurrencyUtils.format(totalEstimatedRemainingInterest),
                CurrencyUtils.format(totalMonthlyEmi),
                activeLoans.size(),
                totalPaidEmiCount,
                totalRemainingEmiCount
        );
    }

    private void generateSchedule(Loan loan, BigDecimal principal, int startNumber, LocalDate firstDueDate, int count, boolean autoMarkPastPaid) {
        BigDecimal monthlyRate = loan.getAnnualInterestRate()
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_EVEN)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_EVEN);

        BigDecimal currentPrincipal = principal;
        LocalDate currentDueDate = firstDueDate;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < count; i++) {
            int installmentNumber = startNumber + i;

            BigDecimal interestComponent = currentPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal principalComponent = CurrencyUtils.subtract(loan.getEmiAmount(), interestComponent);

            if (principalComponent.compareTo(BigDecimal.ZERO) < 0) {
                principalComponent = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
            }

            if (i == count - 1 || principalComponent.compareTo(currentPrincipal) >= 0) {
                principalComponent = currentPrincipal;
            }

            BigDecimal scheduledAmount = CurrencyUtils.add(principalComponent, interestComponent);
            currentPrincipal = CurrencyUtils.subtract(currentPrincipal, principalComponent);

            LoanInstallment installment = new LoanInstallment();
            installment.setId(UUID.randomUUID());
            installment.setLoan(loan);
            installment.setInstallmentNumber(installmentNumber);
            installment.setDueDate(currentDueDate);
            installment.setScheduledAmount(scheduledAmount);
            installment.setPrincipalComponent(principalComponent);
            installment.setInterestComponent(interestComponent);

            if (autoMarkPastPaid && currentDueDate.isBefore(today)) {
                installment.setPaymentState(PaymentState.PAID);
                installment.setPaidDate(currentDueDate);
                installment.setActualPaidAmount(scheduledAmount);

                if (!loan.isOutstandingPrincipalManualOverride()) {
                    BigDecimal updatedPrincipal = CurrencyUtils.subtract(loan.getCurrentOutstandingPrincipal(), principalComponent);
                    if (updatedPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                        updatedPrincipal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN);
                    }
                    loan.setCurrentOutstandingPrincipal(updatedPrincipal);
                }
            } else {
                installment.setPaymentState(PaymentState.UNPAID);
            }

            installment.setCreatedAt(Instant.now());
            installment.setUpdatedAt(Instant.now());

            installmentRepository.persist(installment);

            // Compute next due date considering month lengths & emiDueDay
            LocalDate nextMonth = currentDueDate.plusMonths(1);
            currentDueDate = DateUtils.resolveDueDate(nextMonth.getYear(), nextMonth.getMonthValue(), loan.getEmiDueDay());

            if (currentPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
        }

        if (loan.getCurrentOutstandingPrincipal().compareTo(BigDecimal.ZERO) <= 0) {
            loan.setCurrentOutstandingPrincipal(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN));
            loan.setStatus(LoanStatus.CLOSED);
        }
    }

    private void recalculateFutureInstallments(Loan loan) {
        List<LoanInstallment> unpaid = installmentRepository.findUnpaidByLoanId(loan.getId());
        if (unpaid.isEmpty()) {
            return;
        }

        installmentRepository.deleteUnpaidFutureInstallments(loan.getId());

        int startNumber = unpaid.getFirst().getInstallmentNumber();
        LocalDate firstDueDate = unpaid.getFirst().getDueDate();
        int remainingCount = unpaid.size();

        generateSchedule(loan, loan.getCurrentOutstandingPrincipal(), startNumber, firstDueDate, remainingCount, false);
    }

    public LoanResponse toLoanResponseWithSchedule(Loan loan) {
        List<LoanInstallment> installments = installmentRepository.findByLoanId(loan.getId());
        List<LoanInstallmentResponse> instResponses = installments.stream()
                .map(i -> new LoanInstallmentResponse(
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
                        i.getCreatedAt(),
                        i.getUpdatedAt()
                )).toList();

        BigDecimal remainingRepayment = BigDecimal.ZERO;
        int paidCount = 0;
        int remainingCount = 0;

        for (LoanInstallment inst : installments) {
            if (inst.getPaymentState() == PaymentState.PAID) {
                paidCount++;
            } else if (inst.getPaymentState() == PaymentState.UNPAID) {
                remainingCount++;
                remainingRepayment = remainingRepayment.add(inst.getScheduledAmount());
            }
        }

        BigDecimal estimatedInterest = CurrencyUtils.subtract(remainingRepayment, loan.getCurrentOutstandingPrincipal());
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
                CurrencyUtils.format(remainingRepayment),
                CurrencyUtils.format(estimatedInterest),
                loan.getAnnualInterestRate(),
                loan.getStartDate(),
                loan.getFirstEmiDate(),
                loan.getEmiDueDay(),
                loan.getEmiAmount(),
                loan.getOriginalTenureMonths(),
                loan.getTotalEmiCount(),
                paidCount,
                remainingCount,
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
