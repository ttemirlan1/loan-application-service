package kg.akchabulak.trainee.loan.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService service;

    @PostMapping
    public ResponseEntity<LoanApplicationResponseDto> create(
            @Valid @RequestBody CreateLoanApplicationRequestDto request) {

        LoanApplicationResponseDto response = service.create(request);

        URI location = URI.create(
                "/api/v1/loan-applications/" + response.getId()
        );

        return ResponseEntity
                .created(location)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanApplicationResponseDto> getById(
            @PathVariable Long id) {

        return ResponseEntity.ok(service.getById(id));
    }
    @GetMapping
    public ResponseEntity<Page<LoanApplicationResponseDto>> getAll(
            @RequestParam(required = false)
            LoanApplicationStatus status,

            @RequestParam(required = false)
            String clientPin,

            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(100)
            int size
    ) {
        return ResponseEntity.ok(
                service.getAll(status, clientPin, page, size)
        );
    }
}