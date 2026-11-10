package com.administrapochi.models.dtos.pochis;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PochiResponse {

    private Integer id;
    private String economicNumber;
    private String licensePlate;
    private String unionName;
    private Integer ownerId;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
