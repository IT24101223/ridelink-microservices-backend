package com.ridelink.ride.config;

import com.ridelink.ride.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <p>Stateless (JWT-based), CSRF disabled (REST API, no browser state).
 * Role checks are done via {@code @PreAuthorize} on controller methods
 * AND via ownership checks inside the service layer.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: Swagger UI and OpenAPI
                .requestMatchers(
                        "/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health"
                ).permitAll()
                // All other /api/** routes require authentication; fine-grained
                // role checks happen via @PreAuthorize in controllers.
                .requestMatchers("/api/**").authenticated()
                .anyRequest().denyAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
