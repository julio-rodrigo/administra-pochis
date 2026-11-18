package com.administrapochi.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "weekly_closing_reopens")
public class WeeklyClosingReopen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "weekly_closing_id", nullable = false, updatable = false)
    private WeeklyClosing weeklyClosing;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reopened_by", nullable = false, updatable = false)
    private User reopenedBy;

    @Column(name = "reopened_at", nullable = false, updatable = false)
    private Instant reopenedAt;

    @Column(nullable = false, columnDefinition = "TEXT", updatable = false)
    private String reason;

    @PrePersist
    void applyReopenedTimestamp() {
        if (reopenedAt == null) {
            reopenedAt = Instant.now();
        }
    }
}
