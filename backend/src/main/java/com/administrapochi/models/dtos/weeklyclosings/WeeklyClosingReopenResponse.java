package com.administrapochi.models.dtos.weeklyclosings;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class WeeklyClosingReopenResponse {

    private Integer id;
    private Integer weeklyClosingId;
    private Integer reopenedById;
    private String reopenedByName;
    private Instant reopenedAt;
    private String reason;
}
