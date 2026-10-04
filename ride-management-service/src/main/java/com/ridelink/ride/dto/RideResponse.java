package com.ridelink.ride.dto;

import com.ridelink.ride.domain.RideStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Read model returned on all ride-related endpoints.
 *
 * <p>Deliberately separate from the {@link com.ridelink.ride.domain.Ride} entity
 * so the API contract can evolve independently of the persistence model.
 */
@Data
public class RideResponse {

    private UUID id;
    private UUID passengerId;
    private UUID driverId;

    private String pickupAddress;
    private Double pickupLat;
    private Double pickupLng;

    private String destinationAddress;
    private Double destinationLat;
    private Double destinationLng;

    private RideStatus status;
    private BigDecimal estimatedFare;
    private BigDecimal finalFare;
    private UUID paymentId;
    private String cancelReason;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant assignedAt;
    private Instant acceptedAt;
    private Instant startedAt;
    private Instant completedAt;
    private Instant cancelledAt;
}
