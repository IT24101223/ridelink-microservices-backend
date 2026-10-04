package com.ridelink.ride.exception;

/** Thrown when no eligible driver is available at ride creation time. Maps to HTTP 409. */
public class NoDriverAvailableException extends RuntimeException {
    public NoDriverAvailableException(double lat, double lng) {
        super("No eligible driver available near [" + lat + ", " + lng + "].");
    }
}
