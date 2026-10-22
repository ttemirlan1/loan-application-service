package kg.akchabulak.trainee.loan.consumer;

import kg.akchabulak.trainee.loan.messaging.dto.LoanScoringMessage;
import kg.akchabulak.trainee.loan.service.LoanScoringProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanScoringConsumer {

    private final LoanScoringProcessingService processingService;

    @RabbitListener(queues = "loan.scoring.queue")
    public void consume(LoanScoringMessage message) {

        Long applicationId = message.getApplicationId();

        if (applicationId == null) {
            throw new AmqpRejectAndDontRequeueException(
                    "В сообщении отсутствует applicationId"
            );
        }

        log.info(
                "Получено сообщение для скоринга заявки {}",
                applicationId
        );

        boolean processed = processingService.process(applicationId);

        if (processed) {
            log.info(
                    "Заявка {} успешно обработана",
                    applicationId
            );
        } else {
            log.info(
                    "Обработка заявки {} пропущена",
                    applicationId
            );
        }
    }
}