package com.ridelink.ride.client;

import com.ridelink.ride.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Production implementation of {@link FareServiceClient} using Spring RestClient.
 *
 * <p>Active on all profiles EXCEPT "stub".
 */
@Component
@Profile("!stub")
public class HttpFareServiceClient implements FareServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HttpFareServiceClient.class);

    private final RestClient restClient;

    public HttpFareServiceClient(
            @Value("${services.fare.base-url}") String baseUrl,
            RestClient.Builder builder) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public BigDecimal estimateFare(double pickupLat, double pickupLng,
                                   double destLat, double destLng) {
        log.debug("Calling Fare Service: estimateFare");
        try {
            FareEstimateResponse resp = restClient.get()
                    .uri("/api/fares/estimate?pickupLat={pLat}&pickupLng={pLng}&destLat={dLat}&destLng={dLng}",
                            pickupLat, pickupLng, destLat, destLng)
                    .retrieve()
                    .body(FareEstimateResponse.class);
            return resp != null ? resp.getEstimatedFare() : BigDecimal.ZERO;
        } catch (RestClientException e) {
            throw new DownstreamServiceException("FareService", "Failed to get fare estimate", e);
        }
    }

    @Override
    public FarePaymentResult finalizeAndPay(UUID rideId, UUID passengerId, UUID driverId,
                                            double pickupLat, double pickupLng,
                                            double destLat,   double destLng) {
        log.debug("Calling Fare Service: finalizeAndPay rideId={}", rideId);
        try {
            FinalizeRequest req = new FinalizeRequest(rideId, passengerId, driverId,
                    pickupLat, pickupLng, destLat, destLng);
            return restClient.post()
                    .uri("/api/fares/finalize")
                    .body(req)
                    .retrieve()
                    .body(FarePaymentResult.class);
        } catch (RestClientException e) {
            throw new DownstreamServiceException("FareService", "Failed to finalize fare/payment", e);
        }
    }

    // ── Inner helper DTOs (kept private – not part of public API contract) ─
    private record FareEstimateResponse(BigDecimal estimatedFare) {
        BigDecimal getEstimatedFare() { return estimatedFare; }
    }

    private record FinalizeRequest(
            UUID rideId, UUID passengerId, UUID driverId,
            double pickupLat, double pickupLng,
            double destLat, double destLng) {}
}
