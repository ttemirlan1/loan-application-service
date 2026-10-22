package kg.akchabulak.trainee.loan.repository;
import jakarta.persistence.LockModeType;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, Long>,
        JpaSpecificationExecutor<LoanApplication> {

    boolean existsByClientPinAndStatusIn(
            String clientPin,
            Collection<LoanApplicationStatus> statuses
    );
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM LoanApplication l WHERE l.id = :id")
    Optional<LoanApplication> findByIdForUpdate(
            @Param("id") Long id
    );

    List<LoanApplication>
    findTop100ByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            LoanApplicationStatus status,
            LocalDateTime createdAt
    );
}