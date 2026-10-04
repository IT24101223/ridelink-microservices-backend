package com.ridelink.account.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateProfileRequest(
        @NotBlank(message = "Name cannot be empty")
        String name,

        String phone
) {}
