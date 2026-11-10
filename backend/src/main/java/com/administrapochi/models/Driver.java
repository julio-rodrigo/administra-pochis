package com.administrapochi.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "first_name", nullable = false, length = 40)
    private String firstName;

    @Column(name = "first_last_name", nullable = false, length = 40)
    private String firstLastName;

    @Column(name = "second_last_name", length = 40)
    private String secondLastName;

    @Column(nullable = false, unique = true, length = 18)
    private String curp;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "blood_type", nullable = false, columnDefinition = "blood_type")
    private BloodType bloodType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "employment_status", nullable = false, columnDefinition = "driver_status")
    private DriverStatus employmentStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    private Boolean deleted;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void applyCreationDefaults() {
        Instant now = Instant.now();
        if (employmentStatus == null) {
            employmentStatus = DriverStatus.ACTIVE;
        }
        if (deleted == null) {
            deleted = false;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }
}
