package com.ridelink.ride.strategy;

import com.ridelink.ride.client.EligibleDriverDto;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Default driver selection: pick the candidate with the smallest Haversine distance
 * to the pickup point. On a tie, pick the driver with the lexicographically smaller
 * {@code driverId} (deterministic across nodes).
 *
 * <h2>Haversine formula</h2>
 * <pre>
 *   a = sin²(Δlat/2) + cos(lat1)·cos(lat2)·sin²(Δlng/2)
 *   c = 2·atan2(√a, √(1−a))
 *   d = R·c          (R = 6371 km)
 * </pre>
 *
 * <p>This is documented in the README as the assignment rule so you can explain it in a viva.
 */
@Component
public class NearestEligibleDriverStrategy implements DriverSelectionStrategy {

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public Optional<EligibleDriverDto> selectDriver(List<EligibleDriverDto> candidates,
                                                     double pickupLat, double pickupLng) {
        if (candidates == null || candidates.isEmpty()) {
            return Optional.empty();
        }

        return candidates.stream()
                .min(Comparator
                        .comparingDouble((EligibleDriverDto d) ->
                                haversineKm(pickupLat, pickupLng, d.getCurrentLat(), d.getCurrentLng()))
                        .thenComparing(d -> d.getDriverId().toString())); // tie-break: lowest UUID string
    }

    /**
     * Computes the great-circle distance between two WGS-84 coordinates in kilometres.
     */
    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
