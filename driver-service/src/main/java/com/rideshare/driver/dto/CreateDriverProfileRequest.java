package com.rideshare.driver.dto;

import com.rideshare.driver.model.DriverStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateDriverProfileRequest {
    @NotBlank(message = "driverId is required")
    private String driverId;

    @NotBlank(message = "licenseNumber is required")
    private String licenseNumber;

    @NotNull(message = "vehicle is required")
    @Valid
    private VehicleRequest vehicle;

    @NotBlank(message = "serviceArea is required")
    private String serviceArea;

    @Valid
    private LocationRequest currentLocation;

    private DriverStatus availabilityStatus = DriverStatus.OFFLINE;

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public VehicleRequest getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleRequest vehicle) {
        this.vehicle = vehicle;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public LocationRequest getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationRequest currentLocation) {
        this.currentLocation = currentLocation;
    }

    public DriverStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DriverStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }
}
