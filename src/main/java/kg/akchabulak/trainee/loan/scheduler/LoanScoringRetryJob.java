package kg.akchabulak.trainee.loan.scheduler;

import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.messaging.dto.LoanScoringMessage;
import kg.akchabulak.trainee.loan.messaging.publisher.LoanMessagePublisher;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanScoringRetryJob {

    private final LoanApplicationRepository repository;
    private final LoanMessagePublisher publisher;

    @Scheduled(
            fixedDelayString =
                    "${loan.scoring.retry.fixed-delay-ms:60000}"
    )
    public void retryStuckApplications() {

        LocalDateTime threshold =
                LocalDateTime.now().minusMinutes(5);

        List<LoanApplication> applications =
                repository
                        .findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                                LoanApplicationStatus.NEW,
                                threshold
                        );

        if (applications.isEmpty()) {
            return;
        }

        log.info(
                "Найдено зависших заявок NEW: {}",
                applications.size()
        );

        for (LoanApplication application : applications) {

            LoanScoringMessage message = new LoanScoringMessage(
                    application.getId(),
                    application.getAmount(),
                    application.getTermMonths(),
                    application.getMonthlyIncome()
            );

            try {
                publisher.publish(message);

                log.info(
                        "Повторная отправка скоринга, applicationId={}",
                        application.getId()
                );

            } catch (AmqpException ex) {

                log.error(
                        "Ошибка повторной отправки, applicationId={}",
                        application.getId(),
                        ex
                );
            }
        }
    }
}