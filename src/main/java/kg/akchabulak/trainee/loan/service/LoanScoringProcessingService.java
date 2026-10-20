package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import kg.akchabulak.trainee.loan.scoring.ScoringResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanScoringProcessingService {

    private final LoanApplicationRepository repository;
    private final ScoringService scoringService;

    @Transactional
    public boolean process(Long applicationId) {

        Optional<LoanApplication> optional =
                repository.findByIdForUpdate(applicationId);

        if (optional.isEmpty()) {
            log.warn("Заявка {} не найдена", applicationId);
            return false;
        }

        LoanApplication application = optional.get();

        if (application.getStatus() != LoanApplicationStatus.NEW) {
            log.info(
                    "Заявка {} пропущена, статус: {}",
                    applicationId,
                    application.getStatus()
            );
            return false;
        }

        // NEW -> IN_REVIEW
        application.setStatus(LoanApplicationStatus.IN_REVIEW);

        ScoringResult result = scoringService.score(application);

        // IN_REVIEW -> APPROVED / REJECTED
        application.setStatus(result.status());
        application.setRejectReason(result.rejectReason());

        repository.save(application);

        log.info(
                "Скоринг заявки {}: результат={}, причина={}",
                applicationId,
                result.status(),
                result.rejectReason()
        );

        return true;
    }
}