package com.ridelink.ride.strategy;

import com.ridelink.ride.client.EligibleDriverDto;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting a driver from a list of eligible candidates.
 *
 * <p>Decoupled from the service so the assignment algorithm can be swapped
 * without touching business logic (Open/Closed Principle).
 */
public interface DriverSelectionStrategy {

    /**
     * Selects the best driver from the given list for the pickup location.
     *
     * @param candidates  non-null list of eligible drivers (may be empty)
     * @param pickupLat   pickup latitude
     * @param pickupLng   pickup longitude
     * @return the selected driver, or {@link Optional#empty()} if the list is empty
     */
    Optional<EligibleDriverDto> selectDriver(List<EligibleDriverDto> candidates,
                                              double pickupLat, double pickupLng);
}
