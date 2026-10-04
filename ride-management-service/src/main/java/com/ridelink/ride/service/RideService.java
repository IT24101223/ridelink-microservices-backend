package com.ridelink.ride.service;

import com.ridelink.ride.client.DriverServiceClient;
import com.ridelink.ride.client.EligibleDriverDto;
import com.ridelink.ride.client.FarePaymentResult;
import com.ridelink.ride.client.FareServiceClient;
import com.ridelink.ride.domain.Ride;
import com.ridelink.ride.domain.RideStateMachine;
import com.ridelink.ride.domain.RideStatus;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideMapper;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.AccessDeniedException;
import com.ridelink.ride.exception.NoDriverAvailableException;
import com.ridelink.ride.exception.RideNotFoundException;
import com.ridelink.ride.repository.RideRepository;
import com.ridelink.ride.security.RideLinkPrincipal;
import com.ridelink.ride.strategy.DriverSelectionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core business logic for ride management.
 *
 * <p>Design principles applied:
 * <ul>
 *   <li><b>SRP</b>: state transitions delegated to {@link RideStateMachine};
 *       driver selection to {@link DriverSelectionStrategy}; HTTP calls to client interfaces.</li>
 *   <li><b>DIP</b>: depends on {@link DriverServiceClient} and {@link FareServiceClient}
 *       interfaces, not concrete HTTP implementations.</li>
 *   <li><b>No business logic in controllers</b>: all ownership/role checks are performed here.</li>
 * </ul>
 */
@Service
@Transactional
public class RideService {

    private static final Logger log = LoggerFactory.getLogger(RideService.class);
    private static final double DEFAULT_SEARCH_RADIUS_KM = 10.0;

    private final RideRepository rideRepository;
    private final DriverServiceClient driverClient;
    private final FareServiceClient fareClient;
    private final DriverSelectionStrategy selectionStrategy;
    private final RideMapper rideMapper;

    public RideService(RideRepository rideRepository,
                       DriverServiceClient driverClient,
                       FareServiceClient fareClient,
                       DriverSelectionStrategy selectionStrategy,
                       RideMapper rideMapper) {
        this.rideRepository    = rideRepository;
        this.driverClient      = driverClient;
        this.fareClient        = fareClient;
        this.selectionStrategy = selectionStrategy;
        this.rideMapper        = rideMapper;
    }

    // ──────────────────────────────────────────────────────────────────
    // CREATE
    // ──────────────────────────────────────────────────────────────────

    /**
     * Creates a new ride for the authenticated passenger.
     *
     * <ol>
     *   <li>Validates input (done by @Valid in controller).</li>
     *   <li>Calls Fare Service for an estimate.</li>
     *   <li>Calls Driver Service for eligible drivers near the pickup.</li>
     *   <li>Applies {@link DriverSelectionStrategy} to pick a driver.</li>
     *   <li>If a driver is found: status = ASSIGNED, marks driver BUSY.</li>
     *   <li>If no driver: throws {@link NoDriverAvailableException} → HTTP 409.</li>
     * </ol>
     *
     * @param request     validated request body
     * @param principal   authenticated passenger principal
     * @return persisted ride as {@link RideResponse}
     */
    public RideResponse createRide(CreateRideRequest request, RideLinkPrincipal principal) {
        log.info("Creating ride for passenger={}", principal.userId());

        // 1. Get fare estimate (non-fatal if fails in stub mode – already throws DownstreamServiceException)
        BigDecimal estimatedFare = fareClient.estimateFare(
                request.getPickupLat(), request.getPickupLng(),
                request.getDestinationLat(), request.getDestinationLng());

        // 2. Fetch eligible drivers
        List<EligibleDriverDto> candidates = driverClient.getEligibleDrivers(
                request.getPickupLat(), request.getPickupLng(), DEFAULT_SEARCH_RADIUS_KM);

        // 3. Select nearest driver
        Optional<EligibleDriverDto> selectedDriver = selectionStrategy.selectDriver(
                candidates, request.getPickupLat(), request.getPickupLng());

        if (selectedDriver.isEmpty()) {
            throw new NoDriverAvailableException(request.getPickupLat(), request.getPickupLng());
        }

        UUID driverId = selectedDriver.get().getDriverId();

        // 4. Build entity
        Ride ride = rideMapper.toEntity(request, UUID.randomUUID(), principal.userId(), RideStatus.REQUESTED);
        ride.setEstimatedFare(estimatedFare);
        ride.setDriverId(driverId);

        // Apply REQUESTED → ASSIGNED transition via state machine
        RideStateMachine.transition(RideStatus.REQUESTED, RideStatus.ASSIGNED);
        ride.setStatus(RideStatus.ASSIGNED);
        ride.setAssignedAt(Instant.now());
        ride.addHistory(RideStatus.REQUESTED, RideStatus.ASSIGNED, principal.userId().toString());

        // 5. Persist
        Ride saved = rideRepository.save(ride);

        // 6. Mark driver busy (after save – if this fails, log only; ride is already saved)
        try {
            driverClient.markDriverBusy(driverId);
        } catch (Exception e) {
            log.warn("Failed to mark driver {} busy – ride {} still created. Cause: {}",
                    driverId, saved.getId(), e.getMessage());
        }

        log.info("Ride {} created – assigned to driver={}", saved.getId(), driverId);
        return rideMapper.toResponse(saved);
    }

    // ──────────────────────────────────────────────────────────────────
    // READ
    // ──────────────────────────────────────────────────────────────────

    /**
     * Retrieves a ride by id with ownership enforcement.
     * <ul>
     *   <li>PASSENGER: may only see their own ride.</li>
     *   <li>DRIVER: may only see rides assigned to them.</li>
     *   <li>ADMIN: may see any ride.</li>
     * </ul>
     */
    @Transactional(readOnly = true)
    public RideResponse getRide(UUID rideId, RideLinkPrincipal principal) {
        Ride ride = findByIdOrThrow(rideId);
        checkReadAccess(ride, principal);
        return rideMapper.toResponse(ride);
    }

    /**
     * Returns all rides owned by the authenticated passenger.
     *
     * @param status optional status filter
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getMyRidesAsPassenger(RideLinkPrincipal principal,
                                                     Optional<RideStatus> status) {
        List<Ride> rides = status
                .map(s -> rideRepository.findByPassengerIdAndStatus(principal.userId(), s))
                .orElseGet(() -> rideRepository.findByPassengerId(principal.userId()));
        return rides.stream().map(rideMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Returns all rides assigned to the authenticated driver.
     */
    @Transactional(readOnly = true)
    public List<RideResponse> getMyRidesAsDriver(RideLinkPrincipal principal,
                                                  Optional<RideStatus> status) {
        List<Ride> rides = status
                .map(s -> rideRepository.findByDriverIdAndStatus(principal.userId(), s))
                .orElseGet(() -> rideRepository.findByDriverId(principal.userId()));
        return rides.stream().map(rideMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Admin: paginated view of all rides with optional status filter.
     */
    @Transactional(readOnly = true)
    public Page<RideResponse> getAllRides(Optional<RideStatus> status, Pageable pageable) {
        Page<Ride> page = status
                .map(s -> rideRepository.findByStatus(s, pageable))
                .orElseGet(() -> rideRepository.findAll(pageable));
        return page.map(rideMapper::toResponse);
    }

    // ──────────────────────────────────────────────────────────────────
    // LIFECYCLE TRANSITIONS
    // ──────────────────────────────────────────────────────────────────

    /**
     * Driver accepts the ride (ASSIGNED → ACCEPTED).
     *
     * @param rideId    the ride to accept
     * @param principal the authenticated driver
     */
    public RideResponse acceptRide(UUID rideId, RideLinkPrincipal principal) {
        Ride ride = findByIdOrThrow(rideId);
        requireAssignedDriver(ride, principal);
        RideStateMachine.transition(ride.getStatus(), RideStatus.ACCEPTED);
        ride.addHistory(ride.getStatus(), RideStatus.ACCEPTED, principal.userId().toString());
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setAcceptedAt(Instant.now());
        return rideMapper.toResponse(rideRepository.save(ride));
    }

    /**
     * Driver starts the ride (ACCEPTED → IN_PROGRESS).
     */
    public RideResponse startRide(UUID rideId, RideLinkPrincipal principal) {
        Ride ride = findByIdOrThrow(rideId);
        requireAssignedDriver(ride, principal);
        RideStateMachine.transition(ride.getStatus(), RideStatus.IN_PROGRESS);
        ride.addHistory(ride.getStatus(), RideStatus.IN_PROGRESS, principal.userId().toString());
        ride.setStatus(RideStatus.IN_PROGRESS);
        ride.setStartedAt(Instant.now());
        return rideMapper.toResponse(rideRepository.save(ride));
    }

    /**
     * Driver completes the ride (IN_PROGRESS → COMPLETED).
     *
     * <p>Calls Fare Service to finalize fare and record simulated payment.
     * Then marks the driver available again.
     */
    public RideResponse completeRide(UUID rideId, RideLinkPrincipal principal) {
        Ride ride = findByIdOrThrow(rideId);
        requireAssignedDriver(ride, principal);
        RideStateMachine.transition(ride.getStatus(), RideStatus.COMPLETED);

        // Finalize fare + payment via Fare Service
        FarePaymentResult result = fareClient.finalizeAndPay(
                ride.getId(), ride.getPassengerId(), ride.getDriverId(),
                ride.getPickupLat(), ride.getPickupLng(),
                ride.getDestinationLat(), ride.getDestinationLng());

        ride.addHistory(ride.getStatus(), RideStatus.COMPLETED, principal.userId().toString());
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(Instant.now());
        ride.setFinalFare(result.getFinalFare());
        ride.setPaymentId(result.getPaymentId());

        Ride saved = rideRepository.save(ride);

        // Best-effort: release driver back to available
        try {
            driverClient.markDriverAvailable(ride.getDriverId());
        } catch (Exception e) {
            log.warn("Could not mark driver {} available after completion of ride {}: {}",
                    ride.getDriverId(), rideId, e.getMessage());
        }

        log.info("Ride {} completed. finalFare={} paymentId={}",
                saved.getId(), result.getFinalFare(), result.getPaymentId());
        return rideMapper.toResponse(saved);
    }

    /**
     * Cancels a ride (allowed before IN_PROGRESS).
     *
     * <p>Callers: PASSENGER owner, assigned DRIVER, or ADMIN.
     * Not allowed after IN_PROGRESS (state machine enforces this).
     */
    public RideResponse cancelRide(UUID rideId, CancelRideRequest cancelRequest,
                                   RideLinkPrincipal principal) {
        Ride ride = findByIdOrThrow(rideId);
        checkCancelAccess(ride, principal);
        RideStateMachine.transition(ride.getStatus(), RideStatus.CANCELLED);

        UUID driverId = ride.getDriverId();
        ride.addHistory(ride.getStatus(), RideStatus.CANCELLED, principal.userId().toString());
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());
        ride.setCancelReason(cancelRequest.getReason());

        Ride saved = rideRepository.save(ride);

        // Best-effort: release driver
        if (driverId != null) {
            try {
                driverClient.markDriverAvailable(driverId);
            } catch (Exception e) {
                log.warn("Could not release driver {} after cancel of ride {}: {}",
                        driverId, rideId, e.getMessage());
            }
        }

        return rideMapper.toResponse(saved);
    }

    // ──────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────────────────────────────

    private Ride findByIdOrThrow(UUID rideId) {
        return rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    /** Ensures only the assigned driver can perform driver-only actions. */
    private void requireAssignedDriver(Ride ride, RideLinkPrincipal principal) {
        if (!principal.userId().equals(ride.getDriverId())) {
            throw new AccessDeniedException(
                    "Only the assigned driver can perform this action on ride " + ride.getId());
        }
    }

    /** Read access: passenger owner, assigned driver, or admin. */
    private void checkReadAccess(Ride ride, RideLinkPrincipal principal) {
        if (principal.isAdmin()) return;
        if (principal.isPassenger() && principal.userId().equals(ride.getPassengerId())) return;
        if (principal.isDriver()    && principal.userId().equals(ride.getDriverId()))    return;
        throw new AccessDeniedException("You do not have access to ride " + ride.getId());
    }

    /** Cancel access: passenger owner, assigned driver, or admin. */
    private void checkCancelAccess(Ride ride, RideLinkPrincipal principal) {
        if (principal.isAdmin()) return;
        if (principal.isPassenger() && principal.userId().equals(ride.getPassengerId())) return;
        if (principal.isDriver()    && principal.userId().equals(ride.getDriverId()))    return;
        throw new AccessDeniedException("You are not allowed to cancel ride " + ride.getId());
    }
}
