package com.ridelink.ride.exception;

/** Thrown when a caller tries to operate on a ride they don't own. Maps to HTTP 403. */
public class AccessDeniedException extends RuntimeException {
    public AccessDeniedException(String message) {
        super(message);
    }
}
