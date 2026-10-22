package kg.akchabulak.trainee.loan.exception;

public class LoanApplicationNotFoundException
        extends RuntimeException {

    public LoanApplicationNotFoundException(Long id) {
        super("Заявка с ID " + id + " не найдена");
    }
}
