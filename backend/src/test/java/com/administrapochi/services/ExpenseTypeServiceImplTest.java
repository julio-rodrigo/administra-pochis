package com.administrapochi.services;

import com.administrapochi.exceptions.ExpenseTypeConflictException;
import com.administrapochi.exceptions.ExpenseTypeNotFoundException;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeCreateRequest;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeResponse;
import com.administrapochi.models.dtos.expensetypes.ExpenseTypeUpdateRequest;
import com.administrapochi.models.entities.ExpenseType;
import com.administrapochi.models.mappers.ExpenseTypeMapper;
import com.administrapochi.repositories.ExpenseTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseTypeServiceImplTest {

    @Mock
    private ExpenseTypeRepository expenseTypeRepository;

    @Mock
    private ExpenseTypeMapper expenseTypeMapper;

    @InjectMocks
    private ExpenseTypeServiceImpl expenseTypeService;

    @Test
    void createNormalizesValuesAndAppliesDefaults() {
        ExpenseTypeCreateRequest request = createRequest(" maintenance ", " Maintenance ", " service ", null);
        ExpenseType expenseType = new ExpenseType();
        ExpenseTypeResponse response = responseFor(expenseType);
        when(expenseTypeRepository.existsByCode("MAINTENANCE")).thenReturn(false);
        when(expenseTypeRepository.existsByName("Maintenance")).thenReturn(false);
        when(expenseTypeMapper.toEntity(request)).thenReturn(expenseType);
        when(expenseTypeRepository.save(expenseType)).thenAnswer(invocation -> {
            ExpenseType saved = invocation.getArgument(0);
            assertAll(
                    () -> assertEquals("MAINTENANCE", saved.getCode()),
                    () -> assertEquals("Maintenance", saved.getName()),
                    () -> assertEquals("service", saved.getDescription()),
                    () -> assertTrue(saved.getRequiresPochi()),
                    () -> assertTrue(saved.getActive()));
            return saved;
        });
        when(expenseTypeMapper.toResponse(expenseType)).thenReturn(response);

        ExpenseTypeResponse result = expenseTypeService.create(request);

        assertSame(response, result);
        verify(expenseTypeRepository).existsByCode("MAINTENANCE");
        verify(expenseTypeRepository).existsByName("Maintenance");
        verify(expenseTypeMapper).toEntity(request);
        verify(expenseTypeRepository).save(expenseType);
        verify(expenseTypeMapper).toResponse(expenseType);
    }

    @Test
    void createPreservesExplicitRequiresPochiValueAndNormalizesBlankDescription() {
        ExpenseTypeCreateRequest request = createRequest("office_supplies", "Office supplies", "  ", false);
        ExpenseType expenseType = new ExpenseType();
        when(expenseTypeMapper.toEntity(request)).thenAnswer(invocation -> {
            expenseType.setRequiresPochi(request.getRequiresPochi());
            return expenseType;
        });
        when(expenseTypeRepository.save(expenseType)).thenReturn(expenseType);
        when(expenseTypeMapper.toResponse(expenseType)).thenReturn(responseFor(expenseType));

        expenseTypeService.create(request);

        assertAll(
                () -> assertEquals("OFFICE_SUPPLIES", expenseType.getCode()),
                () -> assertEquals("Office supplies", expenseType.getName()),
                () -> assertNull(expenseType.getDescription()),
                () -> assertFalse(expenseType.getRequiresPochi()),
                () -> assertTrue(expenseType.getActive()));
    }

    @Test
    void createRejectsDuplicateCodeWithoutMappingOrSaving() {
        ExpenseTypeCreateRequest request = createRequest("insurance", "Insurance", null, true);
        when(expenseTypeRepository.existsByCode("INSURANCE")).thenReturn(true);

        ExpenseTypeConflictException exception = assertThrows(
                ExpenseTypeConflictException.class, () -> expenseTypeService.create(request));

        assertEquals("Expense Type code 'INSURANCE' already exists", exception.getMessage());
        verify(expenseTypeRepository, never()).existsByName(any());
        verify(expenseTypeRepository, never()).save(any(ExpenseType.class));
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void createRejectsDuplicateNameWithoutSaving() {
        ExpenseTypeCreateRequest request = createRequest("repair", "Repair", null, true);
        when(expenseTypeRepository.existsByCode("REPAIR")).thenReturn(false);
        when(expenseTypeRepository.existsByName("Repair")).thenReturn(true);

        ExpenseTypeConflictException exception = assertThrows(
                ExpenseTypeConflictException.class, () -> expenseTypeService.create(request));

        assertEquals("Expense Type name 'Repair' already exists", exception.getMessage());
        verify(expenseTypeRepository, never()).save(any(ExpenseType.class));
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void findByIdReturnsMappedExpenseType() {
        ExpenseType expenseType = existingExpenseType();
        ExpenseTypeResponse response = responseFor(expenseType);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeMapper.toResponse(expenseType)).thenReturn(response);

        ExpenseTypeResponse result = expenseTypeService.findById(1);

        assertSame(response, result);
        verify(expenseTypeMapper).toResponse(expenseType);
    }

    @Test
    void findByIdThrowsWhenExpenseTypeDoesNotExist() {
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.empty());

        ExpenseTypeNotFoundException exception = assertThrows(
                ExpenseTypeNotFoundException.class, () -> expenseTypeService.findById(1));

        assertEquals("Expense Type with id 1 was not found", exception.getMessage());
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void findAllReturnsEveryExpenseTypeWhenFilterIsOmitted() {
        List<ExpenseType> expenseTypes = List.of(existingExpenseType());
        List<ExpenseTypeResponse> responses = List.of(responseFor(expenseTypes.getFirst()));
        when(expenseTypeRepository.findAll()).thenReturn(expenseTypes);
        when(expenseTypeMapper.toResponseList(expenseTypes)).thenReturn(responses);

        List<ExpenseTypeResponse> result = expenseTypeService.findAll(null);

        assertSame(responses, result);
        verify(expenseTypeRepository).findAll();
        verify(expenseTypeMapper).toResponseList(expenseTypes);
        verify(expenseTypeRepository, never()).findByActive(any());
    }

    @Test
    void findAllFiltersByActiveStatusWhenRequested() {
        List<ExpenseType> expenseTypes = List.of(existingExpenseType());
        List<ExpenseTypeResponse> responses = List.of(responseFor(expenseTypes.getFirst()));
        when(expenseTypeRepository.findByActive(false)).thenReturn(expenseTypes);
        when(expenseTypeMapper.toResponseList(expenseTypes)).thenReturn(responses);

        List<ExpenseTypeResponse> result = expenseTypeService.findAll(false);

        assertSame(responses, result);
        verify(expenseTypeRepository).findByActive(false);
        verify(expenseTypeMapper).toResponseList(expenseTypes);
    }

    @Test
    void updateMapsEditableValuesAndPreservesSystemManagedFields() {
        ExpenseType expenseType = existingExpenseType();
        LocalDateTime createdAt = expenseType.getCreatedAt();
        ExpenseTypeUpdateRequest request = updateRequest(" repair ", " Repaired ", " updated ", false, false);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeRepository.existsByCodeAndIdNot("REPAIR", 1)).thenReturn(false);
        when(expenseTypeRepository.existsByNameAndIdNot("Repaired", 1)).thenReturn(false);
        doAnswer(invocation -> {
            ExpenseTypeUpdateRequest changes = invocation.getArgument(0);
            ExpenseType target = invocation.getArgument(1);
            target.setCode(changes.getCode());
            target.setName(changes.getName());
            target.setDescription(changes.getDescription());
            target.setRequiresPochi(changes.getRequiresPochi());
            target.setActive(changes.getActive());
            return null;
        }).when(expenseTypeMapper).updateEntity(request, expenseType);
        when(expenseTypeRepository.save(expenseType)).thenReturn(expenseType);
        when(expenseTypeMapper.toResponse(expenseType)).thenAnswer(invocation -> responseFor(expenseType));

        ExpenseTypeResponse result = expenseTypeService.update(1, request);

        assertAll(
                () -> assertEquals("REPAIR", expenseType.getCode()),
                () -> assertEquals("Repaired", expenseType.getName()),
                () -> assertEquals("updated", expenseType.getDescription()),
                () -> assertFalse(expenseType.getRequiresPochi()),
                () -> assertFalse(expenseType.getActive()),
                () -> assertEquals(1, expenseType.getId()),
                () -> assertEquals(createdAt, expenseType.getCreatedAt()),
                () -> assertEquals(1, result.getId()));
        verify(expenseTypeMapper).updateEntity(request, expenseType);
        verify(expenseTypeRepository).save(expenseType);
    }

    @Test
    void updateAllowsExistingCodeAndNameOnCurrentRecord() {
        ExpenseType expenseType = existingExpenseType();
        ExpenseTypeUpdateRequest request = updateRequest(" insurance ", " Insurance ", null, null, null);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeRepository.existsByCodeAndIdNot("INSURANCE", 1)).thenReturn(false);
        when(expenseTypeRepository.existsByNameAndIdNot("Insurance", 1)).thenReturn(false);
        when(expenseTypeRepository.save(expenseType)).thenReturn(expenseType);
        when(expenseTypeMapper.toResponse(expenseType)).thenReturn(responseFor(expenseType));

        expenseTypeService.update(1, request);

        verify(expenseTypeRepository).existsByCodeAndIdNot("INSURANCE", 1);
        verify(expenseTypeRepository).existsByNameAndIdNot("Insurance", 1);
        verify(expenseTypeRepository).save(expenseType);
    }

    @Test
    void updateRejectsCodeUsedByAnotherExpenseType() {
        ExpenseType expenseType = existingExpenseType();
        ExpenseTypeUpdateRequest request = updateRequest("repair", null, null, null, null);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeRepository.existsByCodeAndIdNot("REPAIR", 1)).thenReturn(true);

        ExpenseTypeConflictException exception = assertThrows(
                ExpenseTypeConflictException.class, () -> expenseTypeService.update(1, request));

        assertEquals("Expense Type code 'REPAIR' already exists", exception.getMessage());
        verify(expenseTypeRepository, never()).save(any(ExpenseType.class));
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void updateRejectsNameUsedByAnotherExpenseType() {
        ExpenseType expenseType = existingExpenseType();
        ExpenseTypeUpdateRequest request = updateRequest(null, "Maintenance", null, null, null);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeRepository.existsByNameAndIdNot("Maintenance", 1)).thenReturn(true);

        ExpenseTypeConflictException exception = assertThrows(
                ExpenseTypeConflictException.class, () -> expenseTypeService.update(1, request));

        assertEquals("Expense Type name 'Maintenance' already exists", exception.getMessage());
        verify(expenseTypeRepository, never()).save(any(ExpenseType.class));
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void updateThrowsWhenExpenseTypeDoesNotExist() {
        when(expenseTypeRepository.findById(2)).thenReturn(Optional.empty());

        assertThrows(ExpenseTypeNotFoundException.class,
                () -> expenseTypeService.update(2, updateRequest(null, "Name", null, null, null)));

        verifyNoMoreInteractions(expenseTypeRepository);
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void deleteDeactivatesAndSavesWithoutPhysicallyDeleting() {
        ExpenseType expenseType = existingExpenseType();
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));
        when(expenseTypeRepository.save(expenseType)).thenAnswer(invocation -> {
            ExpenseType saved = invocation.getArgument(0);
            assertFalse(saved.getActive());
            assertEquals(1, saved.getId());
            return saved;
        });

        expenseTypeService.delete(1);

        assertFalse(expenseType.getActive());
        verify(expenseTypeRepository).findById(1);
        verify(expenseTypeRepository).save(expenseType);
        verify(expenseTypeRepository, never()).delete(any(ExpenseType.class));
        verify(expenseTypeRepository, never()).deleteById(any());
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void deleteDoesNotSaveAnAlreadyInactiveExpenseType() {
        ExpenseType expenseType = existingExpenseType();
        expenseType.setActive(false);
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.of(expenseType));

        expenseTypeService.delete(1);

        verify(expenseTypeRepository).findById(1);
        verify(expenseTypeRepository, never()).save(any(ExpenseType.class));
        verify(expenseTypeRepository, never()).delete(any(ExpenseType.class));
        verifyNoInteractions(expenseTypeMapper);
    }

    @Test
    void deleteThrowsWhenExpenseTypeDoesNotExist() {
        when(expenseTypeRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(ExpenseTypeNotFoundException.class, () -> expenseTypeService.delete(1));

        verify(expenseTypeRepository).findById(1);
        verifyNoMoreInteractions(expenseTypeRepository);
        verifyNoInteractions(expenseTypeMapper);
    }

    private ExpenseType existingExpenseType() {
        return ExpenseType.builder()
                .id(1)
                .code("INSURANCE")
                .name("Insurance")
                .description("Insurance coverage")
                .requiresPochi(true)
                .active(true)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 0, 0))
                .build();
    }

    private ExpenseTypeResponse responseFor(ExpenseType expenseType) {
        return ExpenseTypeResponse.builder()
                .id(expenseType.getId())
                .code(expenseType.getCode())
                .name(expenseType.getName())
                .description(expenseType.getDescription())
                .requiresPochi(expenseType.getRequiresPochi())
                .active(expenseType.getActive())
                .createdAt(expenseType.getCreatedAt())
                .updatedAt(expenseType.getUpdatedAt())
                .build();
    }

    private ExpenseTypeCreateRequest createRequest(
            String code, String name, String description, Boolean requiresPochi) {
        ExpenseTypeCreateRequest request = new ExpenseTypeCreateRequest();
        request.setCode(code);
        request.setName(name);
        request.setDescription(description);
        request.setRequiresPochi(requiresPochi);
        return request;
    }

    private ExpenseTypeUpdateRequest updateRequest(
            String code, String name, String description, Boolean requiresPochi, Boolean active) {
        ExpenseTypeUpdateRequest request = new ExpenseTypeUpdateRequest();
        request.setCode(code);
        request.setName(name);
        request.setDescription(description);
        request.setRequiresPochi(requiresPochi);
        request.setActive(active);
        return request;
    }
}
