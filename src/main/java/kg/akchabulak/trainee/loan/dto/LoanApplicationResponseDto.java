package kg.akchabulak.trainee.loan.dto;

import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponseDto {

    private Long id;
    private String clientPin;
    private String clientFullName;
    private String phone;
    private BigDecimal amount;
    private Integer termMonths;
    private BigDecimal monthlyIncome;
    private LoanApplicationStatus status;
    private String rejectReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}