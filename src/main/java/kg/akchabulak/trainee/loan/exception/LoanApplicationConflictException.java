package kg.akchabulak.trainee.loan.exception;

public class LoanApplicationConflictException
        extends RuntimeException {

    public LoanApplicationConflictException(String message) {
        super(message);
    }
}