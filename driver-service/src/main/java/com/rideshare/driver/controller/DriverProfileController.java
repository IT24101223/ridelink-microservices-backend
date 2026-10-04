package com.rideshare.driver.controller;

import com.rideshare.driver.dto.CreateDriverProfileRequest;
import com.rideshare.driver.dto.DriverProfileResponse;
import com.rideshare.driver.dto.LocationRequest;
import com.rideshare.driver.dto.UpdateDriverProfileRequest;
import com.rideshare.driver.service.DriverProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver Profile API", description = "Operations for driver onboarding and availability management")
public class DriverProfileController {

    private final DriverProfileService driverProfileService;

    public DriverProfileController(DriverProfileService driverProfileService) {
        this.driverProfileService = driverProfileService;
    }

    @Operation(summary = "Create operational driver profile")
    @PostMapping
    public ResponseEntity<DriverProfileResponse> createDriverProfile(@Valid @RequestBody CreateDriverProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(driverProfileService.createDriverProfile(request));
    }

    @Operation(summary = "Get driver profile by account driverId")
    @GetMapping("/{driverId}")
    public ResponseEntity<DriverProfileResponse> getDriverProfile(@PathVariable String driverId) {
        return ResponseEntity.ok(driverProfileService.getDriverProfile(driverId));
    }

    @Operation(summary = "Update vehicle info and service area")
    @PutMapping("/{driverId}")
    public ResponseEntity<DriverProfileResponse> updateDriverProfile(
            @PathVariable String driverId,
            @Valid @RequestBody UpdateDriverProfileRequest request) {
        return ResponseEntity.ok(driverProfileService.updateDriverProfile(driverId, request));
    }

    @Operation(summary = "Update driver availability status")
    @PatchMapping("/{driverId}/availability")
    public ResponseEntity<DriverProfileResponse> updateAvailability(
            @PathVariable String driverId,
            @RequestParam String status) {
        return ResponseEntity.ok(driverProfileService.updateAvailability(driverId, status));
    }

    @Operation(summary = "Update simulated GPS location")
    @PatchMapping("/{driverId}/location")
    public ResponseEntity<DriverProfileResponse> updateLocation(
            @PathVariable String driverId,
            @Valid @RequestBody LocationRequest request) {
        return ResponseEntity.ok(driverProfileService.updateLocation(driverId, request));
    }

    @Operation(summary = "Get eligible available drivers for a service area and vehicle type")
    @GetMapping("/available")
    public ResponseEntity<List<DriverProfileResponse>> getAvailableDrivers(
            @RequestParam String serviceArea,
            @RequestParam String vehicleType) {
        return ResponseEntity.ok(driverProfileService.getAvailableDrivers(serviceArea, vehicleType));
    }
}
