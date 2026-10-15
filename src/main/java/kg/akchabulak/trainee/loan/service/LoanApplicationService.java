package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository repository;

    private static final List<LoanApplicationStatus> ACTIVE =
            List.of(
                    LoanApplicationStatus.NEW,
                    LoanApplicationStatus.IN_REVIEW
            );

    @Transactional
    public LoanApplicationResponseDto create(
            CreateLoanApplicationRequestDto request) {

        boolean exists = repository.existsByClientPinAndStatusIn(
                request.getClientPin(),
                ACTIVE
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У клиента уже есть активная заявка"
            );
        }

        LoanApplication application = new LoanApplication();

        application.setClientPin(request.getClientPin());
        application.setClientFullName(request.getClientFullName());
        application.setPhone(request.getPhone());
        application.setAmount(request.getAmount());
        application.setTermMonths(request.getTermMonths());
        application.setMonthlyIncome(request.getMonthlyIncome());
        application.setStatus(LoanApplicationStatus.NEW);

        return toResponse(repository.save(application));
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponseDto getById(Long id) {

        LoanApplication application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Заявка не найдена"
                ));

        return toResponse(application);
    }

    private LoanApplicationResponseDto toResponse(
            LoanApplication application) {

        return new LoanApplicationResponseDto(
                application.getId(),
                application.getClientPin(),
                application.getClientFullName(),
                application.getPhone(),
                application.getAmount(),
                application.getTermMonths(),
                application.getMonthlyIncome(),
                application.getStatus(),
                application.getRejectReason(),
                application.getCreatedAt(),
                application.getUpdatedAt()
        );
    }
}