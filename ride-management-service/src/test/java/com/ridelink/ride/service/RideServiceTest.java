package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.EligibleDriverDto;
import com.ridelink.ride.client.FarePaymentResult;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.domain.Ride;
import com.ridelink.ride.domain.RideStatus;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideMapper;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.RideLinkPrincipal;
import com.ridelink.ride.strategy.DriverSelectionStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RideService}.
 *
 * <p>All downstream clients and the repository are mocked with Mockito,
 * so these tests are fast and isolated.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RideService")
class RideServiceTest {

    @Mock RideRepository      rideRepository;
    @Mock DriverServiceClient driverClient;
    @Mock FareServiceClient   fareClient;
    @Mock DriverSelectionStrategy selectionStrategy;
    @Mock RideMapper          rideMapper;

    @InjectMocks RideService rideService;

    private final UUID PASSENGER_ID = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private final UUID DRIVER_ID    = UUID.fromString("bbbb0000-0000-0000-0000-000000000002");
    private final UUID RIDE_ID      = UUID.fromString("cccc0000-0000-0000-0000-000000000003");

    private RideLinkPrincipal passengerPrincipal;
    private RideLinkPrincipal driverPrincipal;
    private RideLinkPrincipal adminPrincipal;
    private RideLinkPrincipal otherDriverPrincipal;

    @BeforeEach
    void setUp() {
        passengerPrincipal    = new RideLinkPrincipal(PASSENGER_ID, "PASSENGER");
        driverPrincipal       = new RideLinkPrincipal(DRIVER_ID, "DRIVER");
        adminPrincipal        = new RideLinkPrincipal(UUID.randomUUID(), "ADMIN");
        otherDriverPrincipal  = new RideLinkPrincipal(UUID.randomUUID(), "DRIVER");
    }

    // ──────────────────────────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRide: success – assigns nearest driver and persists")
    void createRide_success() {
        CreateRideRequest req = createRequest();
        EligibleDriverDto driver = eligibleDriver(DRIVER_ID);
        Ride rideEntity = rideWithStatus(RideStatus.ASSIGNED);
        RideResponse expectedResponse = new RideResponse();

        when(fareClient.estimateFare(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new BigDecimal("8.50"));
        when(driverClient.getEligibleDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(driver));
        when(selectionStrategy.selectDriver(anyList(), anyDouble(), anyDouble()))
                .thenReturn(Optional.of(driver));
        when(rideMapper.toEntity(any(), any(), any(), any())).thenReturn(rideEntity);
        when(rideRepository.save(any(Ride.class))).thenReturn(rideEntity);
        when(rideMapper.toResponse(any(Ride.class))).thenReturn(expectedResponse);

        RideResponse result = rideService.createRide(req, passengerPrincipal);

        assertThat(result).isSameAs(expectedResponse);
        verify(rideRepository).save(any(Ride.class));
        verify(driverClient).markDriverBusy(DRIVER_ID);
    }

    @Test
    @DisplayName("createRide: throws NoDriverAvailableException when no eligible drivers")
    void createRide_noDriver() {
        CreateRideRequest req = createRequest();

        when(fareClient.estimateFare(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(new BigDecimal("8.50"));
        when(driverClient.getEligibleDrivers(anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.emptyList());
        when(selectionStrategy.selectDriver(anyList(), anyDouble(), anyDouble()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.createRide(req, passengerPrincipal))
                .isInstanceOf(NoDriverAvailableException.class);

        verify(rideRepository, never()).save(any());
    }

    @Test
    @DisplayName("createRide: DownstreamServiceException propagates when fare service fails")
    void createRide_fareServiceDown() {
        CreateRideRequest req = createRequest();

        when(fareClient.estimateFare(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenThrow(new DownstreamServiceException("FareService", "timeout"));

        assertThatThrownBy(() -> rideService.createRide(req, passengerPrincipal))
                .isInstanceOf(DownstreamServiceException.class);
    }

    // ──────────────────────────────────────────────────────────────────
    // ACCEPT
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("acceptRide: success for assigned driver")
    void acceptRide_success() {
        Ride ride = rideWithStatus(RideStatus.ASSIGNED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenReturn(ride);
        when(rideMapper.toResponse(any())).thenReturn(new RideResponse());

        assertThatCode(() -> rideService.acceptRide(RIDE_ID, driverPrincipal))
                .doesNotThrowAnyException();

        assertThat(ride.getStatus()).isEqualTo(RideStatus.ACCEPTED);
    }

    @Test
    @DisplayName("acceptRide: throws AccessDeniedException for wrong driver")
    void acceptRide_wrongDriver() {
        Ride ride = rideWithStatus(RideStatus.ASSIGNED);
        ride.setDriverId(DRIVER_ID);
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.acceptRide(RIDE_ID, otherDriverPrincipal))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ──────────────────────────────────────────────────────────────────
    // COMPLETE
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeRide: stores finalFare and paymentId")
    void completeRide_success() {
        Ride ride = rideWithStatus(RideStatus.IN_PROGRESS);
        ride.setDriverId(DRIVER_ID);
        ride.setPassengerId(PASSENGER_ID);
        FarePaymentResult payment = new FarePaymentResult();
        payment.setFinalFare(new BigDecimal("9.25"));
        payment.setPaymentId(UUID.randomUUID());

        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));
        when(fareClient.finalizeAndPay(any(), any(), any(), anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(payment);
        when(rideRepository.save(any())).thenReturn(ride);
        when(rideMapper.toResponse(any())).thenReturn(new RideResponse());

        rideService.completeRide(RIDE_ID, driverPrincipal);

        assertThat(ride.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(ride.getFinalFare()).isEqualByComparingTo("9.25");
        assertThat(ride.getPaymentId()).isEqualTo(payment.getPaymentId());
        verify(driverClient).markDriverAvailable(DRIVER_ID);
    }

    // ──────────────────────────────────────────────────────────────────
    // CANCEL
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelRide: passenger can cancel own REQUESTED ride")
    void cancelRide_passengerOwnerSuccess() {
        Ride ride = rideWithStatus(RideStatus.ASSIGNED);
        ride.setPassengerId(PASSENGER_ID);
        ride.setDriverId(DRIVER_ID);
        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Changed my mind");

        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any())).thenReturn(ride);
        when(rideMapper.toResponse(any())).thenReturn(new RideResponse());

        rideService.cancelRide(RIDE_ID, req, passengerPrincipal);

        assertThat(ride.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(ride.getCancelReason()).isEqualTo("Changed my mind");
    }

    @Test
    @DisplayName("cancelRide: throws InvalidStatusTransitionException when IN_PROGRESS")
    void cancelRide_afterStart() {
        Ride ride = rideWithStatus(RideStatus.IN_PROGRESS);
        ride.setPassengerId(PASSENGER_ID);
        ride.setDriverId(DRIVER_ID);
        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Too late");

        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(RIDE_ID, req, passengerPrincipal))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("cancelRide: throws AccessDeniedException for unrelated passenger")
    void cancelRide_wrongPassenger() {
        Ride ride = rideWithStatus(RideStatus.ASSIGNED);
        ride.setPassengerId(UUID.randomUUID()); // different passenger
        CancelRideRequest req = new CancelRideRequest();
        req.setReason("Not my ride");

        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.cancelRide(RIDE_ID, req, passengerPrincipal))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ──────────────────────────────────────────────────────────────────
    // GET
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRide: RideNotFoundException when ride doesn't exist")
    void getRide_notFound() {
        when(rideRepository.findById(RIDE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.getRide(RIDE_ID, passengerPrincipal))
                .isInstanceOf(RideNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private CreateRideRequest createRequest() {
        CreateRideRequest req = new CreateRideRequest();
        req.setPickupAddress("University Gate");
        req.setPickupLat(51.5074);
        req.setPickupLng(-0.1278);
        req.setDestinationAddress("Train Station");
        req.setDestinationLat(51.5155);
        req.setDestinationLng(-0.1415);
        return req;
    }

    private EligibleDriverDto eligibleDriver(UUID id) {
        EligibleDriverDto dto = new EligibleDriverDto();
        dto.setDriverId(id);
        dto.setCurrentLat(51.509);
        dto.setCurrentLng(-0.130);
        dto.setDisplayName("Test Driver");
        return dto;
    }

    private Ride rideWithStatus(RideStatus status) {
        Ride ride = new Ride();
        ride.setId(RIDE_ID);
        ride.setStatus(status);
        ride.setPickupAddress("University Gate");
        ride.setPickupLat(51.5074);
        ride.setPickupLng(-0.1278);
        ride.setDestinationAddress("Train Station");
        ride.setDestinationLat(51.5155);
        ride.setDestinationLng(-0.1415);
        return ride;
    }
}
