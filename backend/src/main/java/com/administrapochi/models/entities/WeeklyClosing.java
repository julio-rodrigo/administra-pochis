package com.administrapochi.models.entities;

import com.administrapochi.models.enums.WeeklyClosingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weekly_closings")
public class WeeklyClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false, updatable = false)
    private User owner;

    @Column(name = "week_year", nullable = false, updatable = false)
    private Integer weekYear;

    @Column(name = "week_number", nullable = false, updatable = false)
    private Integer weekNumber;

    @Column(name = "week_start", nullable = false, updatable = false)
    private LocalDate weekStart;

    @Column(name = "week_end", nullable = false, updatable = false)
    private LocalDate weekEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WeeklyClosingStatus status;

    @Column(name = "closing_comment", columnDefinition = "TEXT")
    private String closingComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_closed_by")
    private User adminClosedBy;

    @Column(name = "admin_closed_at")
    private Instant adminClosedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_closed_by")
    private User ownerClosedBy;

    @Column(name = "owner_closed_at")
    private Instant ownerClosedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "weeklyClosing", fetch = FetchType.LAZY)
    private List<WeeklyClosingPochi> pochiSnapshots = new ArrayList<>();

    @PrePersist
    void applyCreationTimestamp() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
