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

    @NotBlank
    @Pattern(regexp = "\\d{14}")
    private String clientPin;

    @NotBlank
    private String clientFullName;

    @NotBlank
    private String phone;
    
    @NotNull
    @DecimalMin("1000")
    private BigDecimal amount;

    @NotNull
    private Integer termMonths;

    @NotNull
    private BigDecimal monthlyIncome;
}