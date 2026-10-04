package com.ridelink.ride.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Servlet filter that extracts and validates the JWT on every request.
 *
 * <p>If the token is valid, a {@link UsernamePasswordAuthenticationToken} is placed
 * in the {@link SecurityContextHolder} with:
 * <ul>
 *   <li>principal = {@link RideLinkPrincipal}</li>
 *   <li>credentials = raw JWT string (useful for forwarding to downstream services)</li>
 *   <li>authority = {@code ROLE_<role>} (e.g. {@code ROLE_PASSENGER})</li>
 * </ul>
 *
 * <p>Invalid/missing tokens: the filter simply does not set the security context,
 * letting Spring Security's default 401 handling take over.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenValidator validator;

    public JwtAuthenticationFilter(JwtTokenValidator validator) {
        this.validator = validator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            Claims claims = validator.validateAndExtract(token);
            RideLinkPrincipal principal = new RideLinkPrincipal(
                    validator.extractUserId(claims),
                    validator.extractRole(claims));

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    token,   // keep raw JWT as credentials for downstream calls
                    List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user={} role={}", principal.userId(), principal.role());

        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            // Do not set authentication; Spring Security will return 401.
        }

        filterChain.doFilter(request, response);
    }
}
