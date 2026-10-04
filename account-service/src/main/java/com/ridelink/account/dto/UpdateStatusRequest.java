package com.ridelink.account.dto;

import com.ridelink.account.entity.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(
        @NotNull(message = "Account status cannot be null")
        AccountStatus status
) {}
