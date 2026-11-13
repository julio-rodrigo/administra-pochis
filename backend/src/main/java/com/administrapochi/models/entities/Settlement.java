package com.administrapochi.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Check;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Check(
        name = "ck_settlement_adjustment_reason",
        constraints = "accepted_amount = expected_amount OR "
                + "(adjustment_reason IS NOT NULL AND btrim(adjustment_reason) <> '')")
@Table(
        name = "settlements",
        indexes = {
                @Index(name = "idx_settlements_driver", columnList = "driver_id"),
                @Index(name = "idx_settlements_pochi", columnList = "pochi_id"),
                @Index(name = "idx_settlements_work_date", columnList = "work_date")
        })
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pochi_id", nullable = false)
    private Pochi pochi;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "expected_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "accepted_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal acceptedAmount;

    @Column(name = "adjustment_reason", length = 500)
    private String adjustmentReason;

    @Column(length = 500)
    private String notes;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "settlement_assignments",
            joinColumns = @JoinColumn(name = "settlement_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "pochi_assignment_id", nullable = false),
            uniqueConstraints = @UniqueConstraint(
                    name = "uq_settlement_assignments_pochi_assignment",
                    columnNames = "pochi_assignment_id"))
    @Builder.Default
    private Set<PochiAssignment> pochiAssignments = Set.of();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modified_by")
    private User modifiedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void applyCreationTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
