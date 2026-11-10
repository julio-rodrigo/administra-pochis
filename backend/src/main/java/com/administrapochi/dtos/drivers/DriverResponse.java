package com.administrapochi.dtos.drivers;

import com.administrapochi.models.BloodType;
import com.administrapochi.models.DriverStatus;
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
