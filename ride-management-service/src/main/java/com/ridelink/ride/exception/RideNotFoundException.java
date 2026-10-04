package com.ridelink.ride.exception;

/** Thrown when a ride is not found by id. Maps to HTTP 404. */
public class RideNotFoundException extends RuntimeException {
    public RideNotFoundException(java.util.UUID id) {
        super("Ride not found: " + id);
    }
}
