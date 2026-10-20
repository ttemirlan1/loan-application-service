package kg.akchabulak.trainee.loan.scoring;

import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;

public record ScoringResult(
        LoanApplicationStatus status,
        String rejectReason
) {
}