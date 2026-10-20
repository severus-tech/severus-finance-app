package com.severus.finance.loan;

import com.severus.finance.expense.ExpenseRepository;
import com.severus.finance.loan.dto.LoanInstallmentResponse;
import com.severus.finance.loan.dto.LoanResponse;
import com.severus.finance.loan.enums.CalculationMode;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.LoanType;
import com.severus.finance.loan.enums.PaymentState;
import com.severus.finance.settings.dto.AppSettingsResponse;
import com.severus.finance.settings.AppSettingsService;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class LoanPdfExportServiceTest {

    private LoanRepository loanRepository;
    private LoanInstallmentRepository installmentRepository;
    private LoanService loanService;
    private AppSettingsService settingsService;
    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private LoanPdfExportService pdfExportService;

    private UUID userId;

    @BeforeEach
    public void setup() {
        loanRepository = Mockito.mock(LoanRepository.class);
        installmentRepository = Mockito.mock(LoanInstallmentRepository.class);
        loanService = Mockito.mock(LoanService.class);
        settingsService = Mockito.mock(AppSettingsService.class);
        expenseRepository = Mockito.mock(ExpenseRepository.class);
        userRepository = Mockito.mock(UserRepository.class);

        pdfExportService = new LoanPdfExportService(
                loanRepository,
                installmentRepository,
                loanService,
                settingsService,
                expenseRepository,
                userRepository
        );

        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should generate valid PDF with %PDF- header for user with multiple loans")
    public void testGeneratePdfWithMultipleLoans() {
        User user = new User(userId, "test.user@example.com", "hash");
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.of(user));

        AppSettingsResponse settings = new AppSettingsResponse(
                new BigDecimal("150000.00"),
                1,
                "INR",
                "Asia/Kolkata",
                "Test User",
                "DARK",
                Instant.now(),
                Instant.now()
        );
        when(settingsService.getSettings(userId)).thenReturn(settings);

        Loan rawLoan1 = new Loan();
        rawLoan1.setId(UUID.randomUUID());
        rawLoan1.setName("Home Loan");

        when(loanRepository.list(eq("user.id = ?1 order by createdAt asc"), eq(userId)))
                .thenReturn(List.of(rawLoan1));

        List<LoanInstallmentResponse> installments = new ArrayList<>();
        installments.add(new LoanInstallmentResponse(
                UUID.randomUUID(),
                rawLoan1.getId(),
                1,
                LocalDate.of(2024, 4, 5),
                new BigDecimal("25000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("10000.00"),
                PaymentState.PAID,
                LocalDate.of(2024, 4, 5),
                new BigDecimal("25000.00"),
                "Paid on time",
                1L,
                false,
                Instant.now(),
                Instant.now()
        ));
        installments.add(new LoanInstallmentResponse(
                UUID.randomUUID(),
                rawLoan1.getId(),
                2,
                LocalDate.of(2024, 5, 5),
                new BigDecimal("25000.00"),
                new BigDecimal("15200.00"),
                new BigDecimal("9800.00"),
                PaymentState.UNPAID,
                null,
                null,
                null,
                1L,
                true, // Overdue
                Instant.now(),
                Instant.now()
        ));
        installments.add(new LoanInstallmentResponse(
                UUID.randomUUID(),
                rawLoan1.getId(),
                3,
                LocalDate.of(2099, 1, 5),
                new BigDecimal("25000.00"),
                new BigDecimal("16000.00"),
                new BigDecimal("9000.00"),
                PaymentState.UNPAID,
                null,
                null,
                null,
                1L,
                false,
                Instant.now(),
                Instant.now()
        ));

        LoanResponse loanResp1 = new LoanResponse(
                rawLoan1.getId(),
                "Home Loan",
                "HDFC Bank",
                LoanType.HOME_LOAN,
                "****4321",
                new BigDecimal("2500000.00"),
                new BigDecimal("2400000.00"),
                new BigDecimal("2400000.00"),
                new BigDecimal("1150000.00"),
                new BigDecimal("3000000.00"),
                new BigDecimal("600000.00"),
                new BigDecimal("8.50"),
                LocalDate.of(2024, 4, 1),
                LocalDate.of(2024, 4, 5),
                LocalDate.of(2028, 3, 5),
                LocalDate.of(2099, 1, 5),
                5,
                new BigDecimal("25000.00"),
                48,
                48,
                2,
                1,
                46,
                1,
                CalculationMode.REDUCING_BALANCE,
                false,
                LoanStatus.ACTIVE,
                "Primary residence mortgage",
                1L,
                installments,
                Instant.now(),
                Instant.now()
        );

        when(loanService.toLoanResponseWithSchedule(rawLoan1)).thenReturn(loanResp1);
        when(installmentRepository.findInstallmentsDueInDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.findActiveFixedExpenses(userId)).thenReturn(Collections.emptyList());
        when(expenseRepository.findVariableExpensesInDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = pdfExportService.generateLoanPortfolioPdf(userId);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 500, "PDF byte array should contain generated document");

        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"), "Generated file must be a valid PDF starting with %PDF-");
    }

    @Test
    @DisplayName("Should generate valid PDF even when user has zero loans")
    public void testGeneratePdfZeroLoans() {
        when(userRepository.findByIdOptional(userId)).thenReturn(Optional.empty());
        AppSettingsResponse settings = new AppSettingsResponse(
                BigDecimal.ZERO, 1, "INR", "Asia/Kolkata", "Anonymous", "LIGHT", Instant.now(), Instant.now()
        );
        when(settingsService.getSettings(userId)).thenReturn(settings);
        when(loanRepository.list(eq("user.id = ?1 order by createdAt asc"), eq(userId)))
                .thenReturn(Collections.emptyList());
        when(installmentRepository.findInstallmentsDueInDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());
        when(expenseRepository.findActiveFixedExpenses(userId)).thenReturn(Collections.emptyList());
        when(expenseRepository.findVariableExpensesInDateRange(eq(userId), any(), any()))
                .thenReturn(Collections.emptyList());

        byte[] pdfBytes = pdfExportService.generateLoanPortfolioPdf(userId);

        assertNotNull(pdfBytes);
        String pdfHeader = new String(pdfBytes, 0, Math.min(pdfBytes.length, 10), StandardCharsets.US_ASCII);
        assertTrue(pdfHeader.startsWith("%PDF-"));
    }
}
