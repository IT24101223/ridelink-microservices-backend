package com.ridelink.ride.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request body for POST /api/rides.
 *
 * <p>Coordinates must be within valid WGS-84 ranges.
 * Addresses must not be blank so drivers and passengers see meaningful text.
 */
@Data
public class CreateRideRequest {

    @NotBlank(message = "Pickup address must not be blank")
    private String pickupAddress;

    @NotNull(message = "Pickup latitude is required")
    @DecimalMin(value = "-90.0",  message = "Pickup latitude must be >= -90")
    @DecimalMax(value =  "90.0",  message = "Pickup latitude must be <= 90")
    private Double pickupLat;

    @NotNull(message = "Pickup longitude is required")
    @DecimalMin(value = "-180.0", message = "Pickup longitude must be >= -180")
    @DecimalMax(value =  "180.0", message = "Pickup longitude must be <= 180")
    private Double pickupLng;

    @NotBlank(message = "Destination address must not be blank")
    private String destinationAddress;

    @NotNull(message = "Destination latitude is required")
    @DecimalMin(value = "-90.0",  message = "Destination latitude must be >= -90")
    @DecimalMax(value =  "90.0",  message = "Destination latitude must be <= 90")
    private Double destinationLat;

    @NotNull(message = "Destination longitude is required")
    @DecimalMin(value = "-180.0", message = "Destination longitude must be >= -180")
    @DecimalMax(value =  "180.0", message = "Destination longitude must be <= 180")
    private Double destinationLng;
}
