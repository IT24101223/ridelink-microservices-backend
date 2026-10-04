package com.ridelink.ride.strategy;

import com.ridelink.ride.client.EligibleDriverDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NearestEligibleDriverStrategy}.
 */
@DisplayName("NearestEligibleDriverStrategy")
class NearestEligibleDriverStrategyTest {

    private final NearestEligibleDriverStrategy strategy = new NearestEligibleDriverStrategy();

    // Pickup: 51.5074, -0.1278  (London)

    @Test
    @DisplayName("Empty list returns empty Optional")
    void emptyList() {
        Optional<EligibleDriverDto> result = strategy.selectDriver(
                Collections.emptyList(), 51.5074, -0.1278);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Null list returns empty Optional")
    void nullList() {
        Optional<EligibleDriverDto> result = strategy.selectDriver(null, 51.5074, -0.1278);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Single driver is always selected")
    void singleDriver() {
        EligibleDriverDto driver = driver("00000000-0000-0000-0000-000000000001", 51.51, -0.12);
        Optional<EligibleDriverDto> result = strategy.selectDriver(List.of(driver), 51.5074, -0.1278);
        assertThat(result).isPresent().contains(driver);
    }

    @Test
    @DisplayName("Nearest driver is selected from multiple candidates")
    void nearestDriverSelected() {
        // Driver A is ~1 km away, Driver B is ~10 km away
        EligibleDriverDto driverA = driver("00000000-0000-0000-0000-AAAAAAAAAAAA".toLowerCase(), 51.514, -0.128);
        EligibleDriverDto driverB = driver("00000000-0000-0000-0000-BBBBBBBBBBBB".toLowerCase(), 51.59,  -0.20);

        Optional<EligibleDriverDto> result = strategy.selectDriver(
                List.of(driverB, driverA), 51.5074, -0.1278);

        assertThat(result).isPresent();
        assertThat(result.get().getDriverId()).isEqualTo(driverA.getDriverId());
    }

    @Test
    @DisplayName("Tie-break: lowest UUID string is selected")
    void tieBreakByUuid() {
        // Both at identical coordinates (distance = 0)
        EligibleDriverDto driverLow  = driver("00000000-0000-0000-0000-000000000001", 51.5074, -0.1278);
        EligibleDriverDto driverHigh = driver("00000000-0000-0000-0000-000000000002", 51.5074, -0.1278);

        Optional<EligibleDriverDto> result = strategy.selectDriver(
                List.of(driverHigh, driverLow), 51.5074, -0.1278);

        assertThat(result).isPresent();
        assertThat(result.get().getDriverId()).isEqualTo(driverLow.getDriverId());
    }

    @Test
    @DisplayName("Haversine distance is positive and non-zero for non-overlapping points")
    void haversinePositive() {
        double dist = NearestEligibleDriverStrategy.haversineKm(51.5074, -0.1278, 48.8566, 2.3522); // London→Paris
        assertThat(dist).isGreaterThan(300).isLessThan(400); // ~340 km
    }

    // ── Helper ──────────────────────────────────────────────────────────

    private EligibleDriverDto driver(String id, double lat, double lng) {
        EligibleDriverDto dto = new EligibleDriverDto();
        dto.setDriverId(UUID.fromString(id));
        dto.setCurrentLat(lat);
        dto.setCurrentLng(lng);
        dto.setDisplayName("Driver-" + id.substring(id.length() - 4));
        return dto;
    }
}
