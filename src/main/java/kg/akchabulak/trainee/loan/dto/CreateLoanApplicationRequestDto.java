package kg.akchabulak.trainee.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanApplicationRequestDto {

    @NotBlank(message = "ПИН клиента обязателен")
    @Pattern(
            regexp = "\\d{14}",
            message = "ПИН должен содержать 14 цифр"
    )
    private String clientPin;

    @NotBlank
    private String clientFullName;

    @NotBlank
    private String phone;

    @NotNull(message = "Сумма кредита обязательна")
    @DecimalMin(
            value = "0.01",
            message = "Сумма кредита должна быть больше нуля"
    )
    private BigDecimal amount;

    @NotNull
    private Integer termMonths;

    @NotNull
    private BigDecimal monthlyIncome;
}