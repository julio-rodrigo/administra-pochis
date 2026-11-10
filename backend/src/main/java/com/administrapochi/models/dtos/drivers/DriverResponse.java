package com.administrapochi.models.dtos.drivers;

import com.administrapochi.models.enums.BloodType;
import com.administrapochi.models.enums.DriverStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class DriverResponse {

    private Integer id;
    private String firstName;
    private String firstLastName;
    private String secondLastName;
    private String fullName;
    private String curp;
    private BloodType bloodType;
    private DriverStatus employmentStatus;
    private Instant createdAt;
    private Instant updatedAt;
}
