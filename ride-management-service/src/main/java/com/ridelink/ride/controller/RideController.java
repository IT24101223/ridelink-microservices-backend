package com.ridelink.ride.controller;

import com.ridelink.ride.domain.RideStatus;
import com.ridelink.ride.dto.CancelRideRequest;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.security.RideLinkPrincipal;
import com.ridelink.ride.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for ride management endpoints.
 *
 * <p>This class is intentionally thin – no business logic lives here.
 * All decisions, ownership checks, and state transitions are delegated to
 * {@link RideService} (SRP / layered architecture).
 *
 * <p>The {@link RideLinkPrincipal} is injected via
 * {@code @AuthenticationPrincipal} from the security context populated by
 * {@link com.ridelink.ride.security.JwtAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/rides")
@Tag(name = "Rides", description = "Ride lifecycle management")
public class RideController {

    private final RideService rideService;

    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    // ──────────────────────────────────────────────────────────────────
    // POST /api/rides   [PASSENGER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Create a ride request", description =
            "Creates a new ride, fetches a fare estimate, assigns the nearest available driver. " +
            "Returns 409 if no driver is available.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ride created and driver assigned"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "No driver available"),
            @ApiResponse(responseCode = "502", description = "Downstream service unavailable")
    })
    @PostMapping
    @PreAuthorize("hasRole('PASSENGER')")
    public ResponseEntity<RideResponse> createRide(
            @Valid @RequestBody CreateRideRequest request,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        RideResponse response = rideService.createRide(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/rides/{id}   [PASSENGER owner | assigned DRIVER | ADMIN]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get ride by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK"),
            @ApiResponse(responseCode = "403", description = "Not your ride"),
            @ApiResponse(responseCode = "404", description = "Ride not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PASSENGER','DRIVER','ADMIN')")
    public RideResponse getRide(
            @PathVariable UUID id,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.getRide(id, principal);
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/rides/me   [PASSENGER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get own rides (passenger)")
    @GetMapping("/me")
    @PreAuthorize("hasRole('PASSENGER')")
    public List<RideResponse> getMyRides(
            @RequestParam(required = false) RideStatus status,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.getMyRidesAsPassenger(principal, Optional.ofNullable(status));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/rides/driver/me   [DRIVER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get rides assigned to me (driver)")
    @GetMapping("/driver/me")
    @PreAuthorize("hasRole('DRIVER')")
    public List<RideResponse> getDriverRides(
            @RequestParam(required = false) RideStatus status,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.getMyRidesAsDriver(principal, Optional.ofNullable(status));
    }

    // ──────────────────────────────────────────────────────────────────
    // GET /api/rides   [ADMIN]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Get all rides paginated (admin only)")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<RideResponse> getAllRides(
            @RequestParam(required = false) RideStatus status,
            Pageable pageable) {
        return rideService.getAllRides(Optional.ofNullable(status), pageable);
    }

    // ──────────────────────────────────────────────────────────────────
    // PATCH /api/rides/{id}/accept   [assigned DRIVER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Accept a ride (driver)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride accepted"),
            @ApiResponse(responseCode = "403", description = "Not the assigned driver"),
            @ApiResponse(responseCode = "409", description = "Invalid state transition")
    })
    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse acceptRide(
            @PathVariable UUID id,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.acceptRide(id, principal);
    }

    // ──────────────────────────────────────────────────────────────────
    // PATCH /api/rides/{id}/start   [assigned DRIVER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Start a ride (driver)")
    @PatchMapping("/{id}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse startRide(
            @PathVariable UUID id,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.startRide(id, principal);
    }

    // ──────────────────────────────────────────────────────────────────
    // PATCH /api/rides/{id}/complete   [assigned DRIVER]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Complete a ride (driver)", description =
            "Finalizes fare and records simulated payment via Fare Service, then marks driver available.")
    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public RideResponse completeRide(
            @PathVariable UUID id,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.completeRide(id, principal);
    }

    // ──────────────────────────────────────────────────────────────────
    // PATCH /api/rides/{id}/cancel   [PASSENGER owner | DRIVER | ADMIN]
    // ──────────────────────────────────────────────────────────────────

    @Operation(summary = "Cancel a ride", description =
            "Allowed for passenger owner, assigned driver, or admin. Not allowed after IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ride cancelled"),
            @ApiResponse(responseCode = "400", description = "Missing reason"),
            @ApiResponse(responseCode = "403", description = "Not authorised"),
            @ApiResponse(responseCode = "409", description = "Cannot cancel after IN_PROGRESS")
    })
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('PASSENGER','DRIVER','ADMIN')")
    public RideResponse cancelRide(
            @PathVariable UUID id,
            @Valid @RequestBody CancelRideRequest cancelRequest,
            @AuthenticationPrincipal RideLinkPrincipal principal) {
        return rideService.cancelRide(id, cancelRequest, principal);
    }
}
