package com.administrapochi.models.dtos.pochidailystatuses;

import com.administrapochi.models.enums.PochiDailyStatusType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class PochiDailyStatusResponse {
    private Integer id;
    private Integer pochiId;
    private String pochiIdentifier;
    private LocalDate statusDate;
    private PochiDailyStatusType status;
    private String comment;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private Integer updatedBy;
    private LocalDateTime updatedAt;
}
