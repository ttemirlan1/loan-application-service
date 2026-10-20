package kg.akchabulak.trainee.loan.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "loan.scoring")
public class ScoringProperties {

    private BigDecimal maxAmount;
    private BigDecimal maxPaymentIncomeRatio;
}