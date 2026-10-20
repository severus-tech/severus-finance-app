package com.severus.finance.loan;

import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.loan.dto.CreateLoanRequest;
import com.severus.finance.loan.enums.CalculationMode;
import com.severus.finance.loan.enums.LoanType;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class LoanServiceTest {

    @Inject
    LoanService loanService;

    @Inject
    com.severus.finance.user.UserRepository userRepository;

    @Test
    @DisplayName("Should generate standard reducing balance loan schedule")
    public void testLoanScheduleGeneration() {
        // Create sample loan request
        CreateLoanRequest request = new CreateLoanRequest(
                "Car Loan",
                "HDFC Bank",
                LoanType.CAR_LOAN,
                "****1234",
                new BigDecimal("500000.00"),
                null,
                new BigDecimal("9.50"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 2, 5),
                null,
                5,
                new BigDecimal("10500.00"),
                60,
                CalculationMode.REDUCING_BALANCE,
                false,
                false,
                "Test loan"
        );

        // Fetch user from DB (seeded)
        UUID userId = userRepository.findByEmail("severus.technology@gmail.com")
                .orElseThrow().getId();

        var response = loanService.createLoan(userId, request);

        assertNotNull(response.id());
        assertEquals("Car Loan", response.name());
        assertEquals(CurrencyUtils.format(new BigDecimal("500000.00")), response.originalPrincipal());
        assertFalse(response.installments().isEmpty());
        assertEquals(60, response.installments().size());
    }
}
