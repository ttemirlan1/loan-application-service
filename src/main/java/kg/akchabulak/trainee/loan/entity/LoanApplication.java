import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "loan_application",
        indexes = {
                @Index(
                        name = "idx_loan_application_client_pin",
                        columnList = "client_pin"
                ),
                @Index(
                        name = "idx_loan_application_status",
                        columnList = "status"
                )
        }
)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_pin", length = 14, nullable = false)
    private String clientPin;

    @Column(name = "client_full_name", length = 255, nullable = false)
    private String clientFullName;

    @Column(name = "phone", length = 13, nullable = false)
    private String phone;

    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(name = "term_months", nullable = false)
    private Integer termMonths;

    @Column(name = "monthly_income", precision = 15, scale = 2, nullable = false)
    private BigDecimal monthlyIncome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LoanApplicationStatus status;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected LoanApplication() {
    }

    // Добавь необходимые getters и setters
}