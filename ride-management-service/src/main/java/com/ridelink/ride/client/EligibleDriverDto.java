package com.ridelink.ride.client;

import lombok.Data;

import java.util.UUID;

/**
 * DTO representing an eligible driver returned by the Driver &amp; Vehicle Service.
 *
 * <p>Fields are assumptions – document them in docs/contracts/ride-service-dependencies.md
 * and update once the team agrees on the actual contract.
 */
@Data
public class EligibleDriverDto {

    /** Account Service userId (same as the JWT sub for the driver). */
    private UUID driverId;

    /** Driver's current simulated latitude. */
    private double currentLat;

    /** Driver's current simulated longitude. */
    private double currentLng;

    /** Human-readable name for logging/debugging. */
    private String displayName;
}
