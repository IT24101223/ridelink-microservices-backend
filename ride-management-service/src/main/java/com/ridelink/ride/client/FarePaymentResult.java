package com.ridelink.ride.client;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Result returned by {@link FareServiceClient#finalizeAndPay}.
 *
 * <p>Assumed contract – subject to team review.
 */
@Data
public class FarePaymentResult {

    /** The final calculated fare for the completed ride. */
    private BigDecimal finalFare;

    /** Simulated payment confirmation identifier. */
    private UUID paymentId;
}
