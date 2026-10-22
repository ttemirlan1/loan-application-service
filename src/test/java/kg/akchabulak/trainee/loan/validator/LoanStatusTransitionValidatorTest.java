package kg.akchabulak.trainee.loan.validator;

import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.exception.LoanApplicationConflictException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static kg.akchabulak.trainee.loan.enums.LoanApplicationStatus.*;
import static org.junit.jupiter.api.Assertions.*;

class LoanStatusTransitionValidatorTest {

    private final LoanStatusTransitionValidator validator = new LoanStatusTransitionValidator();

    @ParameterizedTest(name = "{0} -> {1} разрешён")
    @MethodSource("allowedTransitions")
    void validate_allowsExpectedTransitions(
            LoanApplicationStatus from, LoanApplicationStatus to) {
        assertDoesNotThrow(() -> validator.validate(from, to));
    }

    @ParameterizedTest(name = "{0} -> {1} запрещён")
    @MethodSource("forbiddenTransitions")
    void validate_rejectsForbiddenTransitionsAndDescribesBothStatuses(
            LoanApplicationStatus from, LoanApplicationStatus to) {
        LoanApplicationConflictException exception = assertThrows(
                LoanApplicationConflictException.class,
                () -> validator.validate(from, to));
        assertTrue(exception.getMessage().contains(from + " -> " + to),
                "Текст ошибки должен содержать исходный и целевой статусы");
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(NEW, IN_REVIEW),
                Arguments.of(NEW, CANCELLED),
                Arguments.of(IN_REVIEW, APPROVED),
                Arguments.of(IN_REVIEW, REJECTED),
                Arguments.of(IN_REVIEW, CANCELLED)
        );
    }

    private static Stream<Arguments> forbiddenTransitions() {
        return Stream.of(
                Arguments.of(NEW, APPROVED),
                Arguments.of(NEW, REJECTED),
                Arguments.of(NEW, NEW),
                Arguments.of(IN_REVIEW, NEW),
                Arguments.of(IN_REVIEW, IN_REVIEW),
                Arguments.of(APPROVED, CANCELLED),
                Arguments.of(REJECTED, IN_REVIEW),
                Arguments.of(CANCELLED, NEW)
        );
    }
}
