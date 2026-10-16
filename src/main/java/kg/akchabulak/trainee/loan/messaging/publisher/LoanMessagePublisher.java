package kg.akchabulak.trainee.loan.messaging.publisher;

import kg.akchabulak.trainee.loan.config.RabbitMqConfig;
import kg.akchabulak.trainee.loan.messaging.dto.LoanScoringMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanMessagePublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publish(LoanScoringMessage message) {

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCORING_EXCHANGE,
                RabbitMqConfig.SCORING_ROUTING_KEY,
                message
        );

        log.info(
                "Сообщение скоринга отправлено. applicationId={}",
                message.getApplicationId()
        );
    }
}