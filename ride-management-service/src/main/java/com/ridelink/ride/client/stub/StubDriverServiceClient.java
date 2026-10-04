package com.ridelink.ride.client.stub;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.EligibleDriverDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * In-memory fake {@link DriverServiceClient} for the "stub" Spring profile.
 *
 * <p>Returns a single pre-configured fictitious driver located at
 * latitude 51.5074 (London centre) so ride creation always succeeds in demo mode.
 * Activate with {@code --spring.profiles.active=stub}.
 */
@Component
@Profile("stub")
public class StubDriverServiceClient implements DriverServiceClient {

    private static final Logger log = LoggerFactory.getLogger(StubDriverServiceClient.class);

    private static final UUID STUB_DRIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Override
    public List<EligibleDriverDto> getEligibleDrivers(double lat, double lng, double radiusKm) {
        log.info("[STUB] getEligibleDrivers – returning fake driver");
        EligibleDriverDto driver = new EligibleDriverDto();
        driver.setDriverId(STUB_DRIVER_ID);
        driver.setCurrentLat(lat + 0.002);   // 200 m away
        driver.setCurrentLng(lng + 0.002);
        driver.setDisplayName("Stub Driver Alice");
        return List.of(driver);
    }

    @Override
    public void markDriverBusy(UUID driverId) {
        log.info("[STUB] markDriverBusy driverId={}", driverId);
    }

    @Override
    public void markDriverAvailable(UUID driverId) {
        log.info("[STUB] markDriverAvailable driverId={}", driverId);
    }
}
