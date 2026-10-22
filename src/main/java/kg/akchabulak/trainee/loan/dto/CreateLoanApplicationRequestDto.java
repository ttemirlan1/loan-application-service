package kg.akchabulak.trainee.loan.dto;

import jakarta.validation.constraints.*;
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
    @Pattern(
            regexp = "\\+996\\d{9}",
            message = "Телефон должен быть в формате +996XXXXXXXXX"
    )
    private String phone;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal amount;

    @NotNull
    @Min(1)
    @Max(60) // для примера макс 60 сделал
    private Integer termMonths;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    private BigDecimal monthlyIncome;
}