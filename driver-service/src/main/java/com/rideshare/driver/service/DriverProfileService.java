package com.rideshare.driver.service;

import com.rideshare.driver.dto.CreateDriverProfileRequest;
import com.rideshare.driver.dto.DriverProfileResponse;
import com.rideshare.driver.dto.UpdateDriverProfileRequest;
import com.rideshare.driver.dto.LocationRequest;
import com.rideshare.driver.dto.VehicleRequest;
import com.rideshare.driver.exception.NotFoundException;
import com.rideshare.driver.model.DriverProfile;
import com.rideshare.driver.model.DriverStatus;
import com.rideshare.driver.model.Location;
import com.rideshare.driver.model.Vehicle;
import com.rideshare.driver.model.VehicleType;
import com.rideshare.driver.repository.DriverProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DriverProfileService {

    private final DriverProfileRepository driverProfileRepository;

    public DriverProfileService(DriverProfileRepository driverProfileRepository) {
        this.driverProfileRepository = driverProfileRepository;
    }

    public DriverProfileResponse createDriverProfile(CreateDriverProfileRequest request) {
        if (driverProfileRepository.findByDriverId(request.getDriverId()).isPresent()) {
            throw new IllegalArgumentException("Driver profile already exists for driverId: " + request.getDriverId());
        }

        DriverProfile profile = new DriverProfile();
        Date now = new Date();
        profile.setId(UUID.randomUUID().toString());
        profile.setDriverId(request.getDriverId());
        profile.setLicenseNumber(request.getLicenseNumber());
        profile.setVehicle(toVehicle(request.getVehicle()));
        profile.setServiceArea(request.getServiceArea());
        profile.setCurrentLocation(toLocation(request.getCurrentLocation()));
        profile.setAvailabilityStatus(request.getAvailabilityStatus() != null ? request.getAvailabilityStatus() : DriverStatus.OFFLINE);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        DriverProfile saved = driverProfileRepository.save(profile);
        return toResponse(saved);
    }

    public DriverProfileResponse getDriverProfile(String driverId) {
        DriverProfile profile = driverProfileRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found for driverId: " + driverId));
        return toResponse(profile);
    }

    public DriverProfileResponse updateDriverProfile(String driverId, UpdateDriverProfileRequest request) {
        DriverProfile profile = driverProfileRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found for driverId: " + driverId));

        if (request.getVehicle() != null) {
            profile.setVehicle(toVehicle(request.getVehicle()));
        }
        if (request.getServiceArea() != null && !request.getServiceArea().isBlank()) {
            profile.setServiceArea(request.getServiceArea());
        }
        profile.setUpdatedAt(new Date());

        return toResponse(driverProfileRepository.save(profile));
    }

    public DriverProfileResponse updateAvailability(String driverId, String status) {
        DriverProfile profile = driverProfileRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found for driverId: " + driverId));

        DriverStatus newStatus = parseDriverStatus(status);
        profile.setAvailabilityStatus(newStatus);
        profile.setUpdatedAt(new Date());

        return toResponse(driverProfileRepository.save(profile));
    }

    public DriverProfileResponse updateLocation(String driverId, LocationRequest locationRequest) {
        DriverProfile profile = driverProfileRepository.findByDriverId(driverId)
                .orElseThrow(() -> new NotFoundException("Driver profile not found for driverId: " + driverId));

        profile.setCurrentLocation(toLocation(locationRequest));
        profile.setUpdatedAt(new Date());
        return toResponse(driverProfileRepository.save(profile));
    }

    public List<DriverProfileResponse> getAvailableDrivers(String serviceArea, String vehicleType) {
        VehicleType parsedVehicleType = parseVehicleType(vehicleType);
        List<DriverProfile> profiles = driverProfileRepository
                .findByAvailabilityStatusAndServiceAreaAndVehicle_VehicleType(DriverStatus.AVAILABLE, serviceArea, parsedVehicleType);

        return profiles.stream().map(this::toResponse).toList();
    }

    private DriverStatus parseDriverStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        try {
            return DriverStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid availability status: " + status + ". Allowed values: AVAILABLE, ON_TRIP, OFFLINE");
        }
    }

    private VehicleType parseVehicleType(String vehicleType) {
        if (vehicleType == null || vehicleType.isBlank()) {
            throw new IllegalArgumentException("vehicleType is required");
        }
        try {
            return VehicleType.valueOf(vehicleType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid vehicle type: " + vehicleType + ". Allowed values: CAR, VAN, BIKE, THREE_WHEELER");
        }
    }

    private Vehicle toVehicle(VehicleRequest request) {
        if (request == null) {
            return null;
        }
        return new Vehicle(
                request.getModel(),
                VehicleType.valueOf(request.getVehicleType().trim().toUpperCase(Locale.ROOT)),
                request.getLicensePlate(),
                request.getCapacity(),
                request.getColor()
        );
    }

    private Location toLocation(LocationRequest request) {
        if (request == null) {
            return null;
        }
        return new Location(request.getLatitude(), request.getLongitude(), request.getAddress());
    }

    private DriverProfileResponse toResponse(DriverProfile profile) {
        DriverProfileResponse response = new DriverProfileResponse();
        response.setId(profile.getId());
        response.setDriverId(profile.getDriverId());
        response.setLicenseNumber(profile.getLicenseNumber());
        response.setVehicle(profile.getVehicle() == null ? null : new com.rideshare.driver.dto.VehicleResponse(
                profile.getVehicle().getModel(),
                profile.getVehicle().getVehicleType(),
                profile.getVehicle().getLicensePlate(),
                profile.getVehicle().getCapacity(),
                profile.getVehicle().getColor()
        ));
        response.setServiceArea(profile.getServiceArea());
        response.setCurrentLocation(profile.getCurrentLocation() == null ? null : new com.rideshare.driver.dto.LocationResponse(
                profile.getCurrentLocation().getLatitude(),
                profile.getCurrentLocation().getLongitude(),
                profile.getCurrentLocation().getAddress()
        ));
        response.setAvailabilityStatus(profile.getAvailabilityStatus());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }
}
