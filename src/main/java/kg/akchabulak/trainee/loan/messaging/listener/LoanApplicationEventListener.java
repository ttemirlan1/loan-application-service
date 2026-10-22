package kg.akchabulak.trainee.loan.messaging.listener;

import kg.akchabulak.trainee.loan.messaging.event.LoanApplicationCreatedEvent;
import kg.akchabulak.trainee.loan.messaging.publisher.LoanMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationEventListener {

    private final LoanMessagePublisher publisher;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onApplicationCreated(
            LoanApplicationCreatedEvent event) {

        try {
            publisher.publish(event.getMessage());
        } catch (AmqpException ex) {
            log.error(
                    "Не удалось отправить сообщение скоринга. applicationId={}",
                    event.getMessage().getApplicationId(),
                    ex
            );
        }
    }
}