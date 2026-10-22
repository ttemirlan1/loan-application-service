package kg.akchabulak.trainee.loan.error;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<ErrorDetail> details
) {
}