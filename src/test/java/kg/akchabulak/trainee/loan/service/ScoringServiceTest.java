package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.config.ScoringProperties;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.scoring.ScoringResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ScoringServiceTest {

    private ScoringService service;

    @BeforeEach
    void setUp() {
        ScoringProperties properties = new ScoringProperties();
        properties.setMaxAmount(new BigDecimal("300000.00"));
        properties.setMaxPaymentIncomeRatio(new BigDecimal("0.50"));
        service = new ScoringService(properties);
    }

    @Test
    void score_whenWithinBothLimits_approvesApplication() {
        ScoringResult result = score("120000.00", "30000.00", 12);
        assertEquals(LoanApplicationStatus.APPROVED, result.status());
        assertNull(result.rejectReason());
    }

    @ParameterizedTest(name = "amount={0} => status={1}")
    @CsvSource({
            "149999.99, APPROVED",
            "150000.00, APPROVED",
            "150000.01, REJECTED"
    })
    void score_checksFiftyPercentIncomeBoundary(
            String amount, LoanApplicationStatus expectedStatus) {
        // Доход 25 000; половина дохода за 12 месяцев = 150 000.
        ScoringResult result = score(amount, "25000.00", 12);
        assertEquals(expectedStatus, result.status());
        if (expectedStatus == LoanApplicationStatus.REJECTED) {
            assertEquals("Платёж превышает 50% дохода", result.rejectReason());
        } else {
            assertNull(result.rejectReason());
        }
    }

    @ParameterizedTest(name = "amount={0} => status={1}")
    @CsvSource({
            "299999.99, APPROVED",
            "300000.00, APPROVED",
            "300000.01, REJECTED"
    })
    void score_checksMaximumLoanAmountBoundary(
            String amount, LoanApplicationStatus expectedStatus) {
        // Высокий доход нужен, чтобы проверять только лимит суммы.
        ScoringResult result = score(amount, "100000.00", 12);
        assertEquals(expectedStatus, result.status());
        if (expectedStatus == LoanApplicationStatus.REJECTED) {
            assertEquals("Превышен лимит суммы", result.rejectReason());
        } else {
            assertNull(result.rejectReason());
        }
    }

    @Test
    void score_whenBothLimitsExceeded_returnsIncomeLimitReasonFirst() {
        ScoringResult result = score("300000.01", "1000.00", 12);
        assertEquals(LoanApplicationStatus.REJECTED, result.status());
        assertEquals("Платёж превышает 50% дохода", result.rejectReason());
    }

    private ScoringResult score(String amount, String income, int termMonths) {
        LoanApplication application = new LoanApplication();
        application.setAmount(new BigDecimal(amount));
        application.setMonthlyIncome(new BigDecimal(income));
        application.setTermMonths(termMonths);
        return service.score(application);
    }
}
