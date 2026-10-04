package com.ridelink.account.service;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;

public interface AccountService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserProfileResponse getProfile(String email);

    UserProfileResponse getProfileById(Long userId);

    UserProfileResponse updateProfile(String email, UpdateProfileRequest request);

    UserProfileResponse updateAccountStatus(Long userId, AccountStatus status);
}
