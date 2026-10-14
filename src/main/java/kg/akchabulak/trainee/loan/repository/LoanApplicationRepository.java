import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long> {

    boolean existsByClientPinAndStatusIn(
            String clientPin,
            Collection<LoanApplicationStatus> statuses
    );
}