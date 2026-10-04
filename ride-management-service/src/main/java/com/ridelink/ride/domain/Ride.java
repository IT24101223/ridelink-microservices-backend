package com.ridelink.ride.domain;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core aggregate root representing a ride request and its lifecycle.
 *
 * <p>Design notes:
 * <ul>
 *   <li>UUIDs are generated in the application layer (not DB-auto) to make IDs
 *       predictable across services and environments.</li>
 *   <li>Status transitions must go through {@link RideStateMachine#transition};
 *       never call {@code setStatus} directly from outside the service layer.</li>
 *   <li>{@code driverId} equals the Account Service userId of the assigned driver.</li>
 * </ul>
 */
@Document(collection = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @NotNull
    private UUID id;

    // ── Participants ─────────────────────────────────────────────────
    @Indexed
    @NotNull
    private UUID passengerId;   // extracted from JWT sub

    @Indexed
    private UUID driverId;      // null until assigned

    // ── Pickup ───────────────────────────────────────────────────────
    @NotNull
    private String pickupAddress;

    @NotNull
    private Double pickupLat;

    @NotNull
    private Double pickupLng;

    // ── Destination ──────────────────────────────────────────────────
    @NotNull
    private String destinationAddress;

    @NotNull
    private Double destinationLat;

    @NotNull
    private Double destinationLng;

    // ── Status & Fare ────────────────────────────────────────────────
    @Indexed
    @NotNull
    private RideStatus status;

    private BigDecimal estimatedFare;

    private BigDecimal finalFare;

    private UUID paymentId;     // received from Fare & Payment Service

    private String cancelReason;

    // ── Lifecycle timestamps ──────────────────────────────────────────
    @CreatedDate
    @NotNull
    private Instant createdAt;

    @LastModifiedDate
    @NotNull
    private Instant updatedAt;

    private Instant assignedAt;

    private Instant acceptedAt;

    private Instant startedAt;

    private Instant completedAt;

    private Instant cancelledAt;

    // ── Audit history (optional, for traceability) ────────────────────
    @Builder.Default
    private List<RideStatusHistory> statusHistory = new ArrayList<>();

    // ── Helper: add a history entry ───────────────────────────────────
    public void addHistory(RideStatus from, RideStatus to, String changedBy) {
        RideStatusHistory entry = RideStatusHistory.builder()
                .id(UUID.randomUUID())
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .changedAt(Instant.now())
                .build();
        statusHistory.add(entry);
    }
}
