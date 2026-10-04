package com.ridelink.account.controller;

import com.ridelink.account.dto.UpdateProfileRequest;
import com.ridelink.account.dto.UpdateStatusRequest;
import com.ridelink.account.dto.UserProfileResponse;
import com.ridelink.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account Management", description = "Endpoints for Profile Viewing, Updates, Status Management, and Interservice Lookup")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Get current user profile", description = "Retrieves profile details of the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Profile retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        UserProfileResponse profile = accountService.getProfile(userDetails.getUsername());
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update current user profile", description = "Updates profile details (name, phone) of the authenticated user.")
    @ApiResponse(responseCode = "200", description = "Profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                             @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse profile = accountService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Get user profile by User ID", description = "Endpoint used by other microservices to verify user/driver existence and details.")
    @ApiResponse(responseCode = "200", description = "User profile found")
    @ApiResponse(responseCode = "404", description = "User not found")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileResponse> getProfileById(@PathVariable Long id) {
        UserProfileResponse profile = accountService.getProfileById(id);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "Update account status", description = "Updates status of user account (ACTIVE, SUSPENDED, PENDING_VERIFICATION). Requires ADMIN role or internal call.")
    @ApiResponse(responseCode = "200", description = "Status updated successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role")
    @ApiResponse(responseCode = "404", description = "User not found")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<UserProfileResponse> updateAccountStatus(@PathVariable Long id,
                                                                   @Valid @RequestBody UpdateStatusRequest request) {
        UserProfileResponse profile = accountService.updateAccountStatus(id, request.status());
        return ResponseEntity.ok(profile);
    }
}
