package com.administrapochi.models.dtos.weeklyclosings;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class UnresolvedPochiDayResponse {

    private Integer pochiId;
    private LocalDate date;
}
