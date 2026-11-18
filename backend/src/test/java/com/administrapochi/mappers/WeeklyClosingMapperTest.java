package com.administrapochi.mappers;

import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.entities.WeeklyClosing;
import com.administrapochi.models.entities.WeeklyClosingPochi;
import com.administrapochi.models.enums.WeeklyClosingStatus;
import com.administrapochi.models.mappers.WeeklyClosingMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeeklyClosingMapperTest {

    private final WeeklyClosingMapper mapper = Mappers.getMapper(WeeklyClosingMapper.class);

    @Test
    void toEntityMapsOnlyClientSuppliedWeekFields() {
        WeeklyClosingCreateRequest request = new WeeklyClosingCreateRequest();
        request.setOwnerId(2);
        request.setWeekYear(2026);
        request.setWeekNumber(47);

        WeeklyClosing closing = mapper.toEntity(request);

        assertAll(
                () -> assertEquals(2026, closing.getWeekYear()),
                () -> assertEquals(47, closing.getWeekNumber()),
                () -> assertNull(closing.getId()),
                () -> assertNull(closing.getOwner()),
                () -> assertNull(closing.getWeekStart()),
                () -> assertNull(closing.getWeekEnd()),
                () -> assertNull(closing.getStatus()),
                () -> assertNull(closing.getCreatedAt()));
    }

    @Test
    void toResponseMapsOwnerAuditAndPochiSnapshotIds() {
        WeeklyClosing closing = WeeklyClosing.builder()
                .id(4)
                .owner(User.builder().id(2).build())
                .weekYear(2026)
                .weekNumber(47)
                .weekStart(LocalDate.of(2026, 11, 16))
                .weekEnd(LocalDate.of(2026, 11, 22))
                .status(WeeklyClosingStatus.OPEN)
                .pochiSnapshots(List.of(WeeklyClosingPochi.builder()
                        .pochi(Pochi.builder().id(18).build())
                        .build()))
                .build();

        WeeklyClosingResponse response = mapper.toResponse(closing);

        assertAll(
                () -> assertEquals(4, response.getId()),
                () -> assertEquals(2, response.getOwnerId()),
                () -> assertEquals(WeeklyClosingStatus.OPEN, response.getStatus()),
                () -> assertEquals(List.of(18), response.getPochiIds()));
    }
}
