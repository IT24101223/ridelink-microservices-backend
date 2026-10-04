package com.ridelink.ride.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ridelink.ride.domain.RideStatus;
import com.ridelink.ride.dto.CreateRideRequest;
import com.ridelink.ride.dto.RideResponse;
import com.ridelink.ride.exception.*;
import com.ridelink.ride.security.JwtAuthenticationFilter;
import com.ridelink.ride.security.RideLinkPrincipal;
import com.ridelink.ride.service.RideService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc controller slice tests.
 *
 * <p>Tests only the web layer – no database, no real service, no JWT validation.
 * Uses Spring Security's {@code SecurityMockMvcRequestPostProcessors.jwt()} to inject
 * pre-authenticated principals without needing a real JWT_SECRET in test scope.
 *
 * <p>We {@code @MockBean} the {@link JwtAuthenticationFilter} so the real filter
 * (which requires JWT_SECRET) is replaced, while Spring Security's built-in
 * test support provides authentication via the {@code .with(jwt())} post-processor.
 */
@WebMvcTest(controllers = RideController.class)
@org.springframework.context.annotation.Import({com.ridelink.ride.config.SecurityConfig.class, com.ridelink.ride.security.JwtAuthenticationFilter.class})
@DisplayName("RideController MockMvc")
class RideControllerTest {

    @Autowired MockMvc      mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean RideService             rideService;
    @MockBean com.ridelink.ride.security.JwtTokenValidator jwtTokenValidator;

    private static final UUID PASSENGER_ID = UUID.fromString("aaaa0000-0000-0000-0000-000000000001");
    private static final UUID DRIVER_ID    = UUID.fromString("bbbb0000-0000-0000-0000-000000000002");
    private static final UUID RIDE_ID      = UUID.fromString("cccc0000-0000-0000-0000-000000000003");

    // ── 201 CREATE ────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 201 for PASSENGER")
    void createRide_201() throws Exception {
        RideResponse resp = response(RIDE_ID, RideStatus.ASSIGNED);
        when(rideService.createRide(any(), any())).thenReturn(resp);

        mockMvc.perform(post("/api/rides")
                        .with(passenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RIDE_ID.toString()))
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    // ── 400 Validation ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 400 for blank pickupAddress")
    void createRide_400_validation() throws Exception {
        CreateRideRequest req = validRequest();
        req.setPickupAddress(""); // blank – violates @NotBlank

        mockMvc.perform(post("/api/rides")
                        .with(passenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }

    // ── 401 Unauthenticated ───────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 401 when no token")
    void createRide_401() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized());
    }

    // ── 403 Wrong Role ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 403 for DRIVER role (passengers only)")
    void createRide_403_driver() throws Exception {
        mockMvc.perform(post("/api/rides")
                        .with(driver())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden());
    }

    // ── 404 Not Found ─────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rides/{id} → 404 when ride not found")
    void getRide_404() throws Exception {
        when(rideService.getRide(eq(RIDE_ID), any()))
                .thenThrow(new RideNotFoundException(RIDE_ID));

        mockMvc.perform(get("/api/rides/{id}", RIDE_ID)
                        .with(passenger()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── 409 Invalid Transition ────────────────────────────────────────

    @Test
    @DisplayName("PATCH /api/rides/{id}/accept → 409 invalid state transition")
    void acceptRide_409() throws Exception {
        when(rideService.acceptRide(eq(RIDE_ID), any()))
                .thenThrow(new InvalidStatusTransitionException(RideStatus.COMPLETED, RideStatus.ACCEPTED));

        mockMvc.perform(patch("/api/rides/{id}/accept", RIDE_ID)
                        .with(driver()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── 409 No Driver ─────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 409 when no driver available")
    void createRide_409_noDriver() throws Exception {
        when(rideService.createRide(any(), any()))
                .thenThrow(new NoDriverAvailableException(51.5, -0.1));

        mockMvc.perform(post("/api/rides")
                        .with(passenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isConflict());
    }

    // ── 502 Downstream Failure ────────────────────────────────────────

    @Test
    @DisplayName("POST /api/rides → 502 when downstream service fails")
    void createRide_502() throws Exception {
        when(rideService.createRide(any(), any()))
                .thenThrow(new DownstreamServiceException("FareService", "timeout"));

        mockMvc.perform(post("/api/rides")
                        .with(passenger())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadGateway());
    }

    // ── 403 Admin-only endpoint ────────────────────────────────────────

    @Test
    @DisplayName("GET /api/rides → 403 for PASSENGER (admin only endpoint)")
    void getAllRides_403_passenger() throws Exception {
        mockMvc.perform(get("/api/rides").with(passenger()))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    /** Simulate a PASSENGER authenticated principal. */
    private org.springframework.test.web.servlet.request.RequestPostProcessor passenger() {
        RideLinkPrincipal principal = new RideLinkPrincipal(PASSENGER_ID, "PASSENGER");
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, "mock-jwt-token", java.util.List.of(new SimpleGrantedAuthority("ROLE_PASSENGER")));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    /** Simulate a DRIVER authenticated principal. */
    private org.springframework.test.web.servlet.request.RequestPostProcessor driver() {
        RideLinkPrincipal principal = new RideLinkPrincipal(DRIVER_ID, "DRIVER");
        org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        principal, "mock-jwt-token", java.util.List.of(new SimpleGrantedAuthority("ROLE_DRIVER")));
        return SecurityMockMvcRequestPostProcessors.authentication(auth);
    }

    private CreateRideRequest validRequest() {
        CreateRideRequest req = new CreateRideRequest();
        req.setPickupAddress("University Gate, London");
        req.setPickupLat(51.5074);
        req.setPickupLng(-0.1278);
        req.setDestinationAddress("King's Cross Station");
        req.setDestinationLat(51.5309);
        req.setDestinationLng(-0.1233);
        return req;
    }

    private RideResponse response(UUID id, RideStatus status) {
        RideResponse r = new RideResponse();
        r.setId(id);
        r.setStatus(status);
        r.setEstimatedFare(new BigDecimal("8.50"));
        return r;
    }
}
