package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.config.ScoringProperties;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.scoring.ScoringResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ScoringService {

    private final ScoringProperties properties;

    public ScoringResult score(LoanApplication application) {

        BigDecimal amount = application.getAmount();
        BigDecimal income = application.getMonthlyIncome();
        int termMonths = application.getTermMonths();

        // Проверка допустимого платежа относительно дохода
        BigDecimal maxAffordableAmount = income
                .multiply(properties.getMaxPaymentIncomeRatio())
                .multiply(BigDecimal.valueOf(termMonths));

        if (amount.compareTo(maxAffordableAmount) > 0) {
            return new ScoringResult(
                    LoanApplicationStatus.REJECTED,
                    "Платёж превышает 50% дохода"
            );
        }

        // Проверка максимальной суммы кредита
        if (amount.compareTo(properties.getMaxAmount()) > 0) {
            return new ScoringResult(
                    LoanApplicationStatus.REJECTED,
                    "Превышен лимит суммы"
            );
        }

        return new ScoringResult(
                LoanApplicationStatus.APPROVED,
                null
        );
    }
}