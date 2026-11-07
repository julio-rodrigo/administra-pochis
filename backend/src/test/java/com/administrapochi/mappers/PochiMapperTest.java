package com.administrapochi.mappers;

import com.administrapochi.dtos.pochis.CreatePochiRequest;
import com.administrapochi.dtos.pochis.PochiResponse;
import com.administrapochi.dtos.pochis.UpdatePochiRequest;
import com.administrapochi.models.Pochi;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PochiMapperTest {

    private final PochiMapper pochiMapper = Mappers.getMapper(PochiMapper.class);

    @Test
    void toResponseMapsAllApiFields() {
        Pochi pochi = existingPochi();

        PochiResponse response = pochiMapper.toResponse(pochi);

        assertAll(
                () -> assertEquals(1, response.getId()),
                () -> assertEquals("P-01", response.getEconomicNumber()),
                () -> assertEquals("MX-01", response.getLicensePlate()),
                () -> assertEquals("UNTRAC", response.getUnionName()),
                () -> assertEquals(Boolean.FALSE, response.getActive()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), response.getCreatedAt()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), response.getUpdatedAt()));
    }

    @Test
    void toResponseListMapsEveryPochi() {
        Pochi pochi = existingPochi();

        assertEquals("P-01", pochiMapper.toResponseList(java.util.List.of(pochi))
                .getFirst().getEconomicNumber());
    }

    @Test
    void toEntityMapsOnlyCreateRequestValues() {
        CreatePochiRequest request = new CreatePochiRequest();
        request.setEconomicNumber("P-01");
        request.setLicensePlate("MX-01");
        request.setUnionName("UNTRAC");

        Pochi pochi = pochiMapper.toEntity(request);

        assertAll(
                () -> assertEquals("P-01", pochi.getEconomicNumber()),
                () -> assertEquals("MX-01", pochi.getLicensePlate()),
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertNull(pochi.getId()),
                () -> assertNull(pochi.getActive()),
                () -> assertNull(pochi.getCreatedAt()),
                () -> assertNull(pochi.getUpdatedAt()));
    }

    @Test
    void updateEntityChangesRequestFieldsAndPreservesProtectedValues() {
        Pochi pochi = existingPochi();
        UpdatePochiRequest request = new UpdatePochiRequest();
        request.setEconomicNumber("P-02");
        request.setLicensePlate("MX-02");

        pochiMapper.updateEntity(request, pochi);

        assertAll(
                () -> assertEquals("P-02", pochi.getEconomicNumber()),
                () -> assertEquals("MX-02", pochi.getLicensePlate()),
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertEquals(1, pochi.getId()),
                () -> assertEquals(Boolean.FALSE, pochi.getActive()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), pochi.getCreatedAt()),
                () -> assertEquals(Instant.parse("2026-01-01T00:00:00Z"), pochi.getUpdatedAt()));
    }

    @Test
    void updateEntityClearsAnOptionalNullLicensePlate() {
        Pochi pochi = existingPochi();
        UpdatePochiRequest request = new UpdatePochiRequest();
        request.setEconomicNumber("P-01");

        pochiMapper.updateEntity(request, pochi);

        assertAll(
                () -> assertEquals("P-01", pochi.getEconomicNumber()),
                () -> assertNull(pochi.getLicensePlate()),
                () -> assertEquals("UNTRAC", pochi.getUnionName()),
                () -> assertEquals(1, pochi.getId()));
    }

    private Pochi existingPochi() {
        Instant timestamp = Instant.parse("2026-01-01T00:00:00Z");
        return Pochi.builder()
                .id(1)
                .economicNumber("P-01")
                .licensePlate("MX-01")
                .unionName("UNTRAC")
                .active(false)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }
}
