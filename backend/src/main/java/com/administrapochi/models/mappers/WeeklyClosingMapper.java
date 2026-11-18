package com.administrapochi.models.mappers;

import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingCreateRequest;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingReopenResponse;
import com.administrapochi.models.dtos.weeklyclosings.WeeklyClosingResponse;
import com.administrapochi.models.entities.WeeklyClosing;
import com.administrapochi.models.entities.WeeklyClosingPochi;
import com.administrapochi.models.entities.WeeklyClosingReopen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WeeklyClosingMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", source = "owner.name")
    @Mapping(target = "adminClosedById", source = "adminClosedBy.id")
    @Mapping(target = "ownerClosedById", source = "ownerClosedBy.id")
    @Mapping(target = "pochiIds", source = "pochiSnapshots")
    @Mapping(target = "unresolvedDays", ignore = true)
    @Mapping(target = "resolvedDayCount", ignore = true)
    @Mapping(target = "totalDayCount", ignore = true)
    @Mapping(target = "complete", ignore = true)
    @Mapping(target = "reopens", ignore = true)
    WeeklyClosingResponse toResponse(WeeklyClosing weeklyClosing);

    @Mapping(target = "weeklyClosingId", source = "weeklyClosing.id")
    @Mapping(target = "reopenedById", source = "reopenedBy.id")
    @Mapping(target = "reopenedByName", source = "reopenedBy.name")
    WeeklyClosingReopenResponse toReopenResponse(WeeklyClosingReopen reopen);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "weekStart", ignore = true)
    @Mapping(target = "weekEnd", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "closingComment", ignore = true)
    @Mapping(target = "adminClosedBy", ignore = true)
    @Mapping(target = "adminClosedAt", ignore = true)
    @Mapping(target = "ownerClosedBy", ignore = true)
    @Mapping(target = "ownerClosedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "pochiSnapshots", ignore = true)
    WeeklyClosing toEntity(WeeklyClosingCreateRequest request);

    default Integer toPochiId(WeeklyClosingPochi snapshot) {
        return snapshot.getPochi().getId();
    }
}
