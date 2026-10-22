package kg.akchabulak.trainee.loan.messaging.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanScoringMessage {

    private Long applicationId;
    private BigDecimal amount;
    private Integer termMonths;
    private BigDecimal monthlyIncome;
}