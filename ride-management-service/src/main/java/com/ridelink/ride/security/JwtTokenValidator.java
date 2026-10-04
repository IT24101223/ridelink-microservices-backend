package com.ridelink.ride.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.UUID;

/**
 * Validates and parses JWT tokens issued by the Account Service.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Algorithm: HS256 (symmetric – Account Service and this service share the same secret).</li>
 *   <li>Secret is read exclusively from the {@code JWT_SECRET} env variable.
 *       No fallback value is kept in code or YAML to prevent accidental exposure.</li>
 *   <li>Only validation is performed here; token issuance is Account Service's job.</li>
 * </ul>
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    @Value("${jwt.secret}")
    private String secretBase64;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        // Decode Base64-encoded secret and create HMAC-SHA256 key
        byte[] keyBytes = Base64.getDecoder().decode(secretBase64);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Parses the JWT and returns the extracted {@link Claims}.
     *
     * @param token the raw JWT string (without "Bearer " prefix)
     * @return valid {@link Claims}
     * @throws JwtException if the token is invalid or expired
     */
    public Claims validateAndExtract(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Extracts {@code sub} claim as {@link UUID}. */
    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    /** Extracts the {@code role} custom claim (e.g., "PASSENGER", "DRIVER", "ADMIN"). */
    public String extractRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
