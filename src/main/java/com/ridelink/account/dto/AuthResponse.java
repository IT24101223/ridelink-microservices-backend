package com.ridelink.account.dto;

import com.ridelink.account.entity.Role;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String name,
        String email,
        Role role
) {
    public AuthResponse(String accessToken, Long userId, String name, String email, Role role) {
        this(accessToken, "Bearer", userId, name, email, role);
    }
}
