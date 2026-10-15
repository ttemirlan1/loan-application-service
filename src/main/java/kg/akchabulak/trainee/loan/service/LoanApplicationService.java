package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.mapper.LoanApplicationMapper;
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
    private final LoanApplicationMapper mapper;

    private static final List<LoanApplicationStatus> ACTIVE_STATUSES =
            List.of(
                    LoanApplicationStatus.NEW,
                    LoanApplicationStatus.IN_REVIEW
            );

    @Transactional
    public LoanApplicationResponseDto create(
            CreateLoanApplicationRequestDto request) {

        boolean exists = repository.existsByClientPinAndStatusIn(
                request.getClientPin(),
                ACTIVE_STATUSES
        );

        if (exists) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "У клиента уже есть активная заявка"
            );
        }

        LoanApplication entity = mapper.toEntity(request);
        entity.setStatus(LoanApplicationStatus.NEW);
        LoanApplication saved = repository.save(entity);
        return mapper.toResponseDto(saved);

    }

    @Transactional(readOnly = true)
    public LoanApplicationResponseDto getById(Long id) {

        LoanApplication application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Заявка не найдена"
                ));

        return mapper.toResponseDto(application);
    }

}