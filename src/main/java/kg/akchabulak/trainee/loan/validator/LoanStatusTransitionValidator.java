package kg.akchabulak.trainee.loan.validator;

import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class LoanStatusTransitionValidator {

    private static final Map<LoanApplicationStatus,
            Set<LoanApplicationStatus>> ALLOWED_TRANSITIONS = Map.of(

            LoanApplicationStatus.NEW,
            EnumSet.of(
                    LoanApplicationStatus.IN_REVIEW,
                    LoanApplicationStatus.CANCELLED
            ),

            LoanApplicationStatus.IN_REVIEW,
            EnumSet.of(
                    LoanApplicationStatus.APPROVED,
                    LoanApplicationStatus.REJECTED,
                    LoanApplicationStatus.CANCELLED
            )
    );

    public void validate(
            LoanApplicationStatus currentStatus,
            LoanApplicationStatus newStatus) {

        Set<LoanApplicationStatus> allowed =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Set.of()
                );

        if (!allowed.contains(newStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Недопустимый переход статуса: "
                            + currentStatus + " -> " + newStatus
            );
        }
    }
}