package com.ridelink.ride.security;

import java.util.UUID;

/**
 * Immutable value object representing the authenticated principal extracted from the JWT.
 *
 * <p>Stored in the Spring Security {@link org.springframework.security.core.context.SecurityContext}
 * and retrieved in service methods for ownership checks.
 *
 * @param userId UUID from the JWT {@code sub} claim
 * @param role   role string from the JWT {@code role} claim (e.g., "PASSENGER")
 */
public record RideLinkPrincipal(UUID userId, String role) {

    public boolean isPassenger() { return "PASSENGER".equals(role); }
    public boolean isDriver()    { return "DRIVER".equals(role);    }
    public boolean isAdmin()     { return "ADMIN".equals(role);     }
}
