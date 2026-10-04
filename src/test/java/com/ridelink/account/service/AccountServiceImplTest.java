package com.ridelink.account.service;

import com.ridelink.account.dto.*;
import com.ridelink.account.entity.AccountStatus;
import com.ridelink.account.entity.Role;
import com.ridelink.account.entity.User;
import com.ridelink.account.exception.InvalidCredentialsException;
import com.ridelink.account.exception.ResourceNotFoundException;
import com.ridelink.account.exception.UserAlreadyExistsException;
import com.ridelink.account.repository.UserRepository;
import com.ridelink.account.security.JwtTokenProvider;
import com.ridelink.account.service.impl.AccountServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AccountServiceImpl accountService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setName("John Doe");
        sampleUser.setEmail("john@example.com");
        sampleUser.setPassword("encodedPassword");
        sampleUser.setPhone("+123456789");
        sampleUser.setRole(Role.ROLE_USER);
        sampleUser.setStatus(AccountStatus.ACTIVE);
        sampleUser.setEnabled(true);
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123", "+123456789", Role.ROLE_USER);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.existsByPhone(request.phone())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(tokenProvider.generateTokenForEmail(sampleUser.getEmail())).thenReturn("mockJwtToken");

        AuthResponse response = accountService.register(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.accessToken());
        assertEquals("john@example.com", response.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ThrowsUserAlreadyExistsException_WhenEmailExists() {
        RegisterRequest request = new RegisterRequest("John Doe", "john@example.com", "password123", "+123456789", Role.ROLE_USER);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> accountService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("john@example.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPassword())).thenReturn(true);
        when(tokenProvider.generateTokenForEmail(sampleUser.getEmail())).thenReturn("mockJwtToken");

        AuthResponse response = accountService.login(request);

        assertNotNull(response);
        assertEquals("mockJwtToken", response.accessToken());
        assertEquals(1L, response.userId());
    }

    @Test
    void login_ThrowsInvalidCredentialsException_WhenPasswordInvalid() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongPassword");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches(request.password(), sampleUser.getPassword())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> accountService.login(request));
    }

    @Test
    void getProfile_Success() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = accountService.getProfile("john@example.com");

        assertNotNull(response);
        assertEquals("John Doe", response.name());
        assertEquals(AccountStatus.ACTIVE, response.status());
    }

    @Test
    void getProfileById_ThrowsResourceNotFoundException_WhenNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> accountService.getProfileById(99L));
    }

    @Test
    void updateAccountStatus_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserProfileResponse response = accountService.updateAccountStatus(1L, AccountStatus.SUSPENDED);

        assertNotNull(response);
        verify(userRepository).save(sampleUser);
        assertFalse(sampleUser.isEnabled());
    }
}
