package kg.akchabulak.trainee.loan.service;

import kg.akchabulak.trainee.loan.dto.CreateLoanApplicationRequestDto;
import kg.akchabulak.trainee.loan.dto.LoanApplicationResponseDto;
import kg.akchabulak.trainee.loan.entity.LoanApplication;
import kg.akchabulak.trainee.loan.enums.LoanApplicationStatus;
import kg.akchabulak.trainee.loan.exception.LoanApplicationConflictException;
import kg.akchabulak.trainee.loan.exception.LoanApplicationNotFoundException;
import kg.akchabulak.trainee.loan.mapper.LoanApplicationMapper;
import kg.akchabulak.trainee.loan.messaging.dto.LoanScoringMessage;
import kg.akchabulak.trainee.loan.messaging.event.LoanApplicationCreatedEvent;
import kg.akchabulak.trainee.loan.repository.LoanApplicationRepository;
import kg.akchabulak.trainee.loan.validator.LoanStatusTransitionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

    private static final String CLIENT_PIN = "12345678901234";

    @Mock private LoanApplicationRepository repository;
    @Mock private LoanApplicationMapper mapper;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private LoanStatusTransitionValidator transitionValidator;

    @InjectMocks private LoanApplicationService service;

    @Test
    void create_whenClientHasNoActiveApplication_savesNewApplicationAndPublishesEvent() {
        CreateLoanApplicationRequestDto request = new CreateLoanApplicationRequestDto();
        request.setClientPin(CLIENT_PIN);
        LoanApplication application = application(LoanApplicationStatus.REJECTED);
        application.setId(null);
        LoanApplicationResponseDto response = new LoanApplicationResponseDto();
        response.setId(15L);
        response.setStatus(LoanApplicationStatus.NEW);

        when(mapper.toEntity(request)).thenReturn(application);
        when(repository.save(application)).thenAnswer(invocation -> {
            LoanApplication saved = invocation.getArgument(0);
            saved.setId(15L);
            return saved;
        });
        when(mapper.toResponseDto(application)).thenReturn(response);

        LoanApplicationResponseDto actual = service.create(request);

        assertSame(response, actual);
        assertEquals(LoanApplicationStatus.NEW, application.getStatus());
        verify(repository).existsByClientPinAndStatusIn(eq(CLIENT_PIN), argThat(statuses ->
                statuses.size() == 2 &&
                statuses.contains(LoanApplicationStatus.NEW) &&
                statuses.contains(LoanApplicationStatus.IN_REVIEW)));
        verify(repository).save(same(application));

        ArgumentCaptor<LoanApplicationCreatedEvent> captor =
                ArgumentCaptor.forClass(LoanApplicationCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        LoanScoringMessage message = captor.getValue().getMessage();
        assertEquals(15L, message.getApplicationId());
        assertEquals(0, new BigDecimal("120000.00").compareTo(message.getAmount()));
        assertEquals(Integer.valueOf(12), message.getTermMonths());
        assertEquals(0, new BigDecimal("30000.00").compareTo(message.getMonthlyIncome()));
    }

    @Test
    void create_whenClientAlreadyHasActiveApplication_throwsConflictAndDoesNotSave() {
        CreateLoanApplicationRequestDto request = new CreateLoanApplicationRequestDto();
        request.setClientPin(CLIENT_PIN);
        when(repository.existsByClientPinAndStatusIn(eq(CLIENT_PIN), anyCollection()))
                .thenReturn(true);

        LoanApplicationConflictException exception = assertThrows(
                LoanApplicationConflictException.class,
                () -> service.create(request));

        assertEquals("У клиента уже есть активная заявка", exception.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(mapper, eventPublisher);
    }

    @Test
    void getById_whenApplicationExists_returnsMappedDto() {
        LoanApplication application = application(LoanApplicationStatus.NEW);
        LoanApplicationResponseDto response = new LoanApplicationResponseDto();
        response.setId(7L);
        when(repository.findById(7L)).thenReturn(Optional.of(application));
        when(mapper.toResponseDto(application)).thenReturn(response);

        assertSame(response, service.getById(7L));
        verify(repository).findById(7L);
    }

    @Test
    void getById_whenApplicationDoesNotExist_throwsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(LoanApplicationNotFoundException.class, () -> service.getById(999L));
        verifyNoInteractions(mapper);
    }

    @Test
    void cancel_whenStatusIsNew_changesStatusToCancelled() {
        LoanApplication application = application(LoanApplicationStatus.NEW);
        LoanApplicationResponseDto response = new LoanApplicationResponseDto();
        response.setStatus(LoanApplicationStatus.CANCELLED);
        when(repository.findById(7L)).thenReturn(Optional.of(application));
        when(mapper.toResponseDto(application)).thenReturn(response);

        LoanApplicationResponseDto actual = service.cancel(7L);

        assertEquals(LoanApplicationStatus.CANCELLED, application.getStatus());
        assertEquals(LoanApplicationStatus.CANCELLED, actual.getStatus());
        verify(transitionValidator).validate(
                LoanApplicationStatus.NEW, LoanApplicationStatus.CANCELLED);
        // В @Transactional JPA сохраняет изменение managed entity через dirty checking.
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_whenTransitionIsForbidden_throwsConflictWithoutChangingStatus() {
        LoanApplication application = application(LoanApplicationStatus.APPROVED);
        when(repository.findById(7L)).thenReturn(Optional.of(application));
        doThrow(new LoanApplicationConflictException("Недопустимый переход"))
                .when(transitionValidator)
                .validate(LoanApplicationStatus.APPROVED, LoanApplicationStatus.CANCELLED);

        assertThrows(LoanApplicationConflictException.class, () -> service.cancel(7L));

        assertEquals(LoanApplicationStatus.APPROVED, application.getStatus());
        verifyNoInteractions(mapper);
        verify(repository, never()).save(any());
    }

    @Test
    void cancel_whenApplicationDoesNotExist_throwsNotFound() {
        when(repository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.cancel(999L));
        verifyNoInteractions(transitionValidator, mapper);
    }

    @Test
    void getAll_returnsMappedPageSortedByCreatedAtDescending() {
        LoanApplication application = application(LoanApplicationStatus.IN_REVIEW);
        LoanApplicationResponseDto response = new LoanApplicationResponseDto();
        response.setId(7L);
        Page<LoanApplication> entities = new PageImpl<>(List.of(application));
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<LoanApplication>>any(), any(Pageable.class)))
                .thenReturn(entities);
        when(mapper.toResponseDto(application)).thenReturn(response);

        Page<LoanApplicationResponseDto> result =
                service.getAll(LoanApplicationStatus.IN_REVIEW, CLIENT_PIN, 1, 5);

        assertEquals(1, result.getTotalElements());
        assertSame(response, result.getContent().get(0));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(org.mockito.ArgumentMatchers.<Specification<LoanApplication>>any(), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().getOrderFor("createdAt").getDirection());
    }

    private static LoanApplication application(LoanApplicationStatus status) {
        LoanApplication application = new LoanApplication();
        application.setId(7L);
        application.setClientPin(CLIENT_PIN);
        application.setAmount(new BigDecimal("120000.00"));
        application.setTermMonths(12);
        application.setMonthlyIncome(new BigDecimal("30000.00"));
        application.setStatus(status);
        return application;
    }
}
