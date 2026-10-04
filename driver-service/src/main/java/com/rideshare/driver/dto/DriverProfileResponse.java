package com.rideshare.driver.dto;

import com.rideshare.driver.model.DriverStatus;

import java.util.Date;

public class DriverProfileResponse {
    private String id;
    private String driverId;
    private String licenseNumber;
    private VehicleResponse vehicle;
    private String serviceArea;
    private LocationResponse currentLocation;
    private DriverStatus availabilityStatus;
    private Date createdAt;
    private Date updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public VehicleResponse getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleResponse vehicle) {
        this.vehicle = vehicle;
    }

    public String getServiceArea() {
        return serviceArea;
    }

    public void setServiceArea(String serviceArea) {
        this.serviceArea = serviceArea;
    }

    public LocationResponse getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(LocationResponse currentLocation) {
        this.currentLocation = currentLocation;
    }

    public DriverStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(DriverStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
