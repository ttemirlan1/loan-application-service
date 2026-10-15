package kg.akchabulak.trainee.loan.mapper;

import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import org.springframework.stereotype.Component;

@Component
public class LoanApplicationMapper {

    public LoanApplication toEntity(
            CreateLoanApplicationRequestDto dto) {

        LoanApplication entity = new LoanApplication();

        entity.setClientPin(dto.getClientPin());
        entity.setClientFullName(dto.getClientFullName());
        entity.setPhone(dto.getPhone());
        entity.setAmount(dto.getAmount());
        entity.setTermMonths(dto.getTermMonths());
        entity.setMonthlyIncome(dto.getMonthlyIncome());

        return entity;
    }

    public LoanApplicationResponseDto toResponseDto(
            LoanApplication entity) {

        return new LoanApplicationResponseDto(
                entity.getId(),
                entity.getClientPin(),
                entity.getClientFullName(),
                entity.getPhone(),
                entity.getAmount(),
                entity.getTermMonths(),
                entity.getMonthlyIncome(),
                entity.getStatus(),
                entity.getRejectReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}