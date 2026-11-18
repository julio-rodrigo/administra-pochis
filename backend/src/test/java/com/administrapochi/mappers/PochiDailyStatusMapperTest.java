package com.administrapochi.mappers;

import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusCreateRequest;
import com.administrapochi.models.dtos.pochidailystatuses.PochiDailyStatusUpdateRequest;
import com.administrapochi.models.entities.Pochi;
import com.administrapochi.models.entities.PochiDailyStatus;
import com.administrapochi.models.entities.User;
import com.administrapochi.models.enums.PochiDailyStatusType;
import com.administrapochi.models.mappers.PochiDailyStatusMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PochiDailyStatusMapperTest {

    private final PochiDailyStatusMapper mapper = Mappers.getMapper(PochiDailyStatusMapper.class);

    @Test
    void mapsResponseAndHandlesNullableUpdateAudit() {
        PochiDailyStatus entity = PochiDailyStatus.builder()
                .id(1)
                .pochi(Pochi.builder().id(82).economicNumber("P-82").build())
                .createdBy(User.builder().id(2).build())
                .status(PochiDailyStatusType.DAY_OFF)
                .build();
        var response = mapper.toResponse(entity);
        assertThat(response.getPochiId()).isEqualTo(82);
        assertThat(response.getPochiIdentifier()).isEqualTo("P-82");
        assertThat(response.getCreatedBy()).isEqualTo(2);
        assertThat(response.getUpdatedBy()).isNull();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    void mapsRequestsWithoutChangingPochiOrAuditFields() {
        PochiDailyStatusCreateRequest create = new PochiDailyStatusCreateRequest();
        create.setPochiId(82);
        create.setStatus(PochiDailyStatusType.DAY_OFF);
        create.setStatusDate(LocalDate.of(2026, 10, 19));
        PochiDailyStatus created = mapper.toEntity(create);
        assertThat(created.getPochi()).isNull();
        assertThat(created.getCreatedBy()).isNull();
        assertThat(created.getStatus()).isEqualTo(PochiDailyStatusType.DAY_OFF);

        Pochi pochi = Pochi.builder().id(82).build();
        User user = User.builder().id(2).build();
        LocalDateTime timestamp = LocalDateTime.of(2026, 10, 1, 0, 0);
        PochiDailyStatus entity = PochiDailyStatus.builder().id(1).pochi(pochi)
                .createdBy(user).createdAt(timestamp).build();
        PochiDailyStatusUpdateRequest update = new PochiDailyStatusUpdateRequest();
        update.setStatus(PochiDailyStatusType.NOT_WORKED);
        update.setStatusDate(LocalDate.of(2026, 10, 20));
        update.setComment("Workshop");
        mapper.updateEntity(update, entity);
        assertThat(entity.getPochi()).isSameAs(pochi);
        assertThat(entity.getCreatedBy()).isSameAs(user);
        assertThat(entity.getCreatedAt()).isEqualTo(timestamp);
        assertThat(entity.getStatus()).isEqualTo(PochiDailyStatusType.NOT_WORKED);
        assertThat(entity.getStatusDate()).isEqualTo(update.getStatusDate());
        assertThat(entity.getComment()).isEqualTo("Workshop");
    }
}
