package com.ridelink.ride.domain;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Audit log entry capturing every status change on a {@link Ride}.
 *
 * <p>Embedded in its ride document so status changes are persisted atomically
 * with the ride lifecycle update.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideStatusHistory {

    private UUID id;

    private RideStatus fromStatus;

    private RideStatus toStatus;

    /** userId (sub from JWT) or system identifier that triggered the change. */
    private String changedBy;

    private Instant changedAt;
}
