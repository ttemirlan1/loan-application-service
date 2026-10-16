package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.exception.LoanApplicationConflictException;
import kg.akchabulak.trainee.loan.exception.LoanApplicationNotFoundException;
import kg.akchabulak.trainee.loan.mapper.LoanApplicationMapper;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import kg.akchabulak.trainee.loan.validator.LoanStatusTransitionValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository repository;
    private final LoanApplicationMapper mapper;

    private final LoanStatusTransitionValidator transitionValidator;
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
            throw new LoanApplicationConflictException(
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
                .orElseThrow(
                        () -> new LoanApplicationNotFoundException(id)
                );

        return mapper.toResponseDto(application);
    }

    @Transactional(readOnly = true)
    public Page<LoanApplicationResponseDto> getAll(
            LoanApplicationStatus status,
            String clientPin,
            int page,
            int size
    ) {
        Specification<LoanApplication> specification =
                (root, query, cb) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    if (status != null) {
                        predicates.add(
                                cb.equal(root.get("status"), status)
                        );
                    }

                    if (clientPin != null && !clientPin.isBlank()) {
                        predicates.add(
                                cb.equal(root.get("clientPin"), clientPin)
                        );
                    }

                    return cb.and(predicates.toArray(new Predicate[0]));
                };

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return repository.findAll(specification, pageable)
                .map(mapper::toResponseDto);
    }
    @Transactional
    public LoanApplicationResponseDto cancel(Long id) {

        LoanApplication application = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Заявка не найдена: " + id
                ));

        transitionValidator.validate(
                application.getStatus(),
                LoanApplicationStatus.CANCELLED
        );

        application.setStatus(LoanApplicationStatus.CANCELLED);

        return mapper.toResponseDto(application);
    }
}