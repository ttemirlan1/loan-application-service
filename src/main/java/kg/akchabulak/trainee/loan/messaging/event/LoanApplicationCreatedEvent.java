package kg.akchabulak.trainee.loan.messaging.event;

import kg.akchabulak.trainee.loan.messaging.dto.LoanScoringMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class LoanApplicationCreatedEvent {

    private final LoanScoringMessage message;
}