package com.ridelink.ride.client.stub;

import com.ridelink.ride.client.FarePaymentResult;
import com.ridelink.ride.client.FareServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * In-memory fake {@link FareServiceClient} for the "stub" Spring profile.
 *
 * <p>Returns a fixed estimated fare of £8.50 and a deterministic paymentId
 * so fare calls always succeed in demo mode.
 */
@Component
@Profile("stub")
public class StubFareServiceClient implements FareServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StubFareServiceClient.class);

    @Override
    public BigDecimal estimateFare(double pickupLat, double pickupLng,
                                   double destLat, double destLng) {
        log.info("[STUB] estimateFare – returning £8.50");
        return new BigDecimal("8.50");
    }

    @Override
    public FarePaymentResult finalizeAndPay(UUID rideId, UUID passengerId, UUID driverId,
                                            double pickupLat, double pickupLng,
                                            double destLat,   double destLng) {
        log.info("[STUB] finalizeAndPay rideId={}", rideId);
        FarePaymentResult result = new FarePaymentResult();
        result.setFinalFare(new BigDecimal("9.25"));  // slightly more after completion
        result.setPaymentId(UUID.fromString("00000000-0000-0000-0000-AABBCCDDEEFF".toLowerCase()));
        return result;
    }
}
