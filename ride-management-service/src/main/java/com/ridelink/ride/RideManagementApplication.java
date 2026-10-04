package com.ridelink.ride;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the RideLink Ride Management Service (port 8083).
 *
 * <p>Responsibilities: ride creation, driver assignment, ride lifecycle
 * (REQUESTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED | CANCELLED).
 *
 * <p>Run standalone: {@code --spring.profiles.active=stub} activates in-memory
 * fake clients so downstream services are not required.
 */
@SpringBootApplication
public class RideManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(RideManagementApplication.class, args);
    }
}
