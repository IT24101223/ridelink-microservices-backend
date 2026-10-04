package com.ridelink.ride.client;

import java.util.List;
import java.util.UUID;

/**
 * Abstraction over the Driver &amp; Vehicle Service (port 8082).
 *
 * <p>Defined as an interface for Dependency Inversion:
 * <ul>
 *   <li>Production: {@link HttpDriverServiceClient} uses Spring RestClient.</li>
 *   <li>Stub profile: {@link com.ridelink.ride.client.stub.StubDriverServiceClient}
 *       returns in-memory fake data so the service runs standalone.</li>
 * </ul>
 *
 * <p><strong>Assumed contract</strong> – see docs/contracts/ride-service-dependencies.md.
 */
public interface DriverServiceClient {

    /**
     * Fetches drivers that are AVAILABLE and within {@code radiusKm} km of the given coordinates.
     *
     * @param lat      pickup latitude
     * @param lng      pickup longitude
     * @param radiusKm search radius in km
     * @return ordered list of eligible driver DTOs (may be empty)
     */
    List<EligibleDriverDto> getEligibleDrivers(double lat, double lng, double radiusKm);

    /**
     * Marks the driver as BUSY so they are excluded from future assignments.
     *
     * @param driverId Account Service userId of the driver
     */
    void markDriverBusy(UUID driverId);

    /**
     * Marks the driver as AVAILABLE again (called after COMPLETED or CANCELLED).
     *
     * @param driverId Account Service userId of the driver
     */
    void markDriverAvailable(UUID driverId);
}
