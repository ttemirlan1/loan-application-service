package kg.akchabulak.trainee.loan.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String SCORING_EXCHANGE =
            "loan.scoring.exchange";

    public static final String SCORING_QUEUE =
            "loan.scoring.queue";

    public static final String SCORING_ROUTING_KEY =
            "loan.scoring.requested";

    @Bean
    public DirectExchange scoringExchange() {
        return new DirectExchange(SCORING_EXCHANGE, true, false);
    }

    @Bean
    public Queue scoringQueue() {
        return QueueBuilder.durable(SCORING_QUEUE).build();
    }

    @Bean
    public Binding scoringBinding(
            Queue scoringQueue,
            DirectExchange scoringExchange) {

        return BindingBuilder
                .bind(scoringQueue)
                .to(scoringExchange)
                .with(SCORING_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}