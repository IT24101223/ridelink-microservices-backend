package com.ridelink.ride.client;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Abstraction over the Fare &amp; Payment Service (port 8084).
 *
 * <p><strong>Assumed contract</strong> – see docs/contracts/ride-service-dependencies.md.
 */
public interface FareServiceClient {

    /**
     * Requests a fare estimate before the ride starts.
     *
     * @param pickupLat    pickup latitude
     * @param pickupLng    pickup longitude
     * @param destLat      destination latitude
     * @param destLng      destination longitude
     * @return estimated fare as a {@link BigDecimal}
     */
    BigDecimal estimateFare(double pickupLat, double pickupLng,
                            double destLat,   double destLng);

    /**
     * Finalises the fare and triggers a simulated payment after ride completion.
     *
     * @param rideId      the ride UUID
     * @param passengerId the passenger's Account Service userId
     * @param driverId    the driver's Account Service userId
     * @param pickupLat   pickup latitude  (for distance/duration calculation)
     * @param pickupLng   pickup longitude
     * @param destLat     destination latitude
     * @param destLng     destination longitude
     * @return a {@link FarePaymentResult} containing finalFare and paymentId
     */
    FarePaymentResult finalizeAndPay(UUID rideId, UUID passengerId, UUID driverId,
                                     double pickupLat, double pickupLng,
                                     double destLat,   double destLng);
}
