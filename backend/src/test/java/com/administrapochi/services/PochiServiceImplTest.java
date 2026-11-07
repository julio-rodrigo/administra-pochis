package com.administrapochi.services;

import com.administrapochi.dtos.pochis.CreatePochiRequest;
import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.exceptions.DuplicateEconomicNumberException;
import com.administrapochi.exceptions.DuplicateLicensePlateException;
import com.administrapochi.exceptions.PochiNotFoundException;
import com.administrapochi.mappers.PochiMapper;
import com.administrapochi.models.Pochi;
import com.administrapochi.repositories.PochiRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PochiServiceImplTest {

    @Mock
    private PochiRepository pochiRepository;

    @Mock
    private PochiMapper pochiMapper;

    @InjectMocks
    private PochiServiceImpl pochiService;

    @Test
    void findAllMapsPochisAndSupportsAnEmptyList() {
        List<PochiResponse> responses = List.of();
        when(pochiRepository.findAll()).thenReturn(List.of());
        when(pochiMapper.toResponseList(List.of())).thenReturn(responses);

        assertSame(responses, pochiService.findAll());
        verify(pochiRepository).findAll();
        verify(pochiMapper).toResponseList(List.of());
    }

    @Test
    void findByIdReturnsMappedPochi() {
        Pochi pochi = existingPochi();
        PochiResponse response = responseFor(pochi);
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiMapper.toResponse(pochi)).thenReturn(response);

        assertSame(response, pochiService.findById(1));
        verify(pochiRepository).findById(1);
        verify(pochiMapper).toResponse(pochi);
    }

    @Test
    void findByIdThrowsWhenPochiDoesNotExist() {
        when(pochiRepository.findById(1)).thenReturn(Optional.empty());

        PochiNotFoundException exception = assertThrows(
                PochiNotFoundException.class, () -> pochiService.findById(1));

        assertEquals("Pochi not found with id: 1", exception.getMessage());
        verifyNoMoreInteractions(pochiMapper);
    }

    @Test
    void createNormalizesValuesDefaultsUnionAndStartsActive() {
        CreatePochiRequest request = createRequest(" p-01 ", " mx-01 ", " local ");
        Pochi pochi = new Pochi();
        PochiResponse response = responseFor(pochi);
        when(pochiRepository.existsByEconomicNumber("P-01")).thenReturn(false);
        when(pochiRepository.existsByLicensePlate("MX-01")).thenReturn(false);
        when(pochiMapper.toEntity(request)).thenReturn(pochi);
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(response);

        assertSame(response, pochiService.create(request));

        assertAll(
                () -> assertEquals("P-01", pochi.getEconomicNumber()),
                () -> assertEquals("MX-01", pochi.getLicensePlate()),
                () -> assertEquals("LOCAL", pochi.getUnionName()),
                () -> assertTrue(pochi.getActive()));
        verify(pochiRepository).save(pochi);
    }

    @Test
    void createUsesDefaultUnionForNullOrBlankAndDoesNotCheckMissingPlate() {
        CreatePochiRequest request = createRequest("01", "  ", " \t");
        Pochi pochi = new Pochi();
        when(pochiRepository.existsByEconomicNumber("01")).thenReturn(false);
        when(pochiMapper.toEntity(request)).thenReturn(pochi);
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(responseFor(pochi));

        pochiService.create(request);

        assertAll(
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertNull(pochi.getLicensePlate()),
                () -> assertTrue(pochi.getActive()));
        verify(pochiRepository, never()).existsByLicensePlate(any());
    }

    @Test
    void createThrowsForDuplicateEconomicNumberBeforeMapping() {
        CreatePochiRequest request = createRequest(" p-01 ", null, null);
        when(pochiRepository.existsByEconomicNumber("P-01")).thenReturn(true);

        DuplicateEconomicNumberException exception = assertThrows(
                DuplicateEconomicNumberException.class, () -> pochiService.create(request));

        assertEquals("A pochi with economic number 'P-01' already exists", exception.getMessage());
        verify(pochiRepository, never()).save(any(Pochi.class));
        verifyNoMoreInteractions(pochiMapper);
    }

    @Test
    void createThrowsForDuplicateLicensePlate() {
        CreatePochiRequest request = createRequest("01", " ab-1 ", null);
        when(pochiRepository.existsByEconomicNumber("01")).thenReturn(false);
        when(pochiRepository.existsByLicensePlate("AB-1")).thenReturn(true);

        DuplicateLicensePlateException exception = assertThrows(
                DuplicateLicensePlateException.class, () -> pochiService.create(request));

        assertEquals("A pochi with license plate 'AB-1' already exists", exception.getMessage());
        verify(pochiRepository, never()).save(any(Pochi.class));
        verifyNoMoreInteractions(pochiMapper);
    }

    @Test
    void updateNormalizesAndPreservesSystemControlledValues() {
        Pochi pochi = existingPochi();
        pochi.setActive(false);
        UpdatePochiRequest request = updateRequest(" new-01 ", " plate-2 ");
        PochiResponse response = responseFor(pochi);
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.existsByEconomicNumberAndIdNot("NEW-01", 1)).thenReturn(false);
        when(pochiRepository.existsByLicensePlateAndIdNot("PLATE-2", 1)).thenReturn(false);
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(response);

        assertSame(response, pochiService.update(1, request));

        assertAll(
                () -> assertEquals(1, pochi.getId()),
                () -> assertEquals("NEW-01", pochi.getEconomicNumber()),
                () -> assertEquals("PLATE-2", pochi.getLicensePlate()),
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertFalse(pochi.getActive()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), pochi.getCreatedAt()));
        verify(pochiMapper).updateEntity(request, pochi);
        verify(pochiRepository).save(pochi);
    }

    @Test
    void updateAllowsExistingUniqueValuesAndClearsBlankLicensePlate() {
        Pochi pochi = existingPochi();
        UpdatePochiRequest request = updateRequest("p-01", " ");
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.existsByEconomicNumberAndIdNot("P-01", 1)).thenReturn(false);
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(responseFor(pochi));

        pochiService.update(1, request);

        assertEquals("P-01", pochi.getEconomicNumber());
        assertNull(pochi.getLicensePlate());
        verify(pochiRepository, never()).existsByLicensePlateAndIdNot(any(), any());
    }

    @Test
    void updateRejectsEconomicNumberUsedByAnotherPochi() {
        Pochi pochi = existingPochi();
        UpdatePochiRequest request = updateRequest("taken", "P-01");
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.existsByEconomicNumberAndIdNot("TAKEN", 1)).thenReturn(true);

        assertThrows(DuplicateEconomicNumberException.class, () -> pochiService.update(1, request));

        verify(pochiRepository, never()).save(any(Pochi.class));
        verify(pochiMapper, never()).updateEntity(any(), any());
    }

    @Test
    void updateRejectsLicensePlateUsedByAnotherPochi() {
        Pochi pochi = existingPochi();
        UpdatePochiRequest request = updateRequest("P-01", "taken");
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.existsByEconomicNumberAndIdNot("P-01", 1)).thenReturn(false);
        when(pochiRepository.existsByLicensePlateAndIdNot("TAKEN", 1)).thenReturn(true);

        assertThrows(DuplicateLicensePlateException.class, () -> pochiService.update(1, request));

        verify(pochiRepository, never()).save(any(Pochi.class));
        verify(pochiMapper, never()).updateEntity(any(), any());
    }

    @Test
    void updateThrowsWhenPochiDoesNotExist() {
        when(pochiRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(PochiNotFoundException.class,
                () -> pochiService.update(1, updateRequest("P-01", null)));

        verifyNoMoreInteractions(pochiMapper);
    }

    @Test
    void activateChangesInactivePochiAndReturnsMappedResponse() {
        Pochi pochi = existingPochi();
        pochi.setActive(false);
        PochiResponse response = responseFor(pochi);
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(response);

        assertSame(response, pochiService.activate(1));
        assertTrue(pochi.getActive());
        verify(pochiRepository).save(pochi);
    }

    @Test
    void activateIsIdempotentWhenAlreadyActive() {
        Pochi pochi = existingPochi();
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiMapper.toResponse(pochi)).thenReturn(responseFor(pochi));

        pochiService.activate(1);

        verify(pochiRepository, never()).save(any(Pochi.class));
    }

    @Test
    void deactivateChangesActivePochiAndReturnsMappedResponse() {
        Pochi pochi = existingPochi();
        PochiResponse response = responseFor(pochi);
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiRepository.save(pochi)).thenReturn(pochi);
        when(pochiMapper.toResponse(pochi)).thenReturn(response);

        assertSame(response, pochiService.deactivate(1));
        assertFalse(pochi.getActive());
        verify(pochiRepository).save(pochi);
    }

    @Test
    void deactivateIsIdempotentWhenAlreadyInactive() {
        Pochi pochi = existingPochi();
        pochi.setActive(false);
        when(pochiRepository.findById(1)).thenReturn(Optional.of(pochi));
        when(pochiMapper.toResponse(pochi)).thenReturn(responseFor(pochi));

        pochiService.deactivate(1);

        verify(pochiRepository, never()).save(any(Pochi.class));
    }

    @Test
    void activationAndDeactivationThrowWhenPochiDoesNotExist() {
        when(pochiRepository.findById(1)).thenReturn(Optional.empty());

        assertAll(
                () -> assertThrows(PochiNotFoundException.class, () -> pochiService.activate(1)),
                () -> assertThrows(PochiNotFoundException.class, () -> pochiService.deactivate(1)));
    }

    private Pochi existingPochi() {
        return Pochi.builder()
                .id(1)
                .economicNumber("P-01")
                .licensePlate("MX-01")
                .unionName("UNTRAC")
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    private PochiResponse responseFor(Pochi pochi) {
        return PochiResponse.builder()
                .id(pochi.getId())
                .economicNumber(pochi.getEconomicNumber())
                .licensePlate(pochi.getLicensePlate())
                .unionName(pochi.getUnionName())
                .active(pochi.getActive())
                .createdAt(pochi.getCreatedAt())
                .updatedAt(pochi.getUpdatedAt())
                .build();
    }

    private CreatePochiRequest createRequest(String economicNumber, String licensePlate, String unionName) {
        CreatePochiRequest request = new CreatePochiRequest();
        request.setEconomicNumber(economicNumber);
        request.setLicensePlate(licensePlate);
        request.setUnionName(unionName);
        return request;
    }

    private UpdatePochiRequest updateRequest(String economicNumber, String licensePlate) {
        UpdatePochiRequest request = new UpdatePochiRequest();
        request.setEconomicNumber(economicNumber);
        request.setLicensePlate(licensePlate);
        return request;
    }
}
