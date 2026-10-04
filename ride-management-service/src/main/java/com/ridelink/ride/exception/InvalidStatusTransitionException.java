package com.ridelink.ride.exception;

import com.ridelink.ride.domain.RideStatus;

/**
 * Thrown when a requested state transition is not allowed by the state machine.
 * Maps to HTTP 409 Conflict.
 */
public class InvalidStatusTransitionException extends RuntimeException {

    private final RideStatus from;
    private final RideStatus to;

    public InvalidStatusTransitionException(RideStatus from, RideStatus to) {
        super("Transition from " + from + " to " + to + " is not allowed.");
        this.from = from;
        this.to = to;
    }

    public RideStatus getFrom() { return from; }
    public RideStatus getTo()   { return to;   }
}
