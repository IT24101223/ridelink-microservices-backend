package com.ridelink.ride.domain;

/**
 * All possible lifecycle states of a Ride.
 *
 * <p>Allowed transitions are enforced by {@link RideStateMachine}.
 * Terminal states: COMPLETED, CANCELLED.
 */
public enum RideStatus {
    REQUESTED,
    ASSIGNED,
    ACCEPTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
