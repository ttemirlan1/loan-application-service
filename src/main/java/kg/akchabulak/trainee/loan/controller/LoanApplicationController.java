package kg.akchabulak.trainee.loan.controller;

import jakarta.validation.Valid;
import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
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
}