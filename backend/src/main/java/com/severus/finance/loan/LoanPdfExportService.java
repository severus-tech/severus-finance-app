package com.severus.finance.loan;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.severus.finance.common.util.CurrencyUtils;
import com.severus.finance.common.util.DateUtils;
import com.severus.finance.expense.Expense;
import com.severus.finance.expense.ExpenseRepository;
import com.severus.finance.loan.dto.LoanInstallmentResponse;
import com.severus.finance.loan.dto.LoanResponse;
import com.severus.finance.loan.enums.LoanStatus;
import com.severus.finance.loan.enums.PaymentState;
import com.severus.finance.settings.dto.AppSettingsResponse;
import com.severus.finance.settings.AppSettingsService;
import com.severus.finance.user.User;
import com.severus.finance.user.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@ApplicationScoped
public class LoanPdfExportService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanService loanService;
    private final AppSettingsService settingsService;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    // Palette Colors
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);       // Indigo #4F46E5
    private static final Color COLOR_PRIMARY_DARK = new Color(30, 41, 59);   // Slate Dark #1E293B
    private static final Color COLOR_BG_LIGHT = new Color(248, 250, 252);     // Slate 50 #F8FAFC
    private static final Color COLOR_BG_ALT = new Color(241, 245, 249);       // Slate 100 #F1F5F9
    private static final Color COLOR_BORDER = new Color(226, 232, 240);       // Slate 200 #E2E8F0
    private static final Color COLOR_TEXT_MAIN = new Color(15, 23, 42);       // Slate 900
    private static final Color COLOR_TEXT_MUTED = new Color(100, 116, 139);   // Slate 500
    private static final Color COLOR_TEXT_EMERALD = new Color(4, 120, 87);    // Emerald 700
    private static final Color COLOR_BG_EMERALD = new Color(209, 250, 229);   // Emerald 100
    private static final Color COLOR_TEXT_ROSE = new Color(185, 28, 28);      // Rose 700
    private static final Color COLOR_BG_ROSE = new Color(254, 226, 226);      // Rose 100
    private static final Color COLOR_TEXT_AMBER = new Color(180, 83, 9);      // Amber 700
    private static final Color COLOR_BG_AMBER = new Color(254, 243, 199);     // Amber 100

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss 'IST'")
            .withZone(ZoneId.of("Asia/Kolkata"));

    @Inject
    public LoanPdfExportService(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository,
            LoanService loanService,
            AppSettingsService settingsService,
            ExpenseRepository expenseRepository,
            UserRepository userRepository
    ) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.loanService = loanService;
        this.settingsService = settingsService;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    public byte[] generateLoanPortfolioPdf(UUID userId) {
        LocalDate today = DateUtils.todayInKolkata();
        String generationTime = TS_FMT.format(Instant.now());
        String asOfDateStr = DATE_FMT.format(today);

        // Fetch User and Settings
        Optional<User> userOpt = userRepository.findByIdOptional(userId);
        String userEmail = userOpt.map(User::getEmail).orElse("User");
        AppSettingsResponse settings = settingsService.getSettings(userId);
        String displayName = (settings.displayName() != null && !settings.displayName().isBlank())
                ? settings.displayName() : userEmail;

        // Fetch All Loans for User
        List<Loan> rawLoans = loanRepository.list("user.id = ?1 order by createdAt asc", userId);
        List<LoanResponse> loansWithSchedule = new ArrayList<>();
        for (Loan loan : rawLoans) {
            loansWithSchedule.add(loanService.toLoanResponseWithSchedule(loan));
        }

        // Aggregate Portfolio Metrics
        int activeLoansCount = 0;
        int closedLoansCount = 0;
        int pausedLoansCount = 0;

        BigDecimal totalOriginalPrincipal = BigDecimal.ZERO;
        BigDecimal totalRemainingPrincipal = BigDecimal.ZERO;
        BigDecimal totalMonthlyEmi = BigDecimal.ZERO;
        BigDecimal totalRemainingLoanAmount = BigDecimal.ZERO;

        int totalOriginalTenure = 0;
        int totalElapsedEmis = 0;
        int totalActuallyPaidEmis = 0;
        int totalRemainingScheduledEmis = 0;

        int totalOverdueCount = 0;
        BigDecimal totalOverdueAmount = BigDecimal.ZERO;

        LocalDate nearestClosureDate = null;
        LocalDate latestClosureDate = null;

        for (LoanResponse lr : loansWithSchedule) {
            BigDecimal origPrinc = lr.originalPrincipal() != null ? lr.originalPrincipal() : BigDecimal.ZERO;
            BigDecimal remPrinc = lr.remainingPrincipal() != null ? lr.remainingPrincipal() :
                    (lr.currentOutstandingPrincipal() != null ? lr.currentOutstandingPrincipal() : BigDecimal.ZERO);
            BigDecimal remLoanAmt = lr.remainingLoanAmount() != null ? lr.remainingLoanAmount() : BigDecimal.ZERO;
            BigDecimal emi = lr.emiAmount() != null ? lr.emiAmount() : BigDecimal.ZERO;

            totalOriginalPrincipal = totalOriginalPrincipal.add(origPrinc);

            if (lr.status() == LoanStatus.ACTIVE) {
                activeLoansCount++;
                totalRemainingPrincipal = totalRemainingPrincipal.add(remPrinc);
                totalRemainingLoanAmount = totalRemainingLoanAmount.add(remLoanAmt);
                if (lr.remainingEmiCount() > 0) {
                    totalMonthlyEmi = totalMonthlyEmi.add(emi);
                }
            } else if (lr.status() == LoanStatus.CLOSED) {
                closedLoansCount++;
            } else if (lr.status() == LoanStatus.PAUSED) {
                pausedLoansCount++;
                totalRemainingPrincipal = totalRemainingPrincipal.add(remPrinc);
                totalRemainingLoanAmount = totalRemainingLoanAmount.add(remLoanAmt);
            }

            totalOriginalTenure += lr.totalEmiCount();
            totalElapsedEmis += lr.elapsedEmiCount();
            totalActuallyPaidEmis += lr.paidEmiCount();
            totalRemainingScheduledEmis += lr.remainingEmiCount();
            totalOverdueCount += lr.overdueEmiCount();

            // Calculate overdue amount
            if (lr.installments() != null) {
                for (LoanInstallmentResponse inst : lr.installments()) {
                    if (inst.overdue() && inst.paymentState() != PaymentState.PAID) {
                        totalOverdueAmount = totalOverdueAmount.add(inst.scheduledAmount());
                    }
                }
            }

            if (lr.endDate() != null) {
                if (nearestClosureDate == null || lr.endDate().isBefore(nearestClosureDate)) {
                    nearestClosureDate = lr.endDate();
                }
                if (latestClosureDate == null || lr.endDate().isAfter(latestClosureDate)) {
                    latestClosureDate = lr.endDate();
                }
            }
        }

        BigDecimal totalRepaidPrincipal = totalOriginalPrincipal.subtract(totalRemainingPrincipal);
        if (totalRepaidPrincipal.compareTo(BigDecimal.ZERO) < 0) {
            totalRepaidPrincipal = BigDecimal.ZERO;
        }

        // Current Salary Cycle Metrics
        int salaryCreditDay = settings.salaryCreditDay();
        DateUtils.SalaryCycle currentCycle = DateUtils.calculateSalaryCycle(today, salaryCreditDay);
        LocalDate cycleStart = currentCycle.startDate();
        LocalDate cycleEnd = currentCycle.endDate();

        List<LoanInstallment> cycleInstallments = installmentRepository.findInstallmentsDueInDateRange(userId, cycleStart, cycleEnd);
        BigDecimal currentMonthDueEmis = BigDecimal.ZERO;
        BigDecimal currentMonthPaidEmis = BigDecimal.ZERO;
        BigDecimal currentMonthRemainingEmis = BigDecimal.ZERO;

        for (LoanInstallment inst : cycleInstallments) {
            if (inst.getPaymentState() != PaymentState.SKIPPED) {
                currentMonthDueEmis = currentMonthDueEmis.add(inst.getScheduledAmount());
                if (inst.getPaymentState() == PaymentState.PAID) {
                    currentMonthPaidEmis = currentMonthPaidEmis.add(inst.getScheduledAmount());
                } else {
                    currentMonthRemainingEmis = currentMonthRemainingEmis.add(inst.getScheduledAmount());
                }
            }
        }

        // Current cycle expenses
        List<Expense> activeFixed = expenseRepository.findActiveFixedExpenses(userId);
        BigDecimal fixedExpensesInCycle = BigDecimal.ZERO;
        for (Expense exp : activeFixed) {
            if (exp.getPaymentState() != PaymentState.SKIPPED) {
                fixedExpensesInCycle = fixedExpensesInCycle.add(exp.getAmount());
            }
        }
        List<Expense> variableInCycle = expenseRepository.findVariableExpensesInDateRange(userId, cycleStart, cycleEnd);
        BigDecimal variableExpensesInCycle = BigDecimal.ZERO;
        for (Expense exp : variableInCycle) {
            variableExpensesInCycle = variableExpensesInCycle.add(exp.getAmount());
        }
        BigDecimal totalExpensesInCycle = fixedExpensesInCycle.add(variableExpensesInCycle);

        BigDecimal monthlySalary = settings.monthlySalary() != null ? settings.monthlySalary() : BigDecimal.ZERO;
        boolean hasSalaryConfigured = monthlySalary.compareTo(BigDecimal.ZERO) > 0;

        BigDecimal remainingSalaryAfterEmis = hasSalaryConfigured ?
                monthlySalary.subtract(totalMonthlyEmi).setScale(2, RoundingMode.HALF_EVEN) : null;
        BigDecimal netSalaryAfterAll = hasSalaryConfigured ?
                monthlySalary.subtract(currentMonthDueEmis).subtract(totalExpensesInCycle).setScale(2, RoundingMode.HALF_EVEN) : null;

        // Build OpenPDF Document
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 32f, 32f, 36f, 36f);

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new HeaderFooterPageEvent(generationTime, asOfDateStr));
            document.open();

            // 1. Title Banner
            addReportHeader(document, displayName, userEmail, asOfDateStr, generationTime);

            // 2. Executive KPI Cards
            addExecutiveSummaryKpis(
                    document,
                    totalOriginalPrincipal,
                    totalRemainingPrincipal,
                    totalRepaidPrincipal,
                    totalMonthlyEmi,
                    totalRemainingLoanAmount,
                    totalOriginalTenure,
                    totalElapsedEmis,
                    totalActuallyPaidEmis,
                    totalRemainingScheduledEmis,
                    totalOverdueCount,
                    totalOverdueAmount,
                    activeLoansCount,
                    closedLoansCount,
                    pausedLoansCount,
                    nearestClosureDate,
                    latestClosureDate
            );

            // 3. Cash Flow & Monthly Income Context
            addCashFlowContext(
                    document,
                    hasSalaryConfigured,
                    monthlySalary,
                    totalMonthlyEmi,
                    currentMonthDueEmis,
                    currentMonthPaidEmis,
                    currentMonthRemainingEmis,
                    totalExpensesInCycle,
                    remainingSalaryAfterEmis,
                    netSalaryAfterAll,
                    cycleStart,
                    cycleEnd
            );

            // 4. Portfolio Loans Consolidated Table
            addPortfolioComparisonTable(document, loansWithSchedule);

            // 5. Financial Definitions Glossary
            addGlossarySection(document);

            // 6. Detailed Breakdown for Each Loan
            for (LoanResponse loan : loansWithSchedule) {
                document.newPage();
                addIndividualLoanSection(document, loan, today);
            }

            document.close();
            return baos.toByteArray();
        } catch (DocumentException e) {
            throw new RuntimeException("Failed to generate loan portfolio PDF", e);
        }
    }

    private void addReportHeader(Document document, String displayName, String userEmail, String asOfDate, String generationTime) throws DocumentException {
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{65f, 35f});
        headerTable.setSpacingAfter(10f);

        // Left: Branding & Title
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, COLOR_PRIMARY);
        Paragraph brand = new Paragraph("SEVERUS FINANCE • PORTFOLIO AUDIT REPORT", brandFont);
        brand.setSpacingAfter(2f);
        leftCell.addElement(brand);

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 17, COLOR_TEXT_MAIN);
        Paragraph title = new Paragraph("Loan Portfolio & Liabilities Audit", titleFont);
        title.setSpacingAfter(2f);
        leftCell.addElement(title);

        Font descFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED);
        Paragraph desc = new Paragraph("Consolidated view of outstanding principals, fixed schedules, and debt obligations.", descFont);
        leftCell.addElement(desc);

        // Right: Metadata Box
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.BOX);
        rightCell.setBorderColor(COLOR_BORDER);
        rightCell.setBackgroundColor(COLOR_BG_LIGHT);
        rightCell.setPadding(6f);

        Font metaLabelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_MUTED);
        Font metaValFont = FontFactory.getFont(FontFactory.HELVETICA, 8f, COLOR_TEXT_MAIN);

        rightCell.addElement(new Paragraph("Account Holder: " + displayName, metaValFont));
        rightCell.addElement(new Paragraph("Email: " + userEmail, metaLabelFont));
        rightCell.addElement(new Paragraph("Report As-Of: " + asOfDate, metaValFont));
        rightCell.addElement(new Paragraph("Generated: " + generationTime, metaLabelFont));

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);
    }

    private void addExecutiveSummaryKpis(
            Document document,
            BigDecimal originalPrincipal,
            BigDecimal remainingPrincipal,
            BigDecimal repaidPrincipal,
            BigDecimal monthlyEmi,
            BigDecimal remainingLoanAmount,
            int totalTenure,
            int elapsedEmis,
            int paidEmis,
            int remainingEmis,
            int overdueCount,
            BigDecimal overdueAmount,
            int activeLoans,
            int closedLoans,
            int pausedLoans,
            LocalDate nearestClosure,
            LocalDate latestClosure
    ) throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, COLOR_PRIMARY_DARK);
        Paragraph sectionHeader = new Paragraph("1. Executive Portfolio Summary", sectionTitleFont);
        sectionHeader.setSpacingAfter(5f);
        document.add(sectionHeader);

        PdfPTable kpiTable = new PdfPTable(4);
        kpiTable.setWidthPercentage(100);
        kpiTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        kpiTable.setSpacingAfter(8f);

        // Row 1: Principals and Monthly EMI
        kpiTable.addCell(createKpiCell("ORIGINAL PRINCIPAL", formatInr(originalPrincipal), "Total borrowed across " + (activeLoans + closedLoans + pausedLoans) + " loans"));
        kpiTable.addCell(createKpiCell("REMAINING PRINCIPAL", formatInr(remainingPrincipal), "Current outstanding principal balance"));
        
        double progress = originalPrincipal.compareTo(BigDecimal.ZERO) > 0
                ? repaidPrincipal.divide(originalPrincipal, 4, RoundingMode.HALF_EVEN).doubleValue() * 100
                : 0.0;
        kpiTable.addCell(createKpiCell("PRINCIPAL REPAID", formatInr(repaidPrincipal), String.format("%.1f%% of original principal", progress)));
        kpiTable.addCell(createKpiCell("MONTHLY EMI COMMITMENT", formatInr(monthlyEmi) + "/mo", activeLoans + " Active loan commitments"));

        // Row 2: Remaining Loan Amount, Tenure, Paid, Overdue
        kpiTable.addCell(createKpiCell("REMAINING LOAN AMOUNT", formatInr(remainingLoanAmount), "Fixed EMI × Remaining scheduled EMIs"));
        kpiTable.addCell(createKpiCell("SCHEDULED EMIs LEFT", remainingEmis + " EMIs", "Total tenure: " + totalTenure + " mos (" + elapsedEmis + " elapsed)"));
        kpiTable.addCell(createKpiCell("ACTUALLY PAID EMIs", paidEmis + " EMIs", "Explicitly recorded verified payments"));
        
        String overdueSub = overdueCount > 0 ? formatInr(overdueAmount) + " unpaid past due" : "All installments up to date";
        kpiTable.addCell(createKpiCell("OVERDUE EMIs", overdueCount + " Overdue", overdueSub, overdueCount > 0 ? COLOR_TEXT_ROSE : COLOR_TEXT_EMERALD));

        document.add(kpiTable);

        // Portfolio Status Strip
        PdfPTable stripTable = new PdfPTable(3);
        stripTable.setWidthPercentage(100);
        stripTable.setWidths(new float[]{33f, 33f, 34f});
        stripTable.setSpacingAfter(8f);

        stripTable.addCell(createStripCell("Active Loans: " + activeLoans + "  |  Closed: " + closedLoans + "  |  Paused: " + pausedLoans));
        stripTable.addCell(createStripCell("Nearest Loan Closure: " + (nearestClosure != null ? DATE_FMT.format(nearestClosure) : "N/A")));
        stripTable.addCell(createStripCell("Final Portfolio Closure: " + (latestClosure != null ? DATE_FMT.format(latestClosure) : "N/A")));

        document.add(stripTable);
    }

    private void addCashFlowContext(
            Document document,
            boolean hasSalaryConfigured,
            BigDecimal monthlySalary,
            BigDecimal totalMonthlyEmi,
            BigDecimal currentMonthDueEmis,
            BigDecimal currentMonthPaidEmis,
            BigDecimal currentMonthRemainingEmis,
            BigDecimal totalExpensesInCycle,
            BigDecimal remainingSalaryAfterEmis,
            BigDecimal netSalaryAfterAll,
            LocalDate cycleStart,
            LocalDate cycleEnd
    ) throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PRIMARY_DARK);
        Paragraph sectionHeader = new Paragraph("2. Monthly Cash Flow & Income Context (" + DATE_FMT.format(cycleStart) + " to " + DATE_FMT.format(cycleEnd) + ")", sectionTitleFont);
        sectionHeader.setSpacingAfter(4f);
        document.add(sectionHeader);

        PdfPTable cfTable = new PdfPTable(4);
        cfTable.setWidthPercentage(100);
        cfTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        cfTable.setSpacingAfter(8f);

        String salaryStr = hasSalaryConfigured ? formatInr(monthlySalary) : "Not Configured";
        String remAfterEmiStr = hasSalaryConfigured ? formatInr(remainingSalaryAfterEmis) : "Unavailable";
        String netRemStr = hasSalaryConfigured ? formatInr(netSalaryAfterAll) : "Unavailable";

        cfTable.addCell(createKpiCell("MONTHLY SALARY / INCOME", salaryStr, hasSalaryConfigured ? "Configured in Settings" : "Set in Settings to unlock"));
        cfTable.addCell(createKpiCell("CYCLE EXPENSES", formatInr(totalExpensesInCycle), "Fixed & variable recurring expenses"));
        cfTable.addCell(createKpiCell("SALARY AFTER EMIs", remAfterEmiStr, "Salary − Monthly EMI Commitment"));
        cfTable.addCell(createKpiCell("NET CASH FLOW REMAINING", netRemStr, "Salary − (Cycle EMIs + Expenses)", (netSalaryAfterAll != null && netSalaryAfterAll.compareTo(BigDecimal.ZERO) < 0) ? COLOR_TEXT_ROSE : COLOR_TEXT_EMERALD));

        document.add(cfTable);

        // Current Month EMI breakdown note
        PdfPTable noteTable = new PdfPTable(1);
        noteTable.setWidthPercentage(100);
        noteTable.setSpacingAfter(8f);
        PdfPCell noteCell = new PdfPCell();
        noteCell.setBorder(Rectangle.BOX);
        noteCell.setBorderColor(COLOR_BORDER);
        noteCell.setBackgroundColor(COLOR_BG_LIGHT);
        noteCell.setPadding(5f);

        Font noteFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_MAIN);
        noteCell.addElement(new Paragraph(
                "Current Month EMIs Breakdown: Total Due: " + formatInr(currentMonthDueEmis) +
                        "  •  Paid so far: " + formatInr(currentMonthPaidEmis) +
                        "  •  Remaining to be paid this month: " + formatInr(currentMonthRemainingEmis),
                noteFont
        ));
        noteTable.addCell(noteCell);
        document.add(noteTable);
    }

    private void addPortfolioComparisonTable(Document document, List<LoanResponse> loans) throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PRIMARY_DARK);
        Paragraph sectionHeader = new Paragraph("3. Loan Portfolio Overview Table", sectionTitleFont);
        sectionHeader.setSpacingAfter(4f);
        document.add(sectionHeader);

        if (loans.isEmpty()) {
            Paragraph emptyPara = new Paragraph("No loans currently recorded in the portfolio.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8.5f, COLOR_TEXT_MUTED));
            document.add(emptyPara);
            return;
        }

        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{18f, 10f, 13f, 13f, 12f, 11f, 11f, 12f});
        table.setHeaderRows(1);
        table.setSpacingAfter(8f);

        // Headers
        String[] headers = {"Loan & Lender", "Status", "Orig. Principal", "Rem. Principal", "Monthly EMI", "Tenure / Rem.", "Next EMI", "Rem. Balance"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
            cell.setBackgroundColor(COLOR_PRIMARY_DARK);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        boolean alternate = false;
        for (LoanResponse loan : loans) {
            Color rowBg = alternate ? COLOR_BG_LIGHT : Color.WHITE;
            alternate = !alternate;

            // 1. Name & Lender
            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(rowBg);
            nameCell.setPadding(3.5f);
            nameCell.addElement(new Paragraph(loan.name(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_MAIN)));
            nameCell.addElement(new Paragraph(loan.lenderOrBank(), FontFactory.getFont(FontFactory.HELVETICA, 6.5f, COLOR_TEXT_MUTED)));
            table.addCell(nameCell);

            // 2. Status
            PdfPCell statusCell = createBadgeCell(loan.status().name(), rowBg);
            table.addCell(statusCell);

            // 3. Orig Principal
            table.addCell(createTextCell(formatInr(loan.originalPrincipal()), Element.ALIGN_RIGHT, rowBg));

            // 4. Rem Principal
            table.addCell(createTextCell(formatInr(loan.remainingPrincipal() != null ? loan.remainingPrincipal() : loan.currentOutstandingPrincipal()), Element.ALIGN_RIGHT, rowBg));

            // 5. EMI
            table.addCell(createTextCell(formatInr(loan.emiAmount()), Element.ALIGN_RIGHT, rowBg));

            // 6. Tenure / Remaining
            table.addCell(createTextCell(loan.totalEmiCount() + "m (" + loan.remainingEmiCount() + " left)", Element.ALIGN_CENTER, rowBg));

            // 7. Next EMI
            String nextEmiStr = loan.nextEmiDate() != null ? DATE_FMT.format(loan.nextEmiDate()) : (loan.remainingEmiCount() == 0 ? "Completed" : "N/A");
            table.addCell(createTextCell(nextEmiStr, Element.ALIGN_CENTER, rowBg));

            // 8. Rem Balance
            table.addCell(createTextCell(formatInr(loan.remainingLoanAmount()), Element.ALIGN_RIGHT, rowBg));
        }

        document.add(table);
    }

    private void addGlossarySection(Document document) throws DocumentException {
        Font sectionTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_PRIMARY_DARK);
        Paragraph sectionHeader = new Paragraph("4. Financial Definitions & Calculation Rules Glossary", sectionTitleFont);
        sectionHeader.setSpacingAfter(4f);
        document.add(sectionHeader);

        PdfPTable glossaryTable = new PdfPTable(2);
        glossaryTable.setWidthPercentage(100);
        glossaryTable.setWidths(new float[]{50f, 50f});
        glossaryTable.setSpacingAfter(5f);

        PdfPCell col1 = new PdfPCell();
        col1.setBorder(Rectangle.BOX);
        col1.setBorderColor(COLOR_BORDER);
        col1.setBackgroundColor(COLOR_BG_LIGHT);
        col1.setPadding(5f);

        addGlossaryItem(col1, "Original Principal", "The initial sanctioned borrowed amount from the lender.");
        addGlossaryItem(col1, "Remaining Principal", "Current outstanding principal balance. Decreases only upon verified installment payments or prepayments.");
        addGlossaryItem(col1, "Remaining Loan Amount", "Fixed Monthly EMI × Remaining Scheduled EMIs. The total scheduled future cash commitment (independent of principal amortization).");

        PdfPCell col2 = new PdfPCell();
        col2.setBorder(Rectangle.BOX);
        col2.setBorderColor(COLOR_BORDER);
        col2.setBackgroundColor(COLOR_BG_LIGHT);
        col2.setPadding(5f);

        addGlossaryItem(col2, "Elapsed EMIs", "Installments whose scheduled due date has arrived (dueDate <= report date). Capped at total tenure.");
        addGlossaryItem(col2, "Actually Paid EMIs", "Installments explicitly verified and marked as paid by the user. Never marked paid automatically.");
        addGlossaryItem(col2, "Overdue EMIs", "Installments whose due date has passed without recorded payment (dueDate < report date and paymentState != PAID).");

        glossaryTable.addCell(col1);
        glossaryTable.addCell(col2);
        document.add(glossaryTable);
    }

    private void addIndividualLoanSection(Document document, LoanResponse loan, LocalDate today) throws DocumentException {
        // Loan Header Banner
        PdfPTable loanHeader = new PdfPTable(2);
        loanHeader.setWidthPercentage(100);
        loanHeader.setWidths(new float[]{70f, 30f});
        loanHeader.setSpacingAfter(6f);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(0);

        Font nameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, COLOR_TEXT_MAIN);
        leftCell.addElement(new Paragraph(loan.name(), nameFont));

        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 8, COLOR_TEXT_MUTED);
        String accountStr = loan.maskedAccountNumber() != null && !loan.maskedAccountNumber().isBlank() ?
                " • Acc: " + loan.maskedAccountNumber() : "";
        leftCell.addElement(new Paragraph(loan.lenderOrBank() + " • " + loan.loanType().name().replace('_', ' ') + accountStr, subFont));

        Font idFont = FontFactory.getFont(FontFactory.HELVETICA, 7, COLOR_TEXT_MUTED);
        leftCell.addElement(new Paragraph("Loan ID: " + loan.id(), idFont));

        PdfPCell rightCell = createBadgeCell(loan.status().name(), Color.WHITE);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        loanHeader.addCell(leftCell);
        loanHeader.addCell(rightCell);
        document.add(loanHeader);

        // Parameters Table (Grid of all 15 loan attributes)
        PdfPTable paramsTable = new PdfPTable(4);
        paramsTable.setWidthPercentage(100);
        paramsTable.setWidths(new float[]{25f, 25f, 25f, 25f});
        paramsTable.setSpacingAfter(8f);

        paramsTable.addCell(createParamCell("Original Principal", formatInr(loan.originalPrincipal())));
        paramsTable.addCell(createParamCell("Remaining Principal", formatInr(loan.remainingPrincipal() != null ? loan.remainingPrincipal() : loan.currentOutstandingPrincipal())));
        paramsTable.addCell(createParamCell("Monthly EMI Amount", formatInr(loan.emiAmount())));
        paramsTable.addCell(createParamCell("Annual Interest Rate", loan.annualInterestRate() + "% p.a. (" + loan.calculationMode() + ")"));

        paramsTable.addCell(createParamCell("Original Total Tenure", loan.totalEmiCount() + " Months"));
        paramsTable.addCell(createParamCell("Elapsed EMIs", loan.elapsedEmiCount() + " / " + loan.totalEmiCount()));
        paramsTable.addCell(createParamCell("Actually Paid EMIs", loan.paidEmiCount() + " EMIs"));
        paramsTable.addCell(createParamCell("Remaining Scheduled EMIs", loan.remainingEmiCount() + " EMIs"));

        paramsTable.addCell(createParamCell("Loan Start Date", loan.startDate() != null ? DATE_FMT.format(loan.startDate()) : "N/A"));
        paramsTable.addCell(createParamCell("First EMI Date", loan.firstEmiDate() != null ? DATE_FMT.format(loan.firstEmiDate()) : "N/A"));
        paramsTable.addCell(createParamCell("Expected End Date", loan.endDate() != null ? DATE_FMT.format(loan.endDate()) : "N/A"));
        paramsTable.addCell(createParamCell("Next Upcoming EMI", loan.nextEmiDate() != null ? DATE_FMT.format(loan.nextEmiDate()) : (loan.remainingEmiCount() == 0 ? "Fully Elapsed" : "None scheduled")));

        paramsTable.addCell(createParamCell("EMI Due Day", loan.emiDueDay() + "th of each month"));
        paramsTable.addCell(createParamCell("Overdue EMIs", loan.overdueEmiCount() + " Overdue", loan.overdueEmiCount() > 0 ? COLOR_TEXT_ROSE : COLOR_TEXT_EMERALD));
        paramsTable.addCell(createParamCell("Remaining Loan Amount", formatInr(loan.remainingLoanAmount()), COLOR_PRIMARY));
        paramsTable.addCell(createParamCell("Principal Balance Override", loan.outstandingPrincipalManualOverride() ? "Yes (Manual)" : "No (Amortized)"));

        document.add(paramsTable);

        // Notes and Data-Quality Warnings
        if (loan.installments() == null || loan.installments().size() < loan.totalEmiCount()) {
            PdfPTable warnTable = new PdfPTable(1);
            warnTable.setWidthPercentage(100);
            warnTable.setSpacingAfter(6f);
            PdfPCell warnCell = new PdfPCell(new Phrase("⚠ Schedule Discrepancy: Stored installments (" +
                    (loan.installments() != null ? loan.installments().size() : 0) +
                    ") are fewer than authoritative total tenure (" + loan.totalEmiCount() + "). Automatic reconciliation will repair future rows.",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_TEXT_ROSE)));
            warnCell.setBackgroundColor(COLOR_BG_ROSE);
            warnCell.setBorderColor(COLOR_TEXT_ROSE);
            warnCell.setPadding(4f);
            warnTable.addCell(warnCell);
            document.add(warnTable);
        }

        if (loan.notes() != null && !loan.notes().isBlank()) {
            Paragraph notesPara = new Paragraph("Notes: " + loan.notes(), FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 7.5f, COLOR_TEXT_MUTED));
            notesPara.setSpacingAfter(5f);
            document.add(notesPara);
        }

        // Complete Installment Schedule Table
        Font schedTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9.5f, COLOR_PRIMARY_DARK);
        Paragraph schedHeader = new Paragraph("Amortization & Installment Schedule (" + (loan.installments() != null ? loan.installments().size() : 0) + " Installments)", schedTitleFont);
        schedHeader.setSpacingAfter(4f);
        document.add(schedHeader);

        if (loan.installments() == null || loan.installments().isEmpty()) {
            document.add(new Paragraph("No installments found for this loan.", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8f, COLOR_TEXT_MUTED)));
            return;
        }

        PdfPTable scheduleTable = new PdfPTable(6);
        scheduleTable.setWidthPercentage(100);
        scheduleTable.setWidths(new float[]{8f, 18f, 22f, 16f, 18f, 18f});
        scheduleTable.setHeaderRows(1);
        scheduleTable.setSpacingAfter(10f);

        String[] schedHeaders = {"EMI #", "Due Date", "Scheduled EMI", "Status", "Payment Date", "Actual Paid"};
        for (String h : schedHeaders) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, Color.WHITE)));
            cell.setBackgroundColor(COLOR_PRIMARY_DARK);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(3.5f);
            scheduleTable.addCell(cell);
        }

        boolean alt = false;
        for (LoanInstallmentResponse inst : loan.installments()) {
            Color rowBg = alt ? COLOR_BG_LIGHT : Color.WHITE;
            alt = !alt;

            scheduleTable.addCell(createTextCell("#" + inst.installmentNumber(), Element.ALIGN_CENTER, rowBg));
            scheduleTable.addCell(createTextCell(DATE_FMT.format(inst.dueDate()), Element.ALIGN_CENTER, rowBg));
            scheduleTable.addCell(createTextCell(formatInr(inst.scheduledAmount()), Element.ALIGN_RIGHT, rowBg));

            // Status Badge
            String statusText;
            Color statusFg;
            Color statusBg;
            if (inst.paymentState() == PaymentState.PAID) {
                statusText = "PAID";
                statusFg = COLOR_TEXT_EMERALD;
                statusBg = COLOR_BG_EMERALD;
            } else if (inst.overdue() || inst.dueDate().isBefore(today)) {
                statusText = "OVERDUE";
                statusFg = COLOR_TEXT_ROSE;
                statusBg = COLOR_BG_ROSE;
            } else {
                statusText = "UNPAID";
                statusFg = COLOR_TEXT_AMBER;
                statusBg = COLOR_BG_AMBER;
            }

            PdfPCell badgeCell = new PdfPCell(new Phrase(statusText, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, statusFg)));
            badgeCell.setBackgroundColor(statusBg);
            badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            badgeCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            badgeCell.setPadding(3f);
            badgeCell.setBorderColor(COLOR_BORDER);
            scheduleTable.addCell(badgeCell);

            String paidDateStr = inst.paidDate() != null ? DATE_FMT.format(inst.paidDate()) : "-";
            scheduleTable.addCell(createTextCell(paidDateStr, Element.ALIGN_CENTER, rowBg));

            String actualPaidStr = inst.actualPaidAmount() != null ? formatInr(inst.actualPaidAmount()) : "-";
            scheduleTable.addCell(createTextCell(actualPaidStr, Element.ALIGN_RIGHT, rowBg));
        }

        document.add(scheduleTable);
    }

    // Helper UI Cell Builders
    private PdfPCell createKpiCell(String label, String value, String subtitle) {
        return createKpiCell(label, value, subtitle, COLOR_TEXT_MAIN);
    }

    private PdfPCell createKpiCell(String label, String value, String subtitle, Color valColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setPadding(5f);

        Paragraph pLabel = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_TEXT_MUTED));
        pLabel.setSpacingAfter(1f);
        cell.addElement(pLabel);

        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10.5f, valColor));
        pVal.setSpacingAfter(1f);
        cell.addElement(pVal);

        Paragraph pSub = new Paragraph(subtitle, FontFactory.getFont(FontFactory.HELVETICA, 6f, COLOR_TEXT_MUTED));
        cell.addElement(pSub);

        return cell;
    }

    private PdfPCell createStripCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY_DARK)));
        cell.setBackgroundColor(COLOR_BG_ALT);
        cell.setBorderColor(COLOR_BORDER);
        cell.setPadding(4f);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    private PdfPCell createParamCell(String label, String value) {
        return createParamCell(label, value, COLOR_TEXT_MAIN);
    }

    private PdfPCell createParamCell(String label, String value, Color valColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(COLOR_BORDER);
        cell.setBackgroundColor(COLOR_BG_LIGHT);
        cell.setPadding(4f);

        cell.addElement(new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 6.5f, COLOR_TEXT_MUTED)));
        cell.addElement(new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, valColor)));
        return cell;
    }

    private PdfPCell createTextCell(String text, int align, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 7.5f, COLOR_TEXT_MAIN)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3.5f);
        return cell;
    }

    private PdfPCell createBadgeCell(String status, Color rowBg) {
        Color fg = "ACTIVE".equalsIgnoreCase(status) ? COLOR_PRIMARY : COLOR_TEXT_MUTED;
        Color bg = "ACTIVE".equalsIgnoreCase(status) ? COLOR_BG_ALT : rowBg;

        PdfPCell cell = new PdfPCell(new Phrase(status, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7f, fg)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(COLOR_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        return cell;
    }

    private void addGlossaryItem(PdfPCell cell, String term, String definition) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(term + ": ", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7.5f, COLOR_PRIMARY_DARK)));
        p.add(new Chunk(definition, FontFactory.getFont(FontFactory.HELVETICA, 7f, COLOR_TEXT_MAIN)));
        p.setSpacingAfter(3f);
        cell.addElement(p);
    }

    public static String formatInr(BigDecimal amount) {
        if (amount == null) return "INR 0.00";
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("en", "IN"));
        DecimalFormat df = new DecimalFormat("##,##,##0.00", symbols);
        return "INR " + df.format(amount);
    }

    // Page Event for Page Numbers and Running Header
    public static class HeaderFooterPageEvent extends PdfPageEventHelper {
        private final String generationTime;
        private final String asOfDate;
        private PdfTemplate totalPagesTemplate;
        private BaseFont baseFont;

        public HeaderFooterPageEvent(String generationTime, String asOfDate) {
            this.generationTime = generationTime;
            this.asOfDate = asOfDate;
        }

        @Override
        public void onOpenDocument(PdfWriter writer, Document document) {
            totalPagesTemplate = writer.getDirectContent().createTemplate(30, 16);
            try {
                baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            } catch (Exception e) {
                // Ignore font creation error
            }
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            // Running Header on pages > 1
            if (writer.getPageNumber() > 1 && baseFont != null) {
                cb.beginText();
                cb.setFontAndSize(baseFont, 7.5f);
                cb.setColorFill(new Color(148, 163, 184));
                cb.showTextAligned(Element.ALIGN_LEFT, "SEVERUS FINANCE • LOAN PORTFOLIO REPORT", document.left(), document.top() + 10, 0);
                cb.showTextAligned(Element.ALIGN_RIGHT, "As of: " + asOfDate, document.right(), document.top() + 10, 0);
                cb.endText();

                cb.setColorStroke(new Color(226, 232, 240));
                cb.setLineWidth(0.5f);
                cb.moveTo(document.left(), document.top() + 5);
                cb.lineTo(document.right(), document.top() + 5);
                cb.stroke();
            }

            // Running Footer on all pages
            if (baseFont != null) {
                float footerY = document.bottom() - 14;
                cb.setColorStroke(new Color(226, 232, 240));
                cb.setLineWidth(0.5f);
                cb.moveTo(document.left(), footerY + 10);
                cb.lineTo(document.right(), footerY + 10);
                cb.stroke();

                cb.beginText();
                cb.setFontAndSize(baseFont, 7f);
                cb.setColorFill(new Color(148, 163, 184));
                cb.showTextAligned(Element.ALIGN_LEFT, "Confidential • Generated on " + generationTime, document.left(), footerY, 0);

                String pageText = "Page " + writer.getPageNumber() + " of ";
                cb.showTextAligned(Element.ALIGN_RIGHT, pageText, document.right() - 16, footerY, 0);
                cb.endText();

                cb.addTemplate(totalPagesTemplate, document.right() - 16, footerY);
            }
        }

        @Override
        public void onCloseDocument(PdfWriter writer, Document document) {
            if (totalPagesTemplate != null && baseFont != null) {
                totalPagesTemplate.beginText();
                totalPagesTemplate.setFontAndSize(baseFont, 7f);
                totalPagesTemplate.setColorFill(new Color(148, 163, 184));
                totalPagesTemplate.showText(String.valueOf(writer.getPageNumber()));
                totalPagesTemplate.endText();
            }
        }
    }
}
