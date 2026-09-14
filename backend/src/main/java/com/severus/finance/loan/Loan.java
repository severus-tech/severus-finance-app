package com.severus.finance.loan;

import com.severus.finance.loan.enums.CalculationMode;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.LoanType;
import com.severus.finance.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "lender_or_bank", nullable = false, length = 150)
    private String lenderOrBank;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_type", nullable = false, length = 50)
    private LoanType loanType;

    @Column(name = "masked_account_number", length = 50)
    private String maskedAccountNumber;

    @Column(name = "original_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal originalPrincipal;

    @Column(name = "current_outstanding_principal", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentOutstandingPrincipal;

    @Column(name = "annual_interest_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "first_emi_date", nullable = false)
    private LocalDate firstEmiDate;

    @Column(name = "emi_due_day", nullable = false)
    private int emiDueDay;

    @Column(name = "emi_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "original_tenure_months", nullable = false)
    private int originalTenureMonths;

    @Column(name = "total_emi_count", nullable = false)
    private int totalEmiCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_mode", nullable = false, length = 30)
    private CalculationMode calculationMode;

    @Column(name = "outstanding_principal_manual_override", nullable = false)
    private boolean outstandingPrincipalManualOverride;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanStatus status;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Loan() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLenderOrBank() {
        return lenderOrBank;
    }

    public void setLenderOrBank(String lenderOrBank) {
        this.lenderOrBank = lenderOrBank;
    }

    public LoanType getLoanType() {
        return loanType;
    }

    public void setLoanType(LoanType loanType) {
        this.loanType = loanType;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
    }

    public BigDecimal getOriginalPrincipal() {
        return originalPrincipal;
    }

    public void setOriginalPrincipal(BigDecimal originalPrincipal) {
        this.originalPrincipal = originalPrincipal;
    }

    public BigDecimal getCurrentOutstandingPrincipal() {
        return currentOutstandingPrincipal;
    }

    public void setCurrentOutstandingPrincipal(BigDecimal currentOutstandingPrincipal) {
        this.currentOutstandingPrincipal = currentOutstandingPrincipal;
    }

    public BigDecimal getAnnualInterestRate() {
        return annualInterestRate;
    }

    public void setAnnualInterestRate(BigDecimal annualInterestRate) {
        this.annualInterestRate = annualInterestRate;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getFirstEmiDate() {
        return firstEmiDate;
    }

    public void setFirstEmiDate(LocalDate firstEmiDate) {
        this.firstEmiDate = firstEmiDate;
    }

    public int getEmiDueDay() {
        return emiDueDay;
    }

    public void setEmiDueDay(int emiDueDay) {
        this.emiDueDay = emiDueDay;
    }

    public BigDecimal getEmiAmount() {
        return emiAmount;
    }

    public void setEmiAmount(BigDecimal emiAmount) {
        this.emiAmount = emiAmount;
    }

    public int getOriginalTenureMonths() {
        return originalTenureMonths;
    }

    public void setOriginalTenureMonths(int originalTenureMonths) {
        this.originalTenureMonths = originalTenureMonths;
    }

    public int getTotalEmiCount() {
        return totalEmiCount;
    }

    public void setTotalEmiCount(int totalEmiCount) {
        this.totalEmiCount = totalEmiCount;
    }

    public CalculationMode getCalculationMode() {
        return calculationMode;
    }

    public void setCalculationMode(CalculationMode calculationMode) {
        this.calculationMode = calculationMode;
    }

    public boolean isOutstandingPrincipalManualOverride() {
        return outstandingPrincipalManualOverride;
    }

    public void setOutstandingPrincipalManualOverride(boolean outstandingPrincipalManualOverride) {
        this.outstandingPrincipalManualOverride = outstandingPrincipalManualOverride;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
