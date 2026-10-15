package kg.akchabulak.trainee.loan.repository;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long>,
        JpaSpecificationExecutor<LoanApplication> {

    boolean existsByClientPinAndStatusIn(
            String clientPin,
            Collection<LoanApplicationStatus> statuses
    );
}