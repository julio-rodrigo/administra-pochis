package com.administrapochi.models.dtos.weeklyclosings;

import com.administrapochi.models.enums.WeeklyClosingStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class WeeklyClosingResponse {

    private Integer id;
    private Integer ownerId;
    private String ownerName;
    private Integer weekYear;
    private Integer weekNumber;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private WeeklyClosingStatus status;
    private String closingComment;
    private Integer adminClosedById;
    private Instant adminClosedAt;
    private Integer ownerClosedById;
    private Instant ownerClosedAt;
    private Instant createdAt;
    private List<Integer> pochiIds;
    private List<WeeklyClosingReopenResponse> reopens;
    private List<UnresolvedPochiDayResponse> unresolvedDays;
    private Integer resolvedDayCount;
    private Integer totalDayCount;
    private Boolean complete;
}
