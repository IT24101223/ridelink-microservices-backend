package com.ridelink.ride.exception;

/** Thrown when a downstream service call fails unexpectedly. Maps to HTTP 502/503. */
public class DownstreamServiceException extends RuntimeException {
    private final String serviceName;

    public DownstreamServiceException(String serviceName, String message, Throwable cause) {
        super("[" + serviceName + "] " + message, cause);
        this.serviceName = serviceName;
    }

    public DownstreamServiceException(String serviceName, String message) {
        super("[" + serviceName + "] " + message);
        this.serviceName = serviceName;
    }

    public String getServiceName() { return serviceName; }
}
