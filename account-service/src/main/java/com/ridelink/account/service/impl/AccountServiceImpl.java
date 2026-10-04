package com.ridelink.account.service.impl;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.exception.UserAlreadyExistsException;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtTokenProvider;
import com.ridelink.account.service.AccountService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AccountServiceImpl(UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User already exists with email: " + request.email());
        }

        if (request.phone() != null && !request.phone().isBlank() && userRepository.existsByPhone(request.phone())) {
            throw new UserAlreadyExistsException("User already exists with phone number: " + request.phone());
        }

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setRole(request.role() != null ? request.role() : Role.ROLE_USER);
        user.setStatus(AccountStatus.ACTIVE);

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateTokenForEmail(savedUser.getEmail());

        return new AuthResponse(token, savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled() || user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.INACTIVE) {
            throw new InvalidCredentialsException("User account is disabled or suspended");
        }

        String token = tokenProvider.generateTokenForEmail(user.getEmail());

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        user.setName(request.name());
        if (request.phone() != null && !request.phone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.phone())) {
                throw new UserAlreadyExistsException("Phone number already in use by another account");
            }
            user.setPhone(request.phone());
        }

        User updatedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(updatedUser);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAccountStatus(Long userId, AccountStatus status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        user.setStatus(status);
        if (status == AccountStatus.SUSPENDED || status == AccountStatus.INACTIVE) {
            user.setEnabled(false);
        } else if (status == AccountStatus.ACTIVE) {
            user.setEnabled(true);
        }

        User updatedUser = userRepository.save(user);
        return UserProfileResponse.fromEntity(updatedUser);
    }
}
