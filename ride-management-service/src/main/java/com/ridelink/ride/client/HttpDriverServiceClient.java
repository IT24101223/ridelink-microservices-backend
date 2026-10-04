package com.ridelink.ride.client;

import com.ridelink.ride.exception.DownstreamServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of {@link DriverServiceClient} using Spring RestClient.
 *
 * <p>Active on all profiles EXCEPT "stub".
 * Propagates the caller's JWT via the Authorization header so the Driver Service
 * can authorise inbound requests (service-to-service calls use ADMIN role tokens).
 *
 * <p><strong>Timeouts</strong>: connect=2 s, read=5 s – chosen to fail fast without
 * blocking the ride creation thread for too long.
 */
@Component
@Profile("!stub")
public class HttpDriverServiceClient implements DriverServiceClient {

    private static final Logger log = LoggerFactory.getLogger(HttpDriverServiceClient.class);

    private final RestClient restClient;
    private final String baseUrl;

    public HttpDriverServiceClient(
            @Value("${services.driver.base-url}") String baseUrl,
            RestClient.Builder builder) {
        this.baseUrl = baseUrl;
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<EligibleDriverDto> getEligibleDrivers(double lat, double lng, double radiusKm) {
        log.debug("Calling Driver Service: getEligibleDrivers lat={} lng={} radius={}", lat, lng, radiusKm);
        try {
            return restClient.get()
                    .uri("/api/drivers/eligible?lat={lat}&lng={lng}&radiusKm={radiusKm}",
                            lat, lng, radiusKm)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<EligibleDriverDto>>() {});
        } catch (RestClientException e) {
            throw new DownstreamServiceException("DriverService",
                    "Failed to fetch eligible drivers", e);
        }
    }

    @Override
    public void markDriverBusy(UUID driverId) {
        log.debug("Calling Driver Service: markDriverBusy driverId={}", driverId);
        try {
            restClient.patch()
                    .uri("/api/drivers/{driverId}/availability", driverId)
                    .body("{\"available\": false}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new DownstreamServiceException("DriverService",
                    "Failed to mark driver busy: " + driverId, e);
        }
    }

    @Override
    public void markDriverAvailable(UUID driverId) {
        log.debug("Calling Driver Service: markDriverAvailable driverId={}", driverId);
        try {
            restClient.patch()
                    .uri("/api/drivers/{driverId}/availability", driverId)
                    .body("{\"available\": true}")
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // Non-fatal: log a warning but do not fail the ride completion.
            log.warn("Failed to mark driver {} available after ride completion: {}", driverId, e.getMessage());
        }
    }
}
