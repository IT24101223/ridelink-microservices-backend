package com.ridelink.ride.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for PATCH /api/rides/{id}/cancel. */
@Data
public class CancelRideRequest {

    @NotBlank(message = "A cancellation reason must be provided")
    private String reason;
}
