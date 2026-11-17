package com.administrapochi.services;

import com.administrapochi.exceptions.ExpenseConflictException;
import com.administrapochi.exceptions.ExpenseTypeNotFoundException;
import com.administrapochi.exceptions.InvalidExpenseException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.models.dtos.expenses.CancelExpenseRequest;
import com.administrapochi.models.dtos.expenses.CreateExpenseRequest;
import com.administrapochi.models.dtos.expenses.ExpenseResponse;
import com.administrapochi.models.dtos.expenses.UpdateExpenseRequest;
import com.administrapochi.models.entities.Expense;
import com.administrapochi.models.entities.ExpenseType;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.ExpenseStatus;
import com.administrapochi.models.mappers.ExpenseMapperImpl;
import com.administrapochi.repositories.ExpenseRepository;
import com.administrapochi.repositories.ExpenseTypeRepository;
import com.administrapochi.repositories.PochiRepository;
import com.administrapochi.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceImplTest {

    private static final Integer EXPENSE_ID = 15;
    private static final Integer TYPE_ID = 3;
    private static final Integer POCHI_ID = 9;
    private static final Integer TEMPORARY_USER_ID = 2;

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseTypeRepository expenseTypeRepository;
    @Mock
    private PochiRepository pochiRepository;
    @Mock
    private UserRepository userRepository;
    private ExpenseServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ExpenseServiceImpl(
                expenseRepository,
                expenseTypeRepository,
                pochiRepository,
                userRepository,
                new ExpenseMapperImpl());
    }

    @Test
    void createsActiveExpenseWithTemporaryPayerAndRegistrar() {
        ExpenseType type = expenseType(true);
        Pochi pochi = pochi();
        User user = User.builder().id(TEMPORARY_USER_ID).name("Temporary user").build();
        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(type));
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi));
        when(userRepository.findById(TEMPORARY_USER_ID)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(createRequest(POCHI_ID));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getExpenseType()).isSameAs(type);
        assertThat(saved.getPochi()).isSameAs(pochi);
        assertThat(saved.getAmount()).isEqualByComparingTo("850.00");
        assertThat(saved.getExpenseDate()).isEqualTo(LocalDate.now().minusDays(1));
        assertThat(saved.getPaidByUser()).isSameAs(user);
        assertThat(saved.getRegisteredByUser()).isSameAs(user);
        assertThat(saved.getDescription()).isEqualTo("Brake replacement");
        assertThat(saved.getStatus()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(saved.getCancellationReason()).isNull();
        assertThat(saved.getCancelledAt()).isNull();
        assertThat(saved.getCancelledByUser()).isNull();
    }

    @Test
    void enforcesRequiresPochiButAllowsOptionalPochiWhenTypeDoesNotRequireIt() {
        when(expenseTypeRepository.findById(TYPE_ID))
                .thenReturn(Optional.of(expenseType(true)), Optional.of(expenseType(false)));
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.of(pochi()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThrows(InvalidExpenseException.class, () -> service.create(createRequest(null)));

        Expense optionalPochiExpense = expenseForTest(ExpenseStatus.ACTIVE);
        when(expenseRepository.findByIdForUpdate(EXPENSE_ID)).thenReturn(Optional.of(optionalPochiExpense));
        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(expenseType(false)));
        UpdateExpenseRequest update = updateRequest(POCHI_ID);
        service.update(EXPENSE_ID, update);
        assertThat(optionalPochiExpense.getPochi().getId()).isEqualTo(POCHI_ID);
    }

    @Test
    void allowsNullPochiWhenExpenseTypeDoesNotRequireOne() {
        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(expenseType(false)));
        when(userRepository.findById(TEMPORARY_USER_ID))
                .thenReturn(Optional.of(User.builder().id(TEMPORARY_USER_ID).build()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(createRequest(null));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getPochi()).isNull();
    }

    @Test
    void rejectsInvalidAmountAndUnknownRelatedResources() {
        CreateExpenseRequest invalidAmount = createRequest(POCHI_ID);
        invalidAmount.setAmount(BigDecimal.ZERO);
        assertThrows(InvalidExpenseException.class, () -> service.create(invalidAmount));
        verify(expenseTypeRepository, never()).findById(any());

        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.empty());
        assertThrows(ExpenseTypeNotFoundException.class, () -> service.create(createRequest(POCHI_ID)));
        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(expenseType(true)));
        when(pochiRepository.findById(POCHI_ID)).thenReturn(Optional.empty());
        assertThrows(PochiNotFoundException.class, () -> service.create(createRequest(POCHI_ID)));
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void updatesActiveExpenseWithoutChangingServerControlledFields() {
        Expense expense = expenseForTest(ExpenseStatus.ACTIVE);
        User registrar = expense.getRegisteredByUser();
        expense.setCreatedAt(expense.getCreatedAt().minusDays(2));
        var createdAt = expense.getCreatedAt();
        when(expenseRepository.findByIdForUpdate(EXPENSE_ID)).thenReturn(Optional.of(expense));
        when(expenseTypeRepository.findById(TYPE_ID)).thenReturn(Optional.of(expenseType(false)));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.update(EXPENSE_ID, updateRequest(null));

        assertThat(expense.getRegisteredByUser()).isSameAs(registrar);
        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.ACTIVE);
        assertThat(expense.getCreatedAt()).isEqualTo(createdAt);
        assertThat(expense.getCancellationReason()).isNull();
        assertThat(expense.getCancelledAt()).isNull();
        assertThat(expense.getCancelledByUser()).isNull();
        assertThat(expense.getPochi()).isNull();
        assertThat(expense.getAmount()).isEqualByComparingTo("110.00");
        assertThat(expense.getDescription()).isNull();
    }

    @Test
    void cancellationSetsEveryAuditFieldAndRejectsCancelledExpenses() {
        Expense expense = expenseForTest(ExpenseStatus.ACTIVE);
        User user = User.builder().id(TEMPORARY_USER_ID).build();
        when(expenseRepository.findByIdForUpdate(EXPENSE_ID)).thenReturn(Optional.of(expense));
        when(userRepository.findById(TEMPORARY_USER_ID)).thenReturn(Optional.of(user));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CancelExpenseRequest request = new CancelExpenseRequest();
        request.setCancellationReason(" Duplicate entry ");

        service.cancel(EXPENSE_ID, request);

        assertThat(expense.getStatus()).isEqualTo(ExpenseStatus.CANCELLED);
        assertThat(expense.getCancellationReason()).isEqualTo("Duplicate entry");
        assertThat(expense.getCancelledAt()).isNotNull();
        assertThat(expense.getCancelledByUser()).isSameAs(user);
        assertThat(expense.getUpdatedAt()).isEqualTo(expense.getCancelledAt());
        assertThrows(ExpenseConflictException.class, () -> service.update(
                EXPENSE_ID, updateRequest(null)));
        assertThrows(ExpenseConflictException.class, () -> service.cancel(EXPENSE_ID, request));
    }

    @Test
    void rejectsBlankCancellationAndInvertedDateRange() {
        CancelExpenseRequest blankReason = new CancelExpenseRequest();
        blankReason.setCancellationReason("  ");
        assertThrows(InvalidExpenseException.class, () -> service.cancel(EXPENSE_ID, blankReason));

        assertThrows(InvalidExpenseException.class, () -> service.findAll(
                null, null, null, LocalDate.now(), LocalDate.now().minusDays(1),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "expenseDate"))));
        verify(expenseRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void appliesOptionalFiltersUsingThePagedSpecificationQuery() {
        PageRequest pageable = PageRequest.of(
                0, 20, Sort.by(Sort.Order.desc("expenseDate"), Sort.Order.desc("id")));
        when(expenseRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty(pageable));

        Page<ExpenseResponse> result = service.findAll(
                TYPE_ID,
                POCHI_ID,
                ExpenseStatus.ACTIVE,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                pageable);

        assertThat(result).isEmpty();
        verify(expenseRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    private CreateExpenseRequest createRequest(Integer pochiId) {
        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setExpenseTypeId(TYPE_ID);
        request.setPochiId(pochiId);
        request.setAmount(new BigDecimal("850.00"));
        request.setExpenseDate(LocalDate.now().minusDays(1));
        request.setDescription("  Brake replacement  ");
        return request;
    }

    private UpdateExpenseRequest updateRequest(Integer pochiId) {
        UpdateExpenseRequest request = new UpdateExpenseRequest();
        request.setExpenseTypeId(TYPE_ID);
        request.setPochiId(pochiId);
        request.setAmount(new BigDecimal("110.00"));
        request.setExpenseDate(LocalDate.now().minusDays(2));
        request.setDescription("  ");
        return request;
    }

    private ExpenseType expenseType(boolean requiresPochi) {
        return ExpenseType.builder()
                .id(TYPE_ID)
                .requiresPochi(requiresPochi)
                .active(true)
                .build();
    }

    private Pochi pochi() {
        return Pochi.builder().id(POCHI_ID).active(true).build();
    }

    private Expense expenseForTest(ExpenseStatus status) {
        return Expense.builder()
                .id(EXPENSE_ID)
                .expenseType(expenseType(true))
                .pochi(pochi())
                .amount(new BigDecimal("50.00"))
                .expenseDate(LocalDate.now().minusDays(3))
                .paidByUser(User.builder().id(TEMPORARY_USER_ID).build())
                .registeredByUser(User.builder().id(TEMPORARY_USER_ID).build())
                .description("Existing")
                .status(status)
                .createdAt(java.time.LocalDateTime.now().minusDays(3))
                .updatedAt(java.time.LocalDateTime.now().minusDays(2))
                .build();
    }
}
