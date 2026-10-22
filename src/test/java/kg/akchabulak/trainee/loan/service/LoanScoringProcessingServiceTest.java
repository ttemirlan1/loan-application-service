package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import kg.akchabulak.trainee.loan.scoring.ScoringResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanScoringProcessingServiceTest {

    @Mock private LoanApplicationRepository repository;
    @Mock private ScoringService scoringService;
    @InjectMocks private LoanScoringProcessingService service;

    @Test
    void process_whenApplicationDoesNotExist_returnsFalse() {
        when(repository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertFalse(service.process(99L));
        verifyNoInteractions(scoringService);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @EnumSource(value = LoanApplicationStatus.class,
            names = {"IN_REVIEW", "APPROVED", "REJECTED", "CANCELLED"})
    void process_whenStatusIsNotNew_skipsWithoutSaving(LoanApplicationStatus initialStatus) {
        LoanApplication application = application(initialStatus);
        when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(application));

        assertFalse(service.process(7L));
        assertEquals(initialStatus, application.getStatus());
        verifyNoInteractions(scoringService);
        verify(repository, never()).save(any());
    }

    @Test
    void process_whenScoreIsApproved_setsApprovedAndSaves() {
        LoanApplication application = application(LoanApplicationStatus.NEW);
        when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(application));
        when(scoringService.score(application)).thenAnswer(invocation -> {
            assertEquals(LoanApplicationStatus.IN_REVIEW, application.getStatus());
            return new ScoringResult(LoanApplicationStatus.APPROVED, null);
        });

        assertTrue(service.process(7L));
        assertEquals(LoanApplicationStatus.APPROVED, application.getStatus());
        assertNull(application.getRejectReason());
        verify(repository).save(same(application));
    }

    @Test
    void process_whenScoreIsRejected_setsReasonAndSaves() {
        LoanApplication application = application(LoanApplicationStatus.NEW);
        when(repository.findByIdForUpdate(7L)).thenReturn(Optional.of(application));
        when(scoringService.score(application)).thenReturn(
                new ScoringResult(LoanApplicationStatus.REJECTED,
                        "Платёж превышает 50% дохода"));

        assertTrue(service.process(7L));
        assertEquals(LoanApplicationStatus.REJECTED, application.getStatus());
        assertEquals("Платёж превышает 50% дохода", application.getRejectReason());
        verify(repository).save(same(application));
    }

    private static LoanApplication application(LoanApplicationStatus status) {
        LoanApplication application = new LoanApplication();
        application.setId(7L);
        application.setStatus(status);
        return application;
    }
}
